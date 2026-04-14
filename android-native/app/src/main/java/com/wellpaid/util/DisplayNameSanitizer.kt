package com.wellpaid.util

private val uuidRegex =
    Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

/** Evita mostrar `sub` do JWT ou IDs como saudação. */
fun String.looksLikeUuid(): Boolean = trim().matches(uuidRegex)
