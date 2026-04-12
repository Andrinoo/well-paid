import 'dart:async';
import 'dart:math' as math;

import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';
import 'package:phosphor_flutter/phosphor_flutter.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../../core/format/brl_cents.dart';
import '../../../core/format/locale_dates.dart';
import '../../../core/l10n/context_l10n.dart';
import '../../../l10n/app_localizations.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/theme/well_paid_colors.dart';
import '../application/dashboard_providers.dart';
import '../domain/dashboard_cashflow.dart';

/// Card **Histórico mensal**: `LineChart` (F3) + legenda tocável + tooltips em BRL.
///
/// Quando [embeddedInHomeTabs] é true, omite o cabeçalho duplicado (o tab já
/// identifica a vista) e dá mais altura ao gráfico.
class DashboardCashflowChartCard extends ConsumerWidget {
  const DashboardCashflowChartCard({
    super.key,
    this.embeddedInHomeTabs = false,
  });

  final bool embeddedInHomeTabs;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    final async = ref.watch(dashboardCashflowProvider);
    final showTitleHeader = !embeddedInHomeTabs;

    final reduceMotion =
        MediaQuery.maybeOf(context)?.disableAnimations ?? false;

    return async.when(
      skipLoadingOnReload: true,
      loading: () => _CashflowShell(
        title: l10n.dashCashflowTitle,
        showTitleHeader: showTitleHeader,
        belowTitle: const _CashflowCompactQueryBar(),
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 16),
          child: Center(
            child: reduceMotion
                ? Icon(
                    PhosphorIconsRegular.chartLineUp,
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
        showTitleHeader: showTitleHeader,
        belowTitle: const _CashflowCompactQueryBar(),
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
        showTitleHeader: showTitleHeader,
        belowTitle: const _CashflowCompactQueryBar(),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            if (d.months.isEmpty)
              Text(
                l10n.dashCashflowEmpty,
                style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                  color: WellPaidColors.navy.withValues(alpha: 0.75),
                ),
              )
            else
              _CashflowLineChartBody(
                data: d,
                embeddedInHomeTabs: embeddedInHomeTabs,
              ),
            const SizedBox(height: 8),
            _CashflowSummaryFooter(data: d),
            if (embeddedInHomeTabs) ...[
              const SizedBox(height: 10),
              _CashflowHomeInsightCard(data: d),
            ],
          ],
        ),
      ),
    );
  }
}

/// Resumo útil no espaço abaixo do rodapé do gráfico (tab Início).
class _CashflowHomeInsightCard extends StatelessWidget {
  const _CashflowHomeInsightCard({required this.data});

  final DashboardCashflow data;

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final n = data.months.length;
    if (n == 0) return const SizedBox.shrink();

    var bestI = 0;
    var bestV = 0;
    for (var i = 0; i < n; i++) {
      final v = data.expensePaidCents[i];
      if (v > bestV) {
        bestV = v;
        bestI = i;
      }
    }
    var bestIncI = 0;
    var bestIncV = 0;
    for (var i = 0; i < n; i++) {
      final v = data.incomeCents[i];
      if (v > bestIncV) {
        bestIncV = v;
        bestIncI = i;
      }
    }

    if (bestV <= 0 && bestIncV <= 0) return const SizedBox.shrink();

    final tag = intlDateTagForUi(context);
    final monthFmt = DateFormat.MMM(tag);
    String monthLabel(int idx) {
      final m = data.months[idx];
      return '${monthFmt.format(DateTime(m.year, m.month))} ${m.year}';
    }

    Widget lineRow(String text) {
      return Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(
            PhosphorIconsRegular.chartLine,
            size: 22,
            color: WellPaidColors.navy.withValues(alpha: 0.72),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              text,
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
              style: Theme.of(context).textTheme.labelLarge?.copyWith(
                height: 1.3,
                color: WellPaidColors.navy.withValues(alpha: 0.9),
                fontWeight: FontWeight.w700,
              ),
            ),
          ),
        ],
      );
    }

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
      decoration: BoxDecoration(
        color: WellPaidColors.gold.withValues(alpha: 0.11),
        borderRadius: BorderRadius.circular(14),
        border: Border.all(
          color: WellPaidColors.navy.withValues(alpha: 0.08),
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          if (bestV > 0)
            lineRow(
              l10n.dashCashflowInsightPeakPaid(
                monthLabel(bestI),
                formatBrlFromCents(bestV),
              ),
            ),
          if (bestV > 0 && bestIncV > 0) const SizedBox(height: 10),
          if (bestIncV > 0)
            lineRow(
              l10n.dashCashflowInsightPeakIncome(
                monthLabel(bestIncI),
                formatBrlFromCents(bestIncV),
              ),
            ),
        ],
      ),
    );
  }
}

