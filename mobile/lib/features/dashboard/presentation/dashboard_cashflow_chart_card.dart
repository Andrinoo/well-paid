import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../../core/format/brl_cents.dart';
import '../../../core/format/locale_dates.dart';
import '../../../core/l10n/context_l10n.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/theme/well_paid_colors.dart';
import '../application/dashboard_providers.dart';
import '../domain/dashboard_cashflow.dart';

/// Card **Histórico mensal**: `LineChart` (F3) + legenda tocável + tooltips em BRL.
class DashboardCashflowChartCard extends ConsumerWidget {
  const DashboardCashflowChartCard({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    final async = ref.watch(dashboardCashflowProvider);

    final reduceMotion = MediaQuery.maybeOf(context)?.disableAnimations ?? false;

    return async.when(
      skipLoadingOnReload: true,
      loading: () => _CashflowShell(
        title: l10n.dashCashflowTitle,
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 24),
          child: Center(
            child: reduceMotion
                ? Icon(
                    Icons.show_chart_outlined,
                    size: 32,
                    color: WellPaidColors.navy.withValues(alpha: 0.35),
                  )
                : const SizedBox(
                    width: 28,
                    height: 28,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  ),
          ),
        ),
      ),
      error: (e, _) => _CashflowShell(
        title: l10n.dashCashflowTitle,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(
              messageFromDio(e, l10n) ?? l10n.dashCashflowError,
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: WellPaidColors.navy.withValues(alpha: 0.8),
                  ),
            ),
            const SizedBox(height: 12),
            Align(
              alignment: Alignment.centerRight,
              child: TextButton(
                onPressed: () => ref.invalidate(dashboardCashflowProvider),
                child: Text(l10n.tryAgain),
              ),
            ),
          ],
        ),
      ),
      data: (d) => _CashflowShell(
        title: l10n.dashCashflowTitle,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const _CashflowQueryControls(),
            const SizedBox(height: 12),
            if (d.months.isEmpty)
              Text(
                l10n.dashCashflowEmpty,
                style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                      color: WellPaidColors.navy.withValues(alpha: 0.75),
                    ),
              )
            else
              _CashflowLineChartBody(data: d),
            const SizedBox(height: 12),
            _CashflowSummaryFooter(data: d),
          ],
        ),
      ),
    );
  }
}

class _CashflowShell extends StatelessWidget {
  const _CashflowShell({required this.title, required this.child});

  final String title;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Card(
      elevation: 0,
      color: Colors.white,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: BorderSide(
          color: WellPaidColors.navy.withValues(alpha: 0.08),
        ),
      ),
      child: Padding(
        padding: const EdgeInsets.fromLTRB(16, 14, 16, 16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Semantics(
              header: true,
              child: Row(
                children: [
                  Icon(
                    Icons.show_chart_outlined,
                    size: 22,
                    color: WellPaidColors.navy.withValues(alpha: 0.85),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      title,
                      style: Theme.of(context).textTheme.titleSmall?.copyWith(
                            fontWeight: FontWeight.w700,
                            color: WellPaidColors.navy,
                          ),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 10),
            child,
          ],
        ),
      ),
    );
  }
}

(int, int) _addCalendarMonths(int year, int month, int delta) {
  var m = month + delta;
  var y = year;
  while (m > 12) {
    m -= 12;
    y += 1;
  }
  while (m < 1) {
    m += 12;
    y -= 1;
  }
  return (y, m);
}

/// Controlos F4: janela dinâmica, intervalo manual, meses de previsão, [Aplicar].
class _CashflowQueryControls extends ConsumerStatefulWidget {
  const _CashflowQueryControls();

  @override
  ConsumerState<_CashflowQueryControls> createState() =>
      _CashflowQueryControlsState();
}

class _CashflowQueryControlsState extends ConsumerState<_CashflowQueryControls> {
  late bool _dynamicWindow;
  late int _startYear;
  late int _startMonth;
  late int _endYear;
  late int _endMonth;
  late int _forecastMonths;

