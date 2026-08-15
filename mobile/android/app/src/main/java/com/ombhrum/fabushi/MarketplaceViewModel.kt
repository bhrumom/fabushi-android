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

data class MarketplacePlugin(
    val pluginId: String,
    val displayName: String,
    val description: String,
    val latestVersion: String?,
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

    fun refresh() {
        val query = mutableState.value.query.trim()
        mutableState.value = mutableState.value.copy(loading = true)
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    host.request(
                        "marketplace.browse",
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
                            add(
                                MarketplacePlugin(
                                    pluginId = pluginId,
                                    displayName = item.optString("displayName", pluginId),
                                    description = item.optString("description", "无描述"),
                                    latestVersion = item.optString("latestVersion").takeIf(String::isNotBlank),
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
                        "marketplace.release",
                        JSONObject().put("pluginId", plugin.pluginId).put("version", version),
                    )
                    val release = metadata.optJSONObject("releaseManifest")
                        ?: error("marketplace release has no releaseManifest")
                    host.request(
                        "plugin.install",
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
