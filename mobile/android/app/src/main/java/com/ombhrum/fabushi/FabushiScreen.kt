package com.ombhrum.fabushi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

object TestTags {
    const val AppShell = "app-shell"
    const val RuntimeBadge = "runtime-badge"
    const val SearchField = "marketplace-search"
    const val SearchButton = "marketplace-search-submit"
    const val HostStatus = "host-status"
    const val PermissionDialog = "permission-dialog"
    const val PermissionApprove = "permission-approve"
    const val PermissionDeny = "permission-deny"
    fun plugin(id: String) = "plugin-$id"
    fun install(id: String) = "install-$id"
    fun open(id: String) = "open-$id"
}

@Composable
fun FabushiScreen(
    state: MarketplaceUiState,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onInstall: (MarketplacePlugin) -> Unit,
    onOpen: (MarketplacePlugin) -> Unit,
    onApprovePermissions: () -> Unit,
    onDenyPermissions: () -> Unit,
) {
    state.permissionRequest?.let { request ->
        AlertDialog(
            modifier = Modifier.testTag(TestTags.PermissionDialog),
            onDismissRequest = onDenyPermissions,
            title = { Text("插件权限") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${request.pluginId} 请求以下权限：")
                    request.permissions.forEach { Text("• $it") }
                }
            },
            confirmButton = {
                Button(
                    onClick = onApprovePermissions,
                    modifier = Modifier.testTag(TestTags.PermissionApprove),
                ) { Text("授权") }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = onDenyPermissions,
                    modifier = Modifier.testTag(TestTags.PermissionDeny),
                ) { Text("拒绝") }
            },
        )
    }

    Scaffold(modifier = Modifier.fillMaxSize().testTag(TestTags.AppShell)) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("MAHAYANA RUST HOST", style = MaterialTheme.typography.labelSmall)
                        Text("全球法布施", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "Compose · Rust",
                        modifier = Modifier.testTag(TestTags.RuntimeBadge).semantics { contentDescription = "Android native runtime" },
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("本地插件市场", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text("Android 主壳使用 Jetpack Compose；MiniApp 使用受控 WebMCP Surface；插件安装、权限与后台运行由共享 Mahayana Rust Host 管理。")
                        OutlinedTextField(
                            value = state.query,
                            onValueChange = onQueryChange,
                            modifier = Modifier.fillMaxWidth().testTag(TestTags.SearchField),
                            label = { Text("搜索插件") },
                            singleLine = true,
                        )
                        Button(
                            onClick = onSearch,
                            modifier = Modifier.fillMaxWidth().testTag(TestTags.SearchButton),
                            enabled = !state.loading,
                        ) {
                            if (state.loading) {
                                CircularProgressIndicator(modifier = Modifier.height(20.dp))
                            } else {
                                Text("搜索")
                            }
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth().testTag(TestTags.HostStatus)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Host 状态", fontWeight = FontWeight.Bold)
                        Text(state.message)
                    }
                }
            }

            if (state.plugins.isEmpty() && !state.loading) {
                item { Text("没有匹配的 Android 插件。", modifier = Modifier.padding(16.dp)) }
            }

            items(state.plugins, key = { it.pluginId }) { plugin ->
                Card(modifier = Modifier.fillMaxWidth().testTag(TestTags.plugin(plugin.pluginId))) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(plugin.pluginId, style = MaterialTheme.typography.labelSmall)
                        Text(plugin.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(plugin.description)
                        plugin.latestVersion?.let { Text(it, style = MaterialTheme.typography.labelMedium) }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onOpen(plugin) },
                                modifier = Modifier.weight(1f).testTag(TestTags.open(plugin.pluginId)),
                            ) {
                                Text("打开 WebMCP")
                            }
                            Button(
                                onClick = { onInstall(plugin) },
                                modifier = Modifier.weight(1f).testTag(TestTags.install(plugin.pluginId)),
                                enabled = plugin.latestVersion != null && state.installingPluginId == null,
                            ) {
                                Text(if (state.installingPluginId == plugin.pluginId) "处理中…" else "安装 / 更新")
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
