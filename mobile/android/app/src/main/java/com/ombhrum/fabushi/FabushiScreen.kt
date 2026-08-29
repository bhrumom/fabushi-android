package com.ombhrum.fabushi

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.onDispose
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object TestTags {
    const val AppShell = "app-shell"
    const val Home = "home"
    const val ProfileAvatar = "profile-avatar"
    const val HomeSearchButton = "home-search-button"
    const val HomeSearchField = "home-search-field"
    const val AddButton = "home-add-button"
    const val ConversationList = "conversation-list"
    const val ConversationRow = "conversation-chief-of-staff"
    const val MarketplaceEntry = "marketplace-entry"
    const val RemoteComputerEntry = "remote-computer-entry"
    const val RemoteComputerSurface = "remote-computer-surface"
    const val RemoteComputerClose = "remote-computer-close"
    const val RemoteComputerStatus = "remote-computer-status"
    const val RemoteComputerLoading = "remote-computer-loading"
    const val RemoteComputerError = "remote-computer-error"
    const val RemoteComputerReload = "remote-computer-reload"
    const val RemoteComputerWebView = "remote-computer-webview"
    const val RuntimeBadge = "runtime-badge"
    const val SearchField = "marketplace-search"
    const val SearchButton = "marketplace-search-submit"
    const val HostStatus = "host-status"
    const val PermissionDialog = "permission-dialog"
    const val PermissionApprove = "permission-approve"
    const val PermissionDeny = "permission-deny"
    const val UpdateCard = "android-update-card"
    const val UpdateAction = "android-update-action"
    fun plugin(id: String) = "plugin-$id"
    fun install(id: String) = "install-$id"
    fun open(id: String) = "open-$id"
}

private enum class MobileDestination { HOME, MARKETPLACE, REMOTE_COMPUTER }

private data class ConversationSummary(
    val id: String,
    val title: String,
    val preview: String,
    val time: String,
    val badge: String,
)

private val homeBackground = Color(0xFF0B0B0C)
private val homeSurface = Color(0xFF151516)
private val homeBorder = Color(0xFF29292B)
private val homePrimaryText = Color(0xFFF3F3F4)
private val homeSecondaryText = Color(0xFF8C8C91)
private val homeAccent = Color(0xFFFFB21A)
private val conversationAccent = Color(0xFFFF5A0A)

