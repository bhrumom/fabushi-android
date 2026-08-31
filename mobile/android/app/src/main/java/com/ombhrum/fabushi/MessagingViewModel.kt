package com.ombhrum.fabushi

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ombhrum.fabushi.core.MahayanaHost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class ConversationKind(val wire: String, val label: String) {
    DIRECT("direct", "私聊"), GROUP("group", "群组"), CHANNEL("channel", "频道"),
    SAVED_MESSAGES("savedMessages", "收藏"), SECRET("secret", "加密聊天"),
}

data class ConversationSummary(
    val id: String,
    val title: String,
    val preview: String,
    val time: String,
    val badge: String,
    val kind: ConversationKind,
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val isArchived: Boolean = false,
    val lastMessageId: String? = null,
)

data class MessagingContact(
    val id: String,
    val displayName: String,
    val username: String?,
    val kind: String,
)

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val text: String,
    val outgoing: Boolean,
    val time: String,
)

data class MessagingUiState(
    val loading: Boolean = false,
    val conversations: List<ConversationSummary> = emptyList(),
    val contacts: List<MessagingContact> = emptyList(),
    val messagesByConversation: Map<String, List<ChatMessage>> = emptyMap(),
    val error: String? = null,
)

internal class MessagingViewModel(application: Application) : AndroidViewModel(application) {
    private val host = MahayanaHost(application)
    private val mutableState = MutableStateFlow(MessagingUiState())
    val state: StateFlow<MessagingUiState> = mutableState.asStateFlow()
    private var actorId = ""
    private var displayName = "当前用户"
    private val deviceId = "android:native"
    private val sessionId = "account-session:android-native"

    init { refresh() }