  @override
  void initState() {
    super.initState();
    _loadFromRequest(ref.read(dashboardCashflowRequestProvider));
  }

  void _loadFromRequest(DashboardCashflowRequest r) {
    _dynamicWindow = r.isDynamicWindow;
    _forecastMonths = r.forecastMonths.clamp(1, 12);
    if (!r.isDynamicWindow &&
        r.startYear != null &&
        r.startMonth != null &&
        r.endYear != null &&
        r.endMonth != null) {
      _startYear = r.startYear!;
      _startMonth = r.startMonth!;
      _endYear = r.endYear!;
      _endMonth = r.endMonth!;
    } else {
      _seedManualFromDashboardPeriod();
    }
  }

  void _seedManualFromDashboardPeriod() {
    final p = ref.read(dashboardPeriodProvider);
    _endYear = p.year;
    _endMonth = p.month;
    final s = _addCalendarMonths(p.year, p.month, -5);
    _startYear = s.$1;
    _startMonth = s.$2;
  }

  Future<void> _pickMonth({
    required bool isStart,
  }) async {
    final l10n = context.l10n;
    final y = isStart ? _startYear : _endYear;
    final m = isStart ? _startMonth : _endMonth;
    final picked = await showDatePicker(
      context: context,
      initialDate: DateTime(y, m),
      firstDate: DateTime(2000),
      lastDate: DateTime(DateTime.now().year + 2, 12, 31),
      helpText: isStart ? l10n.dashCashflowStartMonth : l10n.dashCashflowEndMonth,
    );
    if (picked == null || !mounted) return;
    setState(() {
      if (isStart) {
        _startYear = picked.year;
        _startMonth = picked.month;
      } else {
        _endYear = picked.year;
        _endMonth = picked.month;
      }
    });
  }

