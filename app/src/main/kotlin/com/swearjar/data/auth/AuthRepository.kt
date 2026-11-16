package com.swearjar.data.auth

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.swearjar.R
import com.swearjar.domain.model.UserProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await

sealed interface SignInResult {
    data object Success : SignInResult

    /** The person backed out of the Google sheet. Not an error. */
    data object Cancelled : SignInResult

    /** There is no Google account on the device to sign in with. */
    data object NoAccount : SignInResult

    data object Failed : SignInResult
}

/** Google sign-in via Credential Manager, exchanged for a Firebase Authentication session. */
@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val appContext: Context,
) {

    /** The signed-in user, or null. Emits immediately and on every sign-in or sign-out. */
    val user: Flow<UserProfile?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.toProfile()) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    /**
     * Shows the Google account sheet and signs in to Firebase with the chosen account.
     *
     * @param activityContext must be an Activity context; Credential Manager draws over it
     */
    suspend fun signInWithGoogle(activityContext: Context): SignInResult {
        // default_web_client_id is generated from google-services.json by the Google services
        // plugin once Google sign-in is enabled on the Firebase project.
        val option = GetSignInWithGoogleOption
            .Builder(appContext.getString(R.string.default_web_client_id))
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()

        val credential = try {
            CredentialManager.create(activityContext).getCredential(activityContext, request).credential
        } catch (e: GetCredentialCancellationException) {
            return SignInResult.Cancelled
        } catch (e: NoCredentialException) {
            return SignInResult.NoAccount
        } catch (e: GetCredentialException) {
            Log.w(TAG, "Google credential request failed", e)
            return SignInResult.Failed
        }

        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            Log.w(TAG, "Unexpected credential type: ${credential.type}")
            return SignInResult.Failed
        }

        return try {
            val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
            auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
            saveProfile()
            SignInResult.Success
        } catch (e: CancellationException) {
            throw e
        } catch (e: GoogleIdTokenParsingException) {
            Log.w(TAG, "Could not read the Google ID token", e)
            SignInResult.Failed
        } catch (e: Exception) {
            Log.w(TAG, "Firebase sign-in failed", e)
            SignInResult.Failed
        }
    }

    suspend fun signOut() {
        auth.signOut()
        // Also forget the Google account choice, so the next sign-in offers the picker again.
        try {
            CredentialManager.create(appContext).clearCredentialState(ClearCredentialStateRequest())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Could not clear credential state", e)
        }
    }

    /**
     * Writes users/{uid}. A failure here (offline, database not yet created) must not undo a
     * successful sign-in, so it is logged and the profile is written again next time.
     */
    private suspend fun saveProfile() {
        val profile = auth.currentUser?.toProfile() ?: return
        val doc = firestore.collection(USERS).document(profile.uid)
        try {
            firestore.runTransaction { tx ->
                val fields = mutableMapOf<String, Any>(
                    FIELD_DISPLAY_NAME to profile.displayName,
                    FIELD_UPDATED_AT to FieldValue.serverTimestamp(),
                )
                if (tx.get(doc).exists()) {
                    fields[FIELD_PHOTO_URL] = profile.photoUrl ?: FieldValue.delete()
                    tx.update(doc, fields)
                } else {
                    profile.photoUrl?.let { fields[FIELD_PHOTO_URL] = it }
                    fields[FIELD_CREATED_AT] = FieldValue.serverTimestamp()
                    tx.set(doc, fields)
                }
                null
            }.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Could not save the user profile", e)
        }
    }

    private fun FirebaseUser.toProfile() = UserProfile(
        uid = uid,
        displayName = UserProfile.cleanName(displayName),
        // Only https photo URLs are stored; firestore.rules rejects anything else.
        photoUrl = photoUrl?.toString()?.takeIf { it.startsWith("https://") && it.length <= MAX_PHOTO_URL },
    )

    private companion object {
        const val TAG = "AuthRepository"
        const val USERS = "users"
        const val FIELD_DISPLAY_NAME = "displayName"
        const val FIELD_PHOTO_URL = "photoUrl"
        const val FIELD_CREATED_AT = "createdAt"
        const val FIELD_UPDATED_AT = "updatedAt"
        const val MAX_PHOTO_URL = 2000
    }
}
