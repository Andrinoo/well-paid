package com.wellpaid.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.wellpaid.ui.theme.LoginBg
import com.wellpaid.ui.theme.LoginCard
import com.wellpaid.ui.theme.LoginCardBorder
import com.wellpaid.ui.theme.LoginFieldFill
import com.wellpaid.ui.theme.LoginGold
import com.wellpaid.ui.theme.LoginOnCard
import com.wellpaid.ui.theme.WellPaidLoginTheme
import com.wellpaid.ui.theme.WellPaidMaxAuthWidth
import com.wellpaid.ui.theme.wellPaidMaxContentWidth
import com.wellpaid.ui.theme.wellPaidScreenHorizontalPadding

@Composable
fun authOutlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = LoginOnCard,
    unfocusedTextColor = LoginOnCard,
    focusedLabelColor = LoginGold,
    unfocusedLabelColor = LoginGold.copy(alpha = 0.9f),
    focusedBorderColor = LoginGold,
    unfocusedBorderColor = LoginCardBorder.copy(alpha = 0.55f),
    cursorColor = LoginGold,
    focusedContainerColor = LoginFieldFill,
    unfocusedContainerColor = LoginFieldFill,
)

/**
 * Fundo preto #000000, tema auth, cartão com borda dourada; scroll + IME + barras do sistema.
 * Conteúdo limitado em largura para tablets.
 */
@Composable
fun AuthScreenShell(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    WellPaidLoginTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LoginBg),
        ) {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .wellPaidScreenHorizontalPadding()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wellPaidMaxContentWidth(WellPaidMaxAuthWidth),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = LoginCard),
                        border = BorderStroke(1.dp, LoginCardBorder.copy(alpha = 0.85f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
                            content = content,
                        )
                    }
                }
            }
        }
    }
}