  void _apply() {
    final l10n = context.l10n;
    if (!_dynamicWindow) {
      final startBeforeEnd = _startYear < _endYear ||
          (_startYear == _endYear && _startMonth <= _endMonth);
      if (!startBeforeEnd) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(l10n.dashCashflowRangeInvalid)),
        );
        return;
      }
    }
    ref.read(dashboardCashflowRequestProvider.notifier).state =
        DashboardCashflowRequest(
      isDynamicWindow: _dynamicWindow,
      startYear: _dynamicWindow ? null : _startYear,
      startMonth: _dynamicWindow ? null : _startMonth,
      endYear: _dynamicWindow ? null : _endYear,
      endMonth: _dynamicWindow ? null : _endMonth,
      forecastMonths: _forecastMonths,
    );
  }

  String _monthButtonLabel(BuildContext context, int y, int mo) {
    final tag = intlDateTagForUi(context);
    return DateFormat.yMMM(tag).format(DateTime(y, mo));
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    return LayoutBuilder(
      builder: (context, constraints) {
        final narrow = constraints.maxWidth < 400;
        final startLabel =
            '${l10n.dashCashflowStartMonth}: ${_monthButtonLabel(context, _startYear, _startMonth)}';
        final endLabel =
            '${l10n.dashCashflowEndMonth}: ${_monthButtonLabel(context, _endYear, _endMonth)}';

        final startBtn = Semantics(
          label: l10n.dashCashflowA11yPickStartMonth,
          child: SizedBox(
            width: narrow ? double.infinity : null,
            child: OutlinedButton(
              onPressed: () => _pickMonth(isStart: true),
              child: Text(
                startLabel,
                textAlign: TextAlign.center,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
              ),
            ),
          ),
        );
        final endBtn = Semantics(
          label: l10n.dashCashflowA11yPickEndMonth,
          child: SizedBox(
            width: narrow ? double.infinity : null,
            child: OutlinedButton(
              onPressed: () => _pickMonth(isStart: false),
              child: Text(
                endLabel,
                textAlign: TextAlign.center,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
              ),
            ),
          ),
        );

        final forecastDropdown = Semantics(
          label: l10n.dashCashflowA11yForecastDropdown,
          child: DropdownButtonHideUnderline(
            child: DropdownButton<int>(
              isExpanded: narrow,
              value: _forecastMonths,
              items: [
                for (var i = 1; i <= 12; i++)
                  DropdownMenuItem(value: i, child: Text('$i')),
              ],
              onChanged: (v) {
                if (v != null) setState(() => _forecastMonths = v);
              },
            ),
          ),
        );

        return Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Material(
              color: WellPaidColors.navy.withValues(alpha: 0.04),
              borderRadius: BorderRadius.circular(12),
              child: SwitchListTile(
                contentPadding:
                    const EdgeInsets.symmetric(horizontal: 12, vertical: 0),
                title: Text(
                  l10n.dashCashflowDynamicMode,
                  style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                        fontWeight: FontWeight.w600,
                        color: WellPaidColors.navy,
                      ),
                ),
                value: _dynamicWindow,
                activeThumbColor: WellPaidColors.gold,
                activeTrackColor: WellPaidColors.gold.withValues(alpha: 0.45),
                onChanged: (v) {
                  setState(() {
                    _dynamicWindow = v;
                    if (!v) {
                      _seedManualFromDashboardPeriod();
                    }
                  });
                },
              ),
            ),
            if (!_dynamicWindow) ...[
              const SizedBox(height: 10),
              if (narrow) ...[
                startBtn,
                const SizedBox(height: 8),
                endBtn,
              ] else
                Row(
                  children: [
                    Expanded(child: startBtn),
                    const SizedBox(width: 8),
                    Expanded(child: endBtn),
                  ],
                ),
            ],
            const SizedBox(height: 10),
            if (narrow)
              Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Text(
                    l10n.dashCashflowForecastMonths,
                    style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                          color: WellPaidColors.navy.withValues(alpha: 0.85),
                          fontWeight: FontWeight.w600,
                        ),
                  ),
                  const SizedBox(height: 6),
                  forecastDropdown,
                ],
              )
            else
              Row(
                children: [
                  Expanded(
                    child: Text(
                      l10n.dashCashflowForecastMonths,
                      style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                            color: WellPaidColors.navy.withValues(alpha: 0.85),
                            fontWeight: FontWeight.w600,
                          ),
                    ),
                  ),
                  forecastDropdown,
                ],
              ),
            const SizedBox(height: 10),
            Semantics(
              label: l10n.dashCashflowA11yApply,
              child: narrow
                  ? SizedBox(
                      width: double.infinity,
                      child: FilledButton.tonal(
                        onPressed: _apply,
                        child: Text(l10n.dashCashflowApply),
                      ),
                    )
                  : Align(
                      alignment: Alignment.centerRight,
                      child: FilledButton.tonal(
                        onPressed: _apply,
                        child: Text(l10n.dashCashflowApply),
                      ),
                    ),
            ),
          ],
        );
      },
    );
  }
}

class _CashflowSummaryFooter extends StatelessWidget {
  const _CashflowSummaryFooter({required this.data});

  final DashboardCashflow data;

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final forecastStr = formatBrlFromCents(data.totalForecastCents);
    final balanceStr = formatBrlFromCents(data.periodBalanceCents);
    final balanceColor = data.periodBalanceCents >= 0
        ? const Color(0xFF2A7A6E)
        : const Color(0xFFB85C4A);

    return Semantics(
      container: true,
      label: l10n.dashCashflowA11ySummary(forecastStr, balanceStr),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Divider(
            height: 1,
            thickness: 1,
            color: WellPaidColors.navy.withValues(alpha: 0.1),
          ),
          const SizedBox(height: 10),
          ExcludeSemantics(
            child: Text(
              l10n.dashCashflowFooterForecastTotal(forecastStr),
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                    color: WellPaidColors.navy.withValues(alpha: 0.82),
                    fontWeight: FontWeight.w600,
                  ),
            ),
          ),
          const SizedBox(height: 4),
          ExcludeSemantics(
            child: Text(
              l10n.dashCashflowFooterBalance(balanceStr),
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                    color: balanceColor,
                    fontWeight: FontWeight.w700,
                  ),
            ),
          ),
        ],
      ),
    );
  }
}

