import 'dart:math' as math;

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

/// Dashboard: pendentes (se houver) + **duas vistas** (categorias / fluxo) em [PageView]
/// para um gráfico de cada vez e mais altura útil.
class DashboardScrollContent extends ConsumerStatefulWidget {
  const DashboardScrollContent({super.key, required this.data});

  final DashboardOverview data;

  @override
  ConsumerState<DashboardScrollContent> createState() =>
      _DashboardScrollContentState();
}

class _DashboardScrollContentState extends ConsumerState<DashboardScrollContent> {
  late final PageController _pageController;
  int _chartTab = 0;

  /// Persiste a fatia em destaque ao mudar de tab (Categorias ↔ Fluxo).
  int _donutSelectedSliceIndex = 0;

  @override
  void initState() {
    super.initState();
    _pageController = PageController();
  }

  @override
  void dispose() {
    _pageController.dispose();
    super.dispose();
  }

  @override
  void didUpdateWidget(covariant DashboardScrollContent oldWidget) {
    super.didUpdateWidget(oldWidget);
    final o = oldWidget.data;
    final n = widget.data;
    if (o.period.year != n.period.year ||
        o.period.month != n.period.month ||
        o.monthExpenseTotalCents != n.monthExpenseTotalCents) {
      _donutSelectedSliceIndex = 0;
      return;
    }
    final l10n = context.l10n;
    final rows = aggregateSpendingRowsForDonut(
      n.spendingByCategory,
      n.monthExpenseTotalCents,
      l10n.chartCategoryOther,
    );
    if (rows.isNotEmpty && _donutSelectedSliceIndex >= rows.length) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (!mounted) return;
        setState(() => _donutSelectedSliceIndex = rows.length - 1);
      });
    }
  }

  double _chartPageBottomInset(BuildContext context) {
    return MediaQuery.paddingOf(context).bottom + 72;
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final data = widget.data;
    final bottomInset = _chartPageBottomInset(context);
    final donutRows = aggregateSpendingRowsForDonut(
      data.spendingByCategory,
      data.monthExpenseTotalCents,
      l10n.chartCategoryOther,
    );
    final hasDonutData =
        donutRows.isNotEmpty && data.monthExpenseTotalCents > 0;
    final donutIdx = !hasDonutData
        ? 0
        : _donutSelectedSliceIndex.clamp(0, donutRows.length - 1);

    return CustomScrollView(
      physics: const AlwaysScrollableScrollPhysics(),
      slivers: [
        if (data.pendingTotalCents > 0) ...[
          SliverPadding(
            padding: const EdgeInsets.fromLTRB(10, 2, 10, 0),
            sliver: SliverToBoxAdapter(
              child: _PendingThisMonthCard(data: data),
            ),
          ),
          const SliverToBoxAdapter(child: SizedBox(height: 6)),
        ],
        SliverPadding(
          padding: const EdgeInsets.fromLTRB(10, 0, 10, 0),
          sliver: SliverToBoxAdapter(
            child: Semantics(
              container: true,
              label: '${l10n.dashHomeChartTabCategory}, ${l10n.dashHomeChartTabCashflow}',
              child: SegmentedButton<int>(
                segments: [
                  ButtonSegment<int>(
                    value: 0,
                    icon: const Icon(PhosphorIconsRegular.chartPie, size: 18),
                    label: Text(l10n.dashHomeChartTabCategory),
                  ),
                  ButtonSegment<int>(
                    value: 1,
                    icon: const Icon(PhosphorIconsRegular.chartLineUp, size: 18),
                    label: Text(l10n.dashHomeChartTabCashflow),
                  ),
                ],
                selected: {_chartTab},
                onSelectionChanged: (Set<int> next) {
                  final i = next.first;
                  setState(() => _chartTab = i);
                  _pageController.animateToPage(
                    i,
                    duration: const Duration(milliseconds: 280),
                    curve: Curves.easeOutCubic,
                  );
                },
              ),
            ),
          ),
        ),
        const SliverToBoxAdapter(child: SizedBox(height: 8)),
        // [SliverFillRemaining] + [PageView] pode ficar com altura 0 no viewport
        // (área “branca”). Altura explícita via [SliverLayoutBuilder] + mínimo seguro.
        SliverLayoutBuilder(
          builder: (context, constraints) {
            final fromViewport = constraints.viewportMainAxisExtent * 0.74;
            final h = math.max(
              400.0,
              math.max(constraints.remainingPaintExtent, fromViewport),
            );
            return SliverToBoxAdapter(
              child: SizedBox(
                height: h,
                child: PageView(
                  controller: _pageController,
                  onPageChanged: (i) => setState(() => _chartTab = i),
                  children: [
                    Padding(
                      padding: EdgeInsets.fromLTRB(10, 0, 10, bottomInset),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        children: [
                          Expanded(
                            child: _SectionChartCard(
                              title: l10n.dashByCategory,
                              icon: PhosphorIconsRegular.chartPie,
                              accent: WellPaidColors.goldPressed,
                              showHeader: false,
                              expandBody: true,
                              child: CategoryDonutChart(
                                categories: data.spendingByCategory,
                                monthExpenseTotalCents:
                                    data.monthExpenseTotalCents,
                                selectedSliceIndex: donutIdx,
                                onSelectedSliceIndexChanged: (i) => setState(
                                  () => _donutSelectedSliceIndex = i,
                                ),
                                period: data.period,
                                onViewCategoryExpenses: (c) =>
                                    _openDashboardCategoryExpenses(
                                      context,
                                      ref,
                                      data,
                                      c,
                                    ),
                                onRegisterExpense: () =>
                                    context.push('/expenses/new'),
                              ),
                            ),
                          ),
                          Padding(
                            padding: const EdgeInsets.fromLTRB(4, 10, 4, 4),
                            child: Text(
                              l10n.dashHomeCategoriesFootnote,
                              maxLines: 2,
                              overflow: TextOverflow.ellipsis,
                              textAlign: TextAlign.center,
                              style: Theme.of(context).textTheme.labelSmall
                                  ?.copyWith(
                                    color: WellPaidColors.navy.withValues(
                                      alpha: 0.48,
                                    ),
                                    height: 1.25,
                                  ),
                            ),
                          ),
                        ],
                      ),
                    ),
                    Padding(
                      padding: EdgeInsets.fromLTRB(10, 0, 10, bottomInset),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        children: [
                          Expanded(
                            child: SingleChildScrollView(
                              primary: false,
                              physics: const AlwaysScrollableScrollPhysics(),
                              child: DashboardCashflowChartCard(
                                embeddedInHomeTabs: true,
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            );
          },
        ),
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
    this.showHeader = true,
    this.expandBody = false,
  });

  final String title;
  final IconData icon;
  final Color accent;
  final Widget child;
  final bool showHeader;
  final bool expandBody;

  @override
  Widget build(BuildContext context) {
    final body = expandBody ? Expanded(child: child) : child;

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
          mainAxisSize: expandBody ? MainAxisSize.max : MainAxisSize.min,
          children: [
            if (showHeader) ...[
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
            ],
            body,
          ],
        ),
      ),
    );
  }
}