void _applyCashflowRequest(
  WidgetRef ref, {
  required bool isDynamic,
  required int forecastMonths,
}) {
  final f = forecastMonths.clamp(1, 12);
  if (isDynamic) {
    ref.read(dashboardCashflowRequestProvider.notifier).state =
        DashboardCashflowRequest(isDynamicWindow: true, forecastMonths: f);
    return;
  }
  final p = ref.read(dashboardPeriodProvider);
  final s = _addCalendarMonths(p.year, p.month, -5);
  ref
      .read(dashboardCashflowRequestProvider.notifier)
      .state = DashboardCashflowRequest(
    isDynamicWindow: false,
    startYear: s.$1,
    startMonth: s.$2,
    endYear: p.year,
    endMonth: p.month,
    forecastMonths: f,
  );
}

/// Controlos compactos: modo dinâmico; previsão com setas (sem campo de texto).
/// Sem novo toque nas setas durante [kForecastPreviewIdle], repõe os meses de
/// previsão ao valor em que a sessão começou (gráfico “volta ao normal”).
class _CashflowCompactQueryBar extends ConsumerStatefulWidget {
  const _CashflowCompactQueryBar();

  @override
  ConsumerState<_CashflowCompactQueryBar> createState() =>
      _CashflowCompactQueryBarState();
}

