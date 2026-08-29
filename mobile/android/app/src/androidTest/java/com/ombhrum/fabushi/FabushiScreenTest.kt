package com.ombhrum.fabushi

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FabushiScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun homeMatchesConversationReferenceAndSearchesMessages() {
        compose.setContent {
            FabushiScreen(
                state = MarketplaceUiState(),
                onQueryChange = {},
                onSearch = {},
                onInstall = {},
                onOpen = {},
                onApprovePermissions = {},
                onDenyPermissions = {},
            )
        }

        compose.onNodeWithTag(TestTags.AppShell).assertIsDisplayed()
        compose.onNodeWithTag(TestTags.ProfileAvatar).assertIsDisplayed()
        compose.onNodeWithTag(TestTags.HomeSearchButton).assertIsDisplayed()
        compose.onNodeWithTag(TestTags.AddButton).assertIsDisplayed()
        compose.onNodeWithTag(TestTags.ConversationList).assertIsDisplayed()
        compose.onNodeWithTag(TestTags.ConversationRow).assertIsDisplayed()
        compose.onNodeWithText("Chief of Staff").assertIsDisplayed()

        compose.onNodeWithTag(TestTags.HomeSearchButton).performClick()
        compose.onNodeWithTag(TestTags.HomeSearchField).performTextInput("Chief")
        compose.onNodeWithText("Chief of Staff").assertIsDisplayed()
    }

    @Test
    fun addMenuOpensMarketplaceAndKeepsMarketplaceCallbacks() {
        var query by mutableStateOf("")
        var searches = 0
        var installed: MarketplacePlugin? = null
        var opened: MarketplacePlugin? = null
        val plugin = MarketplacePlugin("example-plugin", "示例插件", "描述", "1.0.0")
        compose.setContent {
            FabushiScreen(
                state = MarketplaceUiState(message = "ready", query = query, plugins = listOf(plugin)),
                onQueryChange = { query = it },
                onSearch = { searches += 1 },
                onInstall = { installed = it },
                onOpen = { opened = it },
                onApprovePermissions = {},
                onDenyPermissions = {},
            )
        }

        compose.onNodeWithTag(TestTags.AddButton).performClick()
        compose.onNodeWithTag(TestTags.MarketplaceEntry).assertIsDisplayed().performClick()
        compose.onNodeWithTag(TestTags.RuntimeBadge).assertTextContains("Compose", substring = true)
        compose.onNodeWithTag(TestTags.HostStatus).assertIsDisplayed()
        compose.onNodeWithText("ready").assertIsDisplayed()
        compose.onNodeWithTag(TestTags.SearchField).performTextInput("telegram")
        compose.onNodeWithTag(TestTags.SearchButton).performClick()
        assertEquals("telegram", query)
        assertEquals(1, searches)

        compose.onNodeWithTag(TestTags.plugin(plugin.pluginId)).assertIsDisplayed()
        compose.onNodeWithTag(TestTags.open(plugin.pluginId)).assertIsDisplayed().performClick()
        assertEquals(plugin, opened)
        compose.onNodeWithTag(TestTags.install(plugin.pluginId)).assertIsDisplayed().performClick()
        assertEquals(plugin, installed)
    }

    @Test
    fun addMenuOpensAndClosesRestrictedRemoteComputerSurface() {
        compose.setContent {
            FabushiScreen(
                state = MarketplaceUiState(),
                onQueryChange = {},
                onSearch = {},
                onInstall = {},
                onOpen = {},
                onApprovePermissions = {},
                onDenyPermissions = {},
            )
        }

        compose.onNodeWithTag(TestTags.AddButton).performClick()
        compose.onNodeWithTag(TestTags.RemoteComputerEntry).assertIsDisplayed().performClick()
        compose.onNodeWithTag(TestTags.RemoteComputerSurface).assertIsDisplayed()
        compose.onNodeWithTag(TestTags.RemoteComputerClose).assertIsDisplayed().performClick()
        compose.onNodeWithTag(TestTags.AppShell).assertIsDisplayed()
    }

    @Test
    fun availableUpdateAppearsOnHomeAndStartsInstall() {
        var installRequests = 0
        compose.setContent {
            FabushiScreen(
                state = MarketplaceUiState(),
                onQueryChange = {},
                onSearch = {},
                onInstall = {},
                onOpen = {},
                onApprovePermissions = {},
                onDenyPermissions = {},
                updateState = AndroidUpdateUiState(
                    phase = AndroidUpdatePhase.AVAILABLE,
                    currentVersion = "1.0.4",
                    availableVersion = "1.0.5",
                    availableVersionCode = 3,
                ),
                onInstallUpdate = { installRequests += 1 },
            )
        }

        compose.onNodeWithTag(TestTags.UpdateCard).assertIsDisplayed()
        compose.onNodeWithText("发现新版本 1.0.5").assertIsDisplayed()
        compose.onNodeWithTag(TestTags.UpdateAction).performClick()
        assertEquals(1, installRequests)
    }

    @Test
    fun permissionDialogHasStableApproveAndDenyControls() {
        compose.setContent {
            FabushiScreen(
                state = MarketplaceUiState(
                    permissionRequest = PermissionRequest(
                        pluginId = "example-plugin",
                        runtime = "deepseek-js",
                        permissions = listOf("network.request"),
                    ),
                ),
                onQueryChange = {},
                onSearch = {},
                onInstall = {},
                onOpen = {},
                onApprovePermissions = {},
                onDenyPermissions = {},
            )
        }

        compose.onNodeWithTag(TestTags.PermissionDialog).assertIsDisplayed()
        compose.onNodeWithTag(TestTags.PermissionApprove).assertIsDisplayed()
        compose.onNodeWithTag(TestTags.PermissionDeny).assertIsDisplayed()
    }
}
