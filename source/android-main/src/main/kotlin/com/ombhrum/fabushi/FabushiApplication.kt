package com.ombhrum.fabushi

import android.app.Application
import android.content.Intent
import com.ombhrum.fabushi.androidmain.coordinator.AndroidCoordinatorPorts
import com.ombhrum.fabushi.androidmain.notifications.AndroidNotificationRuntime
import com.ombhrum.fabushi.androidpreload.runtime.AndroidCoordinatorBridge
import com.ombhrum.fabushi.androidpreload.runtime.AndroidPresentationRuntimePort

/**
 * Process owner for Android runtime services that must survive Activity recreation.
 *
 * The Activity only supplies Android launch metadata. Coordinator/Host ownership remains below the
 * typed bridge and the remote-device transport is process-scoped rather than tied to a screen.
 */
class FabushiApplication : Application() {
    @Volatile
    private var processRuntime: FabushiProcessRuntime? = null

    internal fun ensureProcessRuntime(intent: Intent?): FabushiProcessRuntime =
        processRuntime ?: synchronized(this) {
            processRuntime ?: run {
                val ciBootstrapActive = FabushiCiBootstrap.prepare(this)
                FabushiProcessRuntime(
                    application = this,
                    intent = intent,
                    ciBootstrapActive = ciBootstrapActive,
                ).also { processRuntime = it }
            }
        }

    internal fun requireProcessRuntime(): FabushiProcessRuntime =
        checkNotNull(processRuntime) { "Fabushi process runtime has not been initialized" }

    override fun onTerminate() {
        processRuntime?.close()
        processRuntime = null
        super.onTerminate()
    }
}

internal class FabushiProcessRuntime(
    application: Application,
    intent: Intent?,
    ciBootstrapActive: Boolean,
) : AndroidPresentationRuntimePort, AutoCloseable {
    @Volatile
    private var appForeground = false
    private val coordinator = AndroidCoordinatorPorts.presentation(application).also {
        AndroidCoordinatorBridge.installTrustedRuntime(it)
    }
    override val appAgentSurface = FabushiAppAgentSurface()
    private val notificationRuntime = AndroidNotificationRuntime(
        context = application,
        isAppForeground = { appForeground },
    )
    private val notificationEventSubscription =
        coordinator.addFeatureEventListener(notificationRuntime.feed::handleFeatureEvent)
    private val remoteDeviceGateway = FabushiRemoteDeviceGateway(
        context = application,
        coordinator = coordinator,
        surface = appAgentSurface,
        metadata = FabushiCiBootstrap.gatewayMetadata(intent, ciBootstrapActive),
        configuredDeviceName = FabushiCiBootstrap.configuredDeviceName(intent, ciBootstrapActive),
    )

    override fun setLoggedIn(loggedIn: Boolean) {
        remoteDeviceGateway.setLoggedIn(loggedIn)
        if (!loggedIn) notificationRuntime.reset()
    }

    override fun setForeground(foreground: Boolean) {
        appForeground = foreground
    }

    override fun close() {
        notificationEventSubscription.close()
        notificationRuntime.reset()
        remoteDeviceGateway.close()
    }
}
