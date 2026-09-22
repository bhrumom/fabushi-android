package com.ombhrum.fabushi.androidmain.coordinator

import android.app.Application
import com.ombhrum.fabushi.androidpreload.runtime.AndroidCoordinatorPort
import com.ombhrum.fabushi.core.MahayanaHost
import org.json.JSONObject

/**
 * Process-scoped Android owner of the native Mahayana Host.
 *
 * Presentation/ViewModel code receives only [AndroidCoordinatorPort]; the native Host is no longer
 * constructed or closed by screens/ViewModels. The process runtime becomes the single lifecycle
 * owner and is the insertion point for reconnect/resync and process-death recovery.
 */
class AndroidCoordinatorRuntime private constructor(application: Application) : AndroidCoordinatorPort {
    private val epochStore = object : CoordinatorEpochStore {
        private val preferences = application.getSharedPreferences("fabushi-coordinator-runtime", 0)
        override fun read(): Long = preferences.getLong("generation", 0L)
        override fun write(value: Long) { preferences.edit().putLong("generation", value).apply() }
    }
    private val processRuntime = CoordinatorProcessRuntime(epochStore)
    val processGeneration: Long = processRuntime.start()
    private val host = MahayanaHost(application)

    override fun authStatus() = host.request("feature.auth.status")
    override fun authBrowserStart() = host.request("feature.auth.browserStart")
    override fun authBrowserReopen(params: JSONObject) = host.request("feature.auth.browserReopen", params)
    override fun authBrowserCancel(params: JSONObject) = host.request("feature.auth.browserCancel", params)
    override fun authBrowserPoll(params: JSONObject) = host.request("feature.auth.browserPoll", params)
    override fun authLogout() = host.request("feature.auth.logout")

    override fun featureExecute(params: JSONObject) = host.request("feature.execute", params)
    override fun featureReceive(params: JSONObject) = host.request("feature.receive", params)
    override fun featureInterrupt(params: JSONObject) = host.request("feature.interrupt", params)

    override fun marketplaceBrowse(params: JSONObject) = host.request("feature.marketplace.browse", params)
    override fun marketplaceRelease(params: JSONObject) = host.request("feature.marketplace.release", params)
    override fun pluginInstall(params: JSONObject) = host.request("feature.plugin.install", params)
    override fun pluginUiDocument(params: JSONObject) = host.request("feature.plugin.uiDocument", params)
    override fun pluginCompatibility(params: JSONObject) = host.request("plugin.compatibility", params)
    override fun pluginPermissionGrant(params: JSONObject) = host.request("plugin.permission.grant", params)

    override fun runtimeStart(params: JSONObject) = host.request("runtime.start", params)
    override fun runtimeCallValue(params: JSONObject): Any? = host.requestValue("runtime.call", params)

    override fun messagingAccessIssue(params: JSONObject) = host.request("feature.messaging.access.issue", params)
    override fun messagingBlobRead(params: JSONObject) = host.request("feature.messaging.blob.read", params)
    override fun messagingExecute(params: JSONObject) = host.request("feature.messaging.execute", params)

    override fun platformRequest(params: JSONObject) = host.request("platform.request", params)
    override fun publishFeatureEvent(event: JSONObject) = host.publishFeatureEvent(event)

    companion object {
        @Volatile private var instance: AndroidCoordinatorRuntime? = null

        fun get(application: Application): AndroidCoordinatorRuntime =
            instance ?: synchronized(this) {
                instance ?: AndroidCoordinatorRuntime(application).also { instance = it }
            }
    }
}
