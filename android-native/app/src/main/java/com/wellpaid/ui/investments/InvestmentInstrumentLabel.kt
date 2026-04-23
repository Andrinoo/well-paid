package com.wellpaid.ui.investments

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wellpaid.R
import java.util.Locale

@Composable
fun investmentInstrumentLabel(
    key: String,
    fallback: String? = null,
): String {
    return when (key.lowercase(Locale.ROOT)) {
        "cdi" -> stringResource(R.string.investments_bucket_cdi)
        "cdb" -> stringResource(R.string.investments_bucket_cdb)
        "fixed_income" -> stringResource(R.string.investments_bucket_fixed_income)
        "tesouro", "treasury" -> stringResource(R.string.investments_bucket_tesouro)
        "stocks", "stock", "fii", "bdr", "etf" -> stringResource(R.string.investments_bucket_stocks)
        else -> fallback ?: key.uppercase(Locale.ROOT)
    }
}