class _CashflowCompactQueryBarState
    extends ConsumerState<_CashflowCompactQueryBar> {
  static const Duration kForecastPreviewIdle = Duration(seconds: 5);

  Timer? _previewIdleTimer;

  /// Valor de `forecastMonths` antes do primeiro toque nas setas nesta sessão.
  int? _previewRestoreForecastMonths;
  bool _suppressExternalForecastListen = false;

  @override
  void dispose() {
    _previewIdleTimer?.cancel();
    super.dispose();
  }

  void _cancelForecastPreviewSession() {
    _previewIdleTimer?.cancel();
    _previewIdleTimer = null;
    _previewRestoreForecastMonths = null;
  }

  void _applyForecastNudge(int delta) {
    final current = ref.read(dashboardCashflowRequestProvider);
    final before = current.forecastMonths.clamp(1, 12);
    final next = (before + delta).clamp(1, 12);

    _previewIdleTimer?.cancel();

    _previewRestoreForecastMonths ??= before;

    if (next == before) {
      _armPreviewIdleTimer();
      return;
    }

    _suppressExternalForecastListen = true;
    _applyCashflowRequest(
      ref,
      isDynamic: current.isDynamicWindow,
      forecastMonths: next,
    );
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) _suppressExternalForecastListen = false;
    });

    if (_previewRestoreForecastMonths != null &&
        next == _previewRestoreForecastMonths) {
      _cancelForecastPreviewSession();
      return;
    }
    _armPreviewIdleTimer();
  }

  void _armPreviewIdleTimer() {
    _previewIdleTimer?.cancel();
    _previewIdleTimer = Timer(kForecastPreviewIdle, () {
      if (!mounted) return;
      final restore = _previewRestoreForecastMonths;
      if (restore == null) return;
      _previewRestoreForecastMonths = null;
      _previewIdleTimer = null;
      final cur = ref.read(dashboardCashflowRequestProvider);
      if (cur.forecastMonths == restore) return;
      _suppressExternalForecastListen = true;
      _applyCashflowRequest(
        ref,
        isDynamic: cur.isDynamicWindow,
        forecastMonths: restore,
      );
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (mounted) _suppressExternalForecastListen = false;
      });
    });
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final req = ref.watch(dashboardCashflowRequestProvider);

    ref.listen<DashboardCashflowRequest>(dashboardCashflowRequestProvider, (
      prev,
      next,
    ) {
      if (_suppressExternalForecastListen) return;
      if (prev?.forecastMonths != next.forecastMonths) {
        _cancelForecastPreviewSession();
      }
    });

    final fc = req.forecastMonths.clamp(1, 12);
    final labelStyle = Theme.of(context).textTheme.labelSmall?.copyWith(
      color: WellPaidColors.navy.withValues(alpha: 0.78),
      fontWeight: FontWeight.w700,
    );

    return Row(
      crossAxisAlignment: CrossAxisAlignment.center,
      children: [
        Tooltip(
          message: l10n.dashCashflowDynamicWindowTooltip,
          child: Icon(
            PhosphorIconsRegular.arrowsClockwise,
            size: 17,
            color: WellPaidColors.navy.withValues(alpha: 0.62),
          ),
        ),
        const SizedBox(width: 2),
        Transform.scale(
          scale: 0.82,
          alignment: Alignment.centerLeft,
          child: Switch(
            value: req.isDynamicWindow,
            materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
            onChanged: (v) {
              _cancelForecastPreviewSession();
              _applyCashflowRequest(ref, isDynamic: v, forecastMonths: fc);
            },
            activeThumbColor: WellPaidColors.gold,
            activeTrackColor: WellPaidColors.gold.withValues(alpha: 0.45),
          ),
        ),
        Expanded(
          child: Tooltip(
            message: l10n.dashCashflowDynamicWindowTooltip,
            child: Text(
              req.isDynamicWindow
                  ? l10n.dashCashflowBarRollingLabel
                  : l10n.dashCashflowBarFixedLabel,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: labelStyle,
            ),
          ),
        ),
        Tooltip(
          message: l10n.dashCashflowForecastBarTooltip,
          child: Text(
            l10n.dashCashflowForecastBarShort,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: labelStyle,
          ),
        ),
        const SizedBox(width: 2),
        IconButton(
          tooltip: l10n.dashCashflowA11yForecastDecrease,
          padding: EdgeInsets.zero,
          constraints: const BoxConstraints(minWidth: 32, minHeight: 32),
          iconSize: 22,
          onPressed: () => _applyForecastNudge(-1),
          icon: Icon(
            PhosphorIconsRegular.caretLeft,
            color: WellPaidColors.navy.withValues(alpha: 0.75),
          ),
        ),
        SizedBox(
          width: 22,
          child: Text(
            '$fc',
            textAlign: TextAlign.center,
            style: Theme.of(context).textTheme.labelLarge?.copyWith(
              color: WellPaidColors.navy,
              fontWeight: FontWeight.w800,
            ),
          ),
        ),
        IconButton(
          tooltip: l10n.dashCashflowA11yForecastIncrease,
          padding: EdgeInsets.zero,
          constraints: const BoxConstraints(minWidth: 32, minHeight: 32),
          iconSize: 22,
          onPressed: () => _applyForecastNudge(1),
          icon: Icon(
            PhosphorIconsRegular.caretRight,
            color: WellPaidColors.navy.withValues(alpha: 0.75),
          ),
        ),
      ],
    );
  }
}

class _CashflowShell extends StatelessWidget {
  const _CashflowShell({
    required this.title,
    required this.child,
    this.belowTitle,
    this.showTitleHeader = true,
  });

  final String title;
  final Widget child;
  final Widget? belowTitle;
  final bool showTitleHeader;

