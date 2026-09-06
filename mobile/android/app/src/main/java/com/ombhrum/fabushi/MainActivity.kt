package com.ombhrum.fabushi

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableSharedFlow

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
            MaterialTheme {
                val model: MarketplaceViewModel = viewModel()
                val messagingModel: MessagingViewModel = viewModel()
                val botModel: MobileBotViewModel = viewModel()
                val state by model.state.collectAsState()
                val messagingState by messagingModel.state.collectAsState()
                val botState by botModel.state.collectAsState()
                val updateState by updateModel.state.collectAsState()
                var openedMiniApp by remember { mutableStateOf<MarketplacePlugin?>(null) }
                var showLegacyShell by remember { mutableStateOf(false) }

                BackHandler(enabled = showLegacyShell) { showLegacyShell = false }

                LaunchedEffect(model) {
                    deepLinks.collect { uri -> model.handleDeepLink(uri) }
                }
                LaunchedEffect(state.loggedIn) {
                    remoteDeviceGateway.setLoggedIn(state.loggedIn)
                    if (state.loggedIn) messagingModel.refresh()
                    if (!state.loggedIn) showLegacyShell = false
                }
                LaunchedEffect(state.browserLaunchNonce, state.browserLoginUrl) {
                    val loginUrl = state.browserLoginUrl
                    if (state.browserLaunchNonce > 0 && !loginUrl.isNullOrBlank() && !loginUrl.startsWith("about:blank")) {
                        CustomTabsIntent.Builder()
                            .setShowTitle(true)
                            .build()
                            .launchUrl(this@MainActivity, Uri.parse(loginUrl))
                    }
                }

                val active = openedMiniApp
                if (active != null) {
                    MiniAppWebMcpSurface(
                        plugin = active,
                        loadLocalHtml = model::loadLocalMiniAppHtml,
                        callRuntimeToolJson = model::callRuntimeToolJson,
                        onClose = { openedMiniApp = null },
                    )
                } else if (state.onboardingStep >= 3 && state.authResolved && state.loggedIn && !showLegacyShell) {
                    GrokMobileShellAndroid(
                        accountName = state.accountName,
                        messagingState = messagingState,
                        botState = botState,
                        onOpenLegacy = { showLegacyShell = true },
                        onRefreshBots = botModel::refreshBots,
                        onCreateBot = botModel::createBot,
                        onOpenBot = botModel::openBot,
                        onCloseBot = botModel::closeBot,
                        onDraftChange = botModel::setDraft,
                        onSend = botModel::send,
                        onStop = botModel::stop,
                    )
                } else {
                    FabushiScreen(
                        state = state,
                        onQueryChange = model::setQuery,
                        onSearch = model::refresh,
                        onInstall = model::install,
                        onOpen = { openedMiniApp = it },
                        onApprovePermissions = model::approvePermissions,
                        onDenyPermissions = model::denyPermissions,
                        updateState = updateState,
                        onCheckUpdate = { updateModel.checkForUpdates(force = true) },
                        onInstallUpdate = updateModel::downloadAndInstall,
                        messagingState = messagingState,
                        messagingActorId = messagingModel.currentActorId,
                        onMessagingRefresh = messagingModel::refresh,
                        onCreateDirect = messagingModel::createDirect,
                        onCreateConversation = messagingModel::createConversation,
                        onSendText = { conversationId, text, replyTo, silent, scheduledAt -> messagingModel.sendText(conversationId, text, replyTo, silent, scheduledAt) },
                        onSendAttachment = messagingModel::sendAttachment,
                        onSendVoice = messagingModel::sendVoice,
                        onLoadBlob = messagingModel::loadBlob,
                        onSendContact = messagingModel::sendContact,
                        onSendPoll = messagingModel::sendPoll,
                        onVotePoll = messagingModel::votePoll,
                        onSendLocation = { conversationId, latitude, longitude -> messagingModel.sendLocation(conversationId, latitude, longitude) },
                        onEditText = messagingModel::editText,
                        onDeleteMessage = { conversationId, messageId -> messagingModel.deleteMessage(conversationId, messageId) },
                        onSetMessagePinned = messagingModel::setMessagePinned,
                        onSetReaction = messagingModel::setReaction,
                        onForwardMessage = messagingModel::forwardMessage,
                        onStartTyping = messagingModel::startTyping,
                        onStopTyping = messagingModel::stopTyping,
                        onSetPinned = messagingModel::setPinned,
                        onSetArchived = messagingModel::setArchived,
                        onSetMuted = messagingModel::setMuted,
                        onMarkRead = messagingModel::markRead,
                        onSetMarkedUnread = messagingModel::setMarkedUnread,
                        onSetDraft = messagingModel::setDraft,
                        onUpdateConversationInfo = messagingModel::updateConversationInfo,
                        onSetConversationParticipant = messagingModel::setConversationParticipant,
                        onRemoveConversationParticipant = messagingModel::removeConversationParticipant,
                        onUpsertFolder = messagingModel::upsertFolder,
                        onDeleteFolder = messagingModel::deleteFolder,
                        appAgentSurface = appAgentSurface,
                        authGateEnabled = true,
                        onAdvanceOnboarding = model::advanceOnboarding,
                        onSkipOnboarding = model::skipOnboarding,
                        onBeginBrowserLogin = model::beginBrowserLogin,
                        onReopenBrowserLogin = model::reopenBrowserLogin,
                        onCancelBrowserLogin = model::cancelBrowserLogin,
                        onLogout = model::logout,
                        onChatDraftChange = model::setChatDraft,
                        onSendChat = model::sendChat,
                        onStopChat = model::stopChat,
                    )
                }
            }
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
