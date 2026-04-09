import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
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
  'alimentacao': Icons.restaurant_menu_outlined,
  'transporte': Icons.directions_car_outlined,
  'moradia': Icons.home_outlined,
  'saude': Icons.health_and_safety_outlined,
  'educacao': Icons.school_outlined,
  'lazer': Icons.sports_esports_outlined,
  'pessoais': Icons.person_outline,
  'emprestimos': Icons.account_balance_outlined,
  'outros': Icons.category_outlined,
};

const double _chartBox = 220;
const double _sliceRadius = 54;
const double _centerHole = 58;

enum _DonutDepthPreset { soft, medium }

const _donutDepthPreset = _DonutDepthPreset.soft;

/// Ordena por valor e mantém no máximo [_kMaxDonutSlices] fatias; o resto vira uma linha "Outros".
List<CategorySpend> _aggregateSpendingForDonut(
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
    this.period,
  });

  final List<CategorySpend> categories;
  final int monthExpenseTotalCents;
  final PeriodMonth? period;

  @override
  State<CategoryDonutChart> createState() => _CategoryDonutChartState();
}

class _CategoryDonutChartState extends State<CategoryDonutChart>
    with SingleTickerProviderStateMixin {
  late final AnimationController _intro;
  int? _touchedIndex;

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
      _touchedIndex = null;
    }
  }

  @override
  void dispose() {
    _intro.dispose();
    super.dispose();
  }

  void _setTouched(int? next) {
    if (next != _touchedIndex) {
      if (next != null) {
        HapticFeedback.selectionClick();
      }
      setState(() => _touchedIndex = next);
    }
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
  ) {
    if (!hasData || introT < 0.002) {
      return [
        PieChartSectionData(
          color: WellPaidColors.navy.withValues(alpha: 0.12 + 0.08 * introT),
          value: 1,
          title: '',
          radius: _sliceRadius,
          showTitle: false,
          borderSide: BorderSide.none,
        ),
      ];
    }
    final out = <PieChartSectionData>[];
    final total = widget.monthExpenseTotalCents;
    for (var i = 0; i < rows.length; i++) {
      final c = rows[i];
      final base = _palette[i % _palette.length];
      final touched = _touchedIndex == i;
      final scaled = c.amountCents * introT;
      final pct = total > 0 ? ((c.amountCents * 100) ~/ total) : 0;
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
          title: touched ? '$pct%' : '',
          showTitle: touched,
          radius: touched ? _sliceRadius + 10 : _sliceRadius,
          titleStyle: TextStyle(
            fontSize: 11,
            fontWeight: FontWeight.w800,
            color: WellPaidColors.cream,
            shadows: const [
              Shadow(
                color: Colors.black54,
                blurRadius: 3,
                offset: Offset(0, 1),
              ),
            ],
          ),
          titlePositionPercentageOffset: 0.62,
          borderSide: BorderSide(
            color: WellPaidColors.cream.withValues(alpha: touched ? 0.95 : 0.88),
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
  ) {
    if (!hasData || introT < 0.002) {
      return [
        PieChartSectionData(
          color: Colors.black.withValues(alpha: 0.06),
          value: 1,
          title: '',
          radius: _sliceRadius,
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
          radius: _sliceRadius,
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
  }) {
    final p = widget.period;
    final periodLine = p != null && p.month >= 1 && p.month <= 12
        ? formatMonthYearUi(context, DateTime(p.year, p.month))
        : null;

    return ConstrainedBox(
      constraints: const BoxConstraints(maxWidth: 118),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (periodLine != null)
            Padding(
              padding: const EdgeInsets.only(bottom: 4),
              child: Text(
                periodLine,
                style: Theme.of(context).textTheme.labelSmall?.copyWith(
                      color: WellPaidColors.navy.withValues(alpha: 0.58),
                      fontWeight: FontWeight.w500,
                      letterSpacing: 0.2,
                    ),
                textAlign: TextAlign.center,
              ),
            ),
          Text(
            l10n.chartTotalExpenses,
            style: Theme.of(context).textTheme.labelSmall?.copyWith(
                  color: WellPaidColors.navy.withValues(alpha: 0.62),
                  fontWeight: FontWeight.w500,
                ),
          ),
          const SizedBox(height: 2),
          FittedBox(
            fit: BoxFit.scaleDown,
            child: Text(
              totalLabel,
              style: Theme.of(context).textTheme.titleLarge?.copyWith(
                    fontWeight: FontWeight.w800,
                    color: WellPaidColors.navy,
                    height: 1.1,
                  ),
              textAlign: TextAlign.center,
              maxLines: 1,
            ),
          ),
          if (!hasData) ...[
            const SizedBox(height: 8),
            Text(
              periodLine != null
                  ? l10n.chartNoExpensesRegistered
                  : l10n.chartNoExpensesThisMonth,
              textAlign: TextAlign.center,
              style: Theme.of(context).textTheme.labelSmall?.copyWith(
                    color: WellPaidColors.navy.withValues(alpha: 0.52),
                    height: 1.3,
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
  }) {
    return SizedBox(
      width: _chartBox,
      height: _chartBox,
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
                width: 1.2 * _depthStrength,
              ),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withValues(alpha: 0.1),
                  blurRadius: 16 * _depthStrength,
                  offset: Offset(2 * _depthStrength, 8 * _depthStrength),
                ),
              ],
            ),
            child: const SizedBox(width: _chartBox, height: _chartBox),
          ),
          // Camada de realce superior para reforçar o efeito 3D.
          IgnorePointer(
            child: Container(
              width: _chartBox - 10,
              height: _chartBox - 10,
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
            offset: const Offset(2, 4),
            child: PieChart(
              PieChartData(
                sectionsSpace: hasData && introT > 0.01 ? 2.2 : 0,
                centerSpaceRadius: _centerHole,
                sections: shadows,
                pieTouchData: PieTouchData(enabled: false),
                startDegreeOffset: -90 + (1 - introT) * 6,
              ),
            ),
          ),
          PieChart(
            PieChartData(
              sectionsSpace: hasData && introT > 0.01 ? 2.2 : 0,
              centerSpaceRadius: _centerHole,
              sections: sections,
              pieTouchData: PieTouchData(
                enabled: hasData && introT > 0.05,
                touchCallback: (event, pieTouchResponse) {
                  final sec = pieTouchResponse?.touchedSection;
                  if (sec == null) {
                    _setTouched(null);
                    return;
                  }
                  _setTouched(sec.touchedSectionIndex);
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
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _intro,
      builder: (context, _) {
        final l10n = context.l10n;
        final rows = _aggregateSpendingForDonut(
          widget.categories,
          widget.monthExpenseTotalCents,
          l10n.chartCategoryOther,
        );
        final hasData =
            rows.isNotEmpty && widget.monthExpenseTotalCents > 0;
        final totalLabel =
            formatBrlFromCents(hasData ? widget.monthExpenseTotalCents : 0);
        final introT = Curves.easeOutCubic.transform(_intro.value);
        final sections = _sections(hasData, introT, rows);
        final shadows = _shadowSections(hasData, introT, rows);

        CategorySpend? selectedCategory;
        if (hasData && _touchedIndex != null && _touchedIndex! < rows.length) {
          selectedCategory = rows[_touchedIndex!];
        }

        return LayoutBuilder(
          builder: (context, constraints) {
            final narrow = constraints.maxWidth < 340;
            final legend = hasData
                ? narrow
                    ? _LegendWrap(
                        rows: rows,
                        monthTotal: widget.monthExpenseTotalCents,
                        selectedIndex: _touchedIndex,
                        introT: introT,
                        onSelect: (i) {
                          if (_touchedIndex == i) {
                            _setTouched(null);
                          } else {
                            _setTouched(i);
                          }
                        },
                      )
                    : _LegendList(
                        rows: rows,
                        monthTotal: widget.monthExpenseTotalCents,
                        selectedIndex: _touchedIndex,
                        introT: introT,
                        onSelect: (i) {
                          if (_touchedIndex == i) {
                            _setTouched(null);
                          } else {
                            _setTouched(i);
                          }
                        },
                      )
                : Padding(
                    padding: EdgeInsets.only(
                      left: narrow ? 0 : 4,
                      top: narrow ? 8 : 0,
                    ),
                    child: Text(
                      l10n.chartCategoriesHint,
                      textAlign:
                          narrow ? TextAlign.center : TextAlign.start,
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(
                            color:
                                WellPaidColors.navy.withValues(alpha: 0.62),
                            height: 1.35,
                          ),
                    ),
                  );

            final chartCol = Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Center(
                  child: _chartStack(
                    context,
                    l10n: l10n,
                    hasData: hasData,
                    introT: introT,
                    sections: sections,
                    shadows: shadows,
                    totalLabel: totalLabel,
                  ),
                ),
                AnimatedSwitcher(
                  duration: const Duration(milliseconds: 200),
                  switchInCurve: Curves.easeOutCubic,
                  switchOutCurve: Curves.easeInCubic,
                  child: selectedCategory == null
                      ? const SizedBox(height: 10)
                      : Padding(
                          key: ValueKey<String>(selectedCategory.categoryKey),
                          padding: const EdgeInsets.only(top: 10),
                          child: _SelectedCategoryBadge(
                            category: selectedCategory,
                            monthTotal: widget.monthExpenseTotalCents,
                            color: _palette[_touchedIndex! % _palette.length],
                          ),
                        ),
                ),
              ],
            );

            return Semantics(
              label: hasData
                  ? l10n.chartSemanticsWithData(totalLabel)
                  : l10n.chartSemanticsNoData,
              child: narrow
                  ? Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        chartCol,
                        const SizedBox(height: 12),
                        legend,
                      ],
                    )
                  : SizedBox(
                      height: selectedCategory != null ? 298 : 266,
                      child: Row(
                        crossAxisAlignment: CrossAxisAlignment.center,
                        children: [
                          Expanded(flex: 3, child: chartCol),
                          Expanded(flex: 2, child: legend),
                        ],
                      ),
                    ),
            );
          },
        );
      },
    );
  }
}

class _LegendList extends StatelessWidget {
  const _LegendList({
    required this.rows,
    required this.monthTotal,
    required this.selectedIndex,
    required this.introT,
    required this.onSelect,
  });

  final List<CategorySpend> rows;
  final int monthTotal;
  final int? selectedIndex;
  final double introT;
  final ValueChanged<int> onSelect;

  @override
  Widget build(BuildContext context) {
    return ListView.builder(
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      itemCount: rows.length,
      itemBuilder: (context, i) =>
          _legendRow(context, rows[i], i, monthTotal, selectedIndex, introT, onSelect),
    );
  }
}

class _LegendWrap extends StatelessWidget {
  const _LegendWrap({
    required this.rows,
    required this.monthTotal,
    required this.selectedIndex,
    required this.introT,
    required this.onSelect,
  });

  final List<CategorySpend> rows;
  final int monthTotal;
  final int? selectedIndex;
  final double introT;
  final ValueChanged<int> onSelect;

  @override
  Widget build(BuildContext context) {
    return Wrap(
      alignment: WrapAlignment.center,
      spacing: 8,
      runSpacing: 8,
      children: [
        for (var i = 0; i < rows.length; i++)
          _legendChip(context, rows[i], i, monthTotal, selectedIndex, introT, onSelect),
      ],
    );
  }
}

class _SelectedCategoryBadge extends StatelessWidget {
  const _SelectedCategoryBadge({
    required this.category,
    required this.monthTotal,
    required this.color,
  });

  final CategorySpend category;
  final int monthTotal;
  final Color color;

  @override
  Widget build(BuildContext context) {
    final icon = _categoryIcons[category.categoryKey] ?? Icons.category_outlined;
    final pct = monthTotal > 0 ? ((category.amountCents * 100) ~/ monthTotal) : 0;
    return DecoratedBox(
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(16),
        gradient: LinearGradient(
          colors: [
            color.withValues(alpha: 0.22),
            color.withValues(alpha: 0.1),
          ],
        ),
        border: Border.all(color: color.withValues(alpha: 0.45)),
      ),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 7),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            _swatch(icon, color, true, compact: true),
            const SizedBox(width: 6),
            ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 120),
              child: Text(
                category.name,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: Theme.of(context).textTheme.labelMedium?.copyWith(
                      color: WellPaidColors.navy,
                      fontWeight: FontWeight.w700,
                    ),
              ),
            ),
            const SizedBox(width: 6),
            Text(
              '$pct%',
              style: Theme.of(context).textTheme.labelMedium?.copyWith(
                    color: WellPaidColors.navy.withValues(alpha: 0.78),
                    fontWeight: FontWeight.w800,
                  ),
            ),
            const SizedBox(width: 6),
            Text(
              formatBrlFromCents(category.amountCents),
              style: Theme.of(context).textTheme.labelMedium?.copyWith(
                    color: WellPaidColors.navy,
                    fontWeight: FontWeight.w800,
                  ),
            ),
          ],
        ),
      ),
    );
  }
}

