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
