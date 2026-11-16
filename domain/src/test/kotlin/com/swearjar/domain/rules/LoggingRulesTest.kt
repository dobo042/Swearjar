package com.swearjar.domain.rules

import com.swearjar.domain.model.Entry
import com.swearjar.domain.model.EntryStatus
import com.swearjar.domain.model.LoggingPolicy
import com.swearjar.domain.model.Role
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoggingRulesTest {

    private fun entry(offender: String = "bob", loggedBy: String = "alice", status: EntryStatus = EntryStatus.ACTIVE) =
        Entry("e1", "r1", "t1", "Swearing", offender, loggedBy, 50, null, status, 0)

    @Test
    fun `anyone policies let members log against others`() {
        for (policy in listOf(LoggingPolicy.ANYONE, LoggingPolicy.ANYONE_WITH_DISPUTE)) {
            assertTrue(LoggingRules.canLog(policy, "alice", Role.MEMBER, "bob"))
            assertTrue(LoggingRules.canLog(policy, "alice", Role.MEMBER, "alice"))
        }
    }

    @Test
    fun `self only blocks logging against others, even for the owner`() {
        assertTrue(LoggingRules.canLog(LoggingPolicy.SELF_ONLY, "alice", Role.MEMBER, "alice"))
        assertFalse(LoggingRules.canLog(LoggingPolicy.SELF_ONLY, "alice", Role.MEMBER, "bob"))
        assertFalse(LoggingRules.canLog(LoggingPolicy.SELF_ONLY, "alice", Role.OWNER, "bob"))
    }

    @Test
    fun `admins only lets owner and admins log, not members`() {
        assertTrue(LoggingRules.canLog(LoggingPolicy.ADMINS_ONLY, "alice", Role.OWNER, "bob"))
        assertTrue(LoggingRules.canLog(LoggingPolicy.ADMINS_ONLY, "alice", Role.ADMIN, "bob"))
        assertFalse(LoggingRules.canLog(LoggingPolicy.ADMINS_ONLY, "alice", Role.MEMBER, "alice"))
    }

    @Test
    fun `only the accused can dispute, and only under the dispute policy`() {
        assertTrue(LoggingRules.canDispute(LoggingPolicy.ANYONE_WITH_DISPUTE, entry(), "bob"))
        assertFalse(LoggingRules.canDispute(LoggingPolicy.ANYONE_WITH_DISPUTE, entry(), "carol"))
        assertFalse(LoggingRules.canDispute(LoggingPolicy.ANYONE, entry(), "bob"))
    }

    @Test
    fun `cannot dispute twice, a voided entry, or your own self-report`() {
        val policy = LoggingPolicy.ANYONE_WITH_DISPUTE
        assertFalse(LoggingRules.canDispute(policy, entry(status = EntryStatus.DISPUTED), "bob"))
        assertFalse(LoggingRules.canDispute(policy, entry(status = EntryStatus.VOIDED), "bob"))
        assertFalse(LoggingRules.canDispute(policy, entry(offender = "bob", loggedBy = "bob"), "bob"))
    }

    @Test
    fun `admins resolve disputes but not on entries they logged`() {
        val disputed = entry(status = EntryStatus.DISPUTED)
        assertTrue(LoggingRules.canResolveDispute(disputed, "carol", Role.ADMIN))
        assertFalse(LoggingRules.canResolveDispute(disputed, "carol", Role.MEMBER))
        assertFalse(LoggingRules.canResolveDispute(disputed, "alice", Role.OWNER))
        assertFalse(LoggingRules.canResolveDispute(entry(), "carol", Role.ADMIN))
    }

    @Test
    fun `voiding`() {
        assertTrue(LoggingRules.canVoid(entry(), "carol", Role.ADMIN))
        assertTrue("logger can retract", LoggingRules.canVoid(entry(), "alice", Role.MEMBER))
        assertFalse("accused cannot void", LoggingRules.canVoid(entry(), "bob", Role.MEMBER))
        assertFalse("self-report cannot be retracted", LoggingRules.canVoid(entry("bob", "bob"), "bob", Role.MEMBER))
        assertFalse(LoggingRules.canVoid(entry(status = EntryStatus.VOIDED), "carol", Role.OWNER))
    }
}