class _CashflowLineChartBody extends StatefulWidget {
  const _CashflowLineChartBody({required this.data});

  final DashboardCashflow data;

  @override
  State<_CashflowLineChartBody> createState() => _CashflowLineChartBodyState();
}

class _CashflowLineChartBodyState extends State<_CashflowLineChartBody> {
  static const _income = Color(0xFF2A7A6E);
  static const _paid = Color(0xFFB85C4A);
  static const _forecast = Color(0xFFC9A94E);

  bool _showIncome = true;
  bool _showPaid = true;
  bool _showForecast = true;

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final d = widget.data;
    final n = d.months.length;
    final tag = intlDateTagForUi(context);
    final monthFmt = DateFormat.MMM(tag);
    final reduceMotion = MediaQuery.disableAnimationsOf(context);
    final screenW = MediaQuery.sizeOf(context).width;
    final chartHeight = screenW < 340
        ? 196.0
        : screenW < 420
            ? 214.0
            : 228.0;
    final chartDuration = reduceMotion
        ? Duration.zero
        : const Duration(milliseconds: 380);
    final chartCurve = reduceMotion ? Curves.linear : Curves.easeOutCubic;

    final seriesValues = <List<int>>[];
    final barLabels = <String>[];
    final bars = <LineChartBarData>[];

    void addSeries({
      required bool show,
      required List<int> cents,
      required Color color,
      required String label,
      required bool dashed,
    }) {
      if (!show) return;
      seriesValues.add(cents);
      barLabels.add(label);
      final spots = <FlSpot>[
        for (var i = 0; i < n; i++)
          FlSpot(i.toDouble(), cents[i].toDouble()),
      ];
      bars.add(
        LineChartBarData(
          spots: spots,
          color: color,
          barWidth: 2.8,
          isCurved: !reduceMotion,
          curveSmoothness: reduceMotion ? 0 : 0.32,
          preventCurveOverShooting: !reduceMotion,
          dotData: const FlDotData(show: false),
          dashArray: dashed ? [7, 5] : null,
          belowBarData: BarAreaData(
            show: !dashed,
            gradient: LinearGradient(
              begin: Alignment.topCenter,
              end: Alignment.bottomCenter,
              colors: [
                color.withValues(alpha: 0.22),
                color.withValues(alpha: 0.02),
              ],
            ),
          ),
        ),
      );
    }

    addSeries(
      show: _showIncome,
      cents: d.incomeCents,
      color: _income,
      label: l10n.dashCashflowLegendIncome,
      dashed: false,
    );
    addSeries(
      show: _showPaid,
      cents: d.expensePaidCents,
      color: _paid,
      label: l10n.dashCashflowLegendExpensePaid,
      dashed: false,
    );
    addSeries(
      show: _showForecast,
      cents: d.expenseForecastCents,
      color: _forecast,
      label: l10n.dashCashflowLegendExpenseForecast,
      dashed: true,
    );

    if (bars.isEmpty) {
      return Text(
        l10n.dashCashflowEmpty,
        style: Theme.of(context).textTheme.bodyMedium?.copyWith(
              color: WellPaidColors.navy.withValues(alpha: 0.75),
            ),
      );
    }

    var maxCents = 1;
    for (var i = 0; i < n; i++) {
      if (_showIncome) maxCents = maxCents > d.incomeCents[i] ? maxCents : d.incomeCents[i];
      if (_showPaid) {
        maxCents =
            maxCents > d.expensePaidCents[i] ? maxCents : d.expensePaidCents[i];
      }
      if (_showForecast) {
        maxCents = maxCents > d.expenseForecastCents[i]
            ? maxCents
            : d.expenseForecastCents[i];
      }
    }
    final maxY = maxCents * 1.12;

