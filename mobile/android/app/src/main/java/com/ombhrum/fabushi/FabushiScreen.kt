package com.ombhrum.fabushi

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

object TestTags {
    const val AppShell = "app-shell"
    const val Home = "home"
    const val ProfileAvatar = "profile-avatar"
    const val HomeSearchButton = "home-search-button"
    const val HomeSearchField = "home-search-field"
    const val AddButton = "home-add-button"
    const val ConversationList = "conversation-list"
    const val ConversationRow = "conversation-chief-of-staff"
    const val ComposeName = "compose-name"
    const val ComposeCreate = "compose-create"
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
private enum class AndroidMobileSection(val label: String) { CONTACTS("联系人"), BOTS("Bots"), GROUPS("群组"), CHANNELS("频道"), SAVED("收藏"), ARCHIVE("归档"), CALLS("通话"), FOLDERS("文件夹"), SETTINGS("设置") }
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
    messagingState: MessagingUiState = MessagingUiState(),
    onMessagingRefresh: () -> Unit = {},
    onCreateDirect: (MessagingContact) -> Unit = {},
    onCreateConversation: (ConversationKind, String, String, List<String>) -> Unit = { _, _, _, _ -> },
    onSendText: (String, String, String?) -> Unit = { _, _, _ -> },
    onSendAttachment: (String, String, String, ByteArray) -> Unit = { _, _, _, _ -> },
    onSendContact: (String, MessagingContact) -> Unit = { _, _ -> },
    onSendPoll: (String, String, List<String>, Boolean) -> Unit = { _, _, _, _ -> },
    onSendLocation: (String, Double, Double) -> Unit = { _, _, _ -> },
    onEditText: (String, String, String) -> Unit = { _, _, _ -> },
    onDeleteMessage: (String, String) -> Unit = { _, _ -> },
    onSetReaction: (String, String, String, Boolean) -> Unit = { _, _, _, _ -> },
    onForwardMessage: (String, String, String) -> Unit = { _, _, _ -> },
    onStartTyping: (String) -> Unit = {},
    onStopTyping: (String) -> Unit = {},
    onSetPinned: (ConversationSummary, Boolean) -> Unit = { _, _ -> },
    onSetArchived: (ConversationSummary, Boolean) -> Unit = { _, _ -> },
    onSetMuted: (ConversationSummary, Boolean) -> Unit = { _, _ -> },
    onMarkRead: (ConversationSummary) -> Unit = {},
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
                element(TestTags.ProfileAvatar, "button", "个人菜单", action = FabushiAppAgentSurface.Action(setOf("invoke")) { showAddMenu = true })
                element(TestTags.AddButton, "button", "新建对话")
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
            messagingState = messagingState,
            onMessagingRefresh = onMessagingRefresh,
            onCreateDirect = onCreateDirect,
            onCreateConversation = onCreateConversation,
            onSendText = onSendText,
            onSendAttachment = onSendAttachment,
            onSendContact = onSendContact,
            onSendPoll = onSendPoll,
            onSendLocation = onSendLocation,
            onEditText = onEditText,
            onDeleteMessage = onDeleteMessage,
            onSetReaction = onSetReaction,
            onForwardMessage = onForwardMessage,
            onStartTyping = onStartTyping,
            onStopTyping = onStopTyping,
            onSetPinned = onSetPinned,
            onSetArchived = onSetArchived,
            onSetMuted = onSetMuted,
            onMarkRead = onMarkRead,
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
    messagingState: MessagingUiState,
    onMessagingRefresh: () -> Unit,
    onCreateDirect: (MessagingContact) -> Unit,
    onCreateConversation: (ConversationKind, String, String, List<String>) -> Unit,
    onSendText: (String, String, String?) -> Unit,
    onSendAttachment: (String, String, String, ByteArray) -> Unit,
    onSendContact: (String, MessagingContact) -> Unit,
    onSendPoll: (String, String, List<String>, Boolean) -> Unit,
    onSendLocation: (String, Double, Double) -> Unit,
    onEditText: (String, String, String) -> Unit,
    onDeleteMessage: (String, String) -> Unit,
    onSetReaction: (String, String, String, Boolean) -> Unit,
    onForwardMessage: (String, String, String) -> Unit,
    onStartTyping: (String) -> Unit,
    onStopTyping: (String) -> Unit,
    onSetPinned: (ConversationSummary, Boolean) -> Unit,
    onSetArchived: (ConversationSummary, Boolean) -> Unit,
    onSetMuted: (ConversationSummary, Boolean) -> Unit,
    onMarkRead: (ConversationSummary) -> Unit,
) {
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showComposeMenu by remember { mutableStateOf(false) }
    var showContactPicker by remember { mutableStateOf(false) }
    var pendingKind by remember { mutableStateOf<ConversationKind?>(null) }
    var composeName by remember { mutableStateOf("") }
    var composeDescription by remember { mutableStateOf("") }
    var composeParticipantIds by remember { mutableStateOf(setOf<String>()) }
    var selectedConversation by remember { mutableStateOf<ConversationSummary?>(null) }
    var contextConversation by remember { mutableStateOf<ConversationSummary?>(null) }
    var activeSection by remember { mutableStateOf<AndroidMobileSection?>(null) }
    val conversations = messagingState.conversations
    val filteredConversations = conversations
        .filter { !it.isArchived }
        .sortedWith(compareByDescending<ConversationSummary> { it.isPinned }.thenByDescending { it.time })
        .filter { conversation -> searchQuery.isBlank() || conversation.title.contains(searchQuery, true) || conversation.preview.contains(searchQuery, true) }

    contextConversation?.let { conversation ->
        AlertDialog(
            onDismissRequest = { contextConversation = null },
            title = { Text(conversation.title) },
            text = {
                Column {
                    TextButton(onClick = { onSetPinned(conversation, !conversation.isPinned); contextConversation = null }) { Text(if (conversation.isPinned) "取消置顶" else "置顶") }
                    TextButton(onClick = { onSetMuted(conversation, !conversation.isMuted); contextConversation = null }) { Text(if (conversation.isMuted) "取消静音" else "静音") }
                    TextButton(onClick = { onSetArchived(conversation, !conversation.isArchived); contextConversation = null }) { Text(if (conversation.isArchived) "恢复" else "归档") }
                }
            },
            confirmButton = { OutlinedButton(onClick = { contextConversation = null }) { Text("取消") } },
        )
    }

    activeSection?.let { section ->
        AndroidSectionSurface(
            section = section,
            messagingState = messagingState,
            onBack = { activeSection = null },
            onCreateDirect = { contact -> activeSection = null; onCreateDirect(contact) },
            onOpenConversation = { conversation -> activeSection = null; selectedConversation = conversation; onMarkRead(conversation) },
            onUnarchive = { conversation -> onSetArchived(conversation, false) },
        )
        return
    }

    selectedConversation?.let { selected ->
        val conversation = conversations.firstOrNull { it.id == selected.id } ?: selected
        ConversationDetail(
            conversation = conversation,
            messages = messagingState.messagesByConversation[conversation.id].orEmpty(),
            onBack = { selectedConversation = null },
            onSend = { text, replyTo -> onSendText(conversation.id, text, replyTo) },
            onSendAttachment = { fileName, mimeType, bytes -> onSendAttachment(conversation.id, fileName, mimeType, bytes) },
            shareContacts = messagingState.contacts,
            onSendContact = { contact -> onSendContact(conversation.id, contact) },
            onSendPoll = { question, options, multiple -> onSendPoll(conversation.id, question, options, multiple) },
            onSendLocation = { latitude, longitude -> onSendLocation(conversation.id, latitude, longitude) },
            onEdit = { messageId, text -> onEditText(conversation.id, messageId, text) },
            onDelete = { messageId -> onDeleteMessage(conversation.id, messageId) },
            onReact = { messageId, reaction -> onSetReaction(conversation.id, messageId, reaction, true) },
            onForward = { messageId, destinationId -> onForwardMessage(conversation.id, messageId, destinationId) },
            forwardDestinations = conversations.filter { it.id != conversation.id && !it.isArchived },
            typingActorName = messagingState.typingActorByConversation[conversation.id],
            onTypingChanged = { typing -> if (typing) onStartTyping(conversation.id) else onStopTyping(conversation.id) },
            onToggleMute = { onSetMuted(conversation, !conversation.isMuted) },
            onTogglePin = { onSetPinned(conversation, !conversation.isPinned) },
            onArchive = { onSetArchived(conversation, true); selectedConversation = null },
        )
        return
    }

    if (pendingKind != null) {
        AlertDialog(
            onDismissRequest = { pendingKind = null; composeName = "" },
            title = { Text("新建${pendingKind!!.label}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = composeName, onValueChange = { composeName = it }, modifier = Modifier.testTag(TestTags.ComposeName), singleLine = true, label = { Text("名称") })
                    if (pendingKind == ConversationKind.CHANNEL) {
                        OutlinedTextField(value = composeDescription, onValueChange = { composeDescription = it }, label = { Text("描述") }, maxLines = 3)
                    }
                    if (pendingKind == ConversationKind.GROUP) {
                        Text("添加成员", color = homeSecondaryText, style = MaterialTheme.typography.bodySmall)
                        messagingState.contacts.take(12).forEach { contact ->
                            val selected = contact.id in composeParticipantIds
                            Row(Modifier.fillMaxWidth().clickable { composeParticipantIds = if (selected) composeParticipantIds - contact.id else composeParticipantIds + contact.id }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(if (selected) "●" else "○", color = if (selected) homeAccent else homeSecondaryText)
                                Text(contact.displayName, color = homePrimaryText, modifier = Modifier.padding(start = 10.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val title = composeName.trim()
                    if (title.isNotEmpty()) {
                        val kind = pendingKind!!
                        onCreateConversation(kind, title, composeDescription, composeParticipantIds.toList())
                        pendingKind = null
                        composeName = ""
                        composeDescription = ""
                        composeParticipantIds = emptySet()
                    }
                }, modifier = Modifier.testTag(TestTags.ComposeCreate), enabled = composeName.isNotBlank() && (pendingKind != ConversationKind.GROUP || composeParticipantIds.isNotEmpty())) { Text("创建") }
            },
            dismissButton = { OutlinedButton(onClick = { pendingKind = null; composeName = ""; composeDescription = ""; composeParticipantIds = emptySet() }) { Text("取消") } },
        )
    }

    LaunchedEffect(Unit) { onMessagingRefresh() }

    if (showContactPicker) {
        AlertDialog(
            onDismissRequest = { showContactPicker = false },
            title = { Text("新消息") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (messagingState.contacts.isEmpty()) Text("暂无可用联系人", color = homeSecondaryText)
                    messagingState.contacts.take(20).forEach { contact ->
                        Row(Modifier.fillMaxWidth().clickable { showContactPicker = false; onCreateDirect(contact) }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(40.dp).background(homeAccent, CircleShape), contentAlignment = Alignment.Center) { Text(contact.displayName.take(1).uppercase(), color = Color.Black, fontWeight = FontWeight.Bold) }
                            Column(Modifier.padding(start = 12.dp)) {
                                Text(contact.displayName, color = homePrimaryText)
                                Text(contact.username?.let { "@$it" } ?: contact.kind, color = homeSecondaryText, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            },
            confirmButton = { OutlinedButton(onClick = { showContactPicker = false }) { Text("取消") } },
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().testTag(TestTags.AppShell),
        containerColor = homeBackground,
        floatingActionButton = {
            if (!showSearch) Box {
                FloatingActionButton(
                    onClick = { showComposeMenu = true },
                    modifier = Modifier.testTag(TestTags.AddButton),
                    containerColor = homeAccent,
                    contentColor = Color.Black,
                ) { PlusGlyph() }
                DropdownMenu(expanded = showComposeMenu, onDismissRequest = { showComposeMenu = false }, containerColor = homeSurface) {
                    DropdownMenuItem(text = { Text("新消息", color = homePrimaryText) }, onClick = { showComposeMenu = false; showContactPicker = true })
                    DropdownMenuItem(text = { Text("新建群组", color = homePrimaryText) }, onClick = { showComposeMenu = false; pendingKind = ConversationKind.GROUP })
                    DropdownMenuItem(text = { Text("新建频道", color = homePrimaryText) }, onClick = { showComposeMenu = false; pendingKind = ConversationKind.CHANNEL })
                    DropdownMenuItem(text = { Text("联系人分组", color = homePrimaryText) }, onClick = { showComposeMenu = false })
                }
            }
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).testTag(TestTags.ConversationList)) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box {
                        ProfileAvatar(onClick = { onShowAddMenuChange(true) })
                        DropdownMenu(expanded = showAddMenu, onDismissRequest = { onShowAddMenuChange(false) }, containerColor = homeSurface) {
                            DropdownMenuItem(text = { Text("聊天", color = homePrimaryText) }, onClick = { onShowAddMenuChange(false) })
                            DropdownMenuItem(text = { Text("联系人", color = homePrimaryText) }, onClick = { onShowAddMenuChange(false); activeSection = AndroidMobileSection.CONTACTS })
                            DropdownMenuItem(text = { Text("Bots", color = homePrimaryText) }, onClick = { onShowAddMenuChange(false); activeSection = AndroidMobileSection.BOTS })
                            DropdownMenuItem(text = { Text("群组", color = homePrimaryText) }, onClick = { onShowAddMenuChange(false); activeSection = AndroidMobileSection.GROUPS })
                            DropdownMenuItem(text = { Text("频道", color = homePrimaryText) }, onClick = { onShowAddMenuChange(false); activeSection = AndroidMobileSection.CHANNELS })
                            DropdownMenuItem(text = { Text("通话", color = homePrimaryText) }, onClick = { onShowAddMenuChange(false); activeSection = AndroidMobileSection.CALLS })
                            DropdownMenuItem(text = { Text("收藏", color = homePrimaryText) }, onClick = { onShowAddMenuChange(false); activeSection = AndroidMobileSection.SAVED })
                            DropdownMenuItem(text = { Text("归档", color = homePrimaryText) }, onClick = { onShowAddMenuChange(false); activeSection = AndroidMobileSection.ARCHIVE })
                            DropdownMenuItem(text = { Text("文件夹", color = homePrimaryText) }, onClick = { onShowAddMenuChange(false); activeSection = AndroidMobileSection.FOLDERS })
                            DropdownMenuItem(text = { Text("设置", color = homePrimaryText) }, onClick = { onShowAddMenuChange(false); activeSection = AndroidMobileSection.SETTINGS })
                            DropdownMenuItem(modifier = Modifier.testTag(TestTags.MarketplaceEntry), text = { Text("Mini Apps / 插件市场", color = homePrimaryText) }, onClick = { onShowAddMenuChange(false); onOpenMarketplace() })
                            DropdownMenuItem(modifier = Modifier.testTag(TestTags.RemoteComputerEntry), text = { Text("我的电脑", color = homePrimaryText) }, onClick = { onShowAddMenuChange(false); onOpenRemoteComputer() })
                            if (updateState.phase != AndroidUpdatePhase.DISABLED) DropdownMenuItem(text = { Text("检查更新", color = homePrimaryText) }, onClick = { onShowAddMenuChange(false); onCheckUpdate() })
                        }
                    }
                    Text("聊天", color = homePrimaryText, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                    CircularActionButton(TestTags.HomeSearchButton, if (showSearch) "关闭搜索" else "搜索对话", { showSearch = !showSearch; if (!showSearch) searchQuery = "" }) { SearchGlyph() }
                }
            }
            if (showSearch) item {
                OutlinedTextField(
                    value = searchQuery, onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp).testTag(TestTags.HomeSearchField),
                    singleLine = true, placeholder = { Text("搜索", color = homeSecondaryText) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = homePrimaryText, unfocusedTextColor = homePrimaryText, focusedContainerColor = homeSurface, unfocusedContainerColor = homeSurface),
                    shape = RoundedCornerShape(14.dp),
                )
            }
            if (shouldShowUpdateBanner(updateState.phase)) item { UpdateBanner(updateState, onCheckUpdate, onInstallUpdate) }
            if (conversations.any { it.isArchived } && searchQuery.isBlank()) item {
                Row(Modifier.fillMaxWidth().clickable { activeSection = AndroidMobileSection.ARCHIVE }.padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("▣", color = homeSecondaryText); Text("已归档", color = homePrimaryText, modifier = Modifier.padding(start = 12.dp).weight(1f)); Text("${conversations.count { it.isArchived }}", color = homeSecondaryText)
                }
            }
            if (filteredConversations.isEmpty()) item {
                Column(Modifier.fillMaxWidth().padding(vertical = 88.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (searchQuery.isBlank()) "还没有对话" else "没有找到结果", color = homeSecondaryText, fontWeight = FontWeight.SemiBold)
                    Text(if (searchQuery.isBlank()) "点击右下角写消息按钮开始聊天" else "尝试其他关键词", color = homeSecondaryText, style = MaterialTheme.typography.bodySmall)
                }
            } else items(filteredConversations, key = { it.id }) { conversation ->
                ConversationRow(conversation, onClick = {
                    selectedConversation = conversation
                    onMarkRead(conversation)
                }, onLongClick = { contextConversation = conversation })
            }
            item { Spacer(Modifier.height(88.dp)) }
        }
    }
}

@Composable
private fun AndroidSectionSurface(
    section: AndroidMobileSection,
    messagingState: MessagingUiState,
    onBack: () -> Unit,
    onCreateDirect: (MessagingContact) -> Unit,
    onOpenConversation: (ConversationSummary) -> Unit,
    onUnarchive: (ConversationSummary) -> Unit,
) {
    val conversations = when (section) {
        AndroidMobileSection.GROUPS -> messagingState.conversations.filter { it.kind == ConversationKind.GROUP && !it.isArchived }
        AndroidMobileSection.CHANNELS -> messagingState.conversations.filter { it.kind == ConversationKind.CHANNEL && !it.isArchived }
        AndroidMobileSection.SAVED -> messagingState.conversations.filter { it.kind == ConversationKind.SAVED_MESSAGES }
        AndroidMobileSection.ARCHIVE -> messagingState.conversations.filter { it.isArchived }
        else -> emptyList()
    }
    val contacts = when (section) {
        AndroidMobileSection.CONTACTS -> messagingState.contacts
        AndroidMobileSection.BOTS -> messagingState.contacts.filter { it.kind == "bot" || it.kind == "assistant" }
        else -> emptyList()
    }
    Scaffold(containerColor = homeBackground) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("‹", color = homePrimaryText, fontSize = 34.sp, modifier = Modifier.clickable(onClick = onBack).padding(8.dp))
                Text(section.label, color = homePrimaryText, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
            }
            LazyColumn(Modifier.fillMaxSize()) {
                if (contacts.isNotEmpty()) items(contacts, key = { it.id }) { contact ->
                    Row(Modifier.fillMaxWidth().clickable { onCreateDirect(contact) }.padding(horizontal = 18.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(46.dp).background(homeAccent, CircleShape), contentAlignment = Alignment.Center) { Text(contact.displayName.take(1).uppercase(), color = Color.Black, fontWeight = FontWeight.Bold) }
                        Column(Modifier.padding(start = 13.dp)) { Text(contact.displayName, color = homePrimaryText); Text(contact.username?.let { "@$it" } ?: contact.kind, color = homeSecondaryText, style = MaterialTheme.typography.bodySmall) }
                    }
                }
                if (conversations.isNotEmpty()) items(conversations, key = { it.id }) { conversation ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) { ConversationRow(conversation, onClick = { onOpenConversation(conversation) }) }
                        if (section == AndroidMobileSection.ARCHIVE) Text("恢复", color = homeAccent, modifier = Modifier.clickable { onUnarchive(conversation) }.padding(14.dp))
                    }
                }
                if (contacts.isEmpty() && conversations.isEmpty()) item {
                    Column(Modifier.fillMaxWidth().padding(top = 96.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(section.label, color = homePrimaryText, fontWeight = FontWeight.SemiBold)
                        Text(
                            when (section) {
                                AndroidMobileSection.CALLS -> "暂无通话记录"
                                AndroidMobileSection.FOLDERS -> "暂无会话文件夹"
                                AndroidMobileSection.SETTINGS -> "设置由统一账户配置提供"
                                else -> "暂无内容"
                            },
                            color = homeSecondaryText,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileAvatar(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .background(homeSurface, CircleShape)
            .border(1.dp, homeBorder, CircleShape)
            .testTag(TestTags.ProfileAvatar)
            .clickable(onClick = onClick)
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


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConversationDetail(
    conversation: ConversationSummary,
    messages: List<ChatMessage>,
    onBack: () -> Unit,
    onSend: (String, String?) -> Unit,
    onSendAttachment: (String, String, ByteArray) -> Unit,
    shareContacts: List<MessagingContact>,
    onSendContact: (MessagingContact) -> Unit,
    onSendPoll: (String, List<String>, Boolean) -> Unit,
    onSendLocation: (Double, Double) -> Unit,
    onEdit: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onReact: (String, String) -> Unit,
    onForward: (String, String) -> Unit,
    forwardDestinations: List<ConversationSummary>,
    typingActorName: String?,
    onTypingChanged: (Boolean) -> Unit,
    onToggleMute: () -> Unit,
    onTogglePin: () -> Unit,
    onArchive: () -> Unit,
) {
    var draft by remember(conversation.id) { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var selectedMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var replyTarget by remember { mutableStateOf<ChatMessage?>(null) }
    var editingMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var forwardingMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var showChatSearch by remember { mutableStateOf(false) }
    var chatSearchQuery by remember { mutableStateOf("") }
    var showAttachmentMenu by remember { mutableStateOf(false) }
    var showContactShare by remember { mutableStateOf(false) }
    var showPollComposer by remember { mutableStateOf(false) }
    var pollQuestion by remember { mutableStateOf("") }
    var pollOption1 by remember { mutableStateOf("") }
    var pollOption2 by remember { mutableStateOf("") }
    var pollOption3 by remember { mutableStateOf("") }
    var attachmentMime by remember { mutableStateOf("*/*") }
    val context = LocalContext.current
    var showLocationShare by remember { mutableStateOf(false) }
    var currentLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var locationError by remember { mutableStateOf<String?>(null) }
    fun resolveLocation() {
        requestFabushiCurrentLocation(context) { latitude, longitude, error ->
            currentLocation = if (latitude != null && longitude != null) latitude to longitude else null
            locationError = error
        }
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants.values.any { it }) resolveLocation() else locationError = "请允许位置权限后再分享位置"
    }
    val attachmentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            runCatching {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@runCatching
                val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
                val name = uri.lastPathSegment?.substringAfterLast('/') ?: "attachment"
                onSendAttachment(name, mime, bytes)
            }
        }
    }

    if (showLocationShare) {
        AlertDialog(
            onDismissRequest = { showLocationShare = false }, title = { Text("发送位置") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    val location = currentLocation
                    if (location != null) {
                        Text("当前位置", color = homePrimaryText, fontWeight = FontWeight.SemiBold)
                        Text("纬度 %.6f".format(location.first), color = homeSecondaryText, modifier = Modifier.padding(top = 8.dp))
                        Text("经度 %.6f".format(location.second), color = homeSecondaryText)
                    } else {
                        Text(locationError ?: "正在获取位置…", color = homeSecondaryText)
                    }
                }
            },
            confirmButton = {
                val location = currentLocation
                Button(onClick = { if (location != null) { onSendLocation(location.first, location.second); showLocationShare = false } }, enabled = location != null) { Text("发送") }
            },
            dismissButton = { OutlinedButton(onClick = { showLocationShare = false }) { Text("取消") } },
        )
    }

    if (showContactShare) {
        AlertDialog(
            onDismissRequest = { showContactShare = false }, title = { Text("发送联系人") },
            text = {
                Column {
                    if (shareContacts.isEmpty()) Text("暂无可用联系人", color = homeSecondaryText)
                    shareContacts.take(20).forEach { contact -> TextButton(onClick = { onSendContact(contact); showContactShare = false }) { Text(contact.displayName) } }
                }
            },
            confirmButton = { OutlinedButton(onClick = { showContactShare = false }) { Text("取消") } },
        )
    }
    if (showPollComposer) {
        AlertDialog(
            onDismissRequest = { showPollComposer = false }, title = { Text("新建投票") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(pollQuestion, { pollQuestion = it }, label = { Text("问题") })
                OutlinedTextField(pollOption1, { pollOption1 = it }, label = { Text("选项 1") })
                OutlinedTextField(pollOption2, { pollOption2 = it }, label = { Text("选项 2") })
                OutlinedTextField(pollOption3, { pollOption3 = it }, label = { Text("选项 3（可选）") })
            } },
            confirmButton = { Button(onClick = { onSendPoll(pollQuestion, listOf(pollOption1, pollOption2, pollOption3), false); showPollComposer = false }, enabled = pollQuestion.isNotBlank() && pollOption1.isNotBlank() && pollOption2.isNotBlank()) { Text("发送") } },
            dismissButton = { OutlinedButton(onClick = { showPollComposer = false }) { Text("取消") } },
        )
    }

    selectedMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { selectedMessage = null },
            title = { Text("消息操作") },
            text = {
                Column {
                    TextButton(onClick = { replyTarget = message; editingMessage = null; selectedMessage = null }) { Text("回复") }
                    TextButton(onClick = { forwardingMessage = message; selectedMessage = null }) { Text("转发") }
                    TextButton(onClick = { onReact(message.id, "👍"); selectedMessage = null }) { Text("👍 赞") }
                    if (message.outgoing) TextButton(onClick = { editingMessage = message; replyTarget = null; draft = message.text; selectedMessage = null }) { Text("编辑") }
                    TextButton(onClick = { onDelete(message.id); selectedMessage = null }) { Text("删除") }
                }
            },
            confirmButton = { OutlinedButton(onClick = { selectedMessage = null }) { Text("取消") } },
        )
    }

    forwardingMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { forwardingMessage = null },
            title = { Text("转发到") },
            text = {
                Column {
                    if (forwardDestinations.isEmpty()) Text("暂无其他会话", color = homeSecondaryText)
                    forwardDestinations.take(20).forEach { destination ->
                        TextButton(onClick = { onForward(message.id, destination.id); forwardingMessage = null }) { Text(destination.title) }
                    }
                }
            },
            confirmButton = { OutlinedButton(onClick = { forwardingMessage = null }) { Text("取消") } },
        )
    }

    Scaffold(containerColor = homeBackground) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("‹", color = homePrimaryText, fontSize = 34.sp, modifier = Modifier.clickable(onClick = onBack).padding(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(conversation.title, color = homePrimaryText, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                    Text(conversation.kind.label, color = homeSecondaryText, style = MaterialTheme.typography.bodySmall)
                }
                Text("⌕", color = homePrimaryText, fontSize = 23.sp, modifier = Modifier.clickable { showChatSearch = !showChatSearch; if (!showChatSearch) chatSearchQuery = "" }.padding(8.dp))
                Box {
                    Text("⋯", color = homePrimaryText, fontSize = 28.sp, modifier = Modifier.clickable { showMenu = true }.padding(8.dp))
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, containerColor = homeSurface) {
                        DropdownMenuItem(text = { Text(if (conversation.isMuted) "取消静音" else "静音", color = homePrimaryText) }, onClick = { showMenu = false; onToggleMute() })
                        DropdownMenuItem(text = { Text(if (conversation.isPinned) "取消置顶" else "置顶", color = homePrimaryText) }, onClick = { showMenu = false; onTogglePin() })
                        DropdownMenuItem(text = { Text("归档", color = homePrimaryText) }, onClick = { showMenu = false; onArchive() })
                    }
                }
            }
            if (showChatSearch) {
                OutlinedTextField(
                    value = chatSearchQuery, onValueChange = { chatSearchQuery = it }, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    singleLine = true, placeholder = { Text("搜索此聊天", color = homeSecondaryText) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = homePrimaryText, unfocusedTextColor = homePrimaryText, focusedContainerColor = homeSurface, unfocusedContainerColor = homeSurface),
                    shape = RoundedCornerShape(14.dp),
                )
            }
            if (typingActorName != null) {
                Text("$typingActorName 正在输入…", color = homeSecondaryText, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp))
            }
            LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (messages.isEmpty()) item { Text("开始与 ${conversation.title} 对话", color = homeSecondaryText, modifier = Modifier.fillMaxWidth().padding(top = 72.dp)) }
                items(messages.filter { chatSearchQuery.isBlank() || it.text.contains(chatSearchQuery, ignoreCase = true) }, key = { it.id }) { message ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.outgoing) Arrangement.End else Arrangement.Start) {
                        Column(
                            Modifier.fillMaxWidth(0.78f)
                                .background(if (message.outgoing) homeAccent.copy(alpha = 0.18f) else homeSurface, RoundedCornerShape(16.dp))
                                .combinedClickable(onClick = {}, onLongClick = { selectedMessage = message })
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            if (message.forwardOrigin != null) Text("↪ 转发自 ${message.forwardOrigin}", color = homeAccent, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            val replied = message.replyToMessageId?.let { replyId -> messages.firstOrNull { it.id == replyId } }
                            if (replied != null) {
                                Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Text("回复", color = homeAccent, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    Text(replied.text, color = homeSecondaryText, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            when (message.contentType) {
                                "contact" -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(40.dp).background(homeAccent, CircleShape), contentAlignment = Alignment.Center) { Text((message.contactName ?: "联").take(1), color = Color.Black, fontWeight = FontWeight.Bold) }
                                    Column(Modifier.padding(start = 10.dp)) { Text(message.contactName ?: "联系人", color = homePrimaryText, fontWeight = FontWeight.SemiBold); Text("联系人", color = homeSecondaryText, style = MaterialTheme.typography.bodySmall) }
                                }
                                "location" -> Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("📍 位置", color = homePrimaryText, fontWeight = FontWeight.SemiBold)
                                    if (message.latitude != null && message.longitude != null) Text("%.6f, %.6f".format(message.latitude, message.longitude), color = homeSecondaryText, style = MaterialTheme.typography.bodySmall)
                                }
                                "poll" -> Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text(message.pollQuestion ?: "投票", color = homePrimaryText, fontWeight = FontWeight.SemiBold)
                                    message.pollOptions.forEach { option -> Row(verticalAlignment = Alignment.CenterVertically) { Text("○", color = homeAccent); Text(option, color = homePrimaryText, modifier = Modifier.padding(start = 7.dp)) } }
                                }
                                "photo", "video", "document" -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text(if (message.contentType == "photo") "🖼" else if (message.contentType == "video") "🎬" else "📎", fontSize = 24.sp)
                                    Column(Modifier.padding(start = 10.dp)) { Text(message.mediaFileName ?: message.text, color = homePrimaryText, fontWeight = FontWeight.Medium); Text(if (message.contentType == "photo") "图片" else if (message.contentType == "video") "视频" else "文件", color = homeSecondaryText, style = MaterialTheme.typography.bodySmall) }
                                }
                                else -> Text(message.text, color = homePrimaryText)
                            }
                            if (message.reactions.isNotEmpty()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.padding(top = 5.dp)) {
                                    message.reactions.take(5).forEach { reaction ->
                                        Text("${reaction.reaction} ${reaction.count}", color = homePrimaryText, fontSize = 11.sp, modifier = Modifier.background(if (reaction.chosenByMe) homeAccent.copy(alpha = 0.25f) else homeBorder, RoundedCornerShape(10.dp)).padding(horizontal = 7.dp, vertical = 3.dp))
                                    }
                                }
                            }
                            val check = when {
                                message.deliveryState.contains("read", true) -> "✓✓"
                                message.deliveryState.contains("deliver", true) -> "✓✓"
                                else -> "✓"
                            }
                            Text((if (message.edited) "已编辑  " else "") + message.time + if (message.outgoing) "  $check" else "", color = if (message.outgoing && message.deliveryState.contains("read", true)) homeAccent else homeSecondaryText, style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.End))
                        }
                    }
                }
            }
            if (editingMessage != null || replyTarget != null) {
                Row(Modifier.fillMaxWidth().background(homeSurface).padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (editingMessage != null) "✎" else "↩", color = homeAccent, fontSize = 20.sp)
                    Column(Modifier.weight(1f).padding(start = 10.dp)) {
                        Text(if (editingMessage != null) "编辑消息" else "回复", color = homeAccent, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Text((editingMessage ?: replyTarget)?.text.orEmpty(), color = homeSecondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text("×", color = homeSecondaryText, fontSize = 24.sp, modifier = Modifier.clickable { editingMessage = null; replyTarget = null; if (draft.isNotEmpty()) draft = "" }.padding(6.dp))
                }
            }
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.Bottom) {
                Box {
                    Text("＋", color = homeSecondaryText, fontSize = 26.sp, modifier = Modifier.clickable { showAttachmentMenu = true }.padding(8.dp))
                    DropdownMenu(expanded = showAttachmentMenu, onDismissRequest = { showAttachmentMenu = false }, containerColor = homeSurface) {
                        DropdownMenuItem(text = { Text("照片", color = homePrimaryText) }, onClick = { showAttachmentMenu = false; attachmentMime = "image/*"; attachmentLauncher.launch(attachmentMime) })
                        DropdownMenuItem(text = { Text("视频", color = homePrimaryText) }, onClick = { showAttachmentMenu = false; attachmentMime = "video/*"; attachmentLauncher.launch(attachmentMime) })
                        DropdownMenuItem(text = { Text("文件", color = homePrimaryText) }, onClick = { showAttachmentMenu = false; attachmentMime = "*/*"; attachmentLauncher.launch(attachmentMime) })
                        DropdownMenuItem(text = { Text("位置", color = homePrimaryText) }, onClick = {
                            showAttachmentMenu = false; showLocationShare = true; currentLocation = null; locationError = null
                            val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            if (fine || coarse) resolveLocation() else locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                        })
                        DropdownMenuItem(text = { Text("联系人", color = homePrimaryText) }, onClick = { showAttachmentMenu = false; showContactShare = true })
                        DropdownMenuItem(text = { Text("投票", color = homePrimaryText) }, onClick = { showAttachmentMenu = false; pollQuestion = ""; pollOption1 = ""; pollOption2 = ""; pollOption3 = ""; showPollComposer = true })
                    }
                }
                OutlinedTextField(
                    value = draft, onValueChange = { draft = it; onTypingChanged(it.isNotBlank()) }, modifier = Modifier.weight(1f), placeholder = { Text("消息", color = homeSecondaryText) }, maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = homePrimaryText, unfocusedTextColor = homePrimaryText, focusedContainerColor = homeSurface, unfocusedContainerColor = homeSurface), shape = RoundedCornerShape(22.dp),
                )
                Text(if (draft.isBlank()) "●" else "➤", color = if (draft.isBlank()) homeSecondaryText else homeAccent, fontSize = 22.sp,
                    modifier = Modifier.clickable {
                        val text = draft.trim()
                        if (text.isNotEmpty()) {
                            val edit = editingMessage
                            if (edit != null) onEdit(edit.id, text) else onSend(text, replyTarget?.id)
                            draft = ""; onTypingChanged(false); editingMessage = null; replyTarget = null
                        }
                    }.padding(10.dp))
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun requestFabushiCurrentLocation(context: Context, callback: (Double?, Double?, String?) -> Unit) {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        ?: return callback(null, null, "设备不支持位置服务")
    val provider = when {
        manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
        manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
        else -> return callback(null, null, "请先开启系统位置服务")
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        manager.getCurrentLocation(provider, CancellationSignal(), context.mainExecutor) { location ->
            if (location == null) callback(null, null, "暂时无法获取当前位置") else callback(location.latitude, location.longitude, null)
        }
    } else {
        val location = manager.getLastKnownLocation(provider)
        if (location == null) callback(null, null, "暂时无法获取当前位置") else callback(location.latitude, location.longitude, null)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConversationRow(conversation: ConversationSummary, onClick: () -> Unit, onLongClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = 30.dp, end = 24.dp, top = 13.dp, bottom = 13.dp)
            ,
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    conversation.title, color = homePrimaryText, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                )
                if (conversation.isMuted) Text("⌁", color = homeSecondaryText, fontSize = 12.sp)
                if (conversation.isPinned) Text("⌖", color = homeSecondaryText, fontSize = 12.sp)
            }
            Text(
                conversation.preview,
                color = homeSecondaryText,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(conversation.time, color = Color(0xFF56565B), style = MaterialTheme.typography.bodySmall)
            if (conversation.unreadCount > 0) {
                Text("${conversation.unreadCount}", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.background(homeAccent, CircleShape).padding(horizontal = 6.dp, vertical = 2.dp))
            }
        }
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
