package com.wellpaid.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wellpaid.R
import com.wellpaid.core.model.dashboard.DashboardCashflowDto
import com.wellpaid.core.model.dashboard.PeriodMonthDto
import com.wellpaid.ui.theme.WellPaidCream
import com.wellpaid.ui.theme.WellPaidExpenseLine
import com.wellpaid.ui.theme.WellPaidGold
import com.wellpaid.ui.theme.WellPaidNavy
import com.wellpaid.ui.theme.WellPaidPositive
import com.wellpaid.util.formatBrlFromCents
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

private val monthTickFormatter =
    DateTimeFormatter.ofPattern("MMM yy", Locale("pt", "PT"))

private val monthTitleFormatter =
    DateTimeFormatter.ofPattern("LLLL yyyy", Locale("pt", "PT"))

/** Alinhado a GRAFICOS_DONUT_FLUXO_COMPOSE.md §4.2 — smoothness tipo fl_chart ~0.32 */
private const val CurveSmoothness = 0.32f

private fun formatYAxisCents(cents: Long): String {
    val reais = cents / 100
    return when {
        reais >= 1_000_000 -> "R$ ${reais / 1_000_000}M"
        reais >= 10_000 -> "R$ ${reais / 1000}k"
        reais >= 1000 -> "R$ %.1fk".format(Locale.US, reais / 1000.0)
        else -> "R$ $reais"
    }
}

private fun xPadForCount(n: Int): Float = when {
    n <= 1 -> 0.25f
    n > 14 -> 0.58f
    else -> 0.48f
}

private fun xLabelInterval(n: Int): Int = if (n > 12) 2 else 1

/** Curva suave estilo Catmull-Rom → Bézier cúbica (com smoothness limitado). */
private fun buildSmoothLinePath(points: List<Offset>, smoothness: Float = CurveSmoothness): Path {
    val path = Path()
    if (points.isEmpty()) return path
    if (points.size == 1) {
        path.moveTo(points[0].x, points[0].y)
        path.lineTo(points[0].x + 0.01f, points[0].y)
        return path
    }
    path.moveTo(points[0].x, points[0].y)
    for (i in 0 until points.size - 1) {
        val p0 = points.getOrElse(i - 1) { points[i] }
        val p1 = points[i]
        val p2 = points[i + 1]
        val p3 = points.getOrElse(i + 2) { p2 }
        val c1 = Offset(
            p1.x + (p2.x - p0.x) * smoothness,
            p1.y + (p2.y - p0.y) * smoothness,
        )
        val c2 = Offset(
            p2.x - (p3.x - p1.x) * smoothness,
            p2.y - (p3.y - p1.y) * smoothness,
        )
        path.cubicTo(c1.x, c1.y, c2.x, c2.y, p2.x, p2.y)
    }
    return path
}

private fun buildClosedAreaUnderLine(linePath: Path, points: List<Offset>, bottomY: Float): Path {
    if (points.isEmpty()) return Path()
    val area = Path()
    area.addPath(linePath)
    area.lineTo(points.last().x, bottomY)
    area.lineTo(points.first().x, bottomY)
    area.close()
    return area
}