Widget _legendRow(
  BuildContext context,
  CategorySpend c,
  int i,
  int monthTotal,
  int? selectedIndex,
  double introT,
  ValueChanged<int> onSelect,
) {
  final pct = monthTotal > 0 ? ((c.amountCents * 100) ~/ monthTotal) : 0;
  final icon = _categoryIcons[c.categoryKey] ?? Icons.category_outlined;
  final base = _palette[i % _palette.length];
  final selected = selectedIndex == i;
  return Opacity(
    opacity: 0.4 + 0.6 * introT,
    child: Padding(
      padding: const EdgeInsets.symmetric(vertical: 3),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: () => onSelect(i),
          borderRadius: BorderRadius.circular(10),
          child: Padding(
            padding: const EdgeInsets.symmetric(vertical: 5, horizontal: 4),
            child: Row(
              children: [
                _swatch(icon, base, selected),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    c.name,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: Theme.of(context).textTheme.bodySmall?.copyWith(
                          color: const Color(0xFF0F2238),
                          fontWeight:
                              selected ? FontWeight.w700 : FontWeight.w500,
                        ),
                  ),
                ),
                Text(
                  '$pct%',
                  style: Theme.of(context).textTheme.bodySmall?.copyWith(
                        color: WellPaidColors.navy.withValues(alpha: 0.78),
                        fontWeight: FontWeight.w700,
                      ),
                ),
              ],
            ),
          ),
        ),
      ),
    ),
  );
}

