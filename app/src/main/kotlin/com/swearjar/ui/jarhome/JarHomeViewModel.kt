package com.swearjar.ui.jarhome

import androidx.lifecycle.ViewModel
import com.swearjar.domain.model.GoalBehaviour
import com.swearjar.domain.model.JarSettings
import com.swearjar.domain.model.JarStatus
import com.swearjar.domain.model.Minor
import com.swearjar.domain.model.PricingMode
import com.swearjar.domain.model.Topic
import com.swearjar.domain.rules.GoalOutcome
import com.swearjar.domain.rules.GoalRules
import com.swearjar.domain.rules.PricingRules
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class JarHomeState(
    val jarName: String,
    val currency: String,
    val settings: JarSettings,
    val topics: List<Topic>,
    val goalMinor: Minor,
    val roundTotalMinor: Minor,
    val round: Int,
    val entryCount: Int,
    val status: JarStatus,
    /** Offences per topic this round, for escalating prices. */
    val offencesByTopic: Map<String, Int>,
    /** Increments each time the goal is hit, so the screen can replay the celebration. */
    val celebrations: Int,
) {
    val fillFraction: Float get() = GoalRules.fillFraction(roundTotalMinor, goalMinor)

    fun priceOf(topic: Topic): Minor =
        PricingRules.priceFor(settings, topic.priceMinor, priorOffences = offencesByTopic[topic.id] ?: 0)
}

/**
 * Drives the jar screen from an in-memory demo jar so the jar and the rules engine can be
 * exercised before sign-in and sync exist. The state shape is what the Firestore-backed
 * repository will supply; only the source changes.
 */
@HiltViewModel
class JarHomeViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(demoJar())
    val state: StateFlow<JarHomeState> = _state.asStateFlow()

    fun logOffence(topic: Topic) = _state.update { jar ->
        if (jar.status == JarStatus.CLOSED) return@update jar
        val amount = jar.priceOf(topic)
        val outcome = GoalRules.onEntryLogged(jar.settings.goalBehaviour, jar.goalMinor, jar.roundTotalMinor, amount)
        val logged = jar.copy(
            roundTotalMinor = jar.roundTotalMinor + amount,
            entryCount = jar.entryCount + 1,
            offencesByTopic = jar.offencesByTopic + (topic.id to (jar.offencesByTopic[topic.id] ?: 0) + 1),
            celebrations = jar.celebrations + if (outcome.goalReached) 1 else 0,
        )
        when (outcome) {
            GoalOutcome.None, GoalOutcome.KeepFilling -> logged
            GoalOutcome.CloseJar -> logged.copy(status = JarStatus.CLOSED)
            GoalOutcome.ResetRound -> logged.copy(
                roundTotalMinor = 0,
                round = jar.round + 1,
                entryCount = 0,
                offencesByTopic = emptyMap(),
            )
        }
    }

    private fun demoJar() = JarHomeState(
        jarName = "Demo jar",
        currency = "GBP",
        settings = JarSettings(pricingMode = PricingMode.ESCALATING, goalBehaviour = GoalBehaviour.RESET),
        topics = listOf(
            Topic("swearing", "Swearing", "🤬", 50),
            Topic("late", "Being late", "⏰", 200),
            Topic("phone", "Phone at dinner", "📱", 100),
        ),
        goalMinor = 2000,
        roundTotalMinor = 0,
        round = 1,
        entryCount = 0,
        status = JarStatus.ACTIVE,
        offencesByTopic = emptyMap(),
        celebrations = 0,
    )
}
