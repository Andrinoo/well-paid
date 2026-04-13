package com.wellpaid.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.PieChartOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectTapGestures
import com.wellpaid.R
import com.wellpaid.core.model.dashboard.CategorySpendDto
import com.wellpaid.ui.theme.WellPaidCardWhite
import com.wellpaid.ui.theme.WellPaidCreamMuted
import com.wellpaid.ui.theme.WellPaidNavy
import com.wellpaid.util.formatBrlFromCents
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import androidx.compose.ui.res.stringResource

data class AggregatedCategorySlice(
    val categoryKey: String,
    val name: String,
    val amountCents: Int,
)

/**
 * Paleta longa: cada fatia no ecrã usa `palette[i]` pela ordem de exibição — sem repetição
 * enquanto o número de fatias for ≤ tamanho da lista (agregação máx. 6).
 */
private val ExpandedDistinctPalette = listOf(
    Color(0xFF5E35B1),
    Color(0xFFE65100),
    Color(0xFF00897B),
    Color(0xFFC62828),
    Color(0xFF283593),
    Color(0xFFAD1457),
    Color(0xFF2E7D32),
    Color(0xFFF57C00),
    Color(0xFF0277BD),
    Color(0xFF6A1B9A),
    Color(0xFF37474F),
    Color(0xFF00695C),
    Color(0xFFD84315),
    Color(0xFF4527A0),
    Color(0xFF558B2F),
    Color(0xFFBF360C),
    Color(0xFF1565C0),
    Color(0xFF8E24AA),
    Color(0xFF4E342E),
    Color(0xFF0097A7),
)

/**
 * Uma cor por categoria: índice da paleta pela ordem alfabética das `categoryKey` presentes
 * (sem repetição no mesmo gráfico; a mesma categoria mantém a mesma cor quando o mês muda).
 */
private fun distinctColorsByCategoryKey(slices: List<AggregatedCategorySlice>): List<Color> {
    if (slices.isEmpty()) return emptyList()
    val distinctKeys = slices.map { it.categoryKey }.distinct().sorted()
    val keyToPaletteIndex = distinctKeys.mapIndexed { idx, k -> k to idx }.toMap()
    return slices.map { slice ->
        val idx = keyToPaletteIndex.getValue(slice.categoryKey)
        ExpandedDistinctPalette[idx.coerceAtMost(ExpandedDistinctPalette.lastIndex)]
    }
}

fun aggregateCategorySlices(
    spending: List<CategorySpendDto>,
    otherName: String,
): List<AggregatedCategorySlice> {
    val sorted = spending.filter { it.amountCents > 0 }.sortedByDescending { it.amountCents }
    if (sorted.isEmpty()) return emptyList()
    val head = sorted.take(5)
    val tail = sorted.drop(5)
    val out = head.map { AggregatedCategorySlice(it.categoryKey, it.name, it.amountCents) }.toMutableList()
    if (tail.isNotEmpty()) {
        out += AggregatedCategorySlice("outros", otherName, tail.sumOf { it.amountCents })
    }
    return out
}

private fun angleFromTopClockwiseDegrees(dx: Float, dy: Float): Float {
    val deg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
    return (deg + 90f + 360f) % 360f
}

/** Buraco interior (aspecto donut): base fixa do “trapézio” (ponta cortada). */
private const val PieInnerFrac = 0.235f

/**
 * Raio exterior da fatia com **menor** valor (menor “altura” radial).
 * Deve ser > [PieInnerFrac] para manter anel visível.
 */
private const val PieOuterMinFrac = 0.298f

/**
 * Raio exterior da fatia com **maior** valor (maior “altura” radial).
 * Ângulo continua proporcional ao total; o raio escala com o montante (polar area / Coxcomb).
 */
private const val PieOuterMaxFrac = 0.408f

/** Destaque radial extra quando a fatia está selecionada. */
private const val PieSelectedExtraFrac = 0.035f

/** Espaço angular (graus) entre fatias. */
private const val ArcGapDegrees = 2.65f

private fun innerRadiusPx(side: Float) = side * PieInnerFrac

