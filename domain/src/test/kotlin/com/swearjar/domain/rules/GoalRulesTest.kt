package com.swearjar.domain.rules

import com.swearjar.domain.model.GoalBehaviour
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalRulesTest {

    @Test
    fun `nothing happens below the goal`() {
        val outcome = GoalRules.onEntryLogged(GoalBehaviour.RESET, goalMinor = 1000, totalBeforeMinor = 100, amountMinor = 50)
        assertEquals(GoalOutcome.None, outcome)
        assertFalse(outcome.goalReached)
    }

    @Test
    fun `crossing the goal applies the jar's behaviour`() {
        fun cross(b: GoalBehaviour) = GoalRules.onEntryLogged(b, 1000, 950, 50)
        assertEquals(GoalOutcome.ResetRound, cross(GoalBehaviour.RESET))
        assertEquals(GoalOutcome.KeepFilling, cross(GoalBehaviour.KEEP_FILLING))
        assertEquals(GoalOutcome.CloseJar, cross(GoalBehaviour.CLOSE))
        assertTrue(cross(GoalBehaviour.CLOSE).goalReached)
    }

    @Test
    fun `overshooting the goal still counts as reaching it`() {
        assertEquals(GoalOutcome.ResetRound, GoalRules.onEntryLogged(GoalBehaviour.RESET, 1000, 990, 500))
    }

    @Test
    fun `a keep-filling jar celebrates only once`() {
        assertEquals(GoalOutcome.None, GoalRules.onEntryLogged(GoalBehaviour.KEEP_FILLING, 1000, 1000, 50))
        assertEquals(GoalOutcome.None, GoalRules.onEntryLogged(GoalBehaviour.KEEP_FILLING, 1000, 1400, 50))
    }

    @Test
    fun `a jar with no goal never reaches one`() {
        assertEquals(GoalOutcome.None, GoalRules.onEntryLogged(GoalBehaviour.CLOSE, 0, 5000, 50))
    }

    @Test
    fun `fill fraction`() {
        assertEquals(0f, GoalRules.fillFraction(0, 1000), 0f)
        assertEquals(0.25f, GoalRules.fillFraction(250, 1000), 0.0001f)
        assertEquals(1f, GoalRules.fillFraction(1000, 1000), 0f)
        assertEquals(1.5f, GoalRules.fillFraction(1500, 1000), 0.0001f)
        assertEquals(0f, GoalRules.fillFraction(500, 0), 0f)
    }
}
