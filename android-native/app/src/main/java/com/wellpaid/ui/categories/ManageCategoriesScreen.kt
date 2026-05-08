package com.wellpaid.ui.categories

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import com.wellpaid.ui.components.WellPaidBrandCircularProgress
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wellpaid.R
import com.wellpaid.ui.theme.WellPaidCardWhite
import com.wellpaid.ui.theme.WellPaidExpenseLine
import com.wellpaid.ui.theme.WellPaidNavy
import com.wellpaid.ui.theme.WellPaidPositive
import com.wellpaid.ui.theme.wellPaidCenterTopAppBarColors
import com.wellpaid.ui.theme.wellPaidScreenHorizontalPadding

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCategoriesScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ManageCategoriesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        val msg = state.errorMessage ?: return@LaunchedEffect
        snackHostState.showSnackbar(msg)
        viewModel.consumeError()
    }

    LaunchedEffect(state.successMessage) {
        val msg = state.successMessage ?: return@LaunchedEffect
        snackHostState.showSnackbar(msg)
        viewModel.consumeSuccess()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                modifier = Modifier.height(68.dp),
                colors = wellPaidCenterTopAppBarColors(),
                title = {
                    Text(
                        text = stringResource(R.string.manage_categories_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.padding(top = 6.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        if (state.isLoading && state.expenseCategories.isEmpty() && state.incomeCategories.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                WellPaidBrandCircularProgress()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .wellPaidScreenHorizontalPadding()
                .padding(top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            CategoryFormCard(
                sectionTitle = stringResource(R.string.manage_categories_expense_section),
                accentColor = WellPaidExpenseLine,
                accentIcon = Icons.Outlined.ShoppingCart,
                nameValue = state.newExpenseName,
                onNameChange = viewModel::onExpenseNameChange,
                onSubmit = { viewModel.submitExpenseCategory() },
                submitEnabled = state.newExpenseName.isNotBlank() && !state.isSavingExpense,
                isSaving = state.isSavingExpense,
                categories = state.expenseCategories.map { it.name },
            )
            CategoryFormCard(
                sectionTitle = stringResource(R.string.manage_categories_income_section),
                accentColor = WellPaidPositive,
                accentIcon = Icons.Outlined.AttachMoney,
                nameValue = state.newIncomeName,
                onNameChange = viewModel::onIncomeNameChange,
                onSubmit = { viewModel.submitIncomeCategory() },
                submitEnabled = state.newIncomeName.isNotBlank() && !state.isSavingIncome,
                isSaving = state.isSavingIncome,
                categories = state.incomeCategories.map { it.name },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryFormCard(
    sectionTitle: String,
    accentColor: Color,
    accentIcon: ImageVector,
    nameValue: String,
    onNameChange: (String) -> Unit,
    onSubmit: () -> Unit,
    submitEnabled: Boolean,
    isSaving: Boolean,
    categories: List<String>,
) {
    var registeredExpanded by remember { mutableStateOf(false) }
    LaunchedEffect(categories) {
        if (categories.isEmpty()) registeredExpanded = false
    }
    val shape = RoundedCornerShape(20.dp)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = WellPaidCardWhite,
        tonalElevation = 0.dp,
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier
                .border(1.dp, WellPaidNavy.copy(alpha = 0.06f), shape)
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = accentIcon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.size(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = sectionTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = WellPaidNavy,
                    )
                    Text(
                        text = pluralCount(categories.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = WellPaidNavy.copy(alpha = 0.55f),
                    )
                }
            }

            OutlinedTextField(
                value = nameValue,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.manage_categories_name_hint)) },
                singleLine = true,
                enabled = !isSaving,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor.copy(alpha = 0.9f),
                    unfocusedBorderColor = WellPaidNavy.copy(alpha = 0.2f),
                    focusedLabelColor = accentColor.copy(alpha = 0.9f),
                ),
            )
            Button(
                onClick = onSubmit,
                enabled = submitEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WellPaidNavy,
                    contentColor = Color.White,
                    disabledContainerColor = WellPaidNavy.copy(alpha = 0.4f),
                    disabledContentColor = Color.White.copy(alpha = 0.85f),
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    if (isSaving) {
                        stringResource(R.string.manage_categories_saving)
                    } else {
                        stringResource(R.string.manage_categories_add)
                    },
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Text(
                text = stringResource(R.string.manage_categories_existing_dropdown_label),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
            OutlinedButton(
                onClick = {
                    if (categories.isNotEmpty()) registeredExpanded = !registeredExpanded
                },
                enabled = categories.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = WellPaidNavy,
                    disabledContentColor = WellPaidNavy.copy(alpha = 0.45f),
                ),
                border = BorderStroke(1.dp, WellPaidNavy.copy(alpha = 0.22f)),
            ) {
                Text(
                    if (categories.isEmpty()) {
                        stringResource(R.string.manage_categories_existing_empty)
                    } else {
                        stringResource(
                            R.string.manage_categories_existing_summary,
                            categories.size,
                        )
                    },
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = if (registeredExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                )
            }
            AnimatedVisibility(visible = registeredExpanded && categories.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = accentColor.copy(alpha = 0.05f),
                    tonalElevation = 0.dp,
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        categories.forEach { name ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(accentColor),
                                )
                                Spacer(Modifier.size(10.dp))
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = WellPaidNavy,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun pluralCount(size: Int): String =
    when (size) {
        0 -> stringResource(R.string.manage_categories_count_zero)
        1 -> stringResource(R.string.manage_categories_count_one)
        else -> stringResource(R.string.manage_categories_count_many, size)
    }