/** Teto do anel cinza por baixo das fatias (sempre o raio máximo possível). */
private fun outerTrackMaxPx(side: Float) = side * PieOuterMaxFrac

/**
 * Raio exterior da fatia: interpola entre mínimo e máximo segundo o valor em centavos
 * (maior despesa = fatia mais “alta”; o ângulo continua a refletir a % do total).
 */
private fun outerRadiusForSlice(
    side: Float,
    amountCents: Int,
    minAmt: Int,
    maxAmt: Int,
    selected: Boolean,
): Float {
    val outerMin = side * PieOuterMinFrac
    val outerMax = side * PieOuterMaxFrac
    val span = (maxAmt - minAmt).toFloat()
    val t = if (span < 1f) {
        0.5f
    } else {
        ((amountCents - minAmt) / span).coerceIn(0f, 1f)
    }
    val outer = outerMin + t * (outerMax - outerMin)
    val sel = if (selected) side * PieSelectedExtraFrac else 0f
    return outer + sel
}

/** Setor anular (tarte com buraco): infográfico que continua a ler-se como donut. */
private fun annularSectorPath(
    center: Offset,
    innerR: Float,
    outerR: Float,
    startDeg: Float,
    sweepDeg: Float,
): Path {
    val path = Path()
    if (sweepDeg <= 0.01f || innerR >= outerR) return path
    val sweepUse = sweepDeg.coerceIn(0.02f, 359.98f)
    val outerRect = Rect(
        center.x - outerR,
        center.y - outerR,
        center.x + outerR,
        center.y + outerR,
    )
    val innerRect = Rect(
        center.x - innerR,
        center.y - innerR,
        center.x + innerR,
        center.y + innerR,
    )
    val rad = (Math.PI / 180.0).toFloat()
    fun xAt(r: Float, deg: Float) = center.x + r * cos(deg * rad)
    fun yAt(r: Float, deg: Float) = center.y + r * sin(deg * rad)
    path.moveTo(xAt(outerR, startDeg), yAt(outerR, startDeg))
    path.arcTo(outerRect, startDeg, sweepUse, false)
    path.lineTo(xAt(innerR, startDeg + sweepUse), yAt(innerR, startDeg + sweepUse))
    path.arcTo(innerRect, startDeg + sweepUse, -sweepUse, false)
    path.close()
    return path
}

/** Teto do quadrado do donut no ecrã inicial (aumenta o anel; a legenda fica por baixo). */
private val DonutMaxSide = 380.dp

private val DonutMinSide = 132.dp

