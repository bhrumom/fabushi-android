package com.ombhrum.fabushi

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableSharedFlow

class MainActivity : ComponentActivity() {
    private val deepLinks = MutableSharedFlow<Uri>(replay = 1, extraBufferCapacity = 31)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                val model: MarketplaceViewModel = viewModel()
                val state by model.state.collectAsState()
                LaunchedEffect(model) {
                    deepLinks.collect { uri -> model.handleDeepLink(uri) }
                }
                FabushiScreen(
                    state = state,
                    onQueryChange = model::setQuery,
                    onSearch = model::refresh,
                    onInstall = model::install,
                    onApprovePermissions = model::approvePermissions,
                    onDenyPermissions = model::denyPermissions,
                )
            }
        }
        intent?.data?.let(::enqueueDeepLink)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.data?.let(::enqueueDeepLink)
    }

    private fun enqueueDeepLink(uri: Uri) {
        if (uri.scheme != "fabushi") return
        deepLinks.tryEmit(uri)
    }
}
