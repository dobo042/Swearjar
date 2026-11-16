package com.swearjar.domain.model

/** Who is allowed to log an offence. Chosen per jar. */
enum class LoggingPolicy {
    /** Any member can log an offence against any member. */
    ANYONE,

    /** As [ANYONE], but the accused can dispute an entry for an admin to resolve. */
    ANYONE_WITH_DISPUTE,

    /** Members can only log their own offences. */
    SELF_ONLY,

    /** Only the owner and admins can log offences. */
    ADMINS_ONLY,
}

/** How the cost of an offence is worked out. Chosen per jar. */
enum class PricingMode {
    /** Always the topic's price. */
    FIXED,

    /** The topic's price by default; whoever logs it may enter a different amount. */
    FIXED_WITH_OVERRIDE,

    /** The topic's price, multiplied for repeat offenders. See [Escalation]. */
    ESCALATING,
}

/** What happens when the round total reaches the goal. Chosen per jar. */
enum class GoalBehaviour {
    /** Archive the round and start a fresh one with the same topics and members. */
    RESET,

    /** Keep accepting entries; the jar overflows until a new goal is set. */
    KEEP_FILLING,

    /** Lock the jar. */
    CLOSE,
}

/**
 * Repeat-offence pricing. For every [every] offences a member already has on a topic within
 * the last [windowDays] days, the price is multiplied by [multiplierPercent] / 100, at most
 * [maxSteps] times.
 *
 * With the defaults: offences 1-3 cost the base price, 4-6 cost double, 7-9 four times,
 * and 10 onwards eight times.
 */
data class Escalation(
    val every: Int = 3,
    val multiplierPercent: Int = 200,
    val windowDays: Int = 7,
    val maxSteps: Int = 3,
) {
    init {
        require(every >= 1) { "every must be at least 1" }
        require(multiplierPercent >= 100) { "multiplierPercent must be at least 100" }
        require(windowDays >= 1) { "windowDays must be at least 1" }
        require(maxSteps >= 0) { "maxSteps must not be negative" }
    }
}

data class JarSettings(
    val loggingPolicy: LoggingPolicy = LoggingPolicy.ANYONE,
    val pricingMode: PricingMode = PricingMode.FIXED,
    val escalation: Escalation = Escalation(),
    val goalBehaviour: GoalBehaviour = GoalBehaviour.RESET,
)
