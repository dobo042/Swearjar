package com.swearjar.domain.rules

import com.swearjar.domain.model.GoalBehaviour
import com.swearjar.domain.model.Minor

/** What logging an entry did to the round. */
sealed interface GoalOutcome {
    /** The goal was not crossed by this entry (or the jar has no goal). */
    data object None : GoalOutcome

    /** This entry took the round to its goal: celebrate, archive the round, start a new one. */
    data object ResetRound : GoalOutcome

    /** This entry took the round to its goal: celebrate and carry on. */
    data object KeepFilling : GoalOutcome

    /** This entry took the round to its goal: celebrate and lock the jar. */
    data object CloseJar : GoalOutcome

    val goalReached: Boolean get() = this != None
}

/** Interprets a jar's [GoalBehaviour] and works out how full the jar is. */
object GoalRules {

    /**
     * The outcome of adding [amountMinor] to a round standing at [totalBeforeMinor]. A goal
     * fires once, on the entry that crosses it; later entries in a "keep filling" jar do not
     * re-trigger it.
     */
    fun onEntryLogged(
        behaviour: GoalBehaviour,
        goalMinor: Minor,
        totalBeforeMinor: Minor,
        amountMinor: Minor,
    ): GoalOutcome {
        if (goalMinor <= 0) return GoalOutcome.None
        val crossed = totalBeforeMinor < goalMinor && totalBeforeMinor + amountMinor >= goalMinor
        if (!crossed) return GoalOutcome.None
        return when (behaviour) {
            GoalBehaviour.RESET -> GoalOutcome.ResetRound
            GoalBehaviour.KEEP_FILLING -> GoalOutcome.KeepFilling
            GoalBehaviour.CLOSE -> GoalOutcome.CloseJar
        }
    }

    /**
     * How full the jar is: 0 is empty, 1 is at the goal, above 1 is overflowing. A jar with
     * no goal is shown empty, so this returns 0.
     */
    fun fillFraction(totalMinor: Minor, goalMinor: Minor): Float {
        if (goalMinor <= 0 || totalMinor <= 0) return 0f
        return totalMinor.toFloat() / goalMinor.toFloat()
    }
}
