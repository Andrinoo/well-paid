package com.wellpaid.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wellpaid.R
import com.wellpaid.ui.theme.WellPaidGold
import com.wellpaid.ui.theme.WellPaidNavy

/** Botão principal dourado + refresh compacto (listas Despesas / Proventos / Metas). */
@Composable
fun WellPaidPrimaryAddRow(
    label: String,
    leadingIcon: ImageVector,
    onPrimaryClick: () -> Unit,
    onRefresh: () -> Unit,
    refreshEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            onClick = onPrimaryClick,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = WellPaidGold,
                contentColor = WellPaidNavy,
            ),
        ) {
            Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
            )
        }
        FilledTonalIconButton(
            onClick = onRefresh,
            enabled = refreshEnabled,
            modifier = Modifier.size(52.dp),
            shape = RoundedCornerShape(14.dp),
        ) {
            Icon(
                Icons.Filled.Refresh,
                contentDescription = stringResource(R.string.home_refresh),
            )
        }
    }
}