@Composable
fun CategoryDonutChartPage(
    monthTitle: String,
    totalExpenseCents: Int,
    spending: List<CategorySpendDto>,
    modifier: Modifier = Modifier,
) {
    val otherLabel = stringResource(R.string.home_category_other)
    val slices = remember(spending, otherLabel) { aggregateCategorySlices(spending, otherLabel) }
    var selectedIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(monthTitle, spending, totalExpenseCents) {
        selectedIndex = 0
    }

    if (slices.isEmpty() || totalExpenseCents <= 0) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CategoryDonutEmpty(monthTitle = monthTitle, modifier = Modifier.fillMaxWidth())
        }
        return
    }

    val total = slices.sumOf { it.amountCents }.coerceAtLeast(1)
    val sliceColors = remember(slices) { distinctColorsByCategoryKey(slices) }
    Column(modifier = modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(2.75f)
                .fillMaxWidth()
                .padding(horizontal = 0.dp, vertical = 0.dp),
            contentAlignment = Alignment.Center,
        ) {
            val side = minOf(maxWidth * 0.96f, maxHeight * 0.99f, DonutMaxSide).coerceAtLeast(
                minOf(DonutMinSide, maxWidth),
            )
            Box(
                modifier = Modifier
                    .width(side)
                    .aspectRatio(1f),
            ) {
                CategoryDonutCanvas(
                    slices = slices,
                    sliceColors = sliceColors,
                    totalAmount = total,
                    selectedIndex = selectedIndex,
                    onSelectIndex = { selectedIndex = it },
                    monthTitle = monthTitle,
                    centerTotalCents = totalExpenseCents,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        val sel = slices.getOrNull(selectedIndex)
        if (sel != null) {
            val pct = (sel.amountCents * 100f / total).toInt().coerceIn(0, 100)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 2.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(WellPaidNavy.copy(alpha = 0.07f))
                    .border(1.dp, WellPaidNavy.copy(alpha = 0.10f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(sliceColors.getOrElse(selectedIndex) { ExpandedDistinctPalette[0] }),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        text = sel.name,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 16.sp,
                    )
                    Text(
                        text = stringResource(R.string.home_donut_selected_percent, pct),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = formatBrlFromCents(sel.amountCents),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        val listSlices = remember(slices, selectedIndex) {
            slices.mapIndexedNotNull { index, slice ->
                if (index == selectedIndex) null else index to slice
            }
        }
        LazyColumn(
            modifier = Modifier
                .weight(0.85f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            items(
                count = listSlices.size,
                key = { listSlices[it].second.categoryKey },
            ) { pos ->
                val (originalIndex, slice) = listSlices[pos]
                DonutCompactLegendRow(
                    slice = slice,
                    color = sliceColors.getOrElse(originalIndex) { ExpandedDistinctPalette[0] },
                    total = total,
                    onClick = { selectedIndex = originalIndex },
                )
            }
        }
    }
}

@Composable
private fun CategoryDonutEmpty(monthTitle: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 168.dp)
                .fillMaxWidth(0.55f)
                .aspectRatio(1f)
                .clip(CircleShape)
                .border(10.dp, WellPaidNavy.copy(alpha = 0.14f), CircleShape)
                .background(WellPaidCreamMuted),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = monthTitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = WellPaidNavy.copy(alpha = 0.55f),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.home_donut_empty_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp, start = 12.dp, end = 12.dp),
                )
            }
        }
    }
}

/** Legenda compacta (uma linha por fatia) — pouca altura, semelhante a legendas de libs de gráficos. */
@Composable
private fun DonutCompactLegendRow(
    slice: AggregatedCategorySlice,
    color: Color,
    total: Int,
    onClick: () -> Unit,
) {
    val pct = (slice.amountCents * 100f / total).toInt().coerceIn(0, 100)
        Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 26.dp, max = 34.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = 1.dp,
                color = WellPaidNavy.copy(alpha = 0.07f),
                shape = RoundedCornerShape(8.dp),
            )
            .background(WellPaidCardWhite.copy(alpha = 0.6f))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = slice.name,
            style = MaterialTheme.typography.labelMedium,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "$pct%",
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(34.dp),
            textAlign = TextAlign.End,
        )
        Text(
            text = formatBrlFromCents(slice.amountCents),
            style = MaterialTheme.typography.labelMedium,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
    }
}

