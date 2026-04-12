import 'dart:math' as math;

import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:phosphor_flutter/phosphor_flutter.dart';
import 'package:flutter/services.dart';

import '../../../core/format/brl_cents.dart';
import '../../../core/format/locale_dates.dart';
import '../../../core/l10n/context_l10n.dart';
import '../../../core/theme/well_paid_colors.dart';
import '../../../l10n/app_localizations.dart';
import '../domain/dashboard_overview.dart';

/// Máximo de fatias no donut; o resto agrega numa fatia “outros” (rótulo localizado).
const int _kMaxDonutSlices = 5;

const _palette = <Color>[
  Color(0xFF1B4D6F),
  Color(0xFFC9A94E),
  Color(0xFF2A7A6E),
  Color(0xFF3D5A80),
  Color(0xFFB85C4A),
  Color(0xFF6B6560),
];

const _categoryIcons = <String, IconData>{
  'alimentacao': PhosphorIconsRegular.forkKnife,
  'transporte': PhosphorIconsRegular.car,
  'moradia': PhosphorIconsRegular.houseLine,
  'saude': PhosphorIconsRegular.firstAid,
  'educacao': PhosphorIconsRegular.graduationCap,
  'lazer': PhosphorIconsRegular.gameController,
  'pessoais': PhosphorIconsRegular.user,
  'emprestimos': PhosphorIconsRegular.bank,
  'outros': PhosphorIconsRegular.squaresFour,
};

/// Tamanho de referência (largura/altura do quadrado do gráfico em dp “grandes”).
const double _refChartBox = 238;
const double _sliceRadius = 54;
const double _centerHole = 58;

enum _DonutDepthPreset { soft, medium }

const _donutDepthPreset = _DonutDepthPreset.soft;

/// Ordena por valor e mantém no máximo [_kMaxDonutSlices] fatias; o resto vira uma linha "Outros".
///
/// Usado também no scroll do Início para persistir o índice da fatia selecionada.
List<CategorySpend> aggregateSpendingRowsForDonut(
  List<CategorySpend> source,
  int monthExpenseTotalCents,
  String otherLabel,
) {
  if (source.isEmpty) return [];
  final sorted = List<CategorySpend>.from(source)
    ..sort((a, b) => b.amountCents.compareTo(a.amountCents));
  if (sorted.length <= _kMaxDonutSlices) return sorted;
  final top = sorted.take(_kMaxDonutSlices).toList();
  var rest = 0;
  for (var i = _kMaxDonutSlices; i < sorted.length; i++) {
    rest += sorted[i].amountCents;
  }
  if (rest <= 0) return top;
  return [
    ...top,
    CategorySpend(
      categoryKey: 'outros',
      name: otherLabel,
      amountCents: rest,
      shareBps: monthExpenseTotalCents > 0
          ? (rest * 10000 ~/ monthExpenseTotalCents)
          : null,
    ),
  ];
}

/// Rosca por categoria (`fl_chart`): agregação, animação, legenda responsiva.
class CategoryDonutChart extends StatefulWidget {
  const CategoryDonutChart({
    super.key,
    required this.categories,
    required this.monthExpenseTotalCents,
    required this.selectedSliceIndex,
    required this.onSelectedSliceIndexChanged,
    this.period,
    this.onViewCategoryExpenses,
    this.onRegisterExpense,
  });

  final List<CategorySpend> categories;
  final int monthExpenseTotalCents;
  /// Índice na lista agregada [aggregateSpendingRowsForDonut] (0 = maior fatia).
  final int selectedSliceIndex;
  final ValueChanged<int> onSelectedSliceIndexChanged;
  final PeriodMonth? period;

  /// Abre a lista de despesas filtrada pela categoria selecionada no donut.
  final ValueChanged<CategorySpend>? onViewCategoryExpenses;

  /// CTA quando não há despesas no mês (ex.: nova despesa).
  final VoidCallback? onRegisterExpense;

