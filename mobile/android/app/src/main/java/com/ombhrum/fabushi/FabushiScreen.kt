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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

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
    const val MobileOnboarding = "mobile-onboarding"
    const val MobileOnboardingContinue = "mobile-onboarding-continue"
    const val MobileOnboardingSkip = "mobile-onboarding-skip"
    const val MobileLogin = "mobile-login"
    const val MobileLoginBrowser = "mobile-login-browser"
    const val MobileLoginReopen = "mobile-login-reopen"
    const val MobileLoginCancel = "mobile-login-cancel"
    const val MobileLogout = "mobile-logout"
    const val MahayanaAgentEntry = "mahayana-agent-entry"
    const val MahayanaAgentChat = "mahayana-agent-chat"
    const val MahayanaSend = "mahayana-send"
    const val MahayanaStop = "mahayana-stop"
    const val MahayanaThinking = "mahayana-thinking"
    const val MahayanaStep = "mahayana-step"
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
internal val homeBackground = Color(0xFF0B0B0C)
internal val homeSurface = Color(0xFF151516)
internal val homeBorder = Color(0xFF29292B)
internal val homePrimaryText = Color(0xFFF3F3F4)
internal val homeSecondaryText = Color(0xFF8C8C91)
internal val homeAccent = Color(0xFFFFB21A)
internal val conversationAccent = Color(0xFFFF5A0A)

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
    messagingActorId: String = "",
    onMessagingRefresh: () -> Unit = {},
    onCreateDirect: (MessagingContact) -> Unit = {},
    onCreateConversation: (ConversationKind, String, String, List<String>) -> Unit = { _, _, _, _ -> },
    onSendText: (String, String, String?, Boolean, Long?) -> Unit = { _, _, _, _, _ -> },
    onSendAttachment: (String, String, String, ByteArray) -> Unit = { _, _, _, _ -> },
    onSendVoice: (String, String, String, ByteArray, List<Int>) -> Unit = { _, _, _, _, _ -> },
    onLoadBlob: (String, Int, (Result<ByteArray>) -> Unit) -> Unit = { _, _, callback -> callback(Result.failure(IllegalStateException("Blob loader unavailable"))) },
    onSendContact: (String, MessagingContact) -> Unit = { _, _ -> },
    onSendPoll: (String, String, List<String>, Boolean) -> Unit = { _, _, _, _ -> },
    onVotePoll: (String, String, List<String>) -> Unit = { _, _, _ -> },
    onSendLocation: (String, Double, Double) -> Unit = { _, _, _ -> },
    onEditText: (String, String, String) -> Unit = { _, _, _ -> },
    onDeleteMessage: (String, String) -> Unit = { _, _ -> },
    onSetMessagePinned: (String, String, Boolean) -> Unit = { _, _, _ -> },
    onSetReaction: (String, String, String, Boolean) -> Unit = { _, _, _, _ -> },
    onForwardMessage: (String, String, String) -> Unit = { _, _, _ -> },
    onStartTyping: (String) -> Unit = {},
    onStopTyping: (String) -> Unit = {},
    onSetPinned: (ConversationSummary, Boolean) -> Unit = { _, _ -> },
    onSetArchived: (ConversationSummary, Boolean) -> Unit = { _, _ -> },
    onSetMuted: (ConversationSummary, Boolean) -> Unit = { _, _ -> },
    onMarkRead: (ConversationSummary) -> Unit = {},
    onSetMarkedUnread: (ConversationSummary, Boolean) -> Unit = { _, _ -> },
    onSetDraft: (String, String, String?) -> Unit = { _, _, _ -> },
    onUpdateConversationInfo: (String, String, String) -> Unit = { _, _, _ -> },
    onSetConversationParticipant: (ConversationSummary, String, String) -> Unit = { _, _, _ -> },
    onRemoveConversationParticipant: (String, String) -> Unit = { _, _ -> },
    onUpsertFolder: (MessagingFolder) -> Unit = {},
    onDeleteFolder: (String) -> Unit = {},
    appAgentSurface: FabushiAppAgentSurface? = null,
    authGateEnabled: Boolean = false,
    onAdvanceOnboarding: () -> Unit = {},
    onSkipOnboarding: () -> Unit = {},
    onBeginBrowserLogin: () -> Unit = {},
    onReopenBrowserLogin: () -> Unit = {},
    onCancelBrowserLogin: () -> Unit = {},
    onLogout: () -> Unit = {},
    onChatDraftChange: (String) -> Unit = {},
    onSendChat: () -> Unit = {},
    onStopChat: () -> Unit = {},
) {
    var destination by remember { mutableStateOf(MobileDestination.HOME) }
    var showAddMenu by remember { mutableStateOf(false) }
    var showAgentChat by remember { mutableStateOf(false) }
    var showHomeSearch by remember { mutableStateOf(false) }
    var homeSearchQuery by remember { mutableStateOf("") }

    if (authGateEnabled && state.onboardingStep < 3) {
        MobileOnboarding(state.onboardingStep, onAdvanceOnboarding, onSkipOnboarding)
        return
    }
    if (authGateEnabled && !state.authResolved) {
        MobileAuthLoading()
        return
    }
    if (authGateEnabled && !state.loggedIn) {
        MobileLogin(state, onBeginBrowserLogin, onReopenBrowserLogin, onCancelBrowserLogin)
        return
    }
    if (showAgentChat) {
        MobileAgentChat(state, onChatDraftChange, onSendChat, onStopChat) { showAgentChat = false }
        return
    }

    LaunchedEffect(destination, showAddMenu, showHomeSearch, homeSearchQuery, state, updateState.phase, appAgentSurface) {
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
                element(
                    TestTags.HomeSearchButton,
                    "button",
                    if (showHomeSearch) "关闭搜索" else "搜索对话",
                    action = FabushiAppAgentSurface.Action(setOf("invoke")) {
                        showHomeSearch = !showHomeSearch
                        if (!showHomeSearch) homeSearchQuery = ""
                    },
                )
                if (showHomeSearch) {
                    element(
                        TestTags.HomeSearchField,
                        "textbox",
                        "搜索对话",
                        action = FabushiAppAgentSurface.Action(setOf("setValue")) { homeSearchQuery = it.orEmpty() },
                    )
                }
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
            showSearch = showHomeSearch,
            searchQuery = homeSearchQuery,
            onShowSearchChange = { visible ->
                showHomeSearch = visible
                if (!visible) homeSearchQuery = ""
            },
            onSearchQueryChange = { homeSearchQuery = it },
            messagingState = messagingState,
            messagingActorId = messagingActorId,
            onMessagingRefresh = onMessagingRefresh,
            onCreateDirect = onCreateDirect,
            onCreateConversation = onCreateConversation,
            onSendText = onSendText,
            onSendAttachment = onSendAttachment,
            onSendVoice = onSendVoice,
            onLoadBlob = onLoadBlob,
            onSendContact = onSendContact,
            onSendPoll = onSendPoll,
            onVotePoll = onVotePoll,
            onSendLocation = onSendLocation,
            onEditText = onEditText,
            onDeleteMessage = onDeleteMessage,
            onSetMessagePinned = onSetMessagePinned,
            onSetReaction = onSetReaction,
            onForwardMessage = onForwardMessage,
            onStartTyping = onStartTyping,
            onStopTyping = onStopTyping,
            onSetPinned = onSetPinned,
            onSetArchived = onSetArchived,
            onSetMuted = onSetMuted,
            onMarkRead = onMarkRead,
            onSetMarkedUnread = onSetMarkedUnread,
            onSetDraft = onSetDraft,
            onUpdateConversationInfo = onUpdateConversationInfo,
            onSetConversationParticipant = onSetConversationParticipant,
            onRemoveConversationParticipant = onRemoveConversationParticipant,
            onUpsertFolder = onUpsertFolder,
            onDeleteFolder = onDeleteFolder,
            onOpenAgentChat = { showAgentChat = true },
            onLogout = onLogout,
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
private fun MobileOnboarding(step: Int, onContinue: () -> Unit, onSkip: () -> Unit) {
    Box(Modifier.fillMaxSize().background(homeBackground).testTag(TestTags.MobileOnboarding), contentAlignment = Alignment.Center) {
        Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Spacer(Modifier.height(42.dp))
            Box(Modifier.size(92.dp).background(homeSurface, CircleShape).border(1.dp, homeBorder, CircleShape), contentAlignment = Alignment.Center) {
                Text("✦", color = homeAccent, fontSize = 48.sp, fontWeight = FontWeight.Bold)
            }
            Text("欢迎来到法布施", color = homePrimaryText, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("统一的聊天、插件和 Mahayana 多步骤智能体工作台。每一步工作都会像消息一样实时出现。", color = homeSecondaryText, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(3) { index -> Box(Modifier.size(width = 24.dp, height = 5.dp).background(if (index <= step) homeAccent else homeBorder, RoundedCornerShape(8.dp))) }
            }
            Spacer(Modifier.weight(1f))
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().testTag(TestTags.MobileOnboardingContinue), colors = ButtonDefaults.buttonColors(containerColor = homeAccent, contentColor = Color.Black)) {
                Text(if (step >= 2) "开始使用" else "继续")
            }
            TextButton(onClick = onSkip, modifier = Modifier.testTag(TestTags.MobileOnboardingSkip)) { Text("跳过介绍", color = homeSecondaryText) }
        }
    }
}

@Composable
private fun MobileAuthLoading() {
    Box(Modifier.fillMaxSize().background(Color(0xFFFAFAF7)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(70.dp).background(Color(0xFF202020), CircleShape), contentAlignment = Alignment.Center) {
                Text("✦", color = Color(0xFFFF9D22), fontSize = 38.sp, fontWeight = FontWeight.Bold)
            }
            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(28.dp))
            Text("正在连接 Fabushi…", color = Color.Black.copy(alpha = 0.58f))
        }
    }
}

