package com.wellpaid.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wellpaid.R
import com.wellpaid.ui.theme.WellPaidGold
import com.wellpaid.ui.theme.WellPaidNavy
import com.wellpaid.util.formatIsoToUiDate
import com.wellpaid.util.localDateToIso
import com.wellpaid.util.parseIsoDateLocal
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private const val MIN_YEAR = 2000
private const val MAX_YEAR = 2100

/** Altura visível das colunas; o item ativo fica ao centro (ver [wheelVerticalContentPadding]). */
private val wheelRowHeight = 220.dp
private val wheelPageHeight = 44.dp
private val wheelVerticalContentPadding = (wheelRowHeight - wheelPageHeight) / 2
private val wheelPageSize = PageSize.Fixed(wheelPageHeight)

/** Larguras mínimas para não cortar rótulos (ex.: “Setembro”) nem anos de 4 dígitos. */
private val wheelDayMinWidth = 56.dp
private val wheelMonthMinWidth = 132.dp
private val wheelYearMinWidth = 68.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WellPaidDatePickerField(
    label: @Composable () -> Unit,
    isoDate: String,
    onIsoDateChange: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
    shape: RoundedCornerShape = RoundedCornerShape(12.dp),
) {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    var open by remember { mutableStateOf(false) }
    val display = if (isoDate.isNotBlank()) formatIsoToUiDate(isoDate, locale) else ""

    OutlinedTextField(
        value = display,
        onValueChange = {},
        readOnly = true,
        enabled = enabled,
        label = label,
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                enabled = enabled,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) { open = true },
        trailingIcon = {
            IconButton(
                onClick = { open = true },
                enabled = enabled,
            ) {
                Icon(Icons.Default.DateRange, contentDescription = null)
            }
        },
        shape = shape,
        colors = colors,
    )

    if (open) {
        val sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { new -> new != SheetValue.Hidden },
        )
        ModalBottomSheet(
            onDismissRequest = { open = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 48.dp)
                            .height(4.dp)
                            .background(
                                WellPaidNavy.copy(alpha = 0.25f),
                                RoundedCornerShape(2.dp),
                            ),
                    )
                }
            },
        ) {
            WellPaidDateWheelContent(
                locale = locale,
                initialDate = remember(isoDate, open) {
                    parseIsoDateLocal(isoDate) ?: LocalDate.now()
                },
                onDismiss = { open = false },
                onConfirm = { date ->
                    onIsoDateChange(localDateToIso(date))
                    open = false
                },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WellPaidDateWheelContent(
    locale: Locale,
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val yearCount = MAX_YEAR - MIN_YEAR + 1

    val yearPager = rememberPagerState(
        initialPage = (initialDate.year - MIN_YEAR).coerceIn(0, yearCount - 1),
        pageCount = { yearCount },
    )
    val monthPager = rememberPagerState(
        initialPage = (initialDate.monthValue - 1).coerceIn(0, 11),
        pageCount = { 12 },
    )

    val ySel = MIN_YEAR + yearPager.settledPage
    val mSel = monthPager.settledPage + 1
    val lastDayInMonth = YearMonth.of(ySel, mSel).lengthOfMonth()

    val dayPager = rememberPagerState(
        initialPage = (initialDate.dayOfMonth - 1).coerceIn(0, lastDayInMonth - 1),
        pageCount = { lastDayInMonth },
    )

    LaunchedEffect(ySel, mSel, lastDayInMonth) {
        if (dayPager.settledPage >= lastDayInMonth) {
            scope.launch {
                dayPager.scrollToPage(lastDayInMonth - 1)
            }
        }
    }

    var hapticYearPrimed by remember { mutableStateOf(false) }
    LaunchedEffect(yearPager) {
        snapshotFlow { yearPager.settledPage }
            .distinctUntilChanged()
            .collect {
                if (!hapticYearPrimed) {
                    hapticYearPrimed = true
                } else {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }
    }
    var hapticMonthPrimed by remember { mutableStateOf(false) }
    LaunchedEffect(monthPager) {
        snapshotFlow { monthPager.settledPage }
            .distinctUntilChanged()
            .collect {
                if (!hapticMonthPrimed) {
                    hapticMonthPrimed = true
                } else {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }
    }
    var hapticDayPrimed by remember { mutableStateOf(false) }
    LaunchedEffect(dayPager) {
        snapshotFlow { dayPager.settledPage }
            .distinctUntilChanged()
            .collect {
                if (!hapticDayPrimed) {
                    hapticDayPrimed = true
                } else {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }
    }

    fun yFromPager(): Int = MIN_YEAR + yearPager.settledPage
    fun mFromPager(): Int = monthPager.settledPage + 1

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
    ) {
        Text(
            text = stringResource(R.string.datepicker_sheet_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = WellPaidNavy,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        HorizontalDivider(color = WellPaidNavy.copy(alpha = 0.12f))
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WheelColumnLabel(
                text = stringResource(R.string.datepicker_wheel_day),
                modifier = Modifier
                    .weight(1f)
                    .widthIn(min = wheelDayMinWidth),
            )
            WheelColumnLabel(
                text = stringResource(R.string.datepicker_wheel_month),
                modifier = Modifier
                    .weight(1.5f)
                    .widthIn(min = wheelMonthMinWidth),
            )
            WheelColumnLabel(
                text = stringResource(R.string.datepicker_wheel_year),
                modifier = Modifier
                    .weight(1f)
                    .widthIn(min = wheelYearMinWidth),
            )
        }
        Spacer(Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(wheelRowHeight)
                .drawWithContent {
                    drawContent()
                    val padH = 4.dp.toPx()
                    val slotH = wheelPageHeight.toPx()
                    val cy = size.height / 2f
                    val top = cy - slotH / 2f
                    val w = (size.width - 2f * padH).coerceAtLeast(0f)
                    val corner = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                    drawRoundRect(
                        color = WellPaidNavy.copy(alpha = 0.10f),
                        topLeft = Offset(padH, top),
                        size = Size(w, slotH),
                        cornerRadius = corner,
                    )
                    drawRoundRect(
                        color = WellPaidNavy.copy(alpha = 0.50f),
                        topLeft = Offset(padH, top),
                        size = Size(w, slotH),
                        cornerRadius = corner,
                        style = Stroke(width = 1.5.dp.toPx()),
                    )
                },
        ) {
            Row(
                modifier = Modifier
                    .matchParentSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
            WheelFadeBox(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(min = wheelDayMinWidth),
            ) {
                VerticalPager(
                    state = dayPager,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = wheelVerticalContentPadding),
                    pageSize = wheelPageSize,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) { page ->
                    Text(
                        text = "${page + 1}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = WellPaidNavy,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            WheelFadeBox(
                modifier = Modifier
                    .weight(1.5f)
                    .widthIn(min = wheelMonthMinWidth),
            ) {
                VerticalPager(
                    state = monthPager,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = wheelVerticalContentPadding),
                    pageSize = wheelPageSize,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) { page ->
                    val month = Month.of(page + 1)
                    val label = month.getDisplayName(TextStyle.SHORT_STANDALONE, locale)
                    Text(
                        text = label.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = WellPaidNavy,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            WheelFadeBox(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(min = wheelYearMinWidth),
            ) {
                VerticalPager(
                    state = yearPager,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = wheelVerticalContentPadding),
                    pageSize = wheelPageSize,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) { page ->
                    Text(
                        text = "${MIN_YEAR + page}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = WellPaidNavy,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
        ) {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
            Button(
                onClick = {
                    val y = yFromPager()
                    val m = mFromPager()
                    val last = YearMonth.of(y, m).lengthOfMonth()
                    val d = (dayPager.currentPage + 1).coerceIn(1, last)
                    onConfirm(LocalDate.of(y, m, d))
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = WellPaidGold,
                    contentColor = WellPaidNavy,
                ),
                shape = RoundedCornerShape(24.dp),
            ) {
                Text(stringResource(R.string.common_ok))
            }
        }
    }
}

@Composable
private fun WheelColumnLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = WellPaidNavy.copy(alpha = 0.55f),
        textAlign = TextAlign.Center,
        modifier = modifier.padding(horizontal = 4.dp),
    )
}

@Composable
private fun WheelFadeBox(
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    val surface = MaterialTheme.colorScheme.surface
    Box(
        modifier = modifier
            .fillMaxHeight()
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to surface,
                        0.12f to Color.Transparent,
                        0.88f to Color.Transparent,
                        1f to surface,
                    ),
                )
            },
    ) {
        content()
    }
}