    final xInterval = n > 12 ? 2.0 : 1.0;

    return Semantics(
      label: l10n.dashCashflowSemantics,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              _LegendChip(
                color: _income,
                label: l10n.dashCashflowLegendIncome,
                semanticsHint: l10n.dashCashflowA11ySeriesToggle(
                  l10n.dashCashflowLegendIncome,
                ),
                active: _showIncome,
                onTap: () => setState(() => _showIncome = !_showIncome),
              ),
              _LegendChip(
                color: _paid,
                label: l10n.dashCashflowLegendExpensePaid,
                semanticsHint: l10n.dashCashflowA11ySeriesToggle(
                  l10n.dashCashflowLegendExpensePaid,
                ),
                active: _showPaid,
                onTap: () => setState(() => _showPaid = !_showPaid),
              ),
              _LegendChip(
                color: _forecast,
                label: l10n.dashCashflowLegendExpenseForecast,
                semanticsHint: l10n.dashCashflowA11ySeriesToggle(
                  l10n.dashCashflowLegendExpenseForecast,
                ),
                active: _showForecast,
                dashed: true,
                onTap: () => setState(() => _showForecast = !_showForecast),
              ),
            ],
          ),
          const SizedBox(height: 12),
          SizedBox(
            height: chartHeight,
            child: LineChart(
              duration: chartDuration,
              curve: chartCurve,
              LineChartData(
                minX: 0,
                maxX: (n - 1).toDouble(),
                minY: 0,
                maxY: maxY.toDouble(),
                clipData: const FlClipData.all(),
                gridData: FlGridData(
                  show: true,
                  drawVerticalLine: false,
                  horizontalInterval: maxY > 0 ? maxY / 4 : null,
                  getDrawingHorizontalLine: (value) => FlLine(
                    color: WellPaidColors.navy.withValues(alpha: 0.06),
                    strokeWidth: 1,
                  ),
                ),
                borderData: FlBorderData(
                  show: true,
                  border: Border(
                    bottom: BorderSide(
                      color: WellPaidColors.navy.withValues(alpha: 0.12),
                    ),
                    left: BorderSide(
                      color: WellPaidColors.navy.withValues(alpha: 0.12),
                    ),
                  ),
                ),
                titlesData: FlTitlesData(
                  topTitles: const AxisTitles(
                    sideTitles: SideTitles(showTitles: false),
                  ),
                  rightTitles: const AxisTitles(
                    sideTitles: SideTitles(showTitles: false),
                  ),
                  leftTitles: AxisTitles(
                    sideTitles: SideTitles(
                      showTitles: true,
                      reservedSize: 44,
                      interval: maxY > 0 ? maxY / 4 : null,
                      getTitlesWidget: (value, meta) {
                        final c = value.round();
                        return Padding(
                          padding: const EdgeInsets.only(right: 6),
                          child: Text(
                            _compactAxisLabel(c),
                            style: TextStyle(
                              fontSize: 10,
                              color: WellPaidColors.navy.withValues(alpha: 0.55),
                              fontWeight: FontWeight.w500,
                            ),
                            textAlign: TextAlign.right,
                          ),
                        );
                      },
                    ),
                  ),
                  bottomTitles: AxisTitles(
                    sideTitles: SideTitles(
                      showTitles: true,
                      reservedSize: 28,
                      interval: xInterval,
                      getTitlesWidget: (value, meta) {
                        final i = value.round();
                        if (i < 0 || i >= n) {
                          return const SizedBox.shrink();
                        }
                        if (xInterval >= 2 && i % 2 != 0) {
                          return const SizedBox.shrink();
                        }
                        final m = d.months[i];
                        final t = monthFmt.format(DateTime(m.year, m.month));
                        return Padding(
                          padding: const EdgeInsets.only(top: 6),
                          child: Text(
                            t,
                            style: TextStyle(
                              fontSize: 10,
                              color: WellPaidColors.navy.withValues(alpha: 0.65),
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        );
                      },
                    ),
                  ),
                ),
                lineTouchData: LineTouchData(
                  enabled: true,
                  handleBuiltInTouches: true,
                  touchTooltipData: LineTouchTooltipData(
                    fitInsideHorizontally: true,
                    fitInsideVertically: true,
                    getTooltipColor: (_) => Colors.white,
                    tooltipBorder: BorderSide(
                      color: WellPaidColors.navy.withValues(alpha: 0.12),
                    ),
                    tooltipRoundedRadius: 10,
                    tooltipPadding: const EdgeInsets.fromLTRB(12, 10, 12, 10),
                    getTooltipItems: (touchedSpots) {
                      return touchedSpots.map((s) {
                        final bi = s.barIndex;
                        if (bi < 0 || bi >= seriesValues.length) {
                          return null;
                        }
                        final xi = s.x.round().clamp(0, n - 1);
                        final cents = seriesValues[bi][xi];
                        final name = barLabels[bi];
                        return LineTooltipItem(
                          '$name\n${formatBrlFromCents(cents)}',
                          TextStyle(
                            color: WellPaidColors.navy.withValues(alpha: 0.92),
                            fontWeight: FontWeight.w600,
                            fontSize: 12,
                            height: 1.35,
                          ),
                        );
                      }).toList();
                    },
                  ),
                ),
                lineBarsData: bars,
              ),
            ),
          ),
        ],
      ),
    );
  }

  /// Rótulo do eixo Y (centavos → compacto).
  static String _compactAxisLabel(int cents) {
    final v = cents / 100.0;
    if (v.abs() >= 1e6) {
      return 'R\$ ${(v / 1e6).toStringAsFixed(1)}M';
    }
    if (v.abs() >= 1e3) {
      return 'R\$ ${(v / 1e3).toStringAsFixed(1)}k';
    }
    return 'R\$ ${v.round()}';
  }
}