  @override
  Widget build(BuildContext context) {
    return Card(
      elevation: 2,
      shadowColor: WellPaidColors.navy.withValues(alpha: 0.1),
      color: Colors.white,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(20),
        side: BorderSide(color: WellPaidColors.navy.withValues(alpha: 0.06)),
      ),
      child: Padding(
        padding: const EdgeInsets.fromLTRB(10, 6, 10, 8),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            if (showTitleHeader)
              Semantics(
                header: true,
                child: Row(
                  children: [
                    Icon(
                      PhosphorIconsRegular.chartLineUp,
                      size: 20,
                      color: WellPaidColors.navy.withValues(alpha: 0.85),
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Text(
                        title,
                        style: Theme.of(context).textTheme.titleSmall?.copyWith(
                          fontWeight: FontWeight.w700,
                          color: WellPaidColors.navy,
                          fontSize: 15,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            if (belowTitle != null) ...[
              if (showTitleHeader) const SizedBox(height: 4),
              if (!showTitleHeader) const SizedBox(height: 2),
              belowTitle!,
            ],
            const SizedBox(height: 4),
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
          const SizedBox(height: 8),
          ExcludeSemantics(
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Expanded(
                  child: Text(
                    l10n.dashCashflowFooterForecastTotal(forecastStr),
                    style: Theme.of(context).textTheme.labelSmall?.copyWith(
                      color: WellPaidColors.navy.withValues(alpha: 0.82),
                      fontWeight: FontWeight.w600,
                      height: 1.25,
                    ),
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    l10n.dashCashflowFooterBalance(balanceStr),
                    textAlign: TextAlign.end,
                    style: Theme.of(context).textTheme.labelSmall?.copyWith(
                      color: balanceColor,
                      fontWeight: FontWeight.w800,
                      height: 1.25,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _CashflowLineChartBody extends StatefulWidget {
  const _CashflowLineChartBody({
    required this.data,
    this.embeddedInHomeTabs = false,
  });

  final DashboardCashflow data;
  final bool embeddedInHomeTabs;

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

  /// `null` → painel usa o mês com mais movimento nas séries visíveis (evita último mês com tudo a zero).
  int? _touchedMonthIndex;

  @override
  void didUpdateWidget(covariant _CashflowLineChartBody oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.data.months.length != widget.data.months.length) {
      _touchedMonthIndex = null;
    }
  }

  /// Mês com maior soma (séries visíveis) — alinha ao “pico” do gráfico em vez do fim do eixo.
  int _defaultMonthIndexByVisibleActivity(int n) {
    final d = widget.data;
    var bestI = 0;
    var bestScore = -1;
    for (var i = 0; i < n; i++) {
      var s = 0;
      if (_showIncome) s += d.incomeCents[i];
      if (_showPaid) s += d.expensePaidCents[i];
      if (_showForecast) s += d.expenseForecastCents[i];
      if (s > bestScore) {
        bestScore = s;
        bestI = i;
      }
    }
    return bestI;
  }

  int _monthIndexForPanel(int n) {
    if (n <= 0) return 0;
    final t = _touchedMonthIndex;
    if (t != null) return t.clamp(0, n - 1);
    return _defaultMonthIndexByVisibleActivity(n);
  }

  Widget _monthDetailPanel(
    BuildContext context,
    AppLocalizations l10n, {
    required DateFormat monthFmt,
    required int n,
    required List<Color> barAccentColors,
    required List<List<int>> seriesValues,
    required List<String> barLabels,
  }) {
    final d = widget.data;
    final i = _monthIndexForPanel(n);
    final m = d.months[i];
    final title = monthFmt.format(DateTime(m.year, m.month));

    final rows = <Widget>[
      Text(
        title,
        style: Theme.of(context).textTheme.titleSmall?.copyWith(
          fontWeight: FontWeight.w800,
          color: WellPaidColors.navy,
          fontSize: 14,
        ),
      ),
      if (_touchedMonthIndex == null)
        Padding(
          padding: const EdgeInsets.only(top: 3),
          child: Text(
            l10n.dashCashflowTouchChartHint,
            style: Theme.of(context).textTheme.labelSmall?.copyWith(
              color: WellPaidColors.navy.withValues(alpha: 0.5),
              height: 1.25,
              fontSize: 10,
            ),
          ),
        ),
      const SizedBox(height: 6),
    ];

    for (var bi = 0; bi < barLabels.length; bi++) {
      final color = barAccentColors[bi];
      rows.add(
        Padding(
          padding: EdgeInsets.only(bottom: bi < barLabels.length - 1 ? 6 : 0),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                width: 3,
                height: 16,
                margin: const EdgeInsets.only(top: 2),
                decoration: BoxDecoration(
                  color: color,
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  barLabels[bi],
                  style: Theme.of(context).textTheme.labelMedium?.copyWith(
                    fontWeight: FontWeight.w700,
                    color: WellPaidColors.navy.withValues(alpha: 0.88),
                    height: 1.2,
                    fontSize: 12,
                  ),
                ),
              ),
              Text(
                formatBrlFromCents(seriesValues[bi][i]),
                style: Theme.of(context).textTheme.labelMedium?.copyWith(
                  fontWeight: FontWeight.w800,
                  color: WellPaidColors.navy,
                  fontSize: 12,
                ),
              ),
            ],
          ),
        ),
      );
    }

    return Semantics(
      container: true,
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.fromLTRB(10, 8, 10, 10),
        decoration: BoxDecoration(
          color: WellPaidColors.navy.withValues(alpha: 0.045),
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: WellPaidColors.navy.withValues(alpha: 0.1)),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: rows,
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final d = widget.data;
    final n = d.months.length;
    final tag = intlDateTagForUi(context);
    final monthFmt = DateFormat.MMM(tag);
    final reduceMotion = MediaQuery.disableAnimationsOf(context);
    final mq = MediaQuery.sizeOf(context);
    final shortSide = mq.shortestSide;
    final screenW = mq.width;
    final screenH = mq.height;

    /// Evita altura demasiado baixa em telemóveis altos e estreitos (ex.: Poco X7 Pro),
    /// onde só `shortSide` subdimensionava a área útil e cortava o gráfico.
    /// No Início com tabs, um só gráfico por vista → mais altura útil.
    final chartHeight = widget.embeddedInHomeTabs
        ? math
              .max(shortSide * 0.42, screenH * 0.24)
              .clamp(200.0, 320.0)
        : math
              .max(shortSide * 0.36, screenH * 0.195)
              .clamp(186.0, 276.0);
    final leftAxisReserved = screenW < 340 ? 28.0 : 32.0;
    final bottomAxisReserved = shortSide < 640 ? 26.0 : 30.0;
    final bottomLabelSize = shortSide < 640 ? 9.5 : 10.0;
    final chartDuration = reduceMotion
        ? Duration.zero
        : const Duration(milliseconds: 380);
    final chartCurve = reduceMotion ? Curves.linear : Curves.easeOutCubic;

    final seriesValues = <List<int>>[];
    final barLabels = <String>[];
    final bars = <LineChartBarData>[];
    final barAccentColors = <Color>[];

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
      barAccentColors.add(color);
      final spots = <FlSpot>[
        for (var i = 0; i < n; i++) FlSpot(i.toDouble(), cents[i].toDouble()),
      ];
      bars.add(
        LineChartBarData(
          spots: spots,
          color: color,
          barWidth: 2.8,
          isCurved: !reduceMotion,
          curveSmoothness: reduceMotion ? 0 : 0.32,
          preventCurveOverShooting: !reduceMotion,
          dashArray: dashed ? [7, 5] : null,
          dotData: FlDotData(
            show: true,
            getDotPainter: (spot, percent, barData, index) {
              if (dashed) {
                return FlDotCirclePainter(
                  radius: 4.5,
                  color: Colors.transparent,
                  strokeWidth: 2,
                  strokeColor: color,
                );
              }
              return FlDotCirclePainter(
                radius: 5,
                color: color,
                strokeWidth: 1.5,
                strokeColor: Colors.white,
              );
            },
          ),
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
      if (_showIncome) {
        maxCents = maxCents > d.incomeCents[i] ? maxCents : d.incomeCents[i];
      }
      if (_showPaid) {
        maxCents = maxCents > d.expensePaidCents[i]
            ? maxCents
            : d.expensePaidCents[i];
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
          Padding(
            padding: const EdgeInsets.fromLTRB(2, 8, 2, 0),
            child: SizedBox(
              height: 30,
              child: Row(
                children: [
                  Expanded(
                    child: Center(
                      child: FittedBox(
                        fit: BoxFit.scaleDown,
                        child: _LegendChip(
                          color: _income,
                          label: l10n.dashCashflowLegendIncome,
                          semanticsHint: l10n.dashCashflowA11ySeriesToggle(
                            l10n.dashCashflowLegendIncome,
                          ),
                          active: _showIncome,
                          onTap: () => setState(() {
                            _showIncome = !_showIncome;
                            _touchedMonthIndex = null;
                          }),
                        ),
                      ),
                    ),
                  ),
                  Expanded(
                    child: Center(
                      child: FittedBox(
                        fit: BoxFit.scaleDown,
                        child: _LegendChip(
                          color: _paid,
                          label: l10n.dashCashflowLegendExpensePaid,
                          semanticsHint: l10n.dashCashflowA11ySeriesToggle(
                            l10n.dashCashflowLegendExpensePaid,
                          ),
                          active: _showPaid,
                          onTap: () => setState(() {
                            _showPaid = !_showPaid;
                            _touchedMonthIndex = null;
                          }),
                        ),
                      ),
                    ),
                  ),
                  Expanded(
                    child: Center(
                      child: FittedBox(
                        fit: BoxFit.scaleDown,
                        child: _LegendChip(
                          color: _forecast,
                          label: l10n.dashCashflowLegendExpenseForecast,
                          semanticsHint: l10n.dashCashflowA11ySeriesToggle(
                            l10n.dashCashflowLegendExpenseForecast,
                          ),
                          active: _showForecast,
                          dashed: true,
                          onTap: () => setState(() {
                            _showForecast = !_showForecast;
                            _touchedMonthIndex = null;
                          }),
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 14),
          Padding(
            padding: const EdgeInsets.only(bottom: 6),
            child: SizedBox(
              height: chartHeight,
              child: Padding(
                padding: const EdgeInsets.only(top: 10),
                child: LineChart(
                LineChartData(
                  minX: 0,
                  maxX: (n - 1).toDouble(),
                  minY: 0,
                  maxY: maxY.toDouble(),
                  clipData: const FlClipData.none(),
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
                        reservedSize: leftAxisReserved,
                        interval: maxY > 0 ? maxY / 4 : null,
                        getTitlesWidget: (value, meta) {
                          final c = value.round();
                          return Padding(
                            padding: const EdgeInsets.only(right: 6),
                            child: Text(
                              _compactAxisLabel(c),
                              style: TextStyle(
                                fontSize: 10,
                                color: WellPaidColors.navy.withValues(
                                  alpha: 0.55,
                                ),
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
                        reservedSize: bottomAxisReserved,
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
                            padding: const EdgeInsets.only(top: 4),
                            child: Text(
                              t,
                              style: TextStyle(
                                fontSize: bottomLabelSize,
                                color: WellPaidColors.navy.withValues(
                                  alpha: 0.65,
                                ),
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
                    touchSpotThreshold: 22,
                    touchCallback: (event, response) {
                      final spots = response?.lineBarSpots;
                      if (spots == null || spots.isEmpty) return;
                      // O ponto mais próximo em 2D pode ser a linha no zero (ex. previsto).
                      // Usar o spot com maior Y → mês do pico visível, alinhado aos valores reais.
                      TouchLineBarSpot? peak;
                      for (final s in spots) {
                        if (peak == null || s.y > peak.y) peak = s;
                      }
                      final xi = peak!.x.round().clamp(0, n - 1);
                      setState(() => _touchedMonthIndex = xi);
                    },
                    touchTooltipData: LineTouchTooltipData(
                      fitInsideHorizontally: true,
                      fitInsideVertically: true,
                      getTooltipItems: (touchedSpots) =>
                          touchedSpots.map((_) => null).toList(),
                    ),
                  ),
                  lineBarsData: bars,
                ),
                duration: chartDuration,
                curve: chartCurve,
              ),
            ),
            ),
          ),
          const SizedBox(height: 10),
          _monthDetailPanel(
            context,
            l10n,
            monthFmt: monthFmt,
            n: n,
            barAccentColors: barAccentColors,
            seriesValues: seriesValues,
            barLabels: barLabels,
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
          borderRadius: BorderRadius.circular(12),
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 2),
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(10),
              border: Border.all(
                color: WellPaidColors.navy.withValues(
                  alpha: active ? 0.14 : 0.08,
                ),
              ),
              color: WellPaidColors.navy.withValues(
                alpha: active ? 0.04 : 0.02,
              ),
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                SizedBox(
                  width: 12,
                  height: 2,
                  child: CustomPaint(
                    painter: _LegendLinePainter(color: c, dashed: dashed),
                  ),
                ),
                const SizedBox(width: 4),
                Text(
                  label,
                  style: Theme.of(context).textTheme.labelSmall?.copyWith(
                    color: WellPaidColors.navy.withValues(
                      alpha: active ? 0.9 : 0.45,
                    ),
                    fontWeight: FontWeight.w600,
                    fontSize: 9.5,
                    height: 1.05,
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
      ..strokeWidth = 2
      ..strokeCap = StrokeCap.round;
    if (dashed) {
      var x = 0.0;
      while (x < size.width) {
        canvas.drawLine(
          Offset(x, size.height / 2),
          Offset(x + 4, size.height / 2),
          paint,
        );
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
