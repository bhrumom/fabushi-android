package com.ombhrum.fabushi

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ombhrum.fabushi.core.MahayanaHost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID

data class MobileBotSummaryAndroid(
    val id: String,
    val name: String,
    val description: String = "",
)

data class MobileBotUiState(
    val bots: List<MobileBotSummaryAndroid> = emptyList(),
    val activeBot: MobileBotSummaryAndroid? = null,
    val draft: String = "",
    val messages: List<MobileChatMessage> = emptyList(),
    val busy: Boolean = false,
    val operationId: String? = null,
    val error: String? = null,
    val creating: Boolean = false,
)

class MobileBotViewModel(application: Application) : AndroidViewModel(application) {
    private val host = MahayanaHost(application)
    private val mutableState = MutableStateFlow(MobileBotUiState())
    val state: StateFlow<MobileBotUiState> = mutableState.asStateFlow()

    fun refreshBots() {
        if (mutableState.value.busy) return
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val requestId = "android-mobile-bot-list-${UUID.randomUUID()}"
                    host.request(
                        "feature.execute",
                        JSONObject().put(
                            "command",
                            JSONObject().put("type", "bot.list").put("requestId", requestId),
                        ),
                    )
                    repeat(48) {
                        val event = host.request("feature.receive", JSONObject().put("timeoutMs", 120))
                        if (event.optString("type") == "bot.listed") {
                            val rows = event.optJSONArray("bots")
                            val bots = buildList {
                                if (rows != null) for (index in 0 until rows.length()) {
                                    val row = rows.optJSONObject(index) ?: continue
                                    val id = row.optString("id")
                                    if (id.isBlank() || id == "mahayana-assistant") continue
                                    add(
                                        MobileBotSummaryAndroid(
                                            id = id,
                                            name = row.optString("name").ifBlank { row.optString("displayName").ifBlank { id } },
                                            description = row.optString("description"),
                                        ),
                                    )
                                }
                            }
                            return@withContext bots
                        }
                    }
                    emptyList()
                }
            }.onSuccess { bots -> mutableState.value = mutableState.value.copy(bots = bots, error = null) }
        }
    }

    fun createBot(name: String, description: String, onCreated: (() -> Unit)? = null) {
        val cleanName = name.replace(Regex("\\s+"), " ").trim().take(72)
        if (cleanName.isBlank() || mutableState.value.creating) return
        mutableState.value = mutableState.value.copy(creating = true, error = null)
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val requestId = "android-mobile-bot-create-${UUID.randomUUID()}"
                    host.request(
                        "feature.execute",
                        JSONObject().put(
                            "command",
                            JSONObject()
                                .put("type", "bot.create")
                                .put("requestId", requestId)
                                .put("name", cleanName)
                                .put("description", description.trim().take(240)),
                        ),
                    )
                }
            }.onSuccess {
                mutableState.value = mutableState.value.copy(creating = false)
                refreshBots()
                onCreated?.invoke()
            }.onFailure { error ->
                mutableState.value = mutableState.value.copy(creating = false, error = error.message ?: "Bot creation failed")
            }
        }
    }

    fun openBot(bot: MobileBotSummaryAndroid) {
        mutableState.value = mutableState.value.copy(activeBot = bot, draft = "", messages = emptyList(), error = null)
    }

    fun closeBot() {
        if (mutableState.value.busy) return
        mutableState.value = mutableState.value.copy(activeBot = null, draft = "", messages = emptyList(), error = null)
    }

    fun setDraft(value: String) {
        mutableState.value = mutableState.value.copy(draft = value)
    }

    fun send() {
        val snapshot = mutableState.value
        val bot = snapshot.activeBot ?: return
        val text = snapshot.draft.trim()
        if (text.isBlank() || snapshot.busy) return
        val requestId = "android-mobile-bot-chat-${UUID.randomUUID()}"
        mutableState.value = snapshot.copy(
            draft = "",
            busy = true,
            error = null,
            messages = snapshot.messages + MobileChatMessage(requestId, MobileChatRole.USER, text),
        )
        viewModelScope.launch {
            runCatching {
                val operationId = withContext(Dispatchers.IO) {
                    val accepted = host.request(
                        "feature.execute",
                        JSONObject().put(
                            "command",
                            JSONObject()
                                .put("type", "chat.send")
                                .put("requestId", requestId)
                                .put("text", text)
                                .put("agentId", bot.id)
                                .put("mode", "agent"),
                        ),
                    )
                    accepted.optString("operationId").ifBlank { requestId }
                }
                mutableState.value = mutableState.value.copy(
                    operationId = operationId,
                    messages = mutableState.value.messages + MobileChatMessage(
                        id = "thinking:$operationId",
                        role = MobileChatRole.ASSISTANT,
                        text = "",
                        kind = MobileChatEntryKind.THINKING,
                        operationId = operationId,
                        actionTitle = "Thinking",
                        actionStatus = "running",
                    ),
                )
                pump(operationId)
            }.onFailure { error ->
                mutableState.value = mutableState.value.copy(busy = false, operationId = null, error = error.message ?: "Bot run failed")
            }
        }
    }

    fun stop() {
        val operationId = mutableState.value.operationId ?: return
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { host.request("feature.interrupt", JSONObject().put("operationId", operationId)) } }
        }
    }

    private suspend fun pump(operationId: String) {
        repeat(1800) {
            if (!mutableState.value.busy || mutableState.value.operationId != operationId) return
            val event = runCatching {
                withContext(Dispatchers.IO) { host.request("feature.receive", JSONObject().put("timeoutMs", 250)) }
            }.getOrElse { error ->
                mutableState.value = mutableState.value.copy(busy = false, operationId = null, error = error.message ?: "Message stream interrupted")
                return
            }
            val type = event.optString("type")
            val eventOperationId = event.optString("operationId").ifBlank { operationId }
            if (type in setOf("chat.message", "chat.delta", "agent.step", "operation.started", "operation.completed", "operation.interrupted", "operation.failed", "model.routed") && eventOperationId != operationId) {
                delay(60)
                return@repeat
            }
            when (type) {
                "chat.message" -> if (event.optString("role") != "user") {
                    removeThinking(operationId)
                    upsertAssistant(operationId, event.optString("text"), append = false)
                }
                "chat.delta" -> {
                    removeThinking(operationId)
                    upsertAssistant(operationId, event.optString("delta"), append = true)
                }
                "agent.step" -> {
                    val id = "action:$operationId:${event.optString("stepId").ifBlank { UUID.randomUUID().toString() }}"
                    upsert(
                        MobileChatMessage(
                            id = id,
                            role = MobileChatRole.ASSISTANT,
                            text = "",
                            kind = MobileChatEntryKind.ACTION,
                            operationId = operationId,
                            actionTitle = event.optString("title").ifBlank { "Working" },
                            actionDetail = event.optString("detail"),
                            actionStatus = event.optString("status").ifBlank { "completed" },
                        ),
                    )
                }
                "model.routed" -> {
                    val detail = listOf(event.optString("provider"), event.optString("model")).filter { it.isNotBlank() }.joinToString(" · ")
                    upsert(MobileChatMessage("action:$operationId:model", MobileChatRole.ASSISTANT, "", MobileChatEntryKind.ACTION, operationId, "Model", detail, "completed"))
                }
                "operation.completed", "operation.interrupted" -> {
                    removeThinking(operationId)
                    mutableState.value = mutableState.value.copy(busy = false, operationId = null)
                    return
                }
                "operation.failed" -> {
                    removeThinking(operationId)
                    mutableState.value = mutableState.value.copy(busy = false, operationId = null, error = event.optString("message").ifBlank { "Bot run failed" })
                    return
                }
            }
            delay(60)
        }
    }

    private fun removeThinking(operationId: String) {
        mutableState.value = mutableState.value.copy(messages = mutableState.value.messages.filterNot { it.kind == MobileChatEntryKind.THINKING && it.operationId == operationId })
    }

    private fun upsertAssistant(operationId: String, text: String, append: Boolean) {
        if (text.isBlank()) return
        val rows = mutableState.value.messages.toMutableList()
        val index = rows.indexOfLast { it.role == MobileChatRole.ASSISTANT && it.kind == MobileChatEntryKind.MESSAGE && it.operationId == operationId }
        if (index >= 0) {
            val current = rows[index]
            rows[index] = current.copy(text = if (append) current.text + text else text)
        } else {
            rows += MobileChatMessage("assistant:$operationId", MobileChatRole.ASSISTANT, text, operationId = operationId)
        }
        mutableState.value = mutableState.value.copy(messages = rows)
    }

    private fun upsert(message: MobileChatMessage) {
        val rows = mutableState.value.messages.toMutableList()
        val index = rows.indexOfFirst { it.id == message.id }
        if (index >= 0) rows[index] = message else rows += message
        mutableState.value = mutableState.value.copy(messages = rows)
    }

    override fun onCleared() {
        host.close()
        super.onCleared()
    }
}