@Composable
fun CashflowChartCard(
    cashflow: DashboardCashflowDto,
    modifier: Modifier = Modifier,
    cashflowDynamic: Boolean = true,
    forecastMonths: Int = 3,
    onCashflowDynamicChange: (Boolean) -> Unit = {},
    onForecastMonthsDelta: (Int) -> Unit = {},
) {
    val n = cashflow.months.size
    if (n == 0) {
        Column(modifier = modifier.fillMaxSize()) {
            CashflowOptionsBar(
                dynamic = cashflowDynamic,
                onDynamicChange = onCashflowDynamicChange,
                forecastMonths = forecastMonths,
                onForecastMonthsDelta = onForecastMonthsDelta,
            )
            Text(
                text = stringResource(R.string.home_cashflow_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
        return
    }

    val income = cashflow.incomeCents
    val paid = cashflow.expensePaidCents
    val forecast = cashflow.expenseForecastCents

    var showIncome by remember { mutableStateOf(true) }
    var showPaid by remember { mutableStateOf(true) }
    var showForecast by remember { mutableStateOf(true) }
    var touchedIndex by remember { mutableIntStateOf(-1) }

    val hasAnySeries = showIncome || showPaid || showForecast

    val maxCentsRaw = remember(cashflow, showIncome, showPaid, showForecast) {
        (0 until n).maxOfOrNull { i ->
            var m = 0
            if (showIncome) m = max(m, income.getOrElse(i) { 0 })
            if (showPaid) m = max(m, paid.getOrElse(i) { 0 })
            if (showForecast) m = max(m, forecast.getOrElse(i) { 0 })
            m
        }?.coerceAtLeast(1) ?: 1
    }
    val maxY = remember(maxCentsRaw) { (maxCentsRaw * 1.24).toLong().coerceAtLeast(1L) }

    val defaultMonthIndex = remember(cashflow, showIncome, showPaid, showForecast) {
        (0 until n).maxByOrNull { i ->
            var s = 0L
            if (showIncome) s += income.getOrElse(i) { 0 }
            if (showPaid) s += paid.getOrElse(i) { 0 }
            if (showForecast) s += forecast.getOrElse(i) { 0 }
            s
        } ?: 0
    }

    val displayIndex = if (touchedIndex >= 0) touchedIndex else defaultMonthIndex

    val density = LocalDensity.current
    val strokeWidth = 2.8.dp
    val strokePx = with(density) { strokeWidth.toPx() }
    val pointRadiusSolid = with(density) { 5.dp.toPx() }
    val pointStrokeWhite = with(density) { 1.5.dp.toPx() }
    val pointRadiusForecast = with(density) { 4.5.dp.toPx() }
    val pointStrokeForecast = with(density) { 2.dp.toPx() }
    val dashOn = with(density) { 7.dp.toPx() }
    val dashOff = with(density) { 5.dp.toPx() }
    val gridColor = WellPaidNavy.copy(alpha = 0.06f)
    val axisColor = WellPaidNavy.copy(alpha = 0.12f)
    val navyHint = WellPaidNavy.copy(alpha = 0.48f)

    val yAxisWidth = 44.dp
    val plotPadH = 8.dp
    val plotPadTop = 10.dp
    val plotPadBottom = 6.dp

    Column(modifier = modifier.fillMaxSize()) {
        CashflowOptionsBar(
            dynamic = cashflowDynamic,
            onDynamicChange = onCashflowDynamicChange,
            forecastMonths = forecastMonths,
            onForecastMonthsDelta = onForecastMonthsDelta,
        )
        Spacer(Modifier.height(4.dp))
        LegendChipsRow(
            showIncome = showIncome,
            showPaid = showPaid,
            showForecast = showForecast,
            onIncomeToggle = {
                showIncome = !showIncome
                touchedIndex = -1
            },
            onPaidToggle = {
                showPaid = !showPaid
                touchedIndex = -1
            },
            onForecastToggle = {
                showForecast = !showForecast
                touchedIndex = -1
            },
        )

        Spacer(Modifier.height(8.dp))

        if (!hasAnySeries) {
            Text(
                text = stringResource(R.string.home_cashflow_all_series_hidden),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
        } else {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f),
            ) {
            Column(
                modifier = Modifier
                    .width(yAxisWidth)
                    .fillMaxHeight()
                    .padding(end = 2.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
            ) {
                for (k in 4 downTo 0) {
                    val cents = maxY * k / 4
                    Text(
                        text = formatYAxisCents(cents),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp,
                        lineHeight = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                    )
                }
            }

            val xPad = xPadForCount(n)
            val labelInterval = xLabelInterval(n)

            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(n, maxY, showIncome, showPaid, showForecast) {
                        detectTapGestures { offset ->
                            val w = size.width.toFloat()
                            val h = size.height.toFloat()
                            val pL = with(density) { plotPadH.toPx() }
                            val pR = with(density) { plotPadH.toPx() }
                            val pT = with(density) { plotPadTop.toPx() }
                            val pB = with(density) { plotPadBottom.toPx() }
                            val innerW = (w - pL - pR).coerceAtLeast(1f)
                            val minX = -xPad
                            val maxX = (n - 1) + xPad
                            val span = maxX - minX
                            val t = minX + ((offset.x - pL) / innerW).coerceIn(0f, 1f) * span
                            val idx = t.roundToInt().coerceIn(0, n - 1)
                            touchedIndex = idx
                        }
                    },
            ) {
                val w = size.width
                val h = size.height
                val padL = plotPadH.toPx()
                val padR = plotPadH.toPx()
                val padT = plotPadTop.toPx()
                val padB = plotPadBottom.toPx()
                val innerW = (w - padL - padR).coerceAtLeast(1f)
                val innerH = (h - padT - padB).coerceAtLeast(1f)
                val bottomY = padT + innerH
                val minX = -xPad
                val maxX = (n - 1) + xPad
                val span = maxX - minX

                fun yFor(cents: Int): Float =
                    padT + innerH * (1f - (cents.toFloat() / maxY.toFloat()).coerceIn(0f, 1f))

                fun xFor(i: Int): Float =
                    padL + innerW * ((i.toFloat() - minX) / span)

                // Grelha horizontal (sem a linha de R$ 0 — essa é o eixo X, desenhado a seguir por baixo das séries)
                for (g in 1..4) {
                    val yy = padT + innerH * (1f - g / 4f)
                    drawLine(
                        color = gridColor,
                        start = Offset(padL, yy),
                        end = Offset(padL + innerW, yy),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
                drawLine(
                    color = axisColor,
                    start = Offset(padL, padT),
                    end = Offset(padL, bottomY),
                    strokeWidth = 1.dp.toPx(),
                )
                // Linha R$ 0 por baixo de linhas/áreas e por baixo dos marcadores (evita “cortar” os círculos)
                drawLine(
                    color = axisColor,
                    start = Offset(padL, bottomY),
                    end = Offset(padL + innerW, bottomY),
                    strokeWidth = 1.dp.toPx(),
                )

                fun offsetsFor(values: List<Int>): List<Offset> =
                    (0 until n).map { i -> Offset(xFor(i), yFor(values.getOrElse(i) { 0 })) }

                val incomePts = offsetsFor(income)
                val paidPts = offsetsFor(paid)
                val forecastPts = offsetsFor(forecast)

                val incomePath = buildSmoothLinePath(incomePts)
                val paidPath = buildSmoothLinePath(paidPts)
                val forecastPath = buildSmoothLinePath(forecastPts)

                // Corta overshoot da spline abaixo de R$ 0; marcadores ficam fora do clip (por cima do eixo e inteiros em y=0)
                clipRect(
                    left = padL,
                    top = padT,
                    right = padL + innerW,
                    bottom = bottomY,
                    clipOp = ClipOp.Intersect,
                ) {
                    if (showIncome && n > 0) {
                        val area = buildClosedAreaUnderLine(incomePath, incomePts, bottomY)
                        drawPath(
                            path = area,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    WellPaidPositive.copy(alpha = 0.22f),
                                    WellPaidPositive.copy(alpha = 0.02f),
                                ),
                                startY = padT,
                                endY = bottomY,
                            ),
                        )
                    }
                    if (showPaid && n > 0) {
                        val area = buildClosedAreaUnderLine(paidPath, paidPts, bottomY)
                        drawPath(
                            path = area,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    WellPaidExpenseLine.copy(alpha = 0.22f),
                                    WellPaidExpenseLine.copy(alpha = 0.02f),
                                ),
                                startY = padT,
                                endY = bottomY,
                            ),
                        )
                    }

                    if (showIncome) {
                        drawPath(
                            path = incomePath,
                            color = WellPaidPositive,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round),
                        )
                    }
                    if (showPaid) {
                        drawPath(
                            path = paidPath,
                            color = WellPaidExpenseLine,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round),
                        )
                    }
                    if (showForecast) {
                        drawPath(
                            path = forecastPath,
                            color = WellPaidGold,
                            style = Stroke(
                                width = strokePx,
                                cap = StrokeCap.Round,
                                pathEffect = PathEffect.dashPathEffect(
                                    floatArrayOf(dashOn, dashOff),
                                    0f,
                                ),
                            ),
                        )
                    }
                }

                if (showIncome) {
                    for (i in 0 until n) {
                        val c = incomePts[i]
                        drawCircle(
                            color = WellPaidCream,
                            radius = pointRadiusSolid + pointStrokeWhite / 2f,
                            center = c,
                        )
                        drawCircle(
                            color = WellPaidPositive,
                            radius = pointRadiusSolid,
                            center = c,
                        )
                    }
                }
                if (showPaid) {
                    for (i in 0 until n) {
                        val c = paidPts[i]
                        drawCircle(
                            color = WellPaidCream,
                            radius = pointRadiusSolid + pointStrokeWhite / 2f,
                            center = c,
                        )
                        drawCircle(
                            color = WellPaidExpenseLine,
                            radius = pointRadiusSolid,
                            center = c,
                        )
                    }
                }
                if (showForecast) {
                    for (i in 0 until n) {
                        drawCircle(
                            color = WellPaidGold,
                            radius = pointRadiusForecast,
                            center = forecastPts[i],
                            style = Stroke(width = pointStrokeForecast),
                        )
                    }
                }
            }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(start = yAxisWidth, top = 4.dp),
            ) {
            val interval = xLabelInterval(n)
            for (index in 0 until n) {
                val showTick = index % interval == 0 || index == n - 1
                if (showTick) {
                    Text(
                        text = formatMonthTick(cashflow.months[index]),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = if (n > 12) 8.sp else 9.sp,
                        lineHeight = 10.sp,
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }
            }
        }
        }

        Spacer(Modifier.height(6.dp))

        CashflowDetailPanel(
            months = cashflow.months,
            monthIndex = displayIndex,
            incomeCents = income,
            paidCents = paid,
            forecastCents = forecast,
            showIncome = showIncome,
            showPaid = showPaid,
            showForecast = showForecast,
            showTapHint = touchedIndex < 0,
        )
    }
}