@Composable
private fun LoginBlob(
    color: Color,
    width: Dp,
    height: Dp,
    rotation: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .size(width = width, height = height)
            .rotate(rotation)
            .background(color, RoundedCornerShape(percent = 42)),
        contentAlignment = Alignment.Center,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(width = 11.dp, height = 25.dp).background(Color.White, RoundedCornerShape(99.dp)))
            Box(Modifier.size(width = 11.dp, height = 25.dp).background(Color.White, RoundedCornerShape(99.dp)))
        }
    }
}

@Composable
private fun MobileLogin(
    state: MarketplaceUiState,
    onBegin: () -> Unit,
    onReopen: () -> Unit,
    onCancel: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Color(0xFFFAFAF7)).testTag(TestTags.MobileLogin)) {
        LoginBlob(Color(0xFFFF920C), 92.dp, 92.dp, -8f, Modifier.align(Alignment.TopStart).offset(x = 66.dp, y = 100.dp))
        LoginBlob(Color(0xFF8A4CFF), 58.dp, 66.dp, 12f, Modifier.align(Alignment.TopEnd).offset(x = (-88).dp, y = 116.dp))
        LoginBlob(Color(0xFF00C978), 86.dp, 70.dp, 6f, Modifier.align(Alignment.CenterEnd).offset(x = 42.dp, y = (-120).dp))
        LoginBlob(Color(0xFF1585F7), 86.dp, 66.dp, 7f, Modifier.align(Alignment.CenterStart).offset(x = (-36).dp, y = (-74).dp))
        LoginBlob(Color(0xFF00BCAE), 72.dp, 72.dp, -9f, Modifier.align(Alignment.BottomStart).offset(x = 52.dp, y = (-142).dp))
        LoginBlob(Color(0xFFFF253F), 94.dp, 84.dp, 8f, Modifier.align(Alignment.BottomCenter).offset(x = 34.dp, y = (-112).dp))
        LoginBlob(Color(0xFFA66C37), 64.dp, 64.dp, 17f, Modifier.align(Alignment.BottomEnd).offset(x = 22.dp, y = (-208).dp))

        Column(
            Modifier.fillMaxSize().navigationBarsPadding().padding(horizontal = 28.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            Text("Fabushi", color = Color.Black, fontSize = 48.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1.2).sp)
            Spacer(Modifier.height(14.dp))
            Text(
                "你的常驻智能体团队，持续完成工作。",
                color = Color.Black.copy(alpha = 0.42f),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.weight(1f))

            if (state.loginError != null) {
                Text(
                    "登录暂时不可用，请稍后重试。",
                    color = Color(0xFFD14343),
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }

            if (state.browserLoginAttemptId != null) {
                Button(
                    onClick = onReopen,
                    modifier = Modifier.fillMaxWidth().height(58.dp).testTag(TestTags.MobileLoginReopen),
                    shape = RoundedCornerShape(30.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF171717), contentColor = Color.White),
                ) { Text("继续登录", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                TextButton(onClick = onCancel, modifier = Modifier.testTag(TestTags.MobileLoginCancel)) {
                    Text("取消登录", color = Color.Black.copy(alpha = 0.46f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Button(
                    onClick = onBegin,
                    enabled = !state.loginBusy,
                    modifier = Modifier.fillMaxWidth().height(58.dp).testTag(TestTags.MobileLoginBrowser),
                    shape = RoundedCornerShape(30.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF171717), contentColor = Color.White),
                ) {
                    if (state.loginBusy) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("登录", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun MobileAgentChat(
    state: MarketplaceUiState,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onClose: () -> Unit,
) {
    Scaffold(containerColor = homeBackground, modifier = Modifier.fillMaxSize().testTag(TestTags.MahayanaAgentChat)) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("‹", color = homePrimaryText, fontSize = 34.sp, modifier = Modifier.clickable(onClick = onClose).padding(8.dp))
                Column(Modifier.weight(1f).padding(start = 4.dp)) {
                    Text("大乘助手", color = homePrimaryText, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                    Text(if (state.chatBusy) "正在工作" else "Mahayana 多步骤智能体", color = if (state.chatBusy) homeAccent else homeSecondaryText, fontSize = 12.sp)
                }
            }
            LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                if (state.chatMessages.isEmpty()) item {
                    Column(Modifier.fillMaxWidth().padding(top = 92.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Box(Modifier.size(68.dp).background(homeSurface, CircleShape), contentAlignment = Alignment.Center) { Text("✦", color = homeAccent, fontSize = 36.sp, fontWeight = FontWeight.Bold) }
                        Text("大乘助手", color = homePrimaryText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("真实的模型路由、工具调用和每一步工作会逐条显示在这里。", color = homeSecondaryText, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
                items(state.chatMessages, key = { it.id }) { entry -> MobileAgentChatEntry(entry) }
            }
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = state.chatDraft, onValueChange = onDraftChange, modifier = Modifier.weight(1f), enabled = !state.chatBusy, placeholder = { Text("消息大乘助手") }, maxLines = 5)
                if (state.chatBusy) {
                    Button(onClick = onStop, modifier = Modifier.size(52.dp).testTag(TestTags.MahayanaStop), contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE44F61))) { Text("■", color = Color.White) }
                } else {
                    Button(onClick = onSend, enabled = state.chatDraft.trim().isNotEmpty(), modifier = Modifier.size(52.dp).testTag(TestTags.MahayanaSend), contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp), colors = ButtonDefaults.buttonColors(containerColor = homeAccent, contentColor = Color.Black)) { Text("↑", fontSize = 22.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
private fun MobileAgentChatEntry(entry: MobileChatMessage) {
    when {
        entry.kind == MobileChatEntryKind.THINKING -> Row(Modifier.fillMaxWidth().background(homeSurface, RoundedCornerShape(13.dp)).border(1.dp, Color(0xFF6F5BC6), RoundedCornerShape(13.dp)).padding(10.dp).testTag(TestTags.MahayanaThinking), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Box(Modifier.size(30.dp).background(homeBackground, CircleShape), contentAlignment = Alignment.Center) { Text("✦", color = homeAccent, fontSize = 18.sp) }
            Column(Modifier.weight(1f)) { Text(entry.actionTitle ?: "正在思考", color = homePrimaryText, fontWeight = FontWeight.SemiBold); Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(color = homeAccent, modifier = Modifier.size(13.dp), strokeWidth = 2.dp); Text("Mahayana 正在处理…", color = homeSecondaryText, fontSize = 12.sp) } }
        }
        entry.kind == MobileChatEntryKind.ACTION -> Row(Modifier.fillMaxWidth().background(homeSurface, RoundedCornerShape(11.dp)).padding(horizontal = 10.dp, vertical = 8.dp).testTag(TestTags.MahayanaStep), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(25.dp).background(homeBackground, CircleShape), contentAlignment = Alignment.Center) { Text("✦", color = homeAccent, fontSize = 15.sp) }
            Column(Modifier.weight(1f)) { Text(entry.actionTitle ?: "助手动作", color = homePrimaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold); entry.actionDetail?.let { if (it.isNotBlank()) Text(it, color = homeSecondaryText, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis) } }
            Text(if (entry.actionStatus == "failed") "失败" else if (entry.actionStatus == "running") "进行中" else "完成", color = if (entry.actionStatus == "failed") Color(0xFFFF6B6B) else if (entry.actionStatus == "running") homeAccent else Color(0xFF65D6A0), fontSize = 11.sp)
        }
        entry.role == MobileChatRole.USER -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { Text(entry.text, color = Color.White, modifier = Modifier.background(Color.Black, RoundedCornerShape(16.dp)).padding(horizontal = 13.dp, vertical = 10.dp)) }
        else -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.Top) { Text("✦", color = homeAccent, modifier = Modifier.padding(7.dp)); Text(entry.text, color = homePrimaryText, modifier = Modifier.background(homeSurface, RoundedCornerShape(16.dp)).padding(horizontal = 13.dp, vertical = 10.dp)) }
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
    showSearch: Boolean,
    searchQuery: String,
    onShowSearchChange: (Boolean) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    messagingState: MessagingUiState,
    messagingActorId: String,
    onMessagingRefresh: () -> Unit,
    onCreateDirect: (MessagingContact) -> Unit,
    onCreateConversation: (ConversationKind, String, String, List<String>) -> Unit,
    onSendText: (String, String, String?, Boolean, Long?) -> Unit,
    onSendAttachment: (String, String, String, ByteArray) -> Unit,
    onSendVoice: (String, String, String, ByteArray, List<Int>) -> Unit,
    onLoadBlob: (String, Int, (Result<ByteArray>) -> Unit) -> Unit,
    onSendContact: (String, MessagingContact) -> Unit,
    onSendPoll: (String, String, List<String>, Boolean) -> Unit,
    onVotePoll: (String, String, List<String>) -> Unit,
    onSendLocation: (String, Double, Double) -> Unit,
    onEditText: (String, String, String) -> Unit,
    onDeleteMessage: (String, String) -> Unit,
    onSetMessagePinned: (String, String, Boolean) -> Unit,
    onSetReaction: (String, String, String, Boolean) -> Unit,
    onForwardMessage: (String, String, String) -> Unit,
    onStartTyping: (String) -> Unit,
    onStopTyping: (String) -> Unit,
    onSetPinned: (ConversationSummary, Boolean) -> Unit,
    onSetArchived: (ConversationSummary, Boolean) -> Unit,
    onSetMuted: (ConversationSummary, Boolean) -> Unit,
    onMarkRead: (ConversationSummary) -> Unit,
    onSetMarkedUnread: (ConversationSummary, Boolean) -> Unit,
    onSetDraft: (String, String, String?) -> Unit,
    onUpdateConversationInfo: (String, String, String) -> Unit,
    onSetConversationParticipant: (ConversationSummary, String, String) -> Unit,
    onRemoveConversationParticipant: (String, String) -> Unit,
    onUpsertFolder: (MessagingFolder) -> Unit,
    onDeleteFolder: (String) -> Unit,
    onOpenAgentChat: () -> Unit,
    onLogout: () -> Unit,
) {
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
                    TextButton(onClick = { onSetMarkedUnread(conversation, !conversation.markedUnread); contextConversation = null }) { Text(if (conversation.markedUnread) "取消标为未读" else "标为未读") }
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
            onUpsertFolder = onUpsertFolder,
            onDeleteFolder = onDeleteFolder,
        )
        return
    }

    selectedConversation?.let { selected ->
        val conversation = conversations.firstOrNull { it.id == selected.id } ?: selected
        ConversationDetail(
            conversation = conversation,
            messages = messagingState.messagesByConversation[conversation.id].orEmpty(),
            sharedDraft = messagingState.draftsByConversation[conversation.id],
            onDraftChanged = { text, replyTo -> onSetDraft(conversation.id, text, replyTo) },
            currentActorId = messagingActorId,
            contacts = messagingState.contacts,
            onUpdateConversationInfo = { title, description -> onUpdateConversationInfo(conversation.id, title, description) },
            onSetConversationParticipant = { actorId, role -> onSetConversationParticipant(conversation, actorId, role) },
            onRemoveConversationParticipant = { actorId -> onRemoveConversationParticipant(conversation.id, actorId) },
            onBack = { selectedConversation = null },
            onSend = { text, replyTo, silent, scheduledAt -> onSendText(conversation.id, text, replyTo, silent, scheduledAt) },
            onSendAttachment = { fileName, mimeType, bytes -> onSendAttachment(conversation.id, fileName, mimeType, bytes) },
            onSendVoice = { fileName, mimeType, bytes, waveform -> onSendVoice(conversation.id, fileName, mimeType, bytes, waveform) },
            onLoadBlob = onLoadBlob,
            shareContacts = messagingState.contacts,
            onSendContact = { contact -> onSendContact(conversation.id, contact) },
            onSendPoll = { question, options, multiple -> onSendPoll(conversation.id, question, options, multiple) },
            onVotePoll = { messageId, optionIds -> onVotePoll(conversation.id, messageId, optionIds) },
            onSendLocation = { latitude, longitude -> onSendLocation(conversation.id, latitude, longitude) },
            onEdit = { messageId, text -> onEditText(conversation.id, messageId, text) },
            onDelete = { messageId -> onDeleteMessage(conversation.id, messageId) },
            onSetMessagePinned = { messageId, pinned -> onSetMessagePinned(conversation.id, messageId, pinned) },
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
                            DropdownMenuItem(modifier = Modifier.testTag(TestTags.MobileLogout), text = { Text("退出登录", color = Color(0xFFFF6B6B)) }, onClick = { onShowAddMenuChange(false); onLogout() })
                            if (updateState.phase != AndroidUpdatePhase.DISABLED) DropdownMenuItem(text = { Text("检查更新", color = homePrimaryText) }, onClick = { onShowAddMenuChange(false); onCheckUpdate() })
                        }
                    }
                    Text("聊天", color = homePrimaryText, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                    CircularActionButton(TestTags.HomeSearchButton, if (showSearch) "关闭搜索" else "搜索对话", { onShowSearchChange(!showSearch) }) { SearchGlyph() }
                }
            }
            if (showSearch) item {
                OutlinedTextField(
                    value = searchQuery, onValueChange = onSearchQueryChange,
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
            if (searchQuery.isBlank()) item {
                Row(
                    Modifier.fillMaxWidth().testTag(TestTags.MahayanaAgentEntry).clickable(onClick = onOpenAgentChat).padding(horizontal = 18.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(54.dp).background(homeSurface, CircleShape).border(1.dp, homeBorder, CircleShape), contentAlignment = Alignment.Center) {
                        Text("✦", color = homeAccent, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.padding(start = 13.dp).weight(1f)) {
                        Text("大乘助手", color = homePrimaryText, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                        Text("Mahayana 多步骤智能体 · 实时工作流", color = homeSecondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text("›", color = homeSecondaryText, fontSize = 24.sp)
                }
            }
            if (filteredConversations.isEmpty() && searchQuery.isNotBlank()) item {
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
    onUpsertFolder: (MessagingFolder) -> Unit,
    onDeleteFolder: (String) -> Unit,
) {
    var showFolderEditor by remember { mutableStateOf(false) }
    var folderTitle by remember { mutableStateOf("") }
    var folderConversationIds by remember { mutableStateOf(setOf<String>()) }
    var folderIncludeGroups by remember { mutableStateOf(false) }
    var folderIncludeChannels by remember { mutableStateOf(false) }
    var openedFolder by remember { mutableStateOf<MessagingFolder?>(null) }

    if (showFolderEditor) {
        AlertDialog(
            onDismissRequest = { showFolderEditor = false },
            title = { Text("新建文件夹") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(folderTitle, { folderTitle = it }, label = { Text("文件夹名称") }, singleLine = true)
                    Row(Modifier.fillMaxWidth().clickable { folderIncludeGroups = !folderIncludeGroups }.padding(vertical = 4.dp)) { Text(if (folderIncludeGroups) "●" else "○", color = homeAccent); Text("自动包含群组", color = homePrimaryText, modifier = Modifier.padding(start = 8.dp)) }
                    Row(Modifier.fillMaxWidth().clickable { folderIncludeChannels = !folderIncludeChannels }.padding(vertical = 4.dp)) { Text(if (folderIncludeChannels) "●" else "○", color = homeAccent); Text("自动包含频道", color = homePrimaryText, modifier = Modifier.padding(start = 8.dp)) }
                    Text("选择会话", color = homeSecondaryText, style = MaterialTheme.typography.bodySmall)
                    messagingState.conversations.filter { !it.isArchived }.take(14).forEach { conversation ->
                        val selected = conversation.id in folderConversationIds
                        Row(Modifier.fillMaxWidth().clickable { folderConversationIds = if (selected) folderConversationIds - conversation.id else folderConversationIds + conversation.id }.padding(vertical = 4.dp)) {
                            Text(if (selected) "●" else "○", color = if (selected) homeAccent else homeSecondaryText); Text(conversation.title, color = homePrimaryText, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onUpsertFolder(MessagingFolder(id = "folder-${System.nanoTime()}", title = folderTitle.trim(), icon = "folder", conversationIds = folderConversationIds.toList(), includeGroups = folderIncludeGroups, includeChannels = folderIncludeChannels, excludeArchived = true))
                    showFolderEditor = false
                }, enabled = folderTitle.isNotBlank()) { Text("创建") }
            },
            dismissButton = { OutlinedButton(onClick = { showFolderEditor = false }) { Text("取消") } },
        )
    }

    openedFolder?.let { folder ->
        val rows = messagingState.conversations.filter { conversation ->
            (!conversation.isArchived || !folder.excludeArchived) && (!conversation.isMuted || !folder.excludeMuted) && (conversation.unreadCount > 0 || !folder.excludeRead) &&
                (conversation.id in folder.conversationIds || (folder.includeGroups && conversation.kind == ConversationKind.GROUP) || (folder.includeChannels && conversation.kind == ConversationKind.CHANNEL))
        }
        Scaffold(containerColor = homeBackground) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("‹", color = homePrimaryText, fontSize = 34.sp, modifier = Modifier.clickable { openedFolder = null }.padding(8.dp)); Text(folder.title, color = homePrimaryText, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                }
                LazyColumn(Modifier.fillMaxSize()) {
                    if (rows.isEmpty()) item { Text("暂无会话", color = homeSecondaryText, modifier = Modifier.fillMaxWidth().padding(48.dp)) }
                    items(rows, key = { it.id }) { conversation -> ConversationRow(conversation, onClick = { onOpenConversation(conversation) }) }
                }
            }
        }
        return
    }

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
                Text(section.label, color = homePrimaryText, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, modifier = Modifier.weight(1f))
                if (section == AndroidMobileSection.FOLDERS) Text("＋", color = homeAccent, fontSize = 26.sp, modifier = Modifier.clickable { folderTitle = ""; folderConversationIds = emptySet(); folderIncludeGroups = false; folderIncludeChannels = false; showFolderEditor = true }.padding(8.dp))
            }
            LazyColumn(Modifier.fillMaxSize()) {
                if (section == AndroidMobileSection.FOLDERS && messagingState.folders.isNotEmpty()) items(messagingState.folders, key = { it.id }) { folder ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("▣", color = homeAccent, fontSize = 22.sp)
                        Text(folder.title, color = homePrimaryText, modifier = Modifier.weight(1f).clickable { openedFolder = folder }.padding(start = 12.dp, top = 8.dp, bottom = 8.dp))
                        Text("删除", color = Color(0xFFFF6B6B), modifier = Modifier.clickable { onDeleteFolder(folder.id) }.padding(8.dp))
                    }
                }
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
                if (contacts.isEmpty() && conversations.isEmpty() && (section != AndroidMobileSection.FOLDERS || messagingState.folders.isEmpty())) item {
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
    sharedDraft: MessagingDraft?,
    onDraftChanged: (String, String?) -> Unit,
    currentActorId: String,
    contacts: List<MessagingContact>,
    onUpdateConversationInfo: (String, String) -> Unit,
    onSetConversationParticipant: (String, String) -> Unit,
    onRemoveConversationParticipant: (String) -> Unit,
    onBack: () -> Unit,
    onSend: (String, String?, Boolean, Long?) -> Unit,
    onSendAttachment: (String, String, ByteArray) -> Unit,
    onSendVoice: (String, String, ByteArray, List<Int>) -> Unit,
    onLoadBlob: (String, Int, (Result<ByteArray>) -> Unit) -> Unit,
    shareContacts: List<MessagingContact>,
    onSendContact: (MessagingContact) -> Unit,
    onSendPoll: (String, List<String>, Boolean) -> Unit,
    onVotePoll: (String, List<String>) -> Unit,
    onSendLocation: (Double, Double) -> Unit,
    onEdit: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onSetMessagePinned: (String, Boolean) -> Unit,
    onReact: (String, String) -> Unit,
    onForward: (String, String) -> Unit,
    forwardDestinations: List<ConversationSummary>,
    typingActorName: String?,
    onTypingChanged: (Boolean) -> Unit,
    onToggleMute: () -> Unit,
    onTogglePin: () -> Unit,
    onArchive: () -> Unit,
) {
    var draft by remember(conversation.id, sharedDraft?.updatedAtMs) { mutableStateOf(sharedDraft?.text.orEmpty()) }
    var showMenu by remember { mutableStateOf(false) }
    var selectedMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var replyTarget by remember(conversation.id, sharedDraft?.replyToMessageId) { mutableStateOf(sharedDraft?.replyToMessageId?.let { replyId -> messages.firstOrNull { it.id == replyId } }) }
    var editingMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var forwardingMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var mediaViewerMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var showConversationInfo by remember { mutableStateOf(false) }
    var showChatSearch by remember { mutableStateOf(false) }
    var chatSearchQuery by remember { mutableStateOf("") }
    var showAttachmentMenu by remember { mutableStateOf(false) }
    var showSendModes by remember { mutableStateOf(false) }
    var showContactShare by remember { mutableStateOf(false) }
    var showPollComposer by remember { mutableStateOf(false) }
    var pollQuestion by remember { mutableStateOf("") }
    var pollOption1 by remember { mutableStateOf("") }
    var pollOption2 by remember { mutableStateOf("") }
    var pollOption3 by remember { mutableStateOf("") }
    var attachmentMime by remember { mutableStateOf("*/*") }
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val voiceRecorder = remember { NativeVoiceRecorder(context) }
    val voicePlayer = remember { NativeVoicePlayer(context) }
    var playingVoiceMessageId by remember { mutableStateOf<String?>(null) }
    var isRecordingVoice by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableStateOf(0) }
    var voiceError by remember { mutableStateOf<String?>(null) }
    DisposableEffect(Unit) { onDispose { voiceRecorder.cancel(); voicePlayer.stop() } }
    LaunchedEffect(isRecordingVoice) {
        recordingSeconds = 0
        while (isRecordingVoice) { delay(1000); if (isRecordingVoice) recordingSeconds += 1 }
    }
    LaunchedEffect(conversation.id, draft, replyTarget?.id, editingMessage?.id) {
        delay(350)
        if (editingMessage == null) onDraftChanged(draft, replyTarget?.id)
    }
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
    val microphonePermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            voiceRecorder.start().onSuccess { isRecordingVoice = true; voiceError = null }.onFailure { voiceError = it.message }
        } else voiceError = "请允许麦克风权限后再发送语音"
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

    if (showConversationInfo) {
        AndroidConversationInfo(
            conversation = conversation, contacts = contacts, currentActorId = currentActorId, onBack = { showConversationInfo = false },
            onUpdateInfo = onUpdateConversationInfo, onSetParticipant = onSetConversationParticipant, onRemoveParticipant = onRemoveConversationParticipant,
        )
        return
    }

    mediaViewerMessage?.let { mediaMessage ->
        AndroidMediaViewer(message = mediaMessage, onLoadBlob = onLoadBlob, onClose = { mediaViewerMessage = null })
        return
    }

    if (showSendModes) {
        AlertDialog(
            onDismissRequest = { showSendModes = false }, title = { Text("发送方式") },
            text = {
                Column {
                    TextButton(onClick = {
                        val text = draft.trim(); if (text.isNotEmpty()) { onSend(text, replyTarget?.id, true, null); onDraftChanged("", null); draft = ""; replyTarget = null; editingMessage = null }; showSendModes = false
                    }) { Text("静默发送") }
                    TextButton(onClick = {
                        val text = draft.trim(); if (text.isNotEmpty()) { onSend(text, replyTarget?.id, false, System.currentTimeMillis() + 3_600_000); onDraftChanged("", null); draft = ""; replyTarget = null; editingMessage = null }; showSendModes = false
                    }) { Text("1 小时后发送") }
                    TextButton(onClick = {
                        val calendar = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, 1); set(java.util.Calendar.HOUR_OF_DAY, 9); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0) }
                        val text = draft.trim(); if (text.isNotEmpty()) { onSend(text, replyTarget?.id, false, calendar.timeInMillis); onDraftChanged("", null); draft = ""; replyTarget = null; editingMessage = null }; showSendModes = false
                    }) { Text("明天上午 9:00") }
                }
            },
            confirmButton = { OutlinedButton(onClick = { showSendModes = false }) { Text("取消") } },
        )
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
                    TextButton(onClick = { onSetMessagePinned(message.id, !message.pinned); selectedMessage = null }) { Text(if (message.pinned) "取消置顶消息" else "置顶消息") }
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
                Column(Modifier.weight(1f).clickable { showConversationInfo = true }.padding(vertical = 4.dp)) {
                    Text(conversation.title, color = homePrimaryText, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                    Text("${conversation.participants.size} 位成员 · ${conversation.kind.label}", color = homeSecondaryText, style = MaterialTheme.typography.bodySmall)
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
            val pinnedMessage = conversation.pinnedMessageIds.lastOrNull()?.let { pinnedId -> messages.firstOrNull { it.id == pinnedId } }
            if (pinnedMessage != null) {
                Row(Modifier.fillMaxWidth().background(homeSurface).padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(width = 3.dp, height = 34.dp).background(homeAccent))
                    Column(Modifier.weight(1f).padding(start = 9.dp)) { Text("置顶消息", color = homeAccent, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold); Text(pinnedMessage.text, color = homePrimaryText, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    Text("×", color = homeSecondaryText, fontSize = 22.sp, modifier = Modifier.clickable { onSetMessagePinned(pinnedMessage.id, false) }.padding(6.dp))
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
                                .pointerInput(message.id) {
                                    var horizontalDrag = 0f
                                    detectHorizontalDragGestures(
                                        onDragStart = { horizontalDrag = 0f },
                                        onHorizontalDrag = { change, dragAmount -> horizontalDrag += dragAmount; change.consume() },
                                        onDragEnd = {
                                            if (horizontalDrag > 58.dp.toPx()) {
                                                replyTarget = message
                                                editingMessage = null
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                            horizontalDrag = 0f
                                        },
                                        onDragCancel = { horizontalDrag = 0f },
                                    )
                                }
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
                                    message.pollOptions.forEach { option ->
                                        Row(Modifier.fillMaxWidth().clickable {
                                            val chosenIds = message.pollOptions.filter { it.chosen }.map { it.id }.toMutableSet()
                                            val next = if (message.pollMultipleAnswers) { if (option.chosen) chosenIds.remove(option.id) else chosenIds.add(option.id); chosenIds.toList() } else if (option.chosen) emptyList() else listOf(option.id)
                                            onVotePoll(message.id, next)
                                        }.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text(if (option.chosen) "●" else "○", color = homeAccent); Text(option.text, color = homePrimaryText, modifier = Modifier.weight(1f).padding(start = 7.dp)); Text("${option.voterCount}", color = homeSecondaryText, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                                "voice" -> Row(Modifier.fillMaxWidth().clickable {
                                    val blobId = message.mediaBlobId
                                    if (playingVoiceMessageId == message.id) { voicePlayer.stop(); playingVoiceMessageId = null }
                                    else if (blobId != null && message.mediaSizeBytes > 0) {
                                        onLoadBlob(blobId, message.mediaSizeBytes) { result ->
                                            result.onSuccess { bytes -> voicePlayer.toggle(message.id, bytes) { playingVoiceMessageId = null }.onSuccess { playing -> playingVoiceMessageId = if (playing) message.id else null } }
                                        }
                                    }
                                }, verticalAlignment = Alignment.CenterVertically) {
                                    Text(if (playingVoiceMessageId == message.id) "■" else "▶", color = homeAccent, fontSize = 24.sp)
                                    Column(Modifier.padding(start = 10.dp)) { Text("语音消息", color = homePrimaryText, fontWeight = FontWeight.Medium); Text(if (playingVoiceMessageId == message.id) "正在播放" else (message.mediaFileName ?: "录音"), color = homeSecondaryText, style = MaterialTheme.typography.bodySmall) }
                                }
                                "audio" -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("♫", color = homeAccent, fontSize = 24.sp); Text(message.mediaFileName ?: "音频", color = homePrimaryText, modifier = Modifier.padding(start = 10.dp)) }
                                "photo", "video", "document" -> Row(Modifier.fillMaxWidth().clickable { mediaViewerMessage = message }, verticalAlignment = Alignment.CenterVertically) {
                                    Text(if (message.contentType == "photo") "🖼" else if (message.contentType == "video") "🎬" else "📎", fontSize = 24.sp)
                                    Column(Modifier.padding(start = 10.dp)) { Text(message.mediaFileName ?: message.text, color = homePrimaryText, fontWeight = FontWeight.Medium); Text(if (message.contentType == "photo") "图片 · 点击查看" else if (message.contentType == "video") "视频 · 点击播放" else "文件 · 点击打开", color = homeSecondaryText, style = MaterialTheme.typography.bodySmall) }
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
            if (isRecordingVoice) {
                Row(Modifier.fillMaxWidth().background(homeSurface).padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("●", color = Color.Red, fontSize = 14.sp)
                    Text("正在录音 ${recordingSeconds / 60}:${"%02d".format(recordingSeconds % 60)}", color = homePrimaryText, modifier = Modifier.weight(1f).padding(start = 8.dp))
                    Text("取消", color = Color(0xFFFF6B6B), modifier = Modifier.clickable { voiceRecorder.cancel(); isRecordingVoice = false }.padding(6.dp))
                }
            } else if (voiceError != null) {
                Text(voiceError.orEmpty(), color = Color(0xFFFF6B6B), style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp))
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
                Text(if (draft.isBlank()) (if (isRecordingVoice) "■" else "●") else "➤", color = if (isRecordingVoice) Color.Red else if (draft.isBlank()) homeSecondaryText else homeAccent, fontSize = 22.sp,
                    modifier = Modifier.combinedClickable(
                        onClick = {
                            val text = draft.trim()
                            if (text.isNotEmpty()) {
                                val edit = editingMessage
                                if (edit != null) onEdit(edit.id, text) else { onSend(text, replyTarget?.id, false, null); onDraftChanged("", null) }
                                draft = ""; onTypingChanged(false); editingMessage = null; replyTarget = null
                            } else if (isRecordingVoice) {
                                voiceRecorder.stop().onSuccess { recording -> onSendVoice(recording.file.name, "audio/mp4", recording.bytes, emptyList()); isRecordingVoice = false; voiceError = null }.onFailure { voiceError = it.message; isRecordingVoice = false }
                            } else {
                                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                if (granted) voiceRecorder.start().onSuccess { isRecordingVoice = true; voiceError = null }.onFailure { voiceError = it.message } else microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        onLongClick = { if (draft.isNotBlank()) showSendModes = true },
                    ).padding(10.dp))
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