@Composable
fun FabushiScreen(
    state: MarketplaceUiState,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onInstall: (MarketplacePlugin) -> Unit,
    onOpen: (MarketplacePlugin) -> Unit,
    onApprovePermissions: () -> Unit,
    onDenyPermissions: () -> Unit,
    updateState: AndroidUpdateUiState = AndroidUpdateUiState(
        phase = AndroidUpdatePhase.DISABLED,
        currentVersion = BuildConfig.VERSION_NAME,
    ),
    onCheckUpdate: () -> Unit = {},
    onInstallUpdate: () -> Unit = {},
    appAgentSurface: FabushiAppAgentSurface? = null,
) {
    var destination by remember { mutableStateOf(MobileDestination.HOME) }
    var showAddMenu by remember { mutableStateOf(false) }

    LaunchedEffect(destination, showAddMenu, state, updateState.phase, appAgentSurface) {
        val elements = mutableListOf<FabushiAppAgentSurface.Element>()
        val actions = linkedMapOf<String, FabushiAppAgentSurface.Action>()
        fun element(
            id: String,
            role: String,
            name: String,
            enabled: Boolean = true,
            visible: Boolean = true,
            action: FabushiAppAgentSurface.Action? = null,
        ) {
            val normalizedId = id
                .replace(Regex("[^A-Za-z0-9._:/@-]"), "-")
                .take(200)
            elements += FabushiAppAgentSurface.Element(
                agentId = normalizedId,
                role = role.take(80),
                name = name.take(240),
                visible = visible,
                enabled = enabled,
            )
            if (action != null) actions[normalizedId] = action
        }
        val screen = when (destination) {
            MobileDestination.HOME -> {
                element(TestTags.AppShell, "application", "Fabushi")
                element(TestTags.HomeSearchButton, "button", "搜索对话")
                element(
                    TestTags.AddButton,
                    "button",
                    "添加",
                    action = FabushiAppAgentSurface.Action(setOf("invoke")) { showAddMenu = true },
                )
                element(TestTags.ConversationRow, "button", "Chief of Staff")
                if (showAddMenu) {
                    element(
                        TestTags.MarketplaceEntry,
                        "menuitem",
                        "插件市场",
                        action = FabushiAppAgentSurface.Action(setOf("invoke")) {
                            showAddMenu = false
                            destination = MobileDestination.MARKETPLACE
                        },
                    )
                    element(
                        TestTags.RemoteComputerEntry,
                        "menuitem",
                        "我的电脑",
                        action = FabushiAppAgentSurface.Action(setOf("invoke")) {
                            showAddMenu = false
                            destination = MobileDestination.REMOTE_COMPUTER
                        },
                    )
                }
                "home"
            }
            MobileDestination.MARKETPLACE -> {
                element(
                    TestTags.SearchField,
                    "textbox",
                    "搜索插件",
                    action = FabushiAppAgentSurface.Action(setOf("setValue")) { onQueryChange(it.orEmpty()) },
                )
                element(
                    TestTags.SearchButton,
                    "button",
                    "搜索",
                    enabled = !state.loading,
                    action = FabushiAppAgentSurface.Action(setOf("invoke")) { onSearch() },
                )
                element(TestTags.HostStatus, "status", state.message)
                state.plugins.take(100).forEach { plugin ->
                    element(TestTags.plugin(plugin.pluginId), "group", plugin.displayName)
                    element(
                        TestTags.open(plugin.pluginId),
                        "button",
                        "打开 ${plugin.displayName}",
                        action = FabushiAppAgentSurface.Action(setOf("invoke")) { onOpen(plugin) },
                    )
                    element(
                        TestTags.install(plugin.pluginId),
                        "button",
                        "安装 ${plugin.displayName}",
                        enabled = plugin.latestVersion != null && state.installingPluginId == null,
                        action = FabushiAppAgentSurface.Action(setOf("invoke")) { onInstall(plugin) },
                    )
                }
                "marketplace"
            }
            MobileDestination.REMOTE_COMPUTER -> {
                element(TestTags.RemoteComputerSurface, "application", "远程控制我的电脑")
                element(
                    TestTags.RemoteComputerClose,
                    "button",
                    "关闭远程控制",
                    action = FabushiAppAgentSurface.Action(setOf("invoke")) { destination = MobileDestination.HOME },
                )
                "remote-computer"
            }
        }
        if (state.permissionRequest != null) {
            element(
                TestTags.PermissionApprove,
                "button",
                "授权插件权限",
                action = FabushiAppAgentSurface.Action(setOf("invoke")) { onApprovePermissions() },
            )
            element(
                TestTags.PermissionDeny,
                "button",
                "拒绝插件权限",
                action = FabushiAppAgentSurface.Action(setOf("invoke")) { onDenyPermissions() },
            )
        }
        appAgentSurface?.publish(
            screen = if (state.permissionRequest != null) "permission-dialog" else screen,
            elements = elements,
            actions = actions,
        )
    }
    DisposableEffect(appAgentSurface) {
        onDispose { appAgentSurface?.clear() }
    }

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

    when (destination) {
        MobileDestination.HOME -> ConversationHome(
            updateState = updateState,
            onCheckUpdate = onCheckUpdate,
            onInstallUpdate = onInstallUpdate,
            onOpenMarketplace = { destination = MobileDestination.MARKETPLACE },
            onOpenRemoteComputer = { destination = MobileDestination.REMOTE_COMPUTER },
            showAddMenu = showAddMenu,
            onShowAddMenuChange = { showAddMenu = it },
        )
        MobileDestination.MARKETPLACE -> MarketplaceContent(
            state = state,
            onQueryChange = onQueryChange,
            onSearch = onSearch,
            onInstall = onInstall,
            onOpen = onOpen,
            onBack = { destination = MobileDestination.HOME },
        )
        MobileDestination.REMOTE_COMPUTER -> RemoteComputerSurface(
            onClose = { destination = MobileDestination.HOME },
        )
    }
}