  @override
  State<CategoryDonutChart> createState() => _CategoryDonutChartState();
}

class _CategoryDonutChartState extends State<CategoryDonutChart>
    with SingleTickerProviderStateMixin {
  late final AnimationController _intro;

  @override
  void initState() {
    super.initState();
    _intro = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 800),
    );
    _intro.forward();
  }

  @override
  void didUpdateWidget(covariant CategoryDonutChart oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.monthExpenseTotalCents != widget.monthExpenseTotalCents ||
        !listEquals(oldWidget.categories, widget.categories) ||
        oldWidget.period?.year != widget.period?.year ||
        oldWidget.period?.month != widget.period?.month) {
      _intro
        ..reset()
        ..forward();
    }
  }

  @override
  void dispose() {
    _intro.dispose();
    super.dispose();
  }

  void _selectSlice(int i) {
    if (i == widget.selectedSliceIndex) return;
    HapticFeedback.selectionClick();
    widget.onSelectedSliceIndexChanged(i);
  }

  double get _depthStrength {
    return switch (_donutDepthPreset) {
      _DonutDepthPreset.soft => 1.0,
      _DonutDepthPreset.medium => 1.35,
    };
  }

  List<PieChartSectionData> _sections(
    bool hasData,
    double introT,
    List<CategorySpend> rows,
    double g,
  ) {
    final sliceR = _sliceRadius * g;
    if (!hasData || introT < 0.002) {
      return [
        PieChartSectionData(
          color: WellPaidColors.navy.withValues(alpha: 0.12 + 0.08 * introT),
          value: 1,
          title: '',
          radius: sliceR,
          showTitle: false,
          borderSide: BorderSide.none,
        ),
      ];
    }
    final out = <PieChartSectionData>[];
    for (var i = 0; i < rows.length; i++) {
      final c = rows[i];
      final base = _palette[i % _palette.length];
      final touched = widget.selectedSliceIndex == i;
      final scaled = c.amountCents * introT;
      final glowA = (0.42 + 0.58 * introT).clamp(0.0, 1.0) / _depthStrength;
      out.add(
        PieChartSectionData(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [
              Color.lerp(base, Colors.white, 0.2)!.withValues(alpha: glowA),
              base.withValues(alpha: glowA),
              Color.lerp(base, Colors.black, 0.28)!.withValues(alpha: glowA),
            ],
            stops: const [0.0, 0.5, 1.0],
          ),
          value: scaled < 0.001 ? 0.001 : scaled,
          title: '',
          showTitle: false,
          radius: touched ? sliceR + 10 * g : sliceR,
          borderSide: BorderSide(
            color: WellPaidColors.cream.withValues(
              alpha: touched ? 0.95 : 0.88,
            ),
            width: (touched ? 2.5 : 1.8) * _depthStrength,
          ),
        ),
      );
    }
    return out;
  }

  List<PieChartSectionData> _shadowSections(
    bool hasData,
    double introT,
    List<CategorySpend> rows,
    double g,
  ) {
    final sliceR = _sliceRadius * g;
    if (!hasData || introT < 0.002) {
      return [
        PieChartSectionData(
          color: Colors.black.withValues(alpha: 0.06),
          value: 1,
          title: '',
          radius: sliceR,
          showTitle: false,
          borderSide: BorderSide.none,
        ),
      ];
    }
    return [
      for (var i = 0; i < rows.length; i++)
        PieChartSectionData(
          color: Color.alphaBlend(
            Colors.black.withValues(alpha: 0.38),
            _palette[i % _palette.length],
          ),
          value: () {
            final v = rows[i].amountCents * introT;
            return v < 0.001 ? 0.001 : v;
          }(),
          title: '',
          showTitle: false,
          radius: sliceR,
          borderSide: BorderSide(
            color: Colors.black.withValues(alpha: 0.15),
            width: 1,
          ),
        ),
    ];
  }

  Widget _centerContent(
    BuildContext context, {
    required AppLocalizations l10n,
    required bool hasData,
    required String totalLabel,
    required double g,
  }) {
    final p = widget.period;
    final periodLine = p != null && p.month >= 1 && p.month <= 12
        ? formatMonthYearUiHeading(context, DateTime(p.year, p.month))
        : null;

    final maxW = (_centerHole * g) * 2 - (18 + 6 * g);

    return SizedBox(
      width: maxW,
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (periodLine != null)
            Padding(
              padding: const EdgeInsets.only(bottom: 3),
              child: Text(
                periodLine,
                style: Theme.of(context).textTheme.labelSmall?.copyWith(
                  fontSize: 10,
                  color: WellPaidColors.navy.withValues(alpha: 0.58),
                  fontWeight: FontWeight.w600,
                  letterSpacing: 0.15,
                ),
                textAlign: TextAlign.center,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
              ),
            ),
          Text(
            l10n.chartTotalExpenses,
            style: Theme.of(context).textTheme.labelSmall?.copyWith(
              fontSize: 10,
              color: WellPaidColors.navy.withValues(alpha: 0.62),
              fontWeight: FontWeight.w500,
            ),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 2),
          Text(
            totalLabel,
            style: TextStyle(
              fontSize: 12,
              fontWeight: FontWeight.w800,
              color: WellPaidColors.navy,
              height: 1.2,
            ),
            textAlign: TextAlign.center,
            maxLines: 2,
            overflow: TextOverflow.ellipsis,
          ),
          if (!hasData) ...[
            const SizedBox(height: 6),
            Text(
              periodLine != null
                  ? l10n.chartNoExpensesRegistered
                  : l10n.chartNoExpensesThisMonth,
              textAlign: TextAlign.center,
              style: Theme.of(context).textTheme.labelSmall?.copyWith(
                fontSize: 10,
                color: WellPaidColors.navy.withValues(alpha: 0.52),
                height: 1.25,
              ),
            ),
          ],
        ],
      ),
    );
  }

  Widget _chartStack(
    BuildContext context, {
    required AppLocalizations l10n,
    required bool hasData,
    required double introT,
    required List<PieChartSectionData> sections,
    required List<PieChartSectionData> shadows,
    required String totalLabel,
    required double side,
    required double g,
  }) {
    final hole = _centerHole * g;
    return SizedBox(
      width: side,
      height: side,
      child: Stack(
        alignment: Alignment.center,
        clipBehavior: Clip.none,
        children: [
          DecoratedBox(
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              gradient: RadialGradient(
                colors: [
                  WellPaidColors.cream.withValues(alpha: 0.9),
                  WellPaidColors.creamMuted.withValues(alpha: 0.55),
                ],
              ),
              border: Border.all(
                color: WellPaidColors.navy.withValues(alpha: 0.14),
                width: 1.2 * _depthStrength * g.clamp(0.85, 1.0),
              ),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withValues(alpha: 0.1),
                  blurRadius: 16 * _depthStrength * g,
                  offset: Offset(
                    2 * _depthStrength * g,
                    8 * _depthStrength * g,
                  ),
                ),
              ],
            ),
            child: SizedBox(width: side, height: side),
          ),
          // Camada de realce superior para reforçar o efeito 3D.
          IgnorePointer(
            child: Container(
              width: side - 10 * g,
              height: side - 10 * g,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                gradient: LinearGradient(
                  begin: Alignment.topCenter,
                  end: Alignment.bottomCenter,
                  colors: [
                    Colors.white.withValues(alpha: 0.26 * _depthStrength),
                    Colors.white.withValues(alpha: 0.02),
                  ],
                ),
              ),
            ),
          ),
          Transform.translate(
            offset: Offset(2 * g, 4 * g),
            child: PieChart(
              PieChartData(
                sectionsSpace: hasData && introT > 0.01 ? 2.2 : 0,
                centerSpaceRadius: hole,
                sections: shadows,
                pieTouchData: PieTouchData(enabled: false),
                startDegreeOffset: -90 + (1 - introT) * 6,
              ),
            ),
          ),
          PieChart(
            PieChartData(
              sectionsSpace: hasData && introT > 0.01 ? 2.2 : 0,
              centerSpaceRadius: hole,
              sections: sections,
              pieTouchData: PieTouchData(
                enabled: hasData && introT > 0.05,
                touchCallback: (event, pieTouchResponse) {
                  final sec = pieTouchResponse?.touchedSection;
                  if (sec == null) return;
                  final idx = sec.touchedSectionIndex;
                  if (idx < 0) return;
                  _selectSlice(idx);
                },
              ),
              startDegreeOffset: -90 + (1 - introT) * (6 * _depthStrength),
            ),
          ),
          _centerContent(
            context,
            l10n: l10n,
            hasData: hasData,
            totalLabel: totalLabel,
            g: g,
          ),
        ],
      ),
    );
  }

  double _donutSide(BoxConstraints c) {
    final w = c.maxWidth;
    final h = c.maxHeight;
    if (!w.isFinite || !h.isFinite) return _refChartBox;
    final sideW = w.clamp(158.0, _refChartBox + 40);
    return math.min(sideW, h * 0.95);
  }

  Widget _selectedCategoryStrip(
    BuildContext context,
    AppLocalizations l10n, {
    required List<CategorySpend> rows,
    required int index,
  }) {
    final row = rows[index];
    final color = _palette[index % _palette.length];
    final icon =
        _categoryIcons[row.categoryKey] ?? PhosphorIconsRegular.squaresFour;
    final pct = widget.monthExpenseTotalCents > 0
        ? ((row.amountCents * 100) ~/ widget.monthExpenseTotalCents)
        : 0;

    return Material(
      color: color.withValues(alpha: 0.14),
      borderRadius: BorderRadius.circular(12),
      child: InkWell(
        borderRadius: BorderRadius.circular(12),
        onTap: widget.onViewCategoryExpenses != null
            ? () => widget.onViewCategoryExpenses!(row)
            : null,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(
                l10n.chartDonutSelectedHeading,
                style: Theme.of(context).textTheme.labelSmall?.copyWith(
                  fontWeight: FontWeight.w700,
                  color: WellPaidColors.navy.withValues(alpha: 0.52),
                  fontSize: 9.5,
                  letterSpacing: 0.2,
                ),
              ),
              const SizedBox(height: 4),
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  _swatch(icon, color, true, compact: true, size: 24),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      row.name,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: Theme.of(context).textTheme.titleSmall?.copyWith(
                        fontWeight: FontWeight.w800,
                        fontSize: 14,
                        height: 1.2,
                        color: WellPaidColors.navy,
                      ),
                    ),
                  ),
                  const SizedBox(width: 6),
                  Text(
                    formatBrlFromCents(row.amountCents),
                    style: Theme.of(context).textTheme.labelLarge?.copyWith(
                      fontWeight: FontWeight.w800,
                      color: WellPaidColors.navy,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 2),
              Text(
                l10n.chartDonutPctOfTotal(pct),
                style: Theme.of(context).textTheme.labelSmall?.copyWith(
                  color: WellPaidColors.navy.withValues(alpha: 0.55),
                  fontSize: 10,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _categoryGridTile(
    BuildContext context, {
    required int index,
    required CategorySpend row,
  }) {
    final color = _palette[index % _palette.length];
    final selected = widget.selectedSliceIndex == index;
    final icon =
        _categoryIcons[row.categoryKey] ?? PhosphorIconsRegular.squaresFour;
    final pct = widget.monthExpenseTotalCents > 0
        ? ((row.amountCents * 100) ~/ widget.monthExpenseTotalCents)
        : 0;

    return Material(
      color: selected
          ? color.withValues(alpha: 0.12)
          : WellPaidColors.navy.withValues(alpha: 0.03),
      borderRadius: BorderRadius.circular(10),
      child: InkWell(
        borderRadius: BorderRadius.circular(10),
        onTap: () => _selectSlice(index),
        child: Padding(
          padding: const EdgeInsets.fromLTRB(6, 6, 4, 6),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _swatch(icon, color, selected, compact: true, size: 18),
              const SizedBox(width: 6),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Text(
                      row.name,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: Theme.of(context).textTheme.labelLarge?.copyWith(
                        fontWeight: FontWeight.w800,
                        fontSize: 11,
                        height: 1.15,
                        color: WellPaidColors.navy,
                      ),
                    ),
                    Text(
                      '$pct% · ${formatBrlFromCents(row.amountCents)}',
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: Theme.of(context).textTheme.labelSmall?.copyWith(
                        fontSize: 9.5,
                        color: WellPaidColors.navy.withValues(alpha: 0.58),
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _intro,
      builder: (context, _) {
        return LayoutBuilder(
          builder: (context, outerConstraints) {
            final l10n = context.l10n;
            final rows = aggregateSpendingRowsForDonut(
              widget.categories,
              widget.monthExpenseTotalCents,
              l10n.chartCategoryOther,
            );
            final hasData =
                rows.isNotEmpty && widget.monthExpenseTotalCents > 0;
            final totalLabel = formatBrlFromCents(
              hasData ? widget.monthExpenseTotalCents : 0,
            );
            final introT = Curves.easeOutCubic.transform(_intro.value);

            final footerHint = !hasData
                ? Padding(
                    padding: const EdgeInsets.only(top: 8),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        Text(
                          l10n.chartCategoriesHint,
                          textAlign: TextAlign.center,
                          style: Theme.of(context).textTheme.bodySmall
                              ?.copyWith(
                                color: WellPaidColors.navy.withValues(
                                  alpha: 0.62,
                                ),
                                height: 1.35,
                              ),
                        ),
                        if (widget.onRegisterExpense != null) ...[
                          const SizedBox(height: 12),
                          Center(
                            child: FilledButton.tonalIcon(
                              onPressed: widget.onRegisterExpense,
                              icon: Icon(
                                PhosphorIconsRegular.plusCircle,
                                color: WellPaidColors.navy.withValues(
                                  alpha: 0.9,
                                ),
                              ),
                              label: Text(l10n.chartRegisterExpenseCta),
                              style: FilledButton.styleFrom(
                                foregroundColor: WellPaidColors.navy,
                              ),
                            ),
                          ),
                        ],
                      ],
                    ),
                  )
                : const SizedBox.shrink();

            if (!hasData) {
              final side = _donutSide(outerConstraints);
              final g = side / _refChartBox;
              return Semantics(
                label: l10n.chartSemanticsNoData,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Expanded(
                      child: Center(
                        child: _chartStack(
                          context,
                          l10n: l10n,
                          hasData: false,
                          introT: introT,
                          sections: _sections(false, introT, rows, g),
                          shadows: _shadowSections(false, introT, rows, g),
                          totalLabel: totalLabel,
                          side: side,
                          g: g,
                        ),
                      ),
                    ),
                    footerHint,
                  ],
                ),
              );
            }

            final si = widget.selectedSliceIndex.clamp(0, rows.length - 1);
            final selectedCategory = rows[si];
            final otherIndices = <int>[
              for (var i = 0; i < rows.length; i++)
                if (i != si) i,
            ];

            return Semantics(
              label: l10n.chartSemanticsWithData(totalLabel),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Expanded(
                    flex: 4,
                    child: LayoutBuilder(
                      builder: (context, c) {
                        final side = _donutSide(c);
                        final g = side / _refChartBox;
                        return Center(
                          child: _chartStack(
                            context,
                            l10n: l10n,
                            hasData: true,
                            introT: introT,
                            sections: _sections(true, introT, rows, g),
                            shadows: _shadowSections(true, introT, rows, g),
                            totalLabel: totalLabel,
                            side: side,
                            g: g,
                          ),
                        );
                      },
                    ),
                  ),
                  Expanded(
                    flex: 6,
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        _selectedCategoryStrip(
                          context,
                          l10n,
                          rows: rows,
                          index: si,
                        ),
                        const SizedBox(height: 6),
                        Divider(
                          height: 1,
                          thickness: 1,
                          color: WellPaidColors.navy.withValues(alpha: 0.1),
                        ),
                        const SizedBox(height: 4),
                        Padding(
                          padding: const EdgeInsets.fromLTRB(2, 0, 2, 4),
                          child: Text(
                            l10n.chartDonutTapHint,
                            textAlign: TextAlign.center,
                            style: Theme.of(context).textTheme.labelSmall
                                ?.copyWith(
                                  fontSize: 9.5,
                                  height: 1.2,
                                  color: WellPaidColors.navy.withValues(
                                    alpha: 0.48,
                                  ),
                                ),
                          ),
                        ),
                        Expanded(
                          child: GridView.builder(
                            padding: EdgeInsets.zero,
                            physics: const AlwaysScrollableScrollPhysics(),
                            gridDelegate:
                                const SliverGridDelegateWithFixedCrossAxisCount(
                              crossAxisCount: 2,
                              mainAxisSpacing: 6,
                              crossAxisSpacing: 8,
                              mainAxisExtent: 74,
                            ),
                            itemCount: otherIndices.length,
                            itemBuilder: (context, pos) {
                              final i = otherIndices[pos];
                              return _categoryGridTile(
                                context,
                                index: i,
                                row: rows[i],
                              );
                            },
                          ),
                        ),
                        if (widget.onViewCategoryExpenses != null)
                          TextButton.icon(
                            onPressed: () => widget.onViewCategoryExpenses!(
                              selectedCategory,
                            ),
                            icon: Icon(
                              PhosphorIconsRegular.list,
                              size: 18,
                              color: WellPaidColors.navy.withValues(alpha: 0.85),
                            ),
                            label: Text(
                              l10n.chartViewCategoryExpenses,
                              style: const TextStyle(fontSize: 13),
                            ),
                            style: TextButton.styleFrom(
                              foregroundColor: WellPaidColors.navy,
                              padding: const EdgeInsets.symmetric(vertical: 4),
                              tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                            ),
                          ),
                      ],
                    ),
                  ),
                ],
              ),
            );
          },
        );
      },
    );
  }
}

Widget _swatch(
  IconData icon,
  Color base,
  bool selected, {
  bool compact = false,
  double? size,
}) {
  final s = size ?? (compact ? 20.0 : 24.0);
  return AnimatedContainer(
    duration: const Duration(milliseconds: 220),
    curve: Curves.easeOutCubic,
    width: s,
    height: s,
    decoration: BoxDecoration(
      borderRadius: BorderRadius.circular(compact ? 7 : 8),
      gradient: LinearGradient(
        begin: Alignment.topLeft,
        end: Alignment.bottomRight,
        colors: [
          Color.lerp(base, WellPaidColors.cream, 0.2)!,
          Color.lerp(base, const Color(0xFF1A1F2E), 0.2)!,
        ],
      ),
      border: Border.all(
        color: selected ? base : WellPaidColors.cream.withValues(alpha: 0.9),
        width: selected ? 2 : 1,
      ),
      boxShadow: selected
          ? [
              BoxShadow(
                color: base.withValues(alpha: 0.3),
                blurRadius: 6,
                offset: const Offset(0, 2),
              ),
            ]
          : null,
    ),
    child: Icon(
      icon,
      size: (s * 0.52).clamp(9.0, 14.0),
      color: WellPaidColors.cream,
    ),
  );
}
