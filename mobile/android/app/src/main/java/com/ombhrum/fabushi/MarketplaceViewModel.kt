package com.ombhrum.fabushi

import android.app.Application
import android.net.Uri
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

data class MiniAppToolContract(
    val name: String,
    val description: String,
    val approval: String,
)

data class MarketplacePlugin(
    val pluginId: String,
    val displayName: String,
    val description: String,
    val latestVersion: String?,
    val tools: List<MiniAppToolContract> = emptyList(),
)

data class PermissionRequest(
    val pluginId: String,
    val runtime: String,
    val permissions: List<String>,
)

data class MarketplaceUiState(
    val loading: Boolean = false,
    val installingPluginId: String? = null,
    val query: String = "",
    val message: String = "Mahayana Rust Host 已启动",
    val plugins: List<MarketplacePlugin> = emptyList(),
    val permissionRequest: PermissionRequest? = null,
)

class MarketplaceViewModel(application: Application) : AndroidViewModel(application) {
    private val host = MahayanaHost(application)
    private val mutableState = MutableStateFlow(MarketplaceUiState())
    val state: StateFlow<MarketplaceUiState> = mutableState.asStateFlow()

    init {
        refresh()
    }

    fun setQuery(value: String) {
        mutableState.value = mutableState.value.copy(query = value)
    }

    fun handleDeepLink(uri: Uri) {
        if (uri.scheme != "fabushi" || uri.userInfo != null || uri.port != -1) return
        val hostName = uri.host?.lowercase().orEmpty()
        val path = uri.pathSegments.filter { it.isNotBlank() }
        when (hostName) {
            "auth" -> {
                if (path.firstOrNull() != "complete" || path.size != 1) return
                val allowedNames = setOf("attemptId", "status")
                if (uri.queryParameterNames.any { it !in allowedNames }) return
                val attemptIds = uri.getQueryParameters("attemptId")
                val statuses = uri.getQueryParameters("status")
                if (attemptIds.size != 1 || statuses.size > 1) return
                val attemptId = attemptIds.single()
                val status = statuses.singleOrNull()?.lowercase() ?: "completed"
                if (!Regex("^[A-Za-z0-9_-]{8,96}$").matches(attemptId)) return
                if (status !in setOf("completed", "cancelled", "failed")) return
                mutableState.value = mutableState.value.copy(
                    message = when (status) {
                        "completed" -> "登录授权已完成，正在同步账号状态"
                        "cancelled" -> "登录授权已取消"
                        else -> "登录授权失败"
                    },
                )
                if (status == "completed") completeBrowserLogin(attemptId)
            }
            "agent" -> {
                val agentId = path.firstOrNull().orEmpty().take(200)
                if (agentId.isNotBlank()) {
                    mutableState.value = mutableState.value.copy(message = "已接收智能体链接：$agentId")
                }
            }
            "settings", "feedback", "about", "widgets", "onboarding" -> {
                mutableState.value = mutableState.value.copy(message = "已接收应用链接：$hostName")
            }
        }
    }

