import 'package:flutter/material.dart';
import 'package:phosphor_flutter/phosphor_flutter.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/format/brl_cents.dart';
import '../../../core/l10n/context_l10n.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/theme/well_paid_breakpoints.dart';
import '../../../core/theme/well_paid_colors.dart';
import '../../../core/theme/well_paid_radii.dart';
import '../../../core/theme/well_paid_shadows.dart';
import '../../../core/widgets/desktop/desktop.dart';
import '../../dashboard/application/dashboard_providers.dart';
import '../application/incomes_providers.dart';
import '../domain/income_item.dart';

class IncomeListPage extends ConsumerStatefulWidget {
  const IncomeListPage({super.key});

  @override
  ConsumerState<IncomeListPage> createState() => _IncomeListPageState();
}

class _IncomeListPageState extends ConsumerState<IncomeListPage> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      final p = ref.read(dashboardPeriodProvider);
      ref.read(incomeListFiltersProvider.notifier).state = IncomeListFilters(
        year: p.year,
        month: p.month,
      );
    });
  }

  void _shiftMonth(int delta) {
    final f = ref.read(incomeListFiltersProvider);
    var y = f.year;
    var m = f.month + delta;
    while (m > 12) {
      m -= 12;
      y++;
    }
    while (m < 1) {
      m += 12;
      y--;
    }
    ref.read(incomeListFiltersProvider.notifier).state = IncomeListFilters(
      year: y,
      month: m,
    );
  }

  String _dmY(DateTime d) =>
      '${d.day.toString().padLeft(2, '0')}/${d.month.toString().padLeft(2, '0')}/${d.year}';

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final f = ref.watch(incomeListFiltersProvider);
    final async = ref.watch(incomesListProvider);

    return Scaffold(
      appBar: AppBar(
        leading: Navigator.of(context).canPop()
            ? IconButton(
                icon: const Icon(PhosphorIconsRegular.arrowLeft),
                onPressed: () => context.pop(),
              )
            : null,
        title: Text(l10n.incomesTitle),
        actions: [
          IconButton(
            tooltip: l10n.incomesRefresh,
            onPressed: () {
              ref.invalidate(incomesListProvider);
              ref.invalidate(dashboardOverviewProvider);
            },
            icon: const Icon(PhosphorIconsRegular.arrowsClockwise),
          ),
        ],
      ),
      body: LayoutBuilder(
        builder: (context, constraints) {
          final wide = constraints.maxWidth >= WellPaidBreakpoints.medium;
          return Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              SectionCard(
                margin: EdgeInsets.fromLTRB(
                  wide ? 20 : 12,
                  wide ? 12 : 8,
                  wide ? 20 : 12,
                  0,
                ),
                padding: EdgeInsets.symmetric(
                  horizontal: wide ? 16 : 10,
                  vertical: wide ? 14 : 10,
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        IconButton(
                          tooltip: l10n.periodPrevMonth,
                          onPressed: () => _shiftMonth(-1),
                          icon: const Icon(PhosphorIconsRegular.caretLeft),
                          color: WellPaidColors.navy,
                        ),
                        Text(
                          '${f.month.toString().padLeft(2, '0')}/${f.year}',
                          style: Theme.of(context).textTheme.titleMedium
                              ?.copyWith(
                                fontWeight: FontWeight.w700,
                                color: WellPaidColors.navy,
                              ),
                        ),
                        IconButton(
                          tooltip: l10n.periodNextMonth,
                          onPressed: () => _shiftMonth(1),
                          icon: const Icon(PhosphorIconsRegular.caretRight),
                          color: WellPaidColors.navy,
                        ),
                      ],
                    ),
                    Padding(
                      padding: const EdgeInsets.only(top: 6),
                      child: Text(
                        l10n.incomesListHint,
                        textAlign: TextAlign.center,
                        style: Theme.of(context).textTheme.bodySmall?.copyWith(
                          color: WellPaidColors.navy.withValues(alpha: 0.62),
                          height: 1.35,
                        ),
                      ),
                    ),
                    const SizedBox(height: 12),
                    Row(
                      children: [
                        Expanded(
                          child: FilledButton.tonalIcon(
                            onPressed: () => context.push('/incomes/new'),
                            icon: const Icon(PhosphorIconsRegular.coins),
                            label: Text(l10n.incomesAddLong),
                            style: FilledButton.styleFrom(
                              foregroundColor: WellPaidColors.navy,
                            ),
                          ),
                        ),
                        IconButton(
                          tooltip: l10n.incomesRefreshList,
                          onPressed: () {
                            ref.invalidate(incomesListProvider);
                            ref.invalidate(dashboardOverviewProvider);
                          },
                          icon: const Icon(
                            PhosphorIconsRegular.arrowsClockwise,
                          ),
                          color: WellPaidColors.navy,
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              Expanded(
                child: async.when(
                  skipLoadingOnReload: true,
                  loading: () => ListView(
                    physics: const AlwaysScrollableScrollPhysics(),
                    padding: const EdgeInsets.fromLTRB(16, 16, 16, 24),
                    children: [
                      LinearProgressIndicator(
                        minHeight: 3,
                        color: WellPaidColors.gold,
                        backgroundColor: WellPaidColors.navy.withValues(
                          alpha: 0.08,
                        ),
                      ),
                      const SizedBox(height: 20),
                      ...List.generate(
                        5,
                        (i) => Padding(
                          padding: const EdgeInsets.only(bottom: 14),
                          child: Container(
                            height: 52,
                            decoration: BoxDecoration(
                              color: WellPaidColors.navy.withValues(
                                alpha: 0.06,
                              ),
                              borderRadius: BorderRadius.circular(12),
                            ),
                          ),
                        ),
                      ),
                    ],
                  ),
                  error: (e, _) => Center(
                    child: Padding(
                      padding: const EdgeInsets.all(24),
                      child: Text(
                        messageFromDio(e, l10n) ?? l10n.incomesLoadError,
                        textAlign: TextAlign.center,
                      ),
                    ),
                  ),
                  data: (items) {
                    if (items.isEmpty) {
                      return WellPaidEmptyState(
                        title: l10n.incomesEmpty,
                        icon: PhosphorIconsRegular.coins,
                      );
                    }
                    Future<void> onRefresh() async {
                      ref.invalidate(incomesListProvider);
                      await ref.read(incomesListProvider.future);
                    }

                    if (wide) {
                      final headerStyle = Theme.of(context).textTheme.labelSmall
                          ?.copyWith(
                            fontWeight: FontWeight.w700,
                            color: WellPaidColors.navy.withValues(alpha: 0.55),
                            letterSpacing: 0.2,
                          );
                      return RefreshIndicator(
                        color: WellPaidColors.navy,
                        onRefresh: onRefresh,
                        child: ListView(
                          physics: const AlwaysScrollableScrollPhysics(),
                          padding: const EdgeInsets.fromLTRB(20, 8, 20, 24),
                          children: [
                            AppListHeaderRow(
                              cells: [
                                AppListCell(
                                  flex: 2,
                                  child: Text(
                                    l10n.expenseTableColDate,
                                    style: headerStyle,
                                  ),
                                ),
                                AppListCell(
                                  flex: 5,
                                  child: Text(
                                    l10n.expenseTableColDescription,
                                    style: headerStyle,
                                  ),
                                ),
                                AppListCell(
                                  flex: 3,
                                  child: Text(
                                    l10n.expenseTableColCategory,
                                    style: headerStyle,
                                  ),
                                ),
                                AppListCell(
                                  flex: 3,
                                  alignment: Alignment.centerRight,
                                  child: Text(
                                    l10n.expenseTableColAmount,
                                    style: headerStyle,
                                  ),
                                ),
                              ],
                            ),
                            ...List.generate(items.length, (i) {
                              final item = items[i];
                              return Padding(
                                padding: const EdgeInsets.only(bottom: 2),
                                child: _IncomeDesktopRow(
                                  item: item,
                                  dmY: _dmY,
                                  onTap: () =>
                                      context.push('/incomes/${item.id}'),
                                ),
                              );
                            }),
                          ],
                        ),
                      );
                    }

                    return RefreshIndicator(
                      color: WellPaidColors.navy,
                      onRefresh: onRefresh,
                      child: ListView.separated(
                        physics: const AlwaysScrollableScrollPhysics(),
                        padding: const EdgeInsets.fromLTRB(16, 0, 16, 24),
                        itemCount: items.length,
                        separatorBuilder: (context, index) =>
                            const Divider(height: 1),
                        itemBuilder: (context, i) {
                          final item = items[i];
                          return _IncomeTile(
                            item: item,
                            dmY: _dmY,
                            onTap: () => context.push('/incomes/${item.id}'),
                          );
                        },
                      ),
                    );
                  },
                ),
              ),
            ],
          );
        },
      ),
    );
  }
}