@Composable
private fun CashflowOptionsBar(
    dynamic: Boolean,
    onDynamicChange: (Boolean) -> Unit,
    forecastMonths: Int,
    onForecastMonthsDelta: (Int) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                Icons.Filled.Sync,
                contentDescription = null,
                tint = WellPaidNavy.copy(alpha = 0.55f),
                modifier = Modifier.size(20.dp),
            )
            Switch(
                checked = dynamic,
                onCheckedChange = onDynamicChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = WellPaidGold,
                    checkedTrackColor = WellPaidGold.copy(alpha = 0.45f),
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            )
            Text(
                text = stringResource(
                    if (dynamic) {
                        R.string.home_cashflow_mode_dynamic
                    } else {
                        R.string.home_cashflow_mode_fixed
                    },
                ),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 2,
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.home_cashflow_forecast_months, forecastMonths),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onForecastMonthsDelta(-1) },
                    enabled = forecastMonths > 1,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.home_cashflow_forecast_decrease),
                    )
                }
                IconButton(
                    onClick = { onForecastMonthsDelta(1) },
                    enabled = forecastMonths < 12,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.home_cashflow_forecast_increase),
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendChipsRow(
    showIncome: Boolean,
    showPaid: Boolean,
    showForecast: Boolean,
    onIncomeToggle: () -> Unit,
    onPaidToggle: () -> Unit,
    onForecastToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendToggle(
            modifier = Modifier.weight(1f),
            selected = showIncome,
            onClick = onIncomeToggle,
            color = WellPaidPositive,
            dashed = false,
            label = stringResource(R.string.home_cashflow_legend_income),
        )
        LegendToggle(
            modifier = Modifier.weight(1f),
            selected = showPaid,
            onClick = onPaidToggle,
            color = WellPaidExpenseLine,
            dashed = false,
            label = stringResource(R.string.home_cashflow_legend_paid),
        )
        LegendToggle(
            modifier = Modifier.weight(1f),
            selected = showForecast,
            onClick = onForecastToggle,
            color = WellPaidGold,
            dashed = true,
            label = stringResource(R.string.home_cashflow_legend_forecast),
        )
    }
}

