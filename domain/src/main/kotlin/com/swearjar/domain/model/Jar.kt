package com.swearjar.domain.model

/** All money is held as whole minor units (pence, cents) of the jar's currency. */
typealias Minor = Long

enum class Role {
    OWNER, ADMIN, MEMBER;

    val isAdmin: Boolean get() = this == OWNER || this == ADMIN
}

enum class JarStatus { ACTIVE, CLOSED }

data class Jar(
    val id: String,
    val name: String,
    val ownerId: String,
    /** ISO 4217 code, e.g. "GBP". */
    val currency: String,
    val inviteCode: String,
    val settings: JarSettings,
    val currentRoundId: String,
    /** Target for the current round. Zero means the jar has no goal. */
    val goalMinor: Minor,
    /** Total logged in the current round. Drives the jar visual. */
    val roundTotalMinor: Minor,
    val status: JarStatus = JarStatus.ACTIVE,
)

data class Member(
    val userId: String,
    val displayName: String,
    val photoUrl: String?,
    val role: Role,
    /** Everything logged against this member, across all rounds. */
    val owedMinor: Minor = 0,
    /** Everything this member has settled. */
    val paidMinor: Minor = 0,
) {
    val outstandingMinor: Minor get() = owedMinor - paidMinor
}

data class Topic(
    val id: String,
    val name: String,
    val emoji: String,
    val priceMinor: Minor,
    val archived: Boolean = false,
)

data class Round(
    val id: String,
    val goalMinor: Minor,
    val totalMinor: Minor,
    val startedAtMillis: Long,
    val completedAtMillis: Long? = null,
)

enum class EntryStatus {
    ACTIVE,

    /** Challenged by the accused; still counts until an admin voids it. */
    DISPUTED,

    /** Cancelled. Its amount has been taken back off the totals. */
    VOIDED,
}

/** One logged offence. Entries are never deleted, only voided. */
data class Entry(
    val id: String,
    val roundId: String,
    val topicId: String,
    /** Copied from the topic so history survives the topic being renamed. */
    val topicName: String,
    val offenderId: String,
    val loggedById: String,
    val amountMinor: Minor,
    val note: String?,
    val status: EntryStatus,
    val createdAtMillis: Long,
)

/** A record that a member paid some of what they owe, outside the app. */
data class Settlement(
    val id: String,
    val memberId: String,
    val amountMinor: Minor,
    val recordedById: String,
    val createdAtMillis: Long,
)
