package com.swearjar.data.jar

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.swearjar.domain.model.Escalation
import com.swearjar.domain.model.GoalBehaviour
import com.swearjar.domain.model.Jar
import com.swearjar.domain.model.JarDraft
import com.swearjar.domain.model.JarSettings
import com.swearjar.domain.model.JarStatus
import com.swearjar.domain.model.LoggingPolicy
import com.swearjar.domain.model.Member
import com.swearjar.domain.model.PricingMode
import com.swearjar.domain.model.Role
import com.swearjar.domain.model.UserProfile
import com.swearjar.domain.rules.InviteCodes
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

sealed interface JoinResult {
    data class Joined(val jarId: String) : JoinResult

    /** Already in this jar; just open it. */
    data class AlreadyMember(val jarId: String) : JoinResult

    /** What was typed or scanned is not shaped like an invite code. */
    data object InvalidCode : JoinResult

    /** Well-formed, but no jar has this code. */
    data object NotFound : JoinResult

    /** The jar exists but is full or closed. */
    data object Refused : JoinResult
}

/**
 * Jars in Firestore. The document shapes written here are the ones firestore.rules validates;
 * change the two together, and add a case to firestore-tests.
 */
@Singleton
class JarRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) {

    /** Every jar the user belongs to, by name, kept up to date. */
    fun observeMyJars(uid: String): Flow<List<Jar>> =
        firestore.collection(JARS).whereArrayContains(F_MEMBER_IDS, uid).snapshots { docs ->
            docs.mapNotNull { it.toJar() }.sortedBy { it.name.lowercase() }
        }

    /** One jar, or null if it does not exist. */
    fun observeJar(jarId: String): Flow<Jar?> = callbackFlow {
        val registration = firestore.collection(JARS).document(jarId).addSnapshotListener { snapshot, error ->
            if (error != null) close(error) else trySend(snapshot?.toJar())
        }
        awaitClose { registration.remove() }
    }

    /** A jar's members, owner first, then by name. */
    fun observeMembers(jarId: String): Flow<List<Member>> =
        firestore.collection(JARS).document(jarId).collection(MEMBERS).snapshots { docs ->
            docs.mapNotNull { it.toMember() }.sortedWith(compareBy({ it.role.ordinal }, { it.displayName.lowercase() }))
        }

    /**
     * Creates a jar with [owner] as its only member and returns its id. The jar, its invite
     * code, its first round and the owner's member document are written as one batch.
     */
    suspend fun createJar(owner: UserProfile, draft: JarDraft): String {
        require(draft.problems().isEmpty()) { "Invalid jar: ${draft.problems()}" }
        var lastError: Exception? = null
        // An invite code that is already taken makes the batch fail; try another.
        repeat(CODE_ATTEMPTS) {
            val code = InviteCodes.generate()
            val jarRef = firestore.collection(JARS).document()
            val roundRef = jarRef.collection(ROUNDS).document()
            val batch = firestore.batch()
            batch.set(
                jarRef,
                mapOf(
                    F_NAME to draft.cleanName,
                    F_OWNER_ID to owner.uid,
                    F_CURRENCY to draft.currency,
                    F_INVITE_CODE to code,
                    F_MEMBER_IDS to listOf(owner.uid),
                    F_SETTINGS to draft.settings.toMap(),
                    F_CURRENT_ROUND_ID to roundRef.id,
                    F_GOAL_MINOR to draft.goalMinor,
                    F_ROUND_TOTAL_MINOR to 0L,
                    F_STATUS to JarStatus.ACTIVE.name,
                    F_CREATED_AT to FieldValue.serverTimestamp(),
                ),
            )
            batch.set(firestore.collection(INVITE_CODES).document(code), mapOf(F_JAR_ID to jarRef.id))
            batch.set(jarRef.collection(MEMBERS).document(owner.uid), memberFields(owner, Role.OWNER, code))
            batch.set(
                roundRef,
                mapOf(
                    F_GOAL_MINOR to draft.goalMinor,
                    F_TOTAL_MINOR to 0L,
                    F_STARTED_AT to FieldValue.serverTimestamp(),
                ),
            )
            try {
                batch.commit().await()
                return jarRef.id
            } catch (e: FirebaseFirestoreException) {
                if (e.code != FirebaseFirestoreException.Code.PERMISSION_DENIED) throw e
                Log.w(TAG, "Jar creation refused; retrying with another invite code", e)
                lastError = e
            }
        }
        throw lastError ?: IllegalStateException("Could not create the jar")
    }

    /** Adds [user] to the jar with invite code [rawCode]. */
    suspend fun joinJar(user: UserProfile, rawCode: String): JoinResult {
        val code = InviteCodes.normalise(rawCode) ?: return JoinResult.InvalidCode
        val jarId = firestore.collection(INVITE_CODES).document(code).get().await()
            .getString(F_JAR_ID) ?: return JoinResult.NotFound
        val jarRef = firestore.collection(JARS).document(jarId)

        // Only members can read a jar, so a successful read means there is nothing to do.
        val alreadyIn = try {
            jarRef.get().await().exists()
        } catch (e: FirebaseFirestoreException) {
            if (e.code != FirebaseFirestoreException.Code.PERMISSION_DENIED) throw e
            false
        }
        if (alreadyIn) return JoinResult.AlreadyMember(jarId)

        val batch = firestore.batch()
        batch.update(jarRef, F_MEMBER_IDS, FieldValue.arrayUnion(user.uid))
        batch.set(jarRef.collection(MEMBERS).document(user.uid), memberFields(user, Role.MEMBER, code))
        return try {
            batch.commit().await()
            JoinResult.Joined(jarId)
        } catch (e: FirebaseFirestoreException) {
            if (e.code != FirebaseFirestoreException.Code.PERMISSION_DENIED) throw e
            JoinResult.Refused
        }
    }

