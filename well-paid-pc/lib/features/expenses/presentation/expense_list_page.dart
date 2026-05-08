import 'package:flutter/material.dart';
import 'package:phosphor_flutter/phosphor_flutter.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/date/calendar_month.dart';
import '../../../core/format/brl_cents.dart';
import '../../../core/l10n/context_l10n.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/theme/well_paid_breakpoints.dart';
import '../../../core/theme/well_paid_colors.dart';
import '../../../core/theme/well_paid_radii.dart';
import '../../../core/theme/well_paid_shadows.dart';
import '../../../core/widgets/desktop/desktop.dart';
import '../../dashboard/application/dashboard_providers.dart';
import '../../dashboard/presentation/due_urgency.dart';
import '../application/expenses_providers.dart';
import '../domain/expense_item.dart';
import 'expense_recurring_label.dart';
import 'pay_expense_flow.dart';
import 'widgets/expense_type_tags.dart';

class ExpenseListPage extends ConsumerStatefulWidget {
  const ExpenseListPage({super.key, this.initialStatus});

  final String? initialStatus;

  @override
  ConsumerState<ExpenseListPage> createState() => _ExpenseListPageState();
}

class _ExpenseListPageState extends ConsumerState<ExpenseListPage> {
  @override
  void initState() {
    super.initState();
    final s = widget.initialStatus;
    if (s == 'pending' || s == 'paid') {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (!mounted) return;
        final dash = ref.read(dashboardPeriodProvider);
        ref
            .read(expenseListFiltersProvider.notifier)
            .state = ExpenseListFilters(
          year: dash.year,
          month: dash.month,
          status: s,
          categoryId: null,
        );
      });
    }
  }

  void _shiftMonth(int delta) {
    final f = ref.read(expenseListFiltersProvider);
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
    ref.read(expenseListFiltersProvider.notifier).state = ExpenseListFilters(
      year: y,
      month: m,
      status: f.status,
      categoryId: null,
    );
  }

  Future<void> _pay(ExpenseItem e) async {
    await confirmAndPayExpense(context, ref, expense: e);
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final f = ref.watch(expenseListFiltersProvider);
    final async = ref.watch(expensesListProvider);

    return Scaffold(
      appBar: AppBar(
        leading: Navigator.of(context).canPop()
            ? IconButton(
                icon: const Icon(PhosphorIconsRegular.arrowLeft),
                onPressed: () => context.pop(),
              )
            : null,
        title: Text(l10n.expensesTitle),
        actions: [
          IconButton(
            tooltip: l10n.expensesRefresh,
            onPressed: () => ref.invalidate(expensesListProvider),
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
                    Wrap(
                      spacing: 8,
                      runSpacing: 8,
                      alignment: WrapAlignment.center,
                      children: [
                        FilterChip(
                          label: Text(l10n.expensesFilterAll),
                          selected: f.status == null,
                          onSelected: (_) {
                            ref
                                .read(expenseListFiltersProvider.notifier)
                                .state = ExpenseListFilters(
                              year: f.year,
                              month: f.month,
                              categoryId: f.categoryId,
                            );
                          },
                        ),
                        FilterChip(
                          label: Text(l10n.expensesFilterPending),
                          selected: f.status == 'pending',
                          onSelected: (_) {
                            ref
                                .read(expenseListFiltersProvider.notifier)
                                .state = ExpenseListFilters(
                              year: f.year,
                              month: f.month,
                              status: 'pending',
                              categoryId: f.categoryId,
                            );
                          },
                        ),
                        FilterChip(
                          label: Text(l10n.expensesFilterPaid),
                          selected: f.status == 'paid',
                          onSelected: (_) {
                            ref
                                .read(expenseListFiltersProvider.notifier)
                                .state = ExpenseListFilters(
                              year: f.year,
                              month: f.month,
                              status: 'paid',
                              categoryId: f.categoryId,
                            );
                          },
                        ),
                      ],
                    ),
                    const SizedBox(height: 10),
                    Row(
                      children: [
                        Expanded(
                          child: FilledButton.tonalIcon(
                            onPressed: () => context.push('/expenses/new'),
                            icon: const Icon(PhosphorIconsRegular.receipt),
                            label: Text(l10n.expensesNewLong),
                            style: FilledButton.styleFrom(
                              foregroundColor: WellPaidColors.navy,
                            ),
                          ),
                        ),
                        IconButton(
                          tooltip: l10n.expensesRefreshList,
                          onPressed: () => ref.invalidate(expensesListProvider),
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
              if (f.categoryId != null)
                Padding(
                  padding: const EdgeInsets.fromLTRB(12, 8, 12, 0),
                  child: ref
                      .watch(categoriesProvider)
                      .when(
                        skipLoadingOnReload: true,
                        data: (cats) {
                          String title = l10n.expenseListFilteredByCategory;
                          for (final c in cats) {
                            if (c.id == f.categoryId) {
                              title = c.name;
                              break;
                            }
                          }
                          return Material(
                            color: WellPaidColors.gold.withValues(alpha: 0.22),
                            borderRadius: BorderRadius.circular(12),
                            child: ListTile(
                              dense: true,
                              leading: Icon(
                                PhosphorIconsRegular.funnelSimple,
                                color: WellPaidColors.navy.withValues(
                                  alpha: 0.85,
                                ),
                              ),
                              title: Text(
                                title,
                                style: Theme.of(context).textTheme.titleSmall
                                    ?.copyWith(
                                      fontWeight: FontWeight.w700,
                                      color: WellPaidColors.navy,
                                    ),
                              ),
                              trailing: TextButton(
                                onPressed: () {
                                  ref
                                      .read(expenseListFiltersProvider.notifier)
                                      .state = ExpenseListFilters(
                                    year: f.year,
                                    month: f.month,
                                    status: f.status,
                                  );
                                },
                                child: Text(
                                  l10n.expenseListClearCategoryFilter,
                                ),
                              ),
                            ),
                          );
                        },
                        loading: () => const SizedBox.shrink(),
                        error: (_, _) => const SizedBox.shrink(),
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
                        6,
                        (i) => Padding(
                          padding: const EdgeInsets.only(bottom: 14),
                          child: Container(
                            height: 56,
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
                        messageFromDio(e, l10n) ?? l10n.expensesLoadError,
                        textAlign: TextAlign.center,
                      ),
                    ),
                  ),
                  data: (items) {
                    if (items.isEmpty) {
                      return WellPaidEmptyState(
                        title: l10n.expensesEmpty,
                        icon: PhosphorIconsRegular.receipt,
                      );
                    }
                    Future<void> onRefresh() async {
                      ref.invalidate(expensesListProvider);
                      await ref.read(expensesListProvider.future);
                    }

                    if (wide) {
                      const pad = EdgeInsets.fromLTRB(20, 8, 20, 24);
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
                          padding: pad,
                          children: [
                            AppListHeaderRow(
                              minHeight: 36,
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
                                AppListCell(
                                  flex: 2,
                                  alignment: Alignment.centerRight,
                                  child: Text(
                                    l10n.expenseTableColStatus,
                                    style: headerStyle,
                                  ),
                                ),
                              ],
                            ),
                            ...List.generate(items.length, (i) {
                              final e = items[i];
                              return Padding(
                                padding: const EdgeInsets.only(bottom: 2),
                                child: _ExpenseDesktopRow(
                                  item: e,
                                  onTap: () =>
                                      context.push('/expenses/${e.id}'),
                                  onPay: (e.isPending && e.isMine)
                                      ? () => _pay(e)
                                      : null,
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
                        separatorBuilder: (context, _) =>
                            const Divider(height: 1),
                        itemBuilder: (context, i) {
                          final e = items[i];
                          return _ExpenseTile(
                            item: e,
                            onTap: () => context.push('/expenses/${e.id}'),
                            onPay: (e.isPending && e.isMine)
                                ? () => _pay(e)
                                : null,
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

class _ExpenseDesktopRow extends StatelessWidget {
  const _ExpenseDesktopRow({
    required this.item,
    required this.onTap,
    this.onPay,
  });

  final ExpenseItem item;
  final VoidCallback onTap;
  final VoidCallback? onPay;

  String _dmY(DateTime d) =>
      '${d.day.toString().padLeft(2, '0')}/${d.month.toString().padLeft(2, '0')}/${d.year}';

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final catLine = item.isMine
        ? item.categoryName
        : l10n.expenseTileFamilyCategory(item.categoryName);
    final statusLabel = item.isPending
        ? l10n.expenseStatusPending
        : l10n.expenseStatusPaid;

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
                _dmY(item.expenseDate),
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
                catLine,
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
                style: context.moneyTableStyle(),
              ),
            ),
            AppListCell(
              flex: 2,
              alignment: Alignment.centerRight,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.end,
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    statusLabel,
                    style: Theme.of(context).textTheme.labelMedium?.copyWith(
                      fontWeight: FontWeight.w600,
                      color: WellPaidColors.navy.withValues(alpha: 0.72),
                    ),
                  ),
                  if (onPay != null)
                    TextButton(onPressed: onPay, child: Text(l10n.expensePay)),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _ExpenseTile extends StatelessWidget {
  const _ExpenseTile({required this.item, required this.onTap, this.onPay});

  final ExpenseItem item;
  final VoidCallback onTap;
  final VoidCallback? onPay;

  String _dmY(DateTime d) =>
      '${d.day.toString().padLeft(2, '0')}/${d.month.toString().padLeft(2, '0')}/${d.year}';

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final rec = expenseRecurringLabel(item, l10n);
    final catLine = item.isMine
        ? item.categoryName
        : l10n.expenseTileFamilyCategory(item.categoryName);
    final statusLabel = item.isPending
        ? l10n.expenseStatusPending
        : l10n.expenseStatusPaid;
    final today = DateTime.now();
    final anchorThisLine = item.dueDate ?? item.expenseDate;
    final pendingUrgency = item.isPending
        ? dueUrgencyFor(anchorThisLine, today)
        : null;
    final dateLineColor = pendingUrgency != null
        ? dueUrgencyOnLightBackground(pendingUrgency)
        : WellPaidColors.navy.withValues(alpha: 0.55);
    final DateTime? nextInstallmentDue =
        item.isInstallmentPlan && item.installmentNumber < item.installmentTotal
        ? (item.dueDate != null
              ? addCalendarMonths(item.dueDate!, 1)
              : addCalendarMonths(item.expenseDate, 1))
        : null;

    return Semantics(
      label:
          '${item.description}, ${formatBrlFromCents(item.amountCents)}, ${item.status}',
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
                    Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Expanded(
                          child: Text(
                            item.description,
                            style: Theme.of(context).textTheme.bodyLarge
                                ?.copyWith(
                                  fontWeight: FontWeight.w600,
                                  color: WellPaidColors.navy,
                                ),
                          ),
                        ),
                        const SizedBox(width: 8),
                        ExpenseTypeTags(item: item, compact: true),
                      ],
                    ),
                    const SizedBox(height: 4),
                    Text(
                      catLine,
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(
                        color: WellPaidColors.navy.withValues(alpha: 0.65),
                      ),
                    ),
                    if (item.isInstallmentPlan || rec != null)
                      Padding(
                        padding: const EdgeInsets.only(top: 6),
                        child: Wrap(
                          spacing: 6,
                          runSpacing: 4,
                          children: [
                            if (item.isInstallmentPlan)
                              Chip(
                                label: Text(
                                  l10n.expenseInstallmentChip(
                                    item.installmentNumber,
                                    item.installmentTotal,
                                  ),
                                ),
                                visualDensity: VisualDensity.compact,
                                padding: EdgeInsets.zero,
                                labelStyle: const TextStyle(fontSize: 11),
                              ),
                            if (rec != null)
                              Chip(
                                label: Text(
                                  rec,
                                  style: const TextStyle(fontSize: 11),
                                ),
                                visualDensity: VisualDensity.compact,
                                padding: EdgeInsets.zero,
                              ),
                            if (item.isShared)
                              Chip(
                                label: Text(
                                  item.sharedWithLabel != null &&
                                          item.sharedWithLabel!.isNotEmpty
                                      ? l10n.expenseSharedWith(
                                          item.sharedWithLabel!,
                                        )
                                      : l10n.expenseShared,
                                  style: const TextStyle(fontSize: 11),
                                ),
                                visualDensity: VisualDensity.compact,
                                padding: EdgeInsets.zero,
                              ),
                          ],
                        ),
                      ),
                    if (nextInstallmentDue != null) ...[
                      Padding(
                        padding: const EdgeInsets.only(top: 6),
                        child: Text(
                          l10n.expenseListNextInstallmentLine(
                            _dmY(nextInstallmentDue),
                          ),
                          style: Theme.of(context).textTheme.labelSmall
                              ?.copyWith(
                                color: dueUrgencyOnLightBackground(
                                  dueUrgencyFor(nextInstallmentDue, today),
                                ),
                                fontWeight: dueUrgencyValueWeight(
                                  dueUrgencyFor(nextInstallmentDue, today),
                                ),
                              ),
                        ),
                      ),
                    ],
                    Text(
                      l10n.expenseTileDateLine(
                        _dmY(item.expenseDate),
                        statusLabel,
                      ),
                      style: Theme.of(context).textTheme.labelSmall?.copyWith(
                        color: dateLineColor,
                        fontWeight: pendingUrgency != null
                            ? dueUrgencyValueWeight(pendingUrgency)
                            : null,
                      ),
                    ),
                  ],
                ),
              ),
              Column(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  Text(
                    formatBrlFromCents(item.amountCents),
                    style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                      fontWeight: FontWeight.w700,
                      color: WellPaidColors.navy,
                    ),
                  ),
                  if (onPay != null)
                    TextButton(onPressed: onPay, child: Text(l10n.expensePay)),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
