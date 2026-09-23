package com.ombhrum.fabushi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FrontendProductionModelParityTest {
    private data class Agent(
        override val id: String,
        override val isPinned: Boolean,
    ) : SidebarOrderAgent

    @Test
    fun sidebarPartitionHonorsStoredOrderThenAppendsPinnedAgents() {
        val agents = listOf(
            Agent("a", true),
            Agent("b", false),
            Agent("c", true),
            Agent("d", true),
        )
        val partition = partitionSidebarAgents(agents, listOf("c", "missing", "c"))
        assertEquals(listOf("c", "a", "d"), partition.pinned.map { it.id })
        assertEquals(listOf("b"), partition.unpinned.map { it.id })
        assertEquals(
            listOf("c", "a", "d"),
            movePinnedAgent(
                storedIds = listOf("a", "c", "d"),
                movedId = "c",
                targetId = "a",
                position = PinnedMovePosition.BEFORE,
            ),
        )
    }

    @Test
    fun agentRowActionsAreDeterministicAndHiddenRowsHaveNoActions() {
        assertTrue(agentRowActions(isHidden = true, includeDelete = true).isEmpty())
        val actions = agentRowActions(
            isHidden = false,
            isPinned = true,
            hasUnread = true,
            includeCopy = true,
            includeDelete = true,
            includeDuplicate = true,
            includeMarkUnread = true,
            includePin = true,
        )
        assertEquals(
            listOf(
                AgentRowActionId.UNPIN_AGENT,
                AgentRowActionId.MARK_READ,
                AgentRowActionId.DUPLICATE_AGENT,
                AgentRowActionId.COPY_CONVERSATION_ID,
                AgentRowActionId.HIDE_FROM_SIDEBAR,
                AgentRowActionId.DELETE_AGENT,
            ),
            actions.map { it.id },
        )
        assertFalse(togglePinValue(actions.first()))
        assertFalse(markAgentUnreadValue(actions[1]))
        assertTrue(isHideFromSidebarAction(actions[4]))
    }

    @Test
    fun renameCommitTrimsRejectsEmptyAndIgnoresUnchangedValue() {
        assertEquals("Renamed", committedAgentName("Old", "  Renamed  "))
        assertNull(committedAgentName("Old", "   "))
        assertNull(committedAgentName("Old", "Old"))
    }

    @Test
    fun agentDeleteCopyDistinguishesSingleAgentAndGroupSemantics() {
        val single = agentDeleteDescription(
            AgentDeleteTarget(id = "agent-1", name = "Agent"),
        )
        val group = agentDeleteDescription(
            AgentDeleteTarget(id = "group-1", name = "Group", isGroup = true),
        )
        assertTrue(single.contains("agent and its chat history"))
        assertTrue(single.contains("can't be undone"))
        assertTrue(group.contains("group and its chat history"))
        assertTrue(group.contains("Bots in it are not deleted"))
    }

    @Test
    fun commandPaletteSearchFiltersAndScoresAgentsAndActions() {
        var activated = ""
        val entries = listOf(
            CommandPaletteEntry(
                id = "agent:a",
                kind = CommandPaletteEntryKind.AGENT,
                label = "Research Agent",
                searchText = "Research Agent analysis",
                activate = { activated = "agent" },
            ),
            CommandPaletteEntry(
                id = "command:settings",
                kind = CommandPaletteEntryKind.COMMAND,
                label = "Chat Settings",
                searchText = "Chat Settings notifications",
                activate = { activated = "settings" },
            ),
        )
        val agentResults = commandPaletteEntries(
            entries,
            CommandPaletteTab.AGENTS,
            "rsrch",
        )
        assertEquals(listOf("agent:a"), agentResults.map { it.id })
        assertTrue(activateCommandPaletteEntry(agentResults, 0))
        assertEquals("agent", activated)
        assertEquals(0, movePaletteHighlight(-1, 1, 1))
    }

    @Test
    fun commandPaletteRootCommandsAreCurrentChatScoped() {
        val opened = mutableListOf<CommandPaletteInfoSection>()
        assertTrue(
            commandPaletteRootCommands(
                activeAgentIsGroup = null,
                activeAgentIsSharedRoom = false,
                hasChannels = true,
                openInfoSection = opened::add,
            ).isEmpty(),
        )

        val commands = commandPaletteRootCommands(
            activeAgentIsGroup = true,
            activeAgentIsSharedRoom = false,
            hasChannels = true,
            openInfoSection = opened::add,
        )
        assertEquals(
            listOf("info:members", "info:channels", "info:settings"),
            commands.map { it.id },
        )
        commands.first().activate()
        commands.last().activate()
        assertEquals(
            listOf(CommandPaletteInfoSection.MEMBERS, CommandPaletteInfoSection.SETTINGS),
            opened,
        )
    }

    @Test
    fun commandPaletteUpdateCommandUsesAndroidUpdaterStateMachine() {
        var checks = 0
        var installs = 0
        var opens = 0
        assertNull(
            commandPaletteUpdateCommand(
                state = AndroidUpdateUiState(
                    phase = AndroidUpdatePhase.DISABLED,
                    currentVersion = "1.0.0",
                ),
                check = { checks += 1 },
                install = { installs += 1 },
                openUpdates = { opens += 1 },
            ),
        )

        val available = commandPaletteUpdateCommand(
            state = AndroidUpdateUiState(
                phase = AndroidUpdatePhase.AVAILABLE,
                currentVersion = "1.0.0",
                availableVersion = "1.1.0",
            ),
            check = { checks += 1 },
            install = { installs += 1 },
            openUpdates = { opens += 1 },
        )
        assertEquals("Download Update…", available?.label)
        available?.activate?.invoke()
        assertEquals(1, installs)

        val error = commandPaletteUpdateCommand(
            state = AndroidUpdateUiState(
                phase = AndroidUpdatePhase.ERROR,
                currentVersion = "1.0.0",
            ),
            check = { checks += 1 },
            install = { installs += 1 },
            openUpdates = { opens += 1 },
        )
        error?.activate?.invoke()
        assertEquals(1, checks)
        assertEquals(1, opens)
    }

    @Test
    fun permissionScopeRequiresStrictlyNewRevisionAfterAccountReentry() {
        val gate = LocalToolPermissionScopeGate()
        gate.enter("account-a")
        assertTrue(gate.accepts("account-a", 4))
        assertTrue(gate.accepts("account-a", 4))
        assertFalse(gate.accepts("account-a", 3))

        gate.enter(null)
        gate.enter("account-a")
        assertFalse(gate.accepts("account-a", 4))
        assertTrue(gate.accepts("account-a", 5))

        gate.dispose()
        assertFalse(gate.accepts("account-a", 6))
    }

    @Test
    fun disposalGuardDefersTerminalDisposeAndReplacementDisposesImmediately() {
        val queue = ArrayDeque<() -> Unit>()
        var firstDisposed = 0
        var secondDisposed = 0
        val guard = StrictModeDisposalGuard(defer = queue::addLast)
        val first = StrictModeDisposable { firstDisposed += 1 }
        val second = StrictModeDisposable { secondDisposed += 1 }

        val cleanupFirst = guard.attach(first)
        cleanupFirst()
        guard.attach(second)
        assertEquals(1, firstDisposed)

        while (queue.isNotEmpty()) queue.removeFirst().invoke()
        assertEquals(1, firstDisposed)
        assertEquals(0, secondDisposed)

        val cleanupSecond = guard.attach(second)
        cleanupSecond()
        while (queue.isNotEmpty()) queue.removeFirst().invoke()
        assertEquals(1, secondDisposed)
    }
}
