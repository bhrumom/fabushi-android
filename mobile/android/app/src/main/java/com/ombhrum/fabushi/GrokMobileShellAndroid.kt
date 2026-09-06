package com.ombhrum.fabushi

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.absoluteValue
import kotlin.math.sin

private val GrokMobileBackground = Color(0xFFFAFAF7)
private val GrokMobileInk = Color(0xFF111111)
private val GrokMobileMuted = Color(0xFF8B8B8B)

private fun ghostColor(identity: String): Color {
    val palette = listOf(
        Color(0xFF00C978), Color(0xFF1685F7), Color(0xFF8A4CFF),
        Color(0xFFFF681D), Color(0xFFEE2546), Color(0xFFF9A516),
    )
    return palette[identity.hashCode().absoluteValue % palette.size]
}

@Composable
fun ClothGhostAvatarAndroid(
    botId: String,
    size: Dp = 46.dp,
    active: Boolean = false,
    badge: Color? = null,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "cloth-ghost")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (active) 1200 else 2600),
            repeatMode = RepeatMode.Restart,
        ),
        label = "cloth-phase",
    )
    val base = ghostColor(botId)
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().testTag("cloth-ghost-avatar")) {
            val w = this.size.width
            val h = this.size.height
            val lift = sin(phase) * h * 0.018f
            val drift = sin(phase * 0.7f) * w * 0.018f
            val top = h * 0.08f + lift
            val shoulder = h * 0.27f + lift
            val hem = h * 0.79f + lift
            val wave = h * 0.055f
            val left = w * 0.16f + drift
            val right = w * 0.84f + drift

            val path = Path().apply {
                moveTo(left, hem)
                lineTo(w * 0.16f, shoulder)
                cubicTo(w * 0.17f, h * 0.13f + lift, w * 0.34f, top, w * 0.50f, top)
                cubicTo(w * 0.66f, top, w * 0.83f, h * 0.13f + lift, w * 0.84f, shoulder)
                lineTo(right, hem)
                val segment = (right - left) / 3f
                for (index in 3 downTo 1) {
                    val xRight = left + segment * index
                    val xLeft = xRight - segment
                    val local = phase + index * 0.9f
                    quadraticBezierTo(
                        (xLeft + xRight) / 2f,
                        hem + h * 0.12f + sin(local) * wave,
                        xLeft,
                        hem + sin(local + 0.7f) * wave,
                    )
                }
                close()
            }
            drawPath(
                path = path,
                brush = Brush.linearGradient(
                    colors = listOf(base.copy(alpha = 0.98f), base.copy(alpha = 0.84f), base),
                    start = Offset(0f, 0f),
                    end = Offset(w, h),
                ),
            )
            drawPath(path, Color.White.copy(alpha = 0.22f), style = Stroke(width = (w * 0.012f).coerceAtLeast(0.7f)))

            val eyeY = h * 0.38f + lift + sin(phase * 0.39f) * h * 0.012f
            val gaze = sin(phase * 0.48f) * w * 0.025f
            val eyeWidth = w * 0.10f
            val eyeHeight = h * 0.23f
            drawRoundRect(
                Color.White,
                topLeft = Offset(w * 0.37f + gaze, eyeY - eyeHeight / 2f),
                size = androidx.compose.ui.geometry.Size(eyeWidth, eyeHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(eyeWidth / 2f),
            )
            drawRoundRect(
                Color.White,
                topLeft = Offset(w * 0.54f + gaze, eyeY - eyeHeight / 2f),
                size = androidx.compose.ui.geometry.Size(eyeWidth, eyeHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(eyeWidth / 2f),
            )
        }
        if (badge != null) {
            Box(
                Modifier.align(Alignment.TopEnd).size(size * 0.23f)
                    .background(Color.White, CircleShape).padding(2.dp).background(badge, CircleShape),
            )
        }
    }
}