Widget _legendChip(
  BuildContext context,
  CategorySpend c,
  int i,
  int monthTotal,
  int? selectedIndex,
  double introT,
  ValueChanged<int> onSelect,
) {
  final pct = monthTotal > 0 ? ((c.amountCents * 100) ~/ monthTotal) : 0;
  final icon = _categoryIcons[c.categoryKey] ?? Icons.category_outlined;
  final base = _palette[i % _palette.length];
  final selected = selectedIndex == i;
  return Opacity(
    opacity: 0.4 + 0.6 * introT,
    child: Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: () => onSelect(i),
        borderRadius: BorderRadius.circular(20),
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 200),
          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
          decoration: BoxDecoration(
            color: selected
                ? base.withValues(alpha: 0.16)
                : WellPaidColors.creamMuted.withValues(alpha: 0.55),
            borderRadius: BorderRadius.circular(20),
            border: Border.all(
              color: selected
                  ? base
                  : WellPaidColors.navy.withValues(alpha: 0.1),
              width: selected ? 2 : 1,
            ),
          ),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              _swatch(icon, base, selected, compact: true),
              const SizedBox(width: 6),
              ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 100),
                child: Text(
                  c.name,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: Theme.of(context).textTheme.labelMedium?.copyWith(
                        color: WellPaidColors.navy,
                        fontWeight:
                            selected ? FontWeight.w700 : FontWeight.w500,
                      ),
                ),
              ),
              const SizedBox(width: 4),
              Text(
                '$pct%',
                style: Theme.of(context).textTheme.labelMedium?.copyWith(
                      color: WellPaidColors.navy.withValues(alpha: 0.75),
                      fontWeight: FontWeight.w700,
                    ),
              ),
            ],
          ),
        ),
      ),
    ),
  );
}

Widget _swatch(IconData icon, Color base, bool selected, {bool compact = false}) {
  final s = compact ? 20.0 : 24.0;
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
      size: compact ? 11 : 13,
      color: WellPaidColors.cream,
    ),
  );
}
