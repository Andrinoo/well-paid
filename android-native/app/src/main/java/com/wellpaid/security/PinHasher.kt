package com.wellpaid.security

import java.security.MessageDigest
import java.security.SecureRandom

internal object PinHasher {
    private val random = SecureRandom()

    fun newSaltHex(): String {
        val b = ByteArray(16)
        random.nextBytes(b)
        return b.joinToString("") { "%02x".format(it) }
    }

    fun hashPin(pin: String, saltHex: String): String {
        val salt = hexToBytes(saltHex)
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt)
        md.update(pin.toByteArray(Charsets.UTF_8))
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    fun verify(pin: String, saltHex: String?, hashHex: String?): Boolean {
        if (saltHex.isNullOrBlank() || hashHex.isNullOrBlank()) return false
        return hashPin(pin, saltHex).equals(hashHex, ignoreCase = true)
    }

    private fun hexToBytes(hex: String): ByteArray {
        require(hex.length % 2 == 0) { "hex inválido" }
        return ByteArray(hex.length / 2) { i ->
            hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }
}