    private fun memberFields(user: UserProfile, role: Role, code: String): Map<String, Any> = buildMap {
        put(F_ROLE, role.name)
        put(F_DISPLAY_NAME, user.displayName)
        user.photoUrl?.let { put(F_PHOTO_URL, it) }
        put(F_OWED_MINOR, 0L)
        put(F_PAID_MINOR, 0L)
        put(F_JOIN_CODE, code)
        put(F_JOINED_AT, FieldValue.serverTimestamp())
    }

    private fun <T> Query.snapshots(transform: (List<DocumentSnapshot>) -> T): Flow<T> = callbackFlow {
        val registration = addSnapshotListener { snapshot, error ->
            if (error != null) close(error) else if (snapshot != null) trySend(transform(snapshot.documents))
        }
        awaitClose { registration.remove() }
    }

    private companion object {
        const val TAG = "JarRepository"
        const val CODE_ATTEMPTS = 4

        const val JARS = "jars"
        const val MEMBERS = "members"
        const val ROUNDS = "rounds"
        const val INVITE_CODES = "inviteCodes"

        const val F_NAME = "name"
        const val F_OWNER_ID = "ownerId"
        const val F_CURRENCY = "currency"
        const val F_INVITE_CODE = "inviteCode"
        const val F_MEMBER_IDS = "memberIds"
        const val F_SETTINGS = "settings"
        const val F_CURRENT_ROUND_ID = "currentRoundId"
        const val F_GOAL_MINOR = "goalMinor"
        const val F_ROUND_TOTAL_MINOR = "roundTotalMinor"
        const val F_STATUS = "status"
        const val F_CREATED_AT = "createdAt"
        const val F_JAR_ID = "jarId"
        const val F_TOTAL_MINOR = "totalMinor"
        const val F_STARTED_AT = "startedAt"
        const val F_ROLE = "role"
        const val F_DISPLAY_NAME = "displayName"
        const val F_PHOTO_URL = "photoUrl"
        const val F_OWED_MINOR = "owedMinor"
        const val F_PAID_MINOR = "paidMinor"
        const val F_JOIN_CODE = "joinCode"
        const val F_JOINED_AT = "joinedAt"

        const val S_LOGGING_POLICY = "loggingPolicy"
        const val S_PRICING_MODE = "pricingMode"
        const val S_GOAL_BEHAVIOUR = "goalBehaviour"
        const val S_ESCALATION = "escalation"
        const val E_EVERY = "every"
        const val E_MULTIPLIER_PERCENT = "multiplierPercent"
        const val E_WINDOW_DAYS = "windowDays"
        const val E_MAX_STEPS = "maxSteps"

        fun JarSettings.toMap(): Map<String, Any> = mapOf(
            S_LOGGING_POLICY to loggingPolicy.name,
            S_PRICING_MODE to pricingMode.name,
            S_GOAL_BEHAVIOUR to goalBehaviour.name,
            S_ESCALATION to mapOf(
                E_EVERY to escalation.every.toLong(),
                E_MULTIPLIER_PERCENT to escalation.multiplierPercent.toLong(),
                E_WINDOW_DAYS to escalation.windowDays.toLong(),
                E_MAX_STEPS to escalation.maxSteps.toLong(),
            ),
        )

        /** Null (and logged) if the document is missing or not shaped like a jar. */
        fun DocumentSnapshot.toJar(): Jar? {
            if (!exists()) return null
            return try {
                Jar(
                    id = id,
                    name = getString(F_NAME)!!,
                    ownerId = getString(F_OWNER_ID)!!,
                    currency = getString(F_CURRENCY)!!,
                    inviteCode = getString(F_INVITE_CODE)!!,
                    settings = settingsFrom(get(F_SETTINGS) as Map<*, *>),
                    currentRoundId = getString(F_CURRENT_ROUND_ID)!!,
                    goalMinor = getLong(F_GOAL_MINOR)!!,
                    roundTotalMinor = getLong(F_ROUND_TOTAL_MINOR)!!,
                    status = JarStatus.valueOf(getString(F_STATUS)!!),
                    memberIds = (get(F_MEMBER_IDS) as List<*>).filterIsInstance<String>(),
                )
            } catch (e: RuntimeException) {
                Log.w(TAG, "Skipping malformed jar $id", e)
                null
            }
        }

        fun settingsFrom(map: Map<*, *>): JarSettings {
            val e = map[S_ESCALATION] as Map<*, *>
            return JarSettings(
                loggingPolicy = LoggingPolicy.valueOf(map[S_LOGGING_POLICY] as String),
                pricingMode = PricingMode.valueOf(map[S_PRICING_MODE] as String),
                escalation = Escalation(
                    every = (e[E_EVERY] as Long).toInt(),
                    multiplierPercent = (e[E_MULTIPLIER_PERCENT] as Long).toInt(),
                    windowDays = (e[E_WINDOW_DAYS] as Long).toInt(),
                    maxSteps = (e[E_MAX_STEPS] as Long).toInt(),
                ),
                goalBehaviour = GoalBehaviour.valueOf(map[S_GOAL_BEHAVIOUR] as String),
            )
        }

        fun DocumentSnapshot.toMember(): Member? = try {
            Member(
                userId = id,
                displayName = getString(F_DISPLAY_NAME)!!,
                photoUrl = getString(F_PHOTO_URL),
                role = Role.valueOf(getString(F_ROLE)!!),
                owedMinor = getLong(F_OWED_MINOR)!!,
                paidMinor = getLong(F_PAID_MINOR)!!,
            )
        } catch (e: RuntimeException) {
            Log.w(TAG, "Skipping malformed member $id", e)
            null
        }
    }
}