class _IncomeDesktopRow extends StatelessWidget {
  const _IncomeDesktopRow({
    required this.item,
    required this.dmY,
    required this.onTap,
  });

  final IncomeItem item;
  final String Function(DateTime d) dmY;
  final VoidCallback onTap;

  static const Color _amountGreen = Color(0xFF2E7D32);

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final cat = item.isMine
        ? item.categoryName
        : l10n.expenseTileFamilyCategory(item.categoryName);

    return DecoratedBox(
      decoration: context.wellPaidShadows.cardDecoration(
        color: Colors.white.withValues(alpha: 0.96),
        radius: context.wellPaidRadii.md,
      ),
      child: Material(
        color: Colors.transparent,
        clipBehavior: Clip.antiAlias,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(context.wellPaidRadii.md),
        ),
        child: AppListRow(
          onTap: onTap,
          minHeight: 48,
          children: [
            AppListCell(
              flex: 2,
              child: Text(
                dmY(item.incomeDate),
                style: Theme.of(context).textTheme.bodySmall?.copyWith(
                  color: WellPaidColors.navy.withValues(alpha: 0.75),
                ),
              ),
            ),
            AppListCell(
              flex: 5,
              child: Text(
                item.description,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                  fontWeight: FontWeight.w600,
                  color: WellPaidColors.navy,
                ),
              ),
            ),
            AppListCell(
              flex: 3,
              child: Text(
                cat,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: Theme.of(context).textTheme.bodySmall?.copyWith(
                  color: WellPaidColors.navy.withValues(alpha: 0.65),
                ),
              ),
            ),
            AppListCell(
              flex: 3,
              alignment: Alignment.centerRight,
              child: Text(
                formatBrlFromCents(item.amountCents),
                style: context.moneyTableStyle(color: _amountGreen),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _IncomeTile extends StatelessWidget {
  const _IncomeTile({
    required this.item,
    required this.dmY,
    required this.onTap,
  });

  final IncomeItem item;
  final String Function(DateTime d) dmY;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    return Semantics(
      label: '${item.description}, ${formatBrlFromCents(item.amountCents)}',
      button: true,
      child: InkWell(
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 12),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      item.description,
                      style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                        fontWeight: FontWeight.w600,
                        color: WellPaidColors.navy,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      item.isMine
                          ? item.categoryName
                          : l10n.expenseTileFamilyCategory(item.categoryName),
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(
                        color: WellPaidColors.navy.withValues(alpha: 0.65),
                      ),
                    ),
                    Text(
                      l10n.incomeTileDateLine(dmY(item.incomeDate)),
                      style: Theme.of(context).textTheme.labelSmall?.copyWith(
                        color: WellPaidColors.navy.withValues(alpha: 0.55),
                      ),
                    ),
                  ],
                ),
              ),
              Text(
                formatBrlFromCents(item.amountCents),
                style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                  fontWeight: FontWeight.w700,
                  color: const Color(0xFF2E7D32),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