@Composable
private fun ConversationHome(
    updateState: AndroidUpdateUiState,
    onCheckUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
    onOpenMarketplace: () -> Unit,
    onOpenRemoteComputer: () -> Unit,
    showAddMenu: Boolean,
    onShowAddMenuChange: (Boolean) -> Unit,
) {
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val conversations = remember {
        mutableStateListOf(
            ConversationSummary(
                id = "chief-of-staff",
                title = "Chief of Staff",
                preview = "I can also pull in email, Slack, or other tools…",
                time = "02:41",
                badge = "••",
            ),
        )
    }
    val filteredConversations = conversations.filter { conversation ->
        searchQuery.isBlank() ||
            conversation.title.contains(searchQuery, ignoreCase = true) ||
            conversation.preview.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().testTag(TestTags.AppShell),
        containerColor = homeBackground,
        contentColor = homePrimaryText,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag(TestTags.ConversationList),
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 20.dp, top = 10.dp, bottom = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ProfileAvatar()
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularActionButton(
                            tag = TestTags.HomeSearchButton,
                            description = "搜索对话",
                            onClick = { showSearch = !showSearch },
                        ) { SearchGlyph() }
                        Box {
                            CircularActionButton(
                                tag = TestTags.AddButton,
                                description = "添加",
                                onClick = { onShowAddMenuChange(true) },
                            ) { PlusGlyph() }
                            DropdownMenu(
                                expanded = showAddMenu,
                                onDismissRequest = { onShowAddMenuChange(false) },
                                containerColor = homeSurface,
                            ) {
                                DropdownMenuItem(
                                    text = { Text("新建对话", color = homePrimaryText) },
                                    onClick = {
                                        onShowAddMenuChange(false)
                                        val next = conversations.size + 1
                                        conversations.add(
                                            0,
                                            ConversationSummary(
                                                id = "new-$next",
                                                title = "新对话 $next",
                                                preview = "开始一段新的对话",
                                                time = "现在",
                                                badge = "+",
                                            ),
                                        )
                                    },
                                )
                                DropdownMenuItem(
                                    modifier = Modifier.testTag(TestTags.RemoteComputerEntry),
                                    text = { Text("我的电脑", color = homePrimaryText) },
                                    onClick = {
                                        onShowAddMenuChange(false)
                                        onOpenRemoteComputer()
                                    },
                                )
                                DropdownMenuItem(
                                    modifier = Modifier.testTag(TestTags.MarketplaceEntry),
                                    text = { Text("插件市场", color = homePrimaryText) },
                                    onClick = {
                                        onShowAddMenuChange(false)
                                        onOpenMarketplace()
                                    },
                                )
                                if (updateState.phase != AndroidUpdatePhase.DISABLED) {
                                    DropdownMenuItem(
                                        text = { Text("检查更新", color = homePrimaryText) },
                                        onClick = {
                                            onShowAddMenuChange(false)
                                            onCheckUpdate()
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (showSearch) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 2.dp)
                            .testTag(TestTags.HomeSearchField),
                        singleLine = true,
                        placeholder = { Text("搜索消息", color = homeSecondaryText) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = homePrimaryText,
                            unfocusedTextColor = homePrimaryText,
                            cursorColor = homeAccent,
                            focusedBorderColor = Color(0xFF4A4A4E),
                            unfocusedBorderColor = homeBorder,
                            focusedContainerColor = homeSurface,
                            unfocusedContainerColor = homeSurface,
                        ),
                        shape = RoundedCornerShape(16.dp),
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }

            if (shouldShowUpdateBanner(updateState.phase)) {
                item {
                    UpdateBanner(
                        state = updateState,
                        onCheckUpdate = onCheckUpdate,
                        onInstallUpdate = onInstallUpdate,
                    )
                }
            }

            if (filteredConversations.isEmpty()) {
                item {
                    Text(
                        "没有找到匹配的消息",
                        color = homeSecondaryText,
                        modifier = Modifier.padding(horizontal = 30.dp, vertical = 28.dp),
                    )
                }
            } else {
                items(filteredConversations, key = { it.id }) { conversation ->
                    ConversationRow(conversation)
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun ProfileAvatar() {
    Box(
        modifier = Modifier
            .size(56.dp)
            .background(homeSurface, CircleShape)
            .border(1.dp, homeBorder, CircleShape)
            .testTag(TestTags.ProfileAvatar)
            .semantics { contentDescription = "个人头像" },
        contentAlignment = Alignment.Center,
    ) {
        Text("✦", color = homeAccent, fontSize = 34.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CircularActionButton(
    tag: String,
    description: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(54.dp)
            .background(Color(0xFF101011), CircleShape)
            .border(1.dp, homeBorder, CircleShape)
            .clickable(onClick = onClick)
            .testTag(tag)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun SearchGlyph() {
    Canvas(Modifier.size(24.dp)) {
        val stroke = 2.2.dp.toPx()
        drawCircle(
            color = homePrimaryText,
            radius = size.minDimension * 0.28f,
            center = Offset(size.width * 0.43f, size.height * 0.42f),
            style = Stroke(width = stroke),
        )
        drawLine(
            color = homePrimaryText,
            start = Offset(size.width * 0.63f, size.height * 0.63f),
            end = Offset(size.width * 0.84f, size.height * 0.84f),
            strokeWidth = stroke,
        )
    }
}

@Composable
private fun PlusGlyph() {
    Canvas(Modifier.size(25.dp)) {
        val stroke = 2.dp.toPx()
        drawLine(
            color = homePrimaryText,
            start = Offset(size.width * 0.5f, size.height * 0.14f),
            end = Offset(size.width * 0.5f, size.height * 0.86f),
            strokeWidth = stroke,
        )
        drawLine(
            color = homePrimaryText,
            start = Offset(size.width * 0.14f, size.height * 0.5f),
            end = Offset(size.width * 0.86f, size.height * 0.5f),
            strokeWidth = stroke,
        )
    }
}

@Composable
private fun ConversationRow(conversation: ConversationSummary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(start = 30.dp, end = 24.dp, top = 13.dp, bottom = 13.dp)
            .then(if (conversation.id == "chief-of-staff") Modifier.testTag(TestTags.ConversationRow) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(54.dp).background(conversationAccent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(conversation.badge, color = Color(0xFF1A1009), fontWeight = FontWeight.Black, fontSize = 13.sp)
        }
        Column(
            modifier = Modifier.weight(1f).padding(start = 18.dp, end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                conversation.title,
                color = homePrimaryText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                conversation.preview,
                color = homeSecondaryText,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            conversation.time,
            color = Color(0xFF56565B),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private fun shouldShowUpdateBanner(phase: AndroidUpdatePhase): Boolean = when (phase) {
    AndroidUpdatePhase.AVAILABLE,
    AndroidUpdatePhase.DOWNLOADING,
    AndroidUpdatePhase.WAITING_FOR_PERMISSION,
    AndroidUpdatePhase.INSTALLING,
    AndroidUpdatePhase.ERROR -> true
    else -> false
}

@Composable
private fun UpdateBanner(
    state: AndroidUpdateUiState,
    onCheckUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).testTag(TestTags.UpdateCard),
        colors = CardDefaults.cardColors(containerColor = homeSurface),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val label = when (state.phase) {
                AndroidUpdatePhase.AVAILABLE -> "发现新版本 ${state.availableVersion ?: ""}"
                AndroidUpdatePhase.DOWNLOADING -> "正在下载更新 ${state.progressPercent ?: 0}%"
                AndroidUpdatePhase.WAITING_FOR_PERMISSION -> "更新已下载，等待安装权限"
                AndroidUpdatePhase.INSTALLING -> "系统安装器已打开"
                AndroidUpdatePhase.ERROR -> "更新失败"
                else -> "应用更新"
            }
            Text(label, color = homePrimaryText, fontWeight = FontWeight.SemiBold)
            state.message?.takeIf { it.isNotBlank() }?.let {
                Text(it, color = homeSecondaryText, style = MaterialTheme.typography.bodySmall)
            }
            if (state.phase == AndroidUpdatePhase.DOWNLOADING) {
                val progress = (state.progressPercent ?: 0).coerceIn(0, 100) / 100f
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            }
            when (state.phase) {
                AndroidUpdatePhase.AVAILABLE,
                AndroidUpdatePhase.WAITING_FOR_PERMISSION,
                AndroidUpdatePhase.INSTALLING -> Button(
                    onClick = onInstallUpdate,
                    modifier = Modifier.fillMaxWidth().testTag(TestTags.UpdateAction),
                    colors = ButtonDefaults.buttonColors(containerColor = homeAccent, contentColor = Color.Black),
                ) {
                    Text(if (state.phase == AndroidUpdatePhase.AVAILABLE) "下载并安装" else "继续安装")
                }
                AndroidUpdatePhase.ERROR -> OutlinedButton(
                    onClick = onCheckUpdate,
                    modifier = Modifier.fillMaxWidth().testTag(TestTags.UpdateAction),
                ) { Text("重新检查", color = homePrimaryText) }
                else -> Unit
            }
        }
    }
}

@Composable
private fun MarketplaceContent(
    state: MarketplaceUiState,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onInstall: (MarketplacePlugin) -> Unit,
    onOpen: (MarketplacePlugin) -> Unit,
    onBack: () -> Unit,
) {
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
                    OutlinedButton(onClick = onBack) { Text("返回消息") }
                }
            }

            item {
                Text(
                    "Compose · Rust",
                    modifier = Modifier.testTag(TestTags.RuntimeBadge).semantics { contentDescription = "Android native runtime" },
                    style = MaterialTheme.typography.labelMedium,
                )
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
