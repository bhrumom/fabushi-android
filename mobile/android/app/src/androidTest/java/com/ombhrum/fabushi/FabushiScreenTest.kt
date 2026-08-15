package com.ombhrum.fabushi

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FabushiScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun coreControlsExposeStableSemanticsAndSearchCallback() {
        var query by mutableStateOf("")
        var searches = 0
        compose.setContent {
            FabushiScreen(
                state = MarketplaceUiState(message = "ready", query = query),
                onQueryChange = { query = it },
                onSearch = { searches += 1 },
                onInstall = {},
                onApprovePermissions = {},
                onDenyPermissions = {},
            )
        }

        compose.onNodeWithTag(TestTags.AppShell).assertIsDisplayed()
        compose.onNodeWithTag(TestTags.RuntimeBadge).assertTextContains("Compose")
        compose.onNodeWithTag(TestTags.HostStatus).assertTextContains("ready")
        compose.onNodeWithTag(TestTags.SearchField).performTextInput("telegram")
        compose.onNodeWithTag(TestTags.SearchButton).performClick()

        assertEquals("telegram", query)
        assertEquals(1, searches)
    }

    @Test
    fun pluginCardsUseStableSemanticIdentifiers() {
        var installed: MarketplacePlugin? = null
        val plugin = MarketplacePlugin("example.plugin", "示例插件", "描述", "1.0.0")
        compose.setContent {
            FabushiScreen(
                state = MarketplaceUiState(plugins = listOf(plugin)),
                onQueryChange = {},
                onSearch = {},
                onInstall = { installed = it },
                onApprovePermissions = {},
                onDenyPermissions = {},
            )
        }

        compose.onNodeWithTag(TestTags.plugin(plugin.pluginId)).assertIsDisplayed().assertTextContains("示例插件")
        compose.onNodeWithTag(TestTags.install(plugin.pluginId)).assertIsDisplayed().performClick()
        assertEquals(plugin, installed)
    }

    @Test
    fun permissionDialogHasStableApproveAndDenyControls() {
        compose.setContent {
            FabushiScreen(
                state = MarketplaceUiState(
                    permissionRequest = PermissionRequest(
                        pluginId = "example.plugin",
                        runtime = "deepseek-js",
                        permissions = listOf("network.request"),
                    ),
                ),
                onQueryChange = {},
                onSearch = {},
                onInstall = {},
                onApprovePermissions = {},
                onDenyPermissions = {},
            )
        }

        compose.onNodeWithTag(TestTags.PermissionDialog).assertIsDisplayed()
        compose.onNodeWithTag(TestTags.PermissionApprove).assertIsDisplayed()
        compose.onNodeWithTag(TestTags.PermissionDeny).assertIsDisplayed()
    }
}