@Composable
private fun CategoryDonutCanvas(
    slices: List<AggregatedCategorySlice>,
    sliceColors: List<Color>,
    totalAmount: Int,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    monthTitle: String,
    centerTotalCents: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(slices, totalAmount, sliceColors, selectedIndex) {
                    detectTapGestures { pos ->
                        if (slices.isEmpty()) return@detectTapGestures
                        val c = Offset(size.width / 2f, size.height / 2f)
                        val dx = pos.x - c.x
                        val dy = pos.y - c.y
                        val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()
                        val side = min(size.width, size.height).toFloat()
                        val innerR = innerRadiusPx(side)
                        val minAmt = slices.minOf { it.amountCents }.coerceAtLeast(1)
                        val maxAmt = slices.maxOf { it.amountCents }.coerceAtLeast(1)
                        val maxTouchR = outerTrackMaxPx(side) + side * PieSelectedExtraFrac + 12f
                        if (dist < innerR - 6f || dist > maxTouchR) return@detectTapGestures
                        val angle = angleFromTopClockwiseDegrees(dx, dy)
                        val total = totalAmount.toFloat().coerceAtLeast(1f)
                        val gap = if (slices.size > 1) ArcGapDegrees else 0f
                        // Acumulado em graus a partir do topo (igual a start=-90° no canvas + 90°).
                        var start = 0f
                        slices.forEachIndexed { i, sl ->
                            val sweep = (360f * sl.amountCents / total - gap).coerceAtLeast(0.15f)
                            if (angle >= start && angle < start + sweep) {
                                val outer = outerRadiusForSlice(
                                    side,
                                    sl.amountCents,
                                    minAmt,
                                    maxAmt,
                                    i == selectedIndex,
                                )
                                if (dist <= outer + 10f) onSelectIndex(i)
                                return@detectTapGestures
                            }
                            start += sweep + gap
                        }
                    }
                },
        ) {
            val side = min(size.width, size.height)
            val c = Offset(size.width / 2f, size.height / 2f)
            val innerR = innerRadiusPx(side)
            val trackOuterR = outerTrackMaxPx(side)
            // Mesmo branco do cartão: sem anel cinza/bege — fatias leem-se como um único gráfico no fundo.
            val surface = WellPaidCardWhite

            drawCircle(color = surface, radius = side / 2f - 4f, center = c)
            drawCircle(color = surface, radius = trackOuterR, center = c)
            drawCircle(color = surface, radius = innerR, center = c)

            val total = totalAmount.toFloat().coerceAtLeast(1f)
            val minAmt = slices.minOfOrNull { it.amountCents }?.coerceAtLeast(1) ?: 1
            val maxAmt = slices.maxOf { it.amountCents }.coerceAtLeast(1)
            val gap = if (slices.size > 1) ArcGapDegrees else 0f
            var start = -90f
            slices.forEachIndexed { i, sl ->
                val sweep = (360f * sl.amountCents / total - gap).coerceAtLeast(0.12f)
                val outer = outerRadiusForSlice(side, sl.amountCents, minAmt, maxAmt, i == selectedIndex)
                val baseColor = sliceColors.getOrElse(i) { ExpandedDistinctPalette[0] }
                val fillColor = if (i == selectedIndex) {
                    lerp(baseColor, Color.White, 0.12f)
                } else {
                    baseColor
                }
                val sector = annularSectorPath(c, innerR, outer, start, sweep)
                val bisectRad = (start + sweep / 2f) * (Math.PI / 180.0).toFloat()
                val gx = cos(bisectRad)
                val gy = sin(bisectRad)
                val gradCenter = Offset(
                    c.x + gx * outer * 0.22f,
                    c.y + gy * outer * 0.22f,
                )
                val hiMix = if (i == selectedIndex) 0.34f else 0.26f
                val loMix = if (i == selectedIndex) 0.17f else 0.11f
                val brush = Brush.radialGradient(
                    colors = listOf(
                        lerp(fillColor, Color.White, hiMix),
                        fillColor,
                        lerp(fillColor, Color.Black, loMix),
                    ),
                    center = gradCenter,
                    radius = max(outer * 0.9f, innerR + 8f),
                )
                drawPath(path = sector, brush = brush)
                start += sweep + gap
            }

            val holeR = (innerR - 2.5f).coerceAtLeast(1f)
            drawCircle(color = WellPaidCardWhite, radius = holeR, center = c)
            drawCircle(
                color = WellPaidNavy.copy(alpha = 0.07f),
                radius = holeR,
                center = c,
                style = Stroke(width = 1.2.dp.toPx()),
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 4.dp),
        ) {
            Text(
                text = monthTitle,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 8.sp,
                lineHeight = 10.sp,
                color = WellPaidNavy.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.home_donut_center_total),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 8.sp,
                color = WellPaidNavy.copy(alpha = 0.58f),
                modifier = Modifier.padding(top = 0.dp),
            )
            Text(
                text = formatBrlFromCents(centerTotalCents),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 15.sp,
                ),
                color = WellPaidNavy,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Ícones para rótulos do seletor Categorias / Fluxo (alinhado ao doc). */
object HomeChartTabIcons {
    val Categories = Icons.Outlined.PieChartOutline
    val Flow = Icons.AutoMirrored.Outlined.ShowChart
}