@Composable
fun GrokMobileShellAndroid(
    accountName: String,
    messagingState: MessagingUiState,
    botState: MobileBotUiState,
    appAgentSurface: FabushiAppAgentSurface,
    onOpenLegacy: () -> Unit,
    onRefreshBots: () -> Unit,
    onCreateBot: (String, String, (() -> Unit)?) -> Unit,
    onOpenBot: (MobileBotSummaryAndroid) -> Unit,
    onCloseBot: () -> Unit,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
) {
    LaunchedEffect(Unit) { onRefreshBots() }
    val active = botState.activeBot
    if (active != null) {
        GrokBotChatAndroid(active, botState, appAgentSurface, onCloseBot, onDraftChange, onSend, onStop)
        return
    }

    var query by remember { mutableStateOf("") }
    var addOpen by remember { mutableStateOf(false) }
    var createOpen by remember { mutableStateOf(false) }
    var botName by remember { mutableStateOf("") }
    var botDescription by remember { mutableStateOf("") }

    LaunchedEffect(
        query,
        addOpen,
        createOpen,
        botName,
        botDescription,
        botState.creating,
        botState.error,
        botState.bots,
        messagingState.conversations,
        appAgentSurface,
    ) {
        val elements = mutableListOf<FabushiAppAgentSurface.Element>()
        val actions = linkedMapOf<String, FabushiAppAgentSurface.Action>()
        fun element(
            id: String,
            role: String,
            name: String,
            enabled: Boolean = true,
            action: FabushiAppAgentSurface.Action? = null,
        ) {
            val agentId = id.replace(Regex("[^A-Za-z0-9._:/@-]"), "-").take(200)
            elements += FabushiAppAgentSurface.Element(
                agentId = agentId,
                role = role.take(80),
                name = name.take(240),
                enabled = enabled,
            )
            if (action != null) actions[agentId] = action
        }

        val screen = when {
            createOpen -> {
                element("grok-create-bot", "dialog", "创建 Bot")
                element(
                    "new-bot-name",
                    "textbox",
                    "Bot 名称",
                    action = FabushiAppAgentSurface.Action(setOf("setValue")) { botName = it.orEmpty() },
                )
                element(
                    "new-bot-description",
                    "textbox",
                    "Bot 描述",
                    action = FabushiAppAgentSurface.Action(setOf("setValue")) { botDescription = it.orEmpty() },
                )
                element(
                    "create-bot-submit",
                    "button",
                    "创建 Bot",
                    enabled = botName.isNotBlank() && !botState.creating,
                    action = FabushiAppAgentSurface.Action(setOf("invoke")) {
                        if (botName.isNotBlank() && !botState.creating) {
                            onCreateBot(botName, botDescription) {
                                botName = ""
                                botDescription = ""
                                createOpen = false
                            }
                        }
                    },
                )
                element(
                    "create-bot-cancel",
                    "button",
                    "取消创建 Bot",
                    enabled = !botState.creating,
                    action = FabushiAppAgentSurface.Action(setOf("invoke")) { if (!botState.creating) createOpen = false },
                )
                botState.error?.takeIf { it.isNotBlank() }?.let { element("create-bot-error", "status", "Bot 创建失败") }
                "grok-create-bot"
            }
            else -> {
                element("grok-mobile-home", "application", "Fabushi")
                element(
                    "grok-mobile-legacy",
                    "button",
                    "打开完整 Fabushi",
                    action = FabushiAppAgentSurface.Action(setOf("invoke")) { onOpenLegacy() },
                )
                element(
                    "grok-mobile-search-toggle",
                    "button",
                    if (query.isEmpty()) "打开搜索" else "关闭搜索",
                    action = FabushiAppAgentSurface.Action(setOf("invoke")) { query = if (query.isEmpty()) " " else "" },
                )
                if (query.isNotEmpty()) {
                    element(
                        "grok-mobile-search-field",
                        "textbox",
                        "搜索",
                        action = FabushiAppAgentSurface.Action(setOf("setValue")) { query = it.orEmpty() },
                    )
                }
                element(
                    "grok-mobile-add",
                    "button",
                    "新建",
                    action = FabushiAppAgentSurface.Action(setOf("invoke")) { addOpen = true },
                )
                if (addOpen) {
                    element(
                        "grok-mobile-new-bot",
                        "menuitem",
                        "New Bot",
                        action = FabushiAppAgentSurface.Action(setOf("invoke")) { addOpen = false; createOpen = true },
                    )
                    for ((id, name) in listOf(
                        "grok-mobile-new-message" to "New message",
                        "grok-mobile-new-group" to "New group",
                        "grok-mobile-new-channel" to "New channel",
                    )) {
                        element(
                            id,
                            "menuitem",
                            name,
                            action = FabushiAppAgentSurface.Action(setOf("invoke")) { addOpen = false; onOpenLegacy() },
                        )
                    }
                }
                val mahayana = MobileBotSummaryAndroid("mahayana-assistant", "Mahayana", "that's the only new one.")
                element(
                    "grok-bot-mahayana-assistant",
                    "button",
                    "打开 Mahayana",
                    action = FabushiAppAgentSurface.Action(setOf("invoke")) { onOpenBot(mahayana) },
                )
                botState.bots
                    .filter { query.isBlank() || it.name.contains(query.trim(), true) || it.description.contains(query.trim(), true) }
                    .take(100)
                    .forEach { bot ->
                        element(
                            "grok-bot-${bot.id}",
                            "button",
                            "打开 ${bot.name}",
                            action = FabushiAppAgentSurface.Action(setOf("invoke")) { onOpenBot(bot) },
                        )
                    }
                if (botState.error?.isNotBlank() == true) element("grok-bot-error", "status", "Bot 加载失败")
                if (addOpen) "grok-compose" else "grok-home"
            }
        }
        appAgentSurface.publish(screen = screen, elements = elements, actions = actions)
    }
    DisposableEffect(appAgentSurface) {
        onDispose { appAgentSurface.clear() }
    }

    if (createOpen) {
        AlertDialog(
            onDismissRequest = { if (!botState.creating) createOpen = false },
            title = { Text("New Bot") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    ClothGhostAvatarAndroid(botName.ifBlank { "new-bot" }, 82.dp, active = botState.creating)
                    OutlinedTextField(botName, { botName = it }, label = { Text("Bot name") }, modifier = Modifier.testTag("new-bot-name"), singleLine = true)
                    OutlinedTextField(botDescription, { botDescription = it }, label = { Text("What does this Bot do?") }, minLines = 2, maxLines = 4)
                    botState.error?.let { Text(it, color = Color(0xFFD14343), fontSize = 12.sp) }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreateBot(botName, botDescription) {
                            botName = ""
                            botDescription = ""
                            createOpen = false
                        }
                    },
                    enabled = botName.isNotBlank() && !botState.creating,
                    modifier = Modifier.testTag("create-bot-submit"),
                ) { Text(if (botState.creating) "Creating…" else "Create") }
            },
            dismissButton = { TextButton(onClick = { createOpen = false }, enabled = !botState.creating) { Text("Cancel") } },
        )
    }

    Box(Modifier.fillMaxSize().background(GrokMobileBackground).testTag("grok-mobile-home")) {
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(38.dp).background(Color(0xFFFFC7D1), CircleShape).clickable(onClick = onOpenLegacy),
                        contentAlignment = Alignment.Center,
                    ) { Text(accountName.take(1).uppercase().ifBlank { "F" }, color = GrokMobileInk, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.weight(1f))
                    Text("⌕", fontSize = 29.sp, color = GrokMobileInk, modifier = Modifier.padding(horizontal = 10.dp).clickable { query = if (query.isEmpty()) " " else "" })
                    Box {
                        Text("+", fontSize = 31.sp, color = GrokMobileInk, modifier = Modifier.padding(horizontal = 8.dp).clickable { addOpen = true }.testTag("grok-mobile-add"))
                        DropdownMenu(expanded = addOpen, onDismissRequest = { addOpen = false }) {
                            DropdownMenuItem(text = { Text("New Bot") }, onClick = { addOpen = false; createOpen = true })
                            DropdownMenuItem(text = { Text("New message") }, onClick = { addOpen = false; onOpenLegacy() })
                            DropdownMenuItem(text = { Text("New group") }, onClick = { addOpen = false; onOpenLegacy() })
                            DropdownMenuItem(text = { Text("New channel") }, onClick = { addOpen = false; onOpenLegacy() })
                        }
                    }
                }
            }
            item {
                Column(
                    Modifier.fillMaxWidth().padding(top = 30.dp, bottom = 34.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.size(width = 140.dp, height = 88.dp), contentAlignment = Alignment.Center) {
                        ClothGhostAvatarAndroid("all-hands-green", 52.dp, modifier = Modifier.align(Alignment.Center).padding(end = 58.dp, top = 13.dp))
                        ClothGhostAvatarAndroid("all-hands-violet", 52.dp, modifier = Modifier.align(Alignment.Center).padding(start = 2.dp, top = 26.dp))
                        ClothGhostAvatarAndroid("mahayana-assistant", 57.dp, modifier = Modifier.align(Alignment.Center).padding(start = 56.dp))
                        Text("+2", color = GrokMobileInk.copy(alpha = 0.35f), fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.BottomEnd))
                    }
                    Text("All Hands", color = GrokMobileMuted, fontSize = 14.sp)
                }
            }
            if (query.isNotEmpty()) item {
                OutlinedTextField(
                    value = query.trimStart(),
                    onValueChange = { query = it },
                    placeholder = { Text("Search") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(15.dp),
                )
            }
            item { SectionLabelAndroid("Board") }
            item {
                GrokBotRowAndroid(
                    MobileBotSummaryAndroid("mahayana-assistant", "Mahayana", "that's the only new one."),
                    badge = "Board",
                    onClick = onOpenBot,
                )
            }
            val visibleBots = botState.bots.filter { query.isBlank() || it.name.contains(query.trim(), true) || it.description.contains(query.trim(), true) }
            if (visibleBots.isNotEmpty()) {
                item { SectionLabelAndroid("Bots  ${visibleBots.size}") }
                items(visibleBots, key = { it.id }) { bot -> GrokBotRowAndroid(bot, "Bot", onOpenBot) }
            }
            val rows = messagingState.conversations.filter { !it.isArchived && (query.isBlank() || it.title.contains(query.trim(), true) || it.preview.contains(query.trim(), true)) }
            if (rows.isNotEmpty()) {
                item { SectionLabelAndroid("Projects  ${rows.size}") }
                items(rows.take(10), key = { it.id }) { conversation ->
                    Row(
                        Modifier.fillMaxWidth().clickable(onClick = onOpenLegacy).padding(horizontal = 18.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ClothGhostAvatarAndroid("conversation:${conversation.id}", 45.dp, badge = if (conversation.unreadCount > 0) Color(0xFF2A92FE) else null)
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(conversation.title, color = GrokMobileInk, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                                Text(if (conversation.kind == ConversationKind.CHANNEL) "Channel" else "Engineering", color = GrokMobileMuted, fontSize = 11.sp, modifier = Modifier.padding(start = 7.dp).background(Color.Black.copy(alpha = 0.045f), RoundedCornerShape(20.dp)).padding(horizontal = 7.dp, vertical = 3.dp))
                            }
                            Text(conversation.preview.ifBlank { "Ready" }, color = GrokMobileMuted, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text(conversation.time, color = GrokMobileMuted, fontSize = 11.sp, modifier = Modifier.padding(start = 6.dp))
                    }
                }
            }
            item { Spacer(Modifier.height(44.dp)) }
        }
    }
}

