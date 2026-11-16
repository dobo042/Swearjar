package com.swearjar.domain.rules

import kotlin.random.Random

/** Short codes people read aloud or type to join a jar. */
object InviteCodes {
    const val LENGTH = 6

    /** No 0/O, 1/I/L or other look-alikes. */
    const val ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"

    fun generate(random: Random = Random.Default): String =
        buildString(LENGTH) { repeat(LENGTH) { append(ALPHABET[random.nextInt(ALPHABET.length)]) } }

    /**
     * Tidies what someone typed or scanned: trims, upper-cases, drops spaces and dashes.
     * Returns null if the result is not a well-formed code.
     */
    fun normalise(input: String): String? {
        val cleaned = input.uppercase().filterNot { it.isWhitespace() || it == '-' }
        return cleaned.takeIf { it.length == LENGTH && it.all(ALPHABET::contains) }
    }
}
