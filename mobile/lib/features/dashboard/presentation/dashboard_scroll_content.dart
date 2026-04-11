import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:phosphor_flutter/phosphor_flutter.dart';

import '../../../core/format/brl_cents.dart';
import '../../../core/l10n/context_l10n.dart';
import '../../../core/theme/well_paid_colors.dart';
import '../../expenses/application/expenses_providers.dart';
import '../domain/dashboard_overview.dart';
import 'category_donut_chart.dart';
import 'dashboard_cashflow_chart_card.dart';

/// Dashboard: pendentes (se houver) + gráficos. Reserva de emergência: atalho no painel do shell.
class DashboardScrollContent extends ConsumerWidget {
  const DashboardScrollContent({super.key, required this.data});

  final DashboardOverview data;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    return ListView(
      physics: const AlwaysScrollableScrollPhysics(),
      padding: const EdgeInsets.fromLTRB(10, 4, 10, 100),
      children: [
        if (data.pendingTotalCents > 0) ...[
          _PendingThisMonthCard(data: data),
          const SizedBox(height: 6),
        ],
        _SectionChartCard(
          title: l10n.dashByCategory,
          icon: PhosphorIconsRegular.chartPie,
          accent: WellPaidColors.goldPressed,
          child: CategoryDonutChart(
            categories: data.spendingByCategory,
            monthExpenseTotalCents: data.monthExpenseTotalCents,
            period: data.period,
            onViewCategoryExpenses: (c) =>
                _openDashboardCategoryExpenses(context, ref, data, c),
            onRegisterExpense: () => context.push('/expenses/new'),
          ),
        ),
        const SizedBox(height: 4),
        const DashboardCashflowChartCard(),
      ],
    );
  }
}

Future<void> _openDashboardCategoryExpenses(
  BuildContext context,
  WidgetRef ref,
  DashboardOverview data,
  CategorySpend category,
) async {
  try {
    final cats = await ref.read(categoriesProvider.future);
    String? categoryId;
    for (final o in cats) {
      if (o.key == category.categoryKey) {
        categoryId = o.id;
        break;
      }
    }
    if (!context.mounted) return;
    ref.read(expenseListFiltersProvider.notifier).state = ExpenseListFilters(
      year: data.period.year,
      month: data.period.month,
      categoryId: categoryId,
    );
    context.push('/expenses');
  } catch (_) {
    if (!context.mounted) return;
    ref.read(expenseListFiltersProvider.notifier).state = ExpenseListFilters(
      year: data.period.year,
      month: data.period.month,
    );
    context.push('/expenses');
  }
}

class _PendingThisMonthCard extends StatelessWidget {
  const _PendingThisMonthCard({required this.data});

  final DashboardOverview data;

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final amount = formatBrlFromCents(data.pendingTotalCents);

    return Material(
      color: Colors.white,
      elevation: 2,
      shadowColor: WellPaidColors.navy.withValues(alpha: 0.1),
      borderRadius: BorderRadius.circular(20),
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: () => context.push('/to-pay'),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 12),
          child: Row(
            children: [
              Container(
                width: 44,
                height: 44,
                decoration: BoxDecoration(
                  color: WellPaidColors.navy.withValues(alpha: 0.08),
                  borderRadius: BorderRadius.circular(14),
                ),
                child: Icon(
                  PhosphorIconsRegular.notepad,
                  color: WellPaidColors.navy.withValues(alpha: 0.9),
                ),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      l10n.dashPendingThisMonthTitle,
                      style: Theme.of(context).textTheme.titleSmall?.copyWith(
                            fontWeight: FontWeight.w800,
                            color: WellPaidColors.navy,
                          ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      amount,
                      style: Theme.of(context).textTheme.titleMedium?.copyWith(
                            fontWeight: FontWeight.w800,
                            color: WellPaidColors.goldPressed,
                            letterSpacing: -0.3,
                          ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      l10n.dashPendingThisMonthSubtitle,
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(
                            color: WellPaidColors.navy.withValues(alpha: 0.55),
                          ),
                    ),
                  ],
                ),
              ),
              Icon(
                PhosphorIconsRegular.caretRight,
                color: WellPaidColors.navy.withValues(alpha: 0.45),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _SectionChartCard extends StatelessWidget {
  const _SectionChartCard({
    required this.title,
    required this.icon,
    required this.accent,
    required this.child,
  });

  final String title;
  final IconData icon;
  final Color accent;
  final Widget child;

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
        padding: const EdgeInsets.fromLTRB(10, 10, 10, 12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Row(
              children: [
                Container(
                  width: 32,
                  height: 32,
                  decoration: BoxDecoration(
                    color: accent.withValues(alpha: 0.12),
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Icon(icon, size: 19, color: accent),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    title,
                    style: Theme.of(context).textTheme.titleMedium?.copyWith(
                          fontWeight: FontWeight.w800,
                          color: WellPaidColors.navy,
                        ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 6),
            child,
          ],
        ),
      ),
    );
  }
}