@Composable
private fun SectionLabelAndroid(text: String) {
    Text(text, color = GrokMobileInk.copy(alpha = 0.42f), fontSize = 16.sp, modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp))
}

@Composable
private fun GrokBotRowAndroid(bot: MobileBotSummaryAndroid, badge: String, onClick: (MobileBotSummaryAndroid) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick(bot) }.padding(horizontal = 18.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ClothGhostAvatarAndroid(bot.id, 47.dp, badge = Color(0xFF20B967))
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(bot.name, color = GrokMobileInk, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text(badge, color = GrokMobileMuted, fontSize = 11.sp, modifier = Modifier.padding(start = 7.dp).background(Color.Black.copy(alpha = 0.045f), RoundedCornerShape(20.dp)).padding(horizontal = 7.dp, vertical = 3.dp))
            }
            Text(bot.description.ifBlank { "Ready" }, color = GrokMobileMuted, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text("now", color = GrokMobileMuted, fontSize = 11.sp)
    }
}

@Composable
private fun GrokBotChatAndroid(
    bot: MobileBotSummaryAndroid,
    state: MobileBotUiState,
    appAgentSurface: FabushiAppAgentSurface,
    onClose: () -> Unit,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
) {
    LaunchedEffect(bot.id, state.draft, state.busy, state.error, state.messages, appAgentSurface) {
        val elements = mutableListOf(
            FabushiAppAgentSurface.Element("mobile-bot-chat", "application", "Bot ${bot.name}"),
            FabushiAppAgentSurface.Element("mobile-bot-close", "button", "关闭 Bot 对话"),
            FabushiAppAgentSurface.Element("mobile-bot-draft", "textbox", "Bot 消息"),
        )
        val sendId = if (state.busy) "mobile-bot-stop" else "mobile-bot-send"
        elements += FabushiAppAgentSurface.Element(
            sendId,
            "button",
            if (state.busy) "停止 Bot" else "发送 Bot 消息",
            enabled = state.busy || state.draft.trim().isNotEmpty(),
        )
        state.messages.takeLast(50).forEach { entry ->
            val id = "mobile-bot-entry-${entry.id}".replace(Regex("[^A-Za-z0-9._:/@-]"), "-").take(200)
            val roleName = when {
                entry.role == MobileChatRole.USER -> "用户消息"
                entry.kind == MobileChatEntryKind.ACTION -> "Bot 动作"
                entry.kind == MobileChatEntryKind.THINKING -> "Bot 思考"
                else -> "Bot 消息"
            }
            elements += FabushiAppAgentSurface.Element(id, "log", roleName)
        }
        if (state.error?.isNotBlank() == true) {
            elements += FabushiAppAgentSurface.Element("mobile-bot-error", "status", "Bot 对话失败")
        }
        val actions = linkedMapOf(
            "mobile-bot-close" to FabushiAppAgentSurface.Action(setOf("invoke")) { onClose() },
            "mobile-bot-draft" to FabushiAppAgentSurface.Action(setOf("setValue")) { onDraftChange(it.orEmpty()) },
            sendId to FabushiAppAgentSurface.Action(setOf("invoke")) { if (state.busy) onStop() else onSend() },
        )
        appAgentSurface.publish(screen = "bot-chat", elements = elements, actions = actions)
    }
    DisposableEffect(appAgentSurface) {
        onDispose { appAgentSurface.clear() }
    }

    Column(Modifier.fillMaxSize().background(GrokMobileBackground).testTag("mobile-bot-chat")) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("‹", color = GrokMobileInk, fontSize = 34.sp, modifier = Modifier.clickable(onClick = onClose).padding(horizontal = 8.dp))
            Spacer(Modifier.weight(1f))
            Row(Modifier.background(Color.White, RoundedCornerShape(28.dp)).padding(horizontal = 13.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                ClothGhostAvatarAndroid(bot.id, 28.dp, active = state.busy)
                Text(bot.name, color = GrokMobileInk, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp))
            }
            Spacer(Modifier.weight(1f))
            Text("▣", color = GrokMobileInk, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 8.dp))
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            if (state.messages.isEmpty()) item {
                Column(Modifier.fillMaxWidth().padding(top = 96.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ClothGhostAvatarAndroid(bot.id, 82.dp)
                    Text(bot.name, color = GrokMobileInk, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    if (bot.description.isNotBlank()) Text(bot.description, color = GrokMobileMuted, fontSize = 14.sp)
                }
            }
            items(state.messages, key = { it.id }) { entry ->
                when {
                    entry.kind == MobileChatEntryKind.THINKING -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        ClothGhostAvatarAndroid(bot.id, 22.dp, active = true)
                        Text(entry.actionTitle ?: "Thinking…", color = GrokMobileMuted, fontSize = 12.sp)
                        CircularProgressIndicator(Modifier.size(12.dp), strokeWidth = 1.5.dp, color = GrokMobileMuted)
                    }
                    entry.kind == MobileChatEntryKind.ACTION -> Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(7.dp).background(if (entry.actionStatus == "failed") Color.Red else Color(0xFFFF7A1A), CircleShape))
                        Text(entry.actionTitle ?: "Working", color = GrokMobileInk, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        entry.actionDetail?.takeIf { it.isNotBlank() }?.let { Text(it, color = GrokMobileMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    }
                    entry.role == MobileChatRole.USER -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Text(entry.text, color = Color.White, fontSize = 16.sp, modifier = Modifier.background(Color.Black, RoundedCornerShape(18.dp)).padding(horizontal = 15.dp, vertical = 10.dp))
                    }
                    else -> Column {
                        Text(bot.name, color = GrokMobileMuted, fontSize = 11.sp, modifier = Modifier.padding(start = 29.dp, bottom = 2.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            ClothGhostAvatarAndroid(bot.id, 20.dp)
                            Text(entry.text, color = GrokMobileInk, fontSize = 16.sp, modifier = Modifier.padding(start = 7.dp).background(Color.Black.copy(alpha = 0.055f), RoundedCornerShape(18.dp)).padding(horizontal = 15.dp, vertical = 10.dp))
                        }
                    }
                }
            }
            state.error?.let { error -> item { Text(error, color = Color(0xFFD14343), fontSize = 12.sp) } }
        }
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                state.draft,
                onDraftChange,
                placeholder = { Text("Message") },
                maxLines = 5,
                modifier = Modifier.weight(1f).testTag("mobile-bot-draft"),
                shape = RoundedCornerShape(19.dp),
            )
            Button(
                onClick = if (state.busy) onStop else onSend,
                enabled = state.busy || state.draft.trim().isNotEmpty(),
                modifier = Modifier.size(48.dp).testTag(if (state.busy) "mobile-bot-stop" else "mobile-bot-send"),
                colors = ButtonDefaults.buttonColors(containerColor = if (state.busy) Color(0xFFE34B5F) else Color.Black, contentColor = Color.White),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            ) { Text(if (state.busy) "■" else "↑", fontSize = 19.sp, fontWeight = FontWeight.Bold) }
        }
    }
}
