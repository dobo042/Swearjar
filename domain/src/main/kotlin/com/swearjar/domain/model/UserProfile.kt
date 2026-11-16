package com.swearjar.domain.model

/** The signed-in person. Deliberately holds no email: nothing in the app needs it. */
data class UserProfile(
    val uid: String,
    val displayName: String,
    val photoUrl: String?,
) {
    companion object {
        const val MAX_NAME_LENGTH = 80

        /** Tidies whatever name the sign-in provider supplied into one the rules will accept. */
        fun cleanName(raw: String?): String =
            raw?.trim()?.take(MAX_NAME_LENGTH)?.takeIf { it.isNotEmpty() } ?: "Someone"
    }
}
