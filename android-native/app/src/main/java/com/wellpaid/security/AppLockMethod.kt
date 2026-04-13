package com.wellpaid.security

enum class AppLockMethod {
    PIN_ONLY,
    BIOMETRIC_ONLY,
    BOTH;

    fun toStorage(): String =
        when (this) {
            PIN_ONLY -> "pin_only"
            BIOMETRIC_ONLY -> "biometric_only"
            BOTH -> "both"
        }

    companion object {
        fun fromStorage(raw: String?): AppLockMethod =
            when (raw) {
                "pin_only" -> PIN_ONLY
                "biometric_only" -> BIOMETRIC_ONLY
                "both" -> BOTH
                else -> BOTH
            }
    }
}
