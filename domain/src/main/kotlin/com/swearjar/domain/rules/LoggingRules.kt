package com.swearjar.domain.rules

import com.swearjar.domain.model.Entry
import com.swearjar.domain.model.EntryStatus
import com.swearjar.domain.model.LoggingPolicy
import com.swearjar.domain.model.Role

/** Interprets a jar's [LoggingPolicy]. Mirrored by firestore.rules; keep the two in step. */
object LoggingRules {

    /** May [actorId] log an offence against [offenderId]? */
    fun canLog(policy: LoggingPolicy, actorId: String, actorRole: Role, offenderId: String): Boolean =
        when (policy) {
            LoggingPolicy.ANYONE, LoggingPolicy.ANYONE_WITH_DISPUTE -> true
            LoggingPolicy.SELF_ONLY -> actorId == offenderId
            LoggingPolicy.ADMINS_ONLY -> actorRole.isAdmin
        }

    /** Only the accused can dispute, only once, and only where the jar allows disputes. */
    fun canDispute(policy: LoggingPolicy, entry: Entry, actorId: String): Boolean =
        policy == LoggingPolicy.ANYONE_WITH_DISPUTE &&
            entry.status == EntryStatus.ACTIVE &&
            entry.offenderId == actorId &&
            entry.loggedById != actorId

    /** Admins decide disputes, but not ones raised against entries they logged themselves. */
    fun canResolveDispute(entry: Entry, actorId: String, actorRole: Role): Boolean =
        entry.status == EntryStatus.DISPUTED && actorRole.isAdmin && entry.loggedById != actorId

    /**
     * Admins can void any live entry. Whoever logged an entry can take it back, unless it is
     * one they logged against themselves (otherwise self-reporting could be undone at will).
     */
    fun canVoid(entry: Entry, actorId: String, actorRole: Role): Boolean {
        if (entry.status == EntryStatus.VOIDED) return false
        if (actorRole.isAdmin) return true
        return entry.loggedById == actorId && entry.offenderId != actorId
    }
}