@Composable
private fun LegendToggle(
    selected: Boolean,
    onClick: () -> Unit,
    color: Color,
    dashed: Boolean,
    label: String,
    modifier: Modifier = Modifier,
) {
    val borderAlpha = if (selected) 0.45f else 0.22f
    val bgAlpha = if (selected) 0.06f else 0.02f
    Row(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = 1.dp,
                color = WellPaidNavy.copy(alpha = borderAlpha),
                shape = RoundedCornerShape(8.dp),
            )
            .background(WellPaidNavy.copy(alpha = bgAlpha))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(width = 16.dp, height = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxWidth().height(4.dp)) {
                val midY = size.height / 2f
                if (dashed) {
                    drawPath(
                        path = Path().apply {
                            moveTo(0f, midY)
                            lineTo(size.width, midY)
                        },
                        color = color,
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(3.dp.toPx(), 2.dp.toPx()),
                                0f,
                            ),
                        ),
                    )
                } else {
                    drawLine(
                        color = color,
                        start = Offset(0f, midY),
                        end = Offset(size.width, midY),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }
        }
        Text(
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (selected) 1f else 0.55f),
            modifier = Modifier.padding(start = 2.dp),
        )
    }
}

@Composable
private fun CashflowDetailPanel(
    months: List<PeriodMonthDto>,
    monthIndex: Int,
    incomeCents: List<Int>,
    paidCents: List<Int>,
    forecastCents: List<Int>,
    showIncome: Boolean,
    showPaid: Boolean,
    showForecast: Boolean,
    showTapHint: Boolean,
) {
    val m = months.getOrNull(monthIndex) ?: return
    val title = YearMonth.of(m.year, m.month).format(monthTitleFormatter).replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale("pt", "PT")) else it.toString()
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(WellPaidNavy.copy(alpha = 0.045f))
            .border(1.dp, WellPaidNavy.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
        if (showTapHint) {
            Text(
                text = stringResource(R.string.home_cashflow_hint_tap),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 8.sp,
                color = navyHintColor(),
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        if (showIncome) {
            DetailRow(WellPaidPositive, stringResource(R.string.home_cashflow_legend_income), incomeCents, monthIndex)
        }
        if (showPaid) {
            DetailRow(WellPaidExpenseLine, stringResource(R.string.home_cashflow_legend_paid), paidCents, monthIndex)
        }
        if (showForecast) {
            DetailRow(WellPaidGold, stringResource(R.string.home_cashflow_legend_forecast), forecastCents, monthIndex)
        }
    }
}

@Composable
private fun navyHintColor(): Color = WellPaidNavy.copy(alpha = 0.48f)

@Composable
private fun DetailRow(
    barColor: Color,
    title: String,
    values: List<Int>,
    monthIndex: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(16.dp)
                .background(barColor, RoundedCornerShape(2.dp)),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = formatBrlFromCents(values.getOrElse(monthIndex) { 0 }),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
        )
    }
}

private fun formatMonthTick(m: PeriodMonthDto): String =
    YearMonth.of(m.year, m.month).format(monthTickFormatter).replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale("pt", "PT")) else it.toString()
    }