    private fun completeBrowserLogin(attemptId: String) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    host.request(
                        "feature.auth.browserPoll",
                        JSONObject().put("attemptId", attemptId),
                    )
                }
            }.onSuccess { result ->
                when (result.optString("status")) {
                    "completed" -> {
                        mutableState.value = mutableState.value.copy(message = "登录成功，账号状态已同步")
                        refresh()
                    }
                    "cancelled" -> mutableState.value = mutableState.value.copy(message = "登录授权已取消")
                    "failed" -> mutableState.value = mutableState.value.copy(message = "登录授权失败")
                    else -> mutableState.value = mutableState.value.copy(message = "登录结果尚未可用，请返回浏览器重试")
                }
            }.onFailure { error ->
                mutableState.value = mutableState.value.copy(
                    message = "登录状态同步失败：${error.message ?: error::class.java.simpleName}",
                )
            }
        }
    }

    fun refresh() {
        val query = mutableState.value.query.trim()
        mutableState.value = mutableState.value.copy(loading = true)
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    host.request(
                        "feature.marketplace.browse",
                        JSONObject().put("query", query.ifBlank { JSONObject.NULL }).put("platform", "android"),
                    )
                }
            }.onSuccess { result ->
                val plugins = result.optJSONArray("plugins")
                val items = buildList {
                    if (plugins != null) {
                        for (index in 0 until plugins.length()) {
                            val item = plugins.optJSONObject(index) ?: continue
                            val pluginId = item.optString("pluginId")
                            if (pluginId.isBlank()) continue
                            val commands = item.optJSONObject("source")?.optJSONArray("commands")
                                ?: item.optJSONArray("commands")
                            add(
                                MarketplacePlugin(
                                    pluginId = pluginId,
                                    displayName = item.optString("displayName", pluginId),
                                    description = item.optString("description", "无描述"),
                                    latestVersion = item.optString("latestVersion").takeIf(String::isNotBlank),
                                    tools = commands.toToolContracts(),
                                ),
                            )
                        }
                    }
                }
                mutableState.value = mutableState.value.copy(
                    loading = false,
                    message = "原生 Android · Rust Host 已连接",
                    plugins = items,
                )
            }.onFailure { error ->
                mutableState.value = mutableState.value.copy(
                    loading = false,
                    message = "市场加载失败：${error.message ?: error::class.java.simpleName}",
                )
            }
        }
    }

    fun install(plugin: MarketplacePlugin) {
        val version = plugin.latestVersion
        if (version.isNullOrBlank()) {
            mutableState.value = mutableState.value.copy(message = "${plugin.pluginId} 没有可安装版本")
            return
        }
        mutableState.value = mutableState.value.copy(
            installingPluginId = plugin.pluginId,
            message = "正在安装 ${plugin.pluginId}@$version…",
        )
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val metadata = host.request(
                        "feature.marketplace.release",
                        JSONObject().put("pluginId", plugin.pluginId).put("version", version),
                    )
                    val release = metadata.optJSONObject("releaseManifest")
                        ?: error("marketplace release has no releaseManifest")
                    host.request(
                        "feature.plugin.install",
                        JSONObject().put("release", release).put("platform", "android"),
                    )
                }
            }.onSuccess { installed ->
                val pluginId = installed.optString("pluginId", plugin.pluginId)
                val runtime = installed.optString("runtime")
                val permissions = installed.optJSONArray("requestedPermissions").toStringList()
                if (permissions.isEmpty()) {
                    startPortableRuntime(pluginId, runtime)
                } else {
                    mutableState.value = mutableState.value.copy(
                        installingPluginId = null,
                        permissionRequest = PermissionRequest(pluginId, runtime, permissions),
                        message = "$pluginId 请求 ${permissions.size} 项权限",
                    )
                }
            }.onFailure { error ->
                mutableState.value = mutableState.value.copy(
                    installingPluginId = null,
                    message = "安装失败：${error.message ?: error::class.java.simpleName}",
                )
            }
        }
    }

    fun approvePermissions() {
        val request = mutableState.value.permissionRequest ?: return
        mutableState.value = mutableState.value.copy(
            permissionRequest = null,
            installingPluginId = request.pluginId,
            message = "正在授权 ${request.pluginId}…",
        )
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    for (permission in request.permissions) {
                        host.request(
                            "plugin.permission.grant",
                            JSONObject().put("pluginId", request.pluginId).put("permission", permission),
                        )
                    }
                }
            }.onSuccess {
                startPortableRuntime(request.pluginId, request.runtime)
            }.onFailure { error ->
                mutableState.value = mutableState.value.copy(
                    installingPluginId = null,
                    message = "授权失败：${error.message ?: error::class.java.simpleName}",
                )
            }
        }
    }

    fun denyPermissions() {
        val pluginId = mutableState.value.permissionRequest?.pluginId ?: return
        mutableState.value = mutableState.value.copy(
            permissionRequest = null,
            installingPluginId = null,
            message = "$pluginId 已安装，但权限未授权",
        )
    }

    private fun startPortableRuntime(pluginId: String, runtime: String) {
        if (runtime !in setOf("deepseek-js", "javascript", "cordis-js")) {
            mutableState.value = mutableState.value.copy(
                installingPluginId = null,
                message = "$pluginId 已安装 · $runtime",
            )
            return
        }
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val compatibility = host.request(
                        "plugin.compatibility",
                        JSONObject().put("pluginId", pluginId),
                    )
                    check(compatibility.optBoolean("portableCompatible")) {
                        "插件不满足移动端 portable runtime 约束"
                    }
                    host.request(
                        "runtime.start",
                        JSONObject().put("pluginId", pluginId).put("config", JSONObject()),
                    )
                }
            }.onSuccess {
                mutableState.value = mutableState.value.copy(
                    installingPluginId = null,
                    message = "$pluginId 已安装并启动 · $runtime",
                )
            }.onFailure { error ->
                mutableState.value = mutableState.value.copy(
                    installingPluginId = null,
                    message = "$pluginId 已安装但启动失败：${error.message ?: error::class.java.simpleName}",
                )
            }
        }
    }

    suspend fun loadLocalMiniAppHtml(pluginId: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            host.request(
                "feature.plugin.uiDocument",
                JSONObject().put("pluginId", pluginId),
            ).optString("html").takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    fun callRuntimeToolJson(pluginId: String, name: String, argumentsJson: String): String {
        require(Regex("^[A-Za-z0-9_.-]{1,128}$").matches(name)) { "Invalid WebMCP tool name" }
        val arguments = JSONObject(argumentsJson.ifBlank { "{}" })
        val result = host.requestValue(
            "runtime.call",
            JSONObject()
                .put("pluginId", pluginId)
                .put("name", name)
                .put("arguments", arguments),
        )
        return result.toJsonString()
    }

    override fun onCleared() {
        host.close()
        super.onCleared()
    }
}

private fun JSONArray?.toStringList(): List<String> = buildList {
    val array = this@toStringList ?: return@buildList
    for (index in 0 until array.length()) {
        val value = array.optString(index).trim()
        if (value.isNotEmpty()) add(value)
    }
}

private fun JSONArray?.toToolContracts(): List<MiniAppToolContract> = buildList {
    val array = this@toToolContracts ?: return@buildList
    for (index in 0 until array.length()) {
        val command = array.optJSONObject(index) ?: continue
        val name = command.optString("tool", command.optString("name")).trim()
        if (!Regex("^[A-Za-z0-9_.-]{1,128}$").matches(name)) continue
        add(
            MiniAppToolContract(
                name = name,
                description = command.optString("description", name).ifBlank { name },
                approval = command.optString("approval", "none"),
            ),
        )
    }
}

private fun Any?.toJsonString(): String = when (this) {
    null, JSONObject.NULL -> "null"
    is JSONObject, is JSONArray -> toString()
    is Number, is Boolean -> toString()
    is String -> JSONObject.quote(this)
    else -> JSONObject.quote(toString())
}
