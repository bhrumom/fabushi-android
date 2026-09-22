package com.ombhrum.fabushi

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Android lifecycle shell.
 *
 * Product rendering lives under frontend/**. Runtime/domain orchestration is intentionally kept
 * out of the Activity so process recreation and renderer replacement can be tested independently.
 */
class MainActivity : ComponentActivity() {
    private val deepLinks = MutableSharedFlow<Uri>(replay = 1, extraBufferCapacity = 31)
    private val updateModel: AndroidUpdateViewModel by viewModels()
    private val appAgentSurface = FabushiAppAgentSurface()
    private lateinit var remoteDeviceGateway: FabushiRemoteDeviceGateway

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ciBootstrapActive = FabushiCiBootstrap.prepare(this)
        remoteDeviceGateway = FabushiRemoteDeviceGateway(
            context = applicationContext,
            surface = appAgentSurface,
            metadata = FabushiCiBootstrap.gatewayMetadata(intent, ciBootstrapActive),
            configuredDeviceName = FabushiCiBootstrap.configuredDeviceName(intent, ciBootstrapActive),
        )
        enableEdgeToEdge()
        setContent {
            FabushiApplicationRoot(
                activity = this,
                application = application,
                deepLinks = deepLinks,
                updateModel = updateModel,
                appAgentSurface = appAgentSurface,
                remoteDeviceGateway = remoteDeviceGateway,
            )
        }
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

    override fun onDestroy() {
        if (::remoteDeviceGateway.isInitialized) remoteDeviceGateway.close()
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.data?.let(::enqueueDeepLink)
    }

    internal fun appAgentSurfaceForTesting(): FabushiAppAgentSurface = appAgentSurface

    private fun enqueueDeepLink(uri: Uri) {
        if (uri.scheme != "fabushi") return
        deepLinks.tryEmit(uri)
    }
}
