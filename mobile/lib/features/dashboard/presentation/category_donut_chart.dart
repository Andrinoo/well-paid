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

Widget _donutSliceBadge(IconData icon, Color base, bool selected) {
  return Container(
    width: 18,
    height: 18,
    alignment: Alignment.center,
    decoration: BoxDecoration(
      shape: BoxShape.circle,
      color: WellPaidColors.cream.withValues(alpha: 0.94),
      border: Border.all(color: base, width: selected ? 1.55 : 1.0),
      boxShadow: [
        BoxShadow(
          color: Colors.black.withValues(alpha: 0.08),
          blurRadius: 2,
          offset: const Offset(0, 1),
        ),
      ],
    ),
    child: Icon(icon, size: 10, color: base),
  );
}

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
    this.onViewCategoryExpenses,
    this.onRegisterExpense,
  });

  final List<CategorySpend> categories;
  final int monthExpenseTotalCents;
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
      final touched = _touchedIndex == i;
      final scaled = c.amountCents * introT;
      final glowA = (0.42 + 0.58 * introT).clamp(0.0, 1.0) / _depthStrength;
      final icon =
          _categoryIcons[c.categoryKey] ?? PhosphorIconsRegular.squaresFour;
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
          badgeWidget: GestureDetector(
            behavior: HitTestBehavior.opaque,
            onTap: () {
              if (_touchedIndex == i) {
                _setTouched(null);
              } else {
                _setTouched(i);
              }
            },
            child: _donutSliceBadge(icon, base, touched),
          ),
          badgePositionPercentageOffset: 0.91,
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
                  if (sec == null) {
                    _setTouched(null);
                    return;
                  }
                  final idx = sec.touchedSectionIndex;
                  // Toque no badge costuma vir com índice -1; o badge usa GestureDetector.
                  if (idx < 0) return;
                  _setTouched(idx);
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

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _intro,
      builder: (context, _) {
        return LayoutBuilder(
          builder: (context, constraints) {
            final l10n = context.l10n;
            final rows = _aggregateSpendingForDonut(
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
            final rawW = constraints.maxWidth;
            final side = rawW.isFinite
                ? rawW.clamp(158.0, _refChartBox)
                : _refChartBox;
            final g = side / _refChartBox;
            final sections = _sections(hasData, introT, rows, g);
            final shadows = _shadowSections(hasData, introT, rows, g);

            CategorySpend? selectedCategory;
            if (hasData &&
                _touchedIndex != null &&
                _touchedIndex! >= 0 &&
                _touchedIndex! < rows.length) {
              selectedCategory = rows[_touchedIndex!];
            }

            final Widget footerHint;
            if (!hasData) {
              footerHint = Padding(
                padding: const EdgeInsets.only(top: 8),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Text(
                      l10n.chartCategoriesHint,
                      textAlign: TextAlign.center,
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(
                        color: WellPaidColors.navy.withValues(alpha: 0.62),
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
                            color: WellPaidColors.navy.withValues(alpha: 0.9),
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
              );
            } else {
              footerHint = const SizedBox.shrink();
            }

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
                    side: side,
                    g: g,
                  ),
                ),
                if (hasData)
                  SizedBox(
                    height: 40,
                    child: AnimatedSwitcher(
                      duration: const Duration(milliseconds: 200),
                      switchInCurve: Curves.easeOutCubic,
                      switchOutCurve: Curves.easeInCubic,
                      child: selectedCategory == null
                          ? Center(
                              key: const ValueKey<String>('cat-hint'),
                              child: Text(
                                l10n.chartDonutTapHint,
                                textAlign: TextAlign.center,
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                                style: Theme.of(context).textTheme.labelSmall
                                    ?.copyWith(
                                      fontSize: 10,
                                      color: WellPaidColors.navy.withValues(
                                        alpha: 0.48,
                                      ),
                                    ),
                              ),
                            )
                          : Center(
                              key: ValueKey<String>(
                                selectedCategory.categoryKey,
                              ),
                              child: _SelectedCategoryBadge(
                                category: selectedCategory,
                                monthTotal: widget.monthExpenseTotalCents,
                                color:
                                    _palette[_touchedIndex! % _palette.length],
                                onOpenExpenses:
                                    widget.onViewCategoryExpenses != null
                                    ? () => widget.onViewCategoryExpenses!(
                                        selectedCategory!,
                                      )
                                    : null,
                              ),
                            ),
                    ),
                  ),
              ],
            );

            return Semantics(
              label: hasData
                  ? l10n.chartSemanticsWithData(totalLabel)
                  : l10n.chartSemanticsNoData,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [chartCol, footerHint],
              ),
            );
          },
        );
      },
    );
  }
}

class _SelectedCategoryBadge extends StatelessWidget {
  const _SelectedCategoryBadge({
    required this.category,
    required this.monthTotal,
    required this.color,
    this.onOpenExpenses,
  });

  final CategorySpend category;
  final int monthTotal;
  final Color color;
  final VoidCallback? onOpenExpenses;

  @override
  Widget build(BuildContext context) {
    final icon =
        _categoryIcons[category.categoryKey] ??
        PhosphorIconsRegular.squaresFour;
    final pct = monthTotal > 0
        ? ((category.amountCents * 100) ~/ monthTotal)
        : 0;
    final row = Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        _swatch(icon, color, true, compact: true, size: 17),
        const SizedBox(width: 5),
        Flexible(
          child: Text(
            category.name,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: Theme.of(context).textTheme.labelLarge?.copyWith(
              color: WellPaidColors.navy,
              fontWeight: FontWeight.w800,
              fontSize: 12,
            ),
          ),
        ),
        const SizedBox(width: 5),
        Text(
          '$pct%',
          style: Theme.of(context).textTheme.labelSmall?.copyWith(
            color: WellPaidColors.navy.withValues(alpha: 0.72),
            fontWeight: FontWeight.w800,
            fontSize: 10,
          ),
        ),
        const SizedBox(width: 5),
        Text(
          formatBrlFromCents(category.amountCents),
          style: Theme.of(context).textTheme.labelSmall?.copyWith(
            color: WellPaidColors.navy,
            fontWeight: FontWeight.w800,
            fontSize: 11,
          ),
        ),
      ],
    );

    final decoration = BoxDecoration(
      borderRadius: BorderRadius.circular(12),
      gradient: LinearGradient(
        colors: [color.withValues(alpha: 0.2), color.withValues(alpha: 0.08)],
      ),
      border: Border.all(color: color.withValues(alpha: 0.4)),
    );
    final padded = Padding(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 5),
      child: row,
    );

    return ConstrainedBox(
      constraints: const BoxConstraints(maxWidth: 300),
      child: onOpenExpenses == null
          ? DecoratedBox(decoration: decoration, child: padded)
          : Material(
              color: Colors.transparent,
              child: InkWell(
                onTap: onOpenExpenses,
                borderRadius: BorderRadius.circular(12),
                child: Ink(decoration: decoration, child: padded),
              ),
            ),
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