class _LegendChip extends StatelessWidget {
  const _LegendChip({
    required this.color,
    required this.label,
    required this.semanticsHint,
    required this.active,
    required this.onTap,
    this.dashed = false,
  });

  final Color color;
  final String label;
  final String semanticsHint;
  final bool active;
  final VoidCallback onTap;
  final bool dashed;

  @override
  Widget build(BuildContext context) {
    final c = active ? color : color.withValues(alpha: 0.35);
    return Semantics(
      label: semanticsHint,
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(20),
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(20),
              border: Border.all(
                color:
                    WellPaidColors.navy.withValues(alpha: active ? 0.14 : 0.08),
              ),
              color: WellPaidColors.navy.withValues(alpha: active ? 0.04 : 0.02),
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                SizedBox(
                  width: 18,
                  height: 3,
                  child: CustomPaint(
                    painter: _LegendLinePainter(
                      color: c,
                      dashed: dashed,
                    ),
                  ),
                ),
                const SizedBox(width: 6),
                Text(
                  label,
                  style: Theme.of(context).textTheme.labelMedium?.copyWith(
                        color: WellPaidColors.navy
                            .withValues(alpha: active ? 0.9 : 0.45),
                        fontWeight: FontWeight.w600,
                        fontSize: 12,
                      ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _LegendLinePainter extends CustomPainter {
  _LegendLinePainter({required this.color, required this.dashed});

  final Color color;
  final bool dashed;

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = color
      ..strokeWidth = 3
      ..strokeCap = StrokeCap.round;
    if (dashed) {
      var x = 0.0;
      while (x < size.width) {
        canvas.drawLine(Offset(x, size.height / 2), Offset(x + 4, size.height / 2), paint);
        x += 7;
      }
    } else {
      canvas.drawLine(
        Offset(0, size.height / 2),
        Offset(size.width, size.height / 2),
        paint,
      );
    }
  }

  @override
  bool shouldRepaint(covariant _LegendLinePainter oldDelegate) =>
      oldDelegate.color != color || oldDelegate.dashed != dashed;
}