    fun refresh() {
        mutableState.value = mutableState.value.copy(loading = true)
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { ensureIdentity(); execute(JSONObject().put("type", "sync").put("cursor", JSONObject.NULL).put("limit", 1000)) } }
                .onSuccess { mutableState.value = mutableState.value.copy(loading = false, error = null) }
                .onFailure { mutableState.value = mutableState.value.copy(loading = false, error = it.message) }
        }
    }

    fun createDirect(contact: MessagingContact) {
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) {
                ensureIdentity()
                val now = System.currentTimeMillis()
                val id = "direct:${UUID.randomUUID()}"
                val participants = JSONArray()
                    .put(JSONObject().put("actorId", actorId).put("role", "owner").put("joinedAtMs", now).put("mutedUntilMs", JSONObject.NULL))
                    .put(JSONObject().put("actorId", contact.id).put("role", "member").put("joinedAtMs", now).put("mutedUntilMs", JSONObject.NULL))
                val conversation = baseConversation(id, ConversationKind.DIRECT, contact.displayName, "", participants, now)
                execute(JSONObject().put("type", "createConversation").put("conversation", conversation))
            }}.onFailure { mutableState.value = mutableState.value.copy(error = it.message) }
        }
    }

    fun createConversation(kind: ConversationKind, title: String, description: String = "") {
        if (kind != ConversationKind.GROUP && kind != ConversationKind.CHANNEL) return
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) {
                ensureIdentity()
                val now = System.currentTimeMillis()
                val id = "${kind.wire}:${UUID.randomUUID()}"
                val participants = JSONArray().put(JSONObject().put("actorId", actorId).put("role", "owner").put("joinedAtMs", now).put("mutedUntilMs", JSONObject.NULL))
                val conversation = baseConversation(id, kind, title.trim(), description.trim(), participants, now)
                execute(JSONObject().put("type", "createConversation").put("conversation", conversation))
            }}.onFailure { mutableState.value = mutableState.value.copy(error = it.message) }
        }
    }

    fun sendText(conversationId: String, text: String) {
        val value = text.trim(); if (value.isEmpty()) return
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) {
                ensureIdentity()
                execute(JSONObject().put("type", "sendMessage").put("conversationId", conversationId)
                    .put("clientMessageId", "android:${UUID.randomUUID()}")
                    .put("content", JSONObject().put("type", "text").put("data", JSONObject().put("text", JSONObject().put("text", value).put("entities", JSONArray()))))
                    .put("replyToMessageId", JSONObject.NULL).put("threadRootMessageId", JSONObject.NULL).put("scheduledAtMs", JSONObject.NULL)
                    .put("silent", false).put("protectedContent", false))
            }}.onFailure { mutableState.value = mutableState.value.copy(error = it.message) }
        }
    }

    fun setPinned(conversation: ConversationSummary, pinned: Boolean) = executeAsync(JSONObject().put("type", "pinConversation").put("conversationId", conversation.id).put("pinned", pinned))
    fun setArchived(conversation: ConversationSummary, archived: Boolean) = executeAsync(JSONObject().put("type", "archiveConversation").put("conversationId", conversation.id).put("archived", archived))
    fun setMuted(conversation: ConversationSummary, muted: Boolean) = executeAsync(JSONObject().put("type", "setConversationNotifications").put("conversationId", conversation.id).put("settings", JSONObject().put("mutedUntilMs", if (muted) Long.MAX_VALUE / 4 else JSONObject.NULL).put("sound", JSONObject.NULL).put("showPreview", true).put("notifyMentions", true)))
    fun markRead(conversation: ConversationSummary) { conversation.lastMessageId?.let { executeAsync(JSONObject().put("type", "markRead").put("conversationId", conversation.id).put("messageId", it)) } }

    private fun executeAsync(command: JSONObject) {
        viewModelScope.launch { runCatching { withContext(Dispatchers.IO) { ensureIdentity(); execute(command) } }.onFailure { mutableState.value = mutableState.value.copy(error = it.message) } }
    }

    private fun ensureIdentity() {
        if (actorId.isNotEmpty()) return
        val auth = host.request("feature.auth.status")
        auth.optJSONObject("user")?.let { displayName = it.optString("nickname").ifBlank { it.optString("username").ifBlank { displayName } } }
        val access = host.request("feature.messaging.access.issue", JSONObject().put("deviceId", deviceId).put("sessionId", sessionId)
            .put("scopes", JSONArray(listOf("messaging", "calls", "blobsRead", "blobsWrite", "payments", "miniApps"))))
        actorId = access.optString("actorId")
        check(actorId.isNotBlank()) { "Messaging identity is unavailable" }
        execute(JSONObject().put("type", "upsertProfile").put("actor", JSONObject()
            .put("id", actorId).put("kind", "human").put("displayName", displayName).put("username", JSONObject.NULL).put("avatarUrl", JSONObject.NULL).put("bio", JSONObject.NULL)
            .put("capabilities", JSONArray(listOf("messages", "groups", "channels", "calls", "payments", "miniApps")))
            .put("presence", JSONObject().put("status", "online").put("lastSeenAtMs", System.currentTimeMillis()).put("statusText", JSONObject.NULL)).put("verified", false)))
    }

    private fun execute(command: JSONObject) {
        val requestId = "android-messaging-${UUID.randomUUID()}"
        val envelope = JSONObject().put("protocolVersion", 2)
            .put("context", JSONObject().put("requestId", requestId).put("deviceId", deviceId).put("actorId", actorId).put("sessionId", sessionId).put("sentAtMs", System.currentTimeMillis()))
            .put("command", command)
        val result = host.request("feature.messaging.execute", JSONObject().put("requestId", requestId).put("envelope", envelope))
        apply(result.optJSONArray("envelopes") ?: JSONArray())
    }

    private fun apply(envelopes: JSONArray) {
        var conversations = mutableState.value.conversations.toMutableList()
        var contacts = mutableState.value.contacts
        val messageMap = mutableState.value.messagesByConversation.mapValues { it.value.toMutableList() }.toMutableMap()
        for (i in 0 until envelopes.length()) {
            val event = envelopes.optJSONObject(i)?.optJSONObject("event") ?: continue
            when (event.optString("type")) {
                "syncBatch" -> {
                    conversations = parseConversations(event.optJSONArray("conversations") ?: JSONArray()).toMutableList()
                    contacts = parseContacts(event.optJSONArray("actors") ?: JSONArray())
                    messageMap.clear()
                    parseMessages(event.optJSONArray("messages") ?: JSONArray()).forEach { messageMap.getOrPut(it.conversationId) { mutableListOf() }.add(it) }
                }
                "conversationChanged" -> parseConversation(event.optJSONObject("conversation"))?.let { item ->
                    val index = conversations.indexOfFirst { it.id == item.id }; if (index >= 0) conversations[index] = item else conversations.add(item)
                }
                "messageAdded", "messageChanged" -> parseMessage(event.optJSONObject("message"))?.let { item ->
                    val list = messageMap.getOrPut(item.conversationId) { mutableListOf() }; val index = list.indexOfFirst { it.id == item.id }; if (index >= 0) list[index] = item else list.add(item)
                }
                "messagesDeleted" -> {
                    val conversationId = event.optString("conversationId"); val ids = event.optJSONArray("messageIds")?.toStringSet().orEmpty(); messageMap[conversationId]?.removeAll { it.id in ids }
                }
            }
        }
        conversations = conversations.map { conversation ->
            val last = messageMap[conversation.id]?.lastOrNull(); if (last == null) conversation else conversation.copy(preview = last.text, time = last.time)
        }.toMutableList()
        mutableState.value = mutableState.value.copy(conversations = conversations, contacts = contacts, messagesByConversation = messageMap.mapValues { it.value.toList() }, error = null)
    }

    private fun parseContacts(array: JSONArray) = buildList {
        for (i in 0 until array.length()) {
            val raw = array.optJSONObject(i) ?: continue
            val id = raw.optString("id"); if (id.isBlank() || id == actorId) continue
            val name = raw.optString("displayName"); if (name.isBlank()) continue
            add(MessagingContact(id, name, raw.optString("username").takeIf { it.isNotBlank() }, raw.optString("kind", "human")))
        }
    }
    private fun parseConversations(array: JSONArray) = buildList { for (i in 0 until array.length()) parseConversation(array.optJSONObject(i))?.let(::add) }
    private fun parseMessages(array: JSONArray) = buildList { for (i in 0 until array.length()) parseMessage(array.optJSONObject(i))?.let(::add) }

    private fun baseConversation(id: String, kind: ConversationKind, title: String, description: String, participants: JSONArray, now: Long): JSONObject =
        JSONObject().put("id", id).put("kind", kind.wire).put("title", title)
            .put("description", description.let { if (it.isBlank()) JSONObject.NULL else it }).put("avatarUrl", JSONObject.NULL).put("participants", participants)
            .put("ownerId", actorId).put("lastMessageId", JSONObject.NULL).put("lastReadMessageId", JSONObject.NULL)
            .put("unreadCount", 0).put("mentionCount", 0).put("pinnedMessageIds", JSONArray())
            .put("notificationSettings", JSONObject().put("mutedUntilMs", JSONObject.NULL).put("sound", JSONObject.NULL).put("showPreview", true).put("notifyMentions", true))
            .put("permissions", JSONObject().put("canSendMessages", true).put("canSendMedia", true).put("canSendPolls", true).put("canAddMembers", true).put("canPinMessages", true).put("canManageTopics", true).put("canManageCalls", true))
            .put("historyVisibility", "allMembers").put("topics", JSONArray()).put("folderIds", JSONArray())
            .put("archived", false).put("pinned", false).put("markedUnread", false).put("createdAtMs", now).put("updatedAtMs", now)

    private fun parseConversation(raw: JSONObject?): ConversationSummary? {
        raw ?: return null; val id = raw.optString("id"); val title = raw.optString("title"); if (id.isBlank() || title.isBlank()) return null
        val kind = ConversationKind.entries.firstOrNull { it.wire == raw.optString("kind") } ?: ConversationKind.DIRECT
        val mutedUntil = raw.optJSONObject("notificationSettings")?.optLong("mutedUntilMs", 0L) ?: 0L
        return ConversationSummary(id, title, raw.optString("description"), timeLabel(raw.optLong("updatedAtMs")), title.take(1).uppercase().ifBlank { "✦" }, kind,
            raw.optInt("unreadCount"), raw.optBoolean("pinned"), mutedUntil > System.currentTimeMillis(), raw.optBoolean("archived"), raw.optString("lastMessageId").takeIf { it.isNotBlank() })
    }

    private fun parseMessage(raw: JSONObject?): ChatMessage? {
        raw ?: return null; val id = raw.optString("id"); val conversationId = raw.optString("conversationId"); val senderId = raw.optString("senderId"); if (id.isBlank() || conversationId.isBlank()) return null
        val content = raw.optJSONObject("content") ?: JSONObject(); val text = when (content.optString("type")) {
            "text" -> content.optJSONObject("data")?.optJSONObject("text")?.optString("text").orEmpty()
            "photo" -> "🖼 图片"; "video" -> "🎬 视频"; "document" -> "📎 文件"; "location" -> "📍 位置"; "contact" -> "👤 联系人"; "invoice" -> "🧾 账单"; "miniApp" -> "▣ Mini App"; else -> "消息"
        }
        return ChatMessage(id, conversationId, text, senderId == actorId, timeLabel(raw.optLong("createdAtMs")))
    }

    private fun timeLabel(ms: Long): String = if (ms <= 0) "" else java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(java.util.Date(ms))

    override fun onCleared() { host.close(); super.onCleared() }
}

private fun JSONArray.toStringSet(): Set<String> = buildSet { for (i in 0 until length()) optString(i).takeIf { it.isNotBlank() }?.let(::add) }
