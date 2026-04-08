import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';

import '../../../core/format/brl_cents.dart';
import '../../../core/theme/well_paid_colors.dart';
import '../domain/dashboard_overview.dart';

const _palette = <Color>[
  WellPaidColors.navy,
  WellPaidColors.gold,
  Color(0xFF2E7D8C),
  Color(0xFF8B5A6B),
  Color(0xFF4A6FA5),
  Color(0xFF6B8E23),
  Color(0xFF7D5BA6),
  Color(0xFFC06040),
  Color(0xFF5C6BC0),
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

/// Tamanho fixo: o `PieChart` do fl_chart precisa de constraints explícitas;
/// dentro de `Stack`/`Row` sem isso o canvas pode ficar com área 0×0.
const double _chartBox = 220;

/// Rosca por categoria + total no centro (Telas §5.4).
class CategoryDonutChart extends StatelessWidget {
  const CategoryDonutChart({
    super.key,
    required this.categories,
    required this.monthExpenseTotalCents,
  });

  final List<CategorySpend> categories;
  final int monthExpenseTotalCents;

  @override
  Widget build(BuildContext context) {
    final hasData =
        categories.isNotEmpty && monthExpenseTotalCents > 0;

    final sections = <PieChartSectionData>[];
    if (hasData) {
      for (var i = 0; i < categories.length; i++) {
        final c = categories[i];
        sections.add(
          PieChartSectionData(
            color: _palette[i % _palette.length],
            value: c.amountCents.toDouble(),
            title: '',
            radius: 54,
            borderSide: BorderSide(
              color: WellPaidColors.cream.withValues(alpha: 0.95),
              width: 2,
            ),
          ),
        );
      }
    } else {
      // Uma fatia “cheia” para a rosca ser visível mesmo sem despesas.
      sections.add(
        PieChartSectionData(
          color: WellPaidColors.navy.withValues(alpha: 0.14),
          value: 1,
          title: '',
          radius: 54,
          borderSide: BorderSide.none,
        ),
      );
    }

    final totalLabel = formatBrlFromCents(
      hasData ? monthExpenseTotalCents : 0,
    );

    return Semantics(
      label: hasData
          ? 'Gráfico de despesas por categoria, total $totalLabel'
          : 'Gráfico de despesas por categoria, sem dados no mês',
      child: SizedBox(
        height: 260,
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            Expanded(
              flex: 3,
              child: Center(
                child: SizedBox(
                  width: _chartBox,
                  height: _chartBox,
                  child: Stack(
                    alignment: Alignment.center,
                    clipBehavior: Clip.none,
                    children: [
                      PieChart(
                        PieChartData(
                          sectionsSpace: hasData ? 2 : 0,
                          centerSpaceRadius: 58,
                          sections: sections,
                          pieTouchData: PieTouchData(enabled: false),
                          startDegreeOffset: -90,
                        ),
                      ),
                      Column(
                        mainAxisSize: MainAxisSize.min,
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Text(
                            'Total',
                            style: Theme.of(context)
                                .textTheme
                                .labelSmall
                                ?.copyWith(
                                  color: WellPaidColors.navy
                                      .withValues(alpha: 0.7),
                                ),
                          ),
                          Text(
                            totalLabel,
                            style: Theme.of(context)
                                .textTheme
                                .titleMedium
                                ?.copyWith(
                                  fontWeight: FontWeight.w800,
                                  color: WellPaidColors.navy,
                                ),
                            textAlign: TextAlign.center,
                          ),
                          if (!hasData) ...[
                            const SizedBox(height: 6),
                            Text(
                              'Sem despesas\nneste mês',
                              textAlign: TextAlign.center,
                              style: Theme.of(context)
                                  .textTheme
                                  .labelSmall
                                  ?.copyWith(
                                    color: WellPaidColors.navy
                                        .withValues(alpha: 0.55),
                                    height: 1.25,
                                  ),
                            ),
                          ],
                        ],
                      ),
                    ],
                  ),
                ),
              ),
            ),
            Expanded(
              flex: 2,
              child: hasData
                  ? _Legend(
                      categories: categories,
                      monthTotal: monthExpenseTotalCents,
                    )
                  : Padding(
                      padding: const EdgeInsets.only(left: 4),
                      child: Text(
                        'As categorias aparecem aqui quando existirem '
                        'despesas registadas neste mês (API ou futuro '
                        'formulário).',
                        style: Theme.of(context).textTheme.bodySmall?.copyWith(
                              color:
                                  WellPaidColors.navy.withValues(alpha: 0.65),
                              height: 1.35,
                            ),
                      ),
                    ),
            ),
          ],
        ),
      ),
    );
  }
}

class _Legend extends StatelessWidget {
  const _Legend({
    required this.categories,
    required this.monthTotal,
  });

  final List<CategorySpend> categories;
  final int monthTotal;

  @override
  Widget build(BuildContext context) {
    return ListView.builder(
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      itemCount: categories.length,
      itemBuilder: (context, i) {
        final c = categories[i];
        final pct =
            monthTotal > 0 ? ((c.amountCents * 100) ~/ monthTotal) : 0;
        final icon = _categoryIcons[c.categoryKey] ?? Icons.category_outlined;
        return Padding(
          padding: const EdgeInsets.symmetric(vertical: 4),
          child: Row(
            children: [
              Container(
                width: 22,
                height: 22,
                decoration: BoxDecoration(
                  color: _palette[i % _palette.length].withValues(alpha: 0.16),
                  borderRadius: BorderRadius.circular(6),
                ),
                child: Icon(
                  icon,
                  size: 14,
                  color: _palette[i % _palette.length],
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  c.name,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: Theme.of(context).textTheme.bodySmall?.copyWith(
                        color: WellPaidColors.navy,
                        fontWeight: FontWeight.w500,
                      ),
                ),
              ),
              Text(
                '$pct%',
                style: Theme.of(context).textTheme.bodySmall?.copyWith(
                      color: WellPaidColors.navy.withValues(alpha: 0.75),
                    ),
              ),
            ],
          ),
        );
      },
    );
  }
}
