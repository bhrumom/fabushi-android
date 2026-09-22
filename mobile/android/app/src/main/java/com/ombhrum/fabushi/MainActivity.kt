package com.ombhrum.fabushi

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.ombhrum.fabushi.androidmain.deeplink.AndroidDeepLinkController
import com.ombhrum.fabushi.androidpreload.deeplink.AndroidDeepLink
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Android lifecycle shell.
 *
 * Product rendering lives under the frontend source tree. Runtime/domain orchestration is intentionally kept
 * out of the Activity so process recreation and renderer replacement can be tested independently.
 */
class MainActivity : ComponentActivity() {
    private val deepLinks = MutableSharedFlow<AndroidDeepLink>(replay = 1, extraBufferCapacity = 31)
    private val deepLinkController = AndroidDeepLinkController(
        dispatch = deepLinks::tryEmit,
    )
    private val updateModel: AndroidUpdateViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val processRuntime = (application as FabushiApplication).ensureProcessRuntime(intent)
        enableEdgeToEdge()
        setContent {
            FabushiApplicationRoot(
                activity = this,
                application = application,
                deepLinks = deepLinks,
                updateModel = updateModel,
                runtimePort = processRuntime,
            )
        }
        deepLinkController.markReady()
        intent?.data?.let(::enqueueDeepLink)
    }

    override fun onStart() {
        super.onStart()
        updateModel.setForeground(true)
    }

    override fun onStop() {
        updateModel.setForeground(false)
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.data?.let(::enqueueDeepLink)
    }

    override fun onDestroy() {
        deepLinkController.markNotReady()
        super.onDestroy()
    }

    internal fun appAgentSurfaceForTesting(): FabushiAppAgentSurface =
        (application as FabushiApplication).requireProcessRuntime().appAgentSurface

    private fun enqueueDeepLink(uri: android.net.Uri) {
        deepLinkController.handleCandidate(uri.toString(), "android-intent")
    }
}
