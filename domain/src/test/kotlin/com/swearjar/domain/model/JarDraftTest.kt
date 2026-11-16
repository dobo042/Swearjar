package com.swearjar.domain.model

import com.swearjar.domain.model.JarDraft.Problem
import org.junit.Assert.assertEquals
import org.junit.Test

class JarDraftTest {

    private fun draft(name: String = "Flat 4", currency: String = "GBP", goal: Minor = 2000) =
        JarDraft(name, currency, goal, JarSettings())

    @Test
    fun `a sensible draft has no problems, with or without a goal`() {
        assertEquals(emptySet<Problem>(), draft().problems())
        assertEquals(emptySet<Problem>(), draft(goal = 0).problems())
        assertEquals(emptySet<Problem>(), draft(name = "x".repeat(60), goal = JarDraft.MAX_GOAL_MINOR).problems())
    }

    @Test
    fun `name is trimmed before checking`() {
        assertEquals(setOf(Problem.NAME_BLANK), draft(name = "   ").problems())
        assertEquals("Flat 4", draft(name = "  Flat 4 ").cleanName)
        assertEquals(setOf(Problem.NAME_TOO_LONG), draft(name = "x".repeat(61)).problems())
    }

    @Test
    fun `currency and goal are checked`() {
        assertEquals(setOf(Problem.CURRENCY_INVALID), draft(currency = "gbp").problems())
        assertEquals(setOf(Problem.GOAL_NEGATIVE), draft(goal = -1).problems())
        assertEquals(setOf(Problem.GOAL_TOO_LARGE), draft(goal = JarDraft.MAX_GOAL_MINOR + 1).problems())
    }
}
