package com.ombhrum.fabushi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                val model: MarketplaceViewModel = viewModel()
                val state by model.state.collectAsState()
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
    }
}
