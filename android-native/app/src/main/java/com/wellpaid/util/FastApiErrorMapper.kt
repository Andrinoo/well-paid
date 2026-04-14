package com.wellpaid.util

import android.content.Context
import com.wellpaid.R
import retrofit2.HttpException

object FastApiErrorMapper {

    fun message(context: Context, t: Throwable): String {
        if (t !is HttpException) {
            return context.getString(R.string.login_error_network)
        }
        val raw = try {
            t.response()?.errorBody()?.string().orEmpty()
        } catch (_: Exception) {
            ""
        }
        extractJsonDetail(raw)?.let { return it }
        return when (t.code()) {
            400 -> context.getString(R.string.error_bad_request)
            401 -> context.getString(R.string.login_error_invalid)
            403 -> context.getString(R.string.login_error_forbidden)
            404 -> context.getString(R.string.error_not_found)
            409 -> context.getString(R.string.error_conflict)
            422 -> context.getString(R.string.login_error_validation)
            429 -> context.getString(R.string.login_error_rate_limit)
            else -> context.getString(R.string.login_error_generic)
        }
    }

    private fun extractJsonDetail(json: String): String? {
        val key = "\"detail\""
        val i = json.indexOf(key)
        if (i < 0) return null
        val afterColon = json.indexOf(':', i).takeIf { it >= 0 } ?: return null
        var rest = json.substring(afterColon + 1).trimStart()
        if (rest.startsWith('"')) {
            rest = rest.drop(1)
            val end = rest.indexOf('"')
            if (end > 0) return rest.substring(0, end)
        }
        return null
    }
}
