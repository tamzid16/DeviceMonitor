package com.omega.devicemonitor.util

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object SecurityHelper {
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val VERSION = "pbkdf2_sha256"
    private const val ITERATIONS = 600_000
    private const val KEY_LENGTH_BITS = 256
    private const val LEGACY_SALT = "DeviceMonitor_Omega_Salt_2026"
    private const val SALT_LENGTH_BYTES = 16

    fun hashPassword(password: String): String {
        val salt = ByteArray(SALT_LENGTH_BYTES).also(SecureRandom()::nextBytes)
        val hash = deriveKey(password, salt, ITERATIONS)
        return listOf(
            VERSION,
            ITERATIONS.toString(),
            toHex(salt),
            toHex(hash)
        ).joinToString("$")
    }

    fun verifyPassword(password: String, storedHash: String): Boolean {
        if (password.isBlank() || storedHash.isBlank()) return false

        val parts = storedHash.split('$')
        if (parts.size == 4 && parts[0] == VERSION) {
            val iterations = parts[1].toIntOrNull() ?: return false
            if (iterations < 1) return false
            return try {
                val salt = fromHex(parts[2])
                val expected = fromHex(parts[3])
                val actual = deriveKey(password, salt, iterations)
                MessageDigest.isEqual(actual, expected)
            } catch (_: IllegalArgumentException) {
                false
            }
        }

        // Verify existing accounts created by the earlier fixed-salt SHA-256 implementation.
        return MessageDigest.isEqual(
            legacyHash(password).toByteArray(Charsets.UTF_8),
            storedHash.lowercase().toByteArray(Charsets.UTF_8)
        )
    }

    fun needsRehash(storedHash: String): Boolean = !storedHash.startsWith("$VERSION\$")

    private fun deriveKey(password: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun legacyHash(password: String): String {
        val input = "$LEGACY_SALT:$password"
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun toHex(value: ByteArray): String =
        value.joinToString("") { "%02x".format(it) }

    private fun fromHex(value: String): ByteArray {
        require(value.length % 2 == 0) { "Invalid hexadecimal value" }
        return ByteArray(value.length / 2) { index ->
            val start = index * 2
            value.substring(start, start + 2).toInt(16).toByte()
        }
    }
}
