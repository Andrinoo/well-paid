import 'package:flutter/material.dart';
import 'package:phosphor_flutter/phosphor_flutter.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/date/calendar_month.dart';
import '../../../core/format/brl_cents.dart';
import '../../../core/l10n/context_l10n.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/theme/well_paid_colors.dart';
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
        ref.read(expenseListFiltersProvider.notifier).state = ExpenseListFilters(
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
      body: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Material(
            color: WellPaidColors.creamMuted.withValues(alpha: 0.6),
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
              child: Row(
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
                    style: Theme.of(context).textTheme.titleMedium?.copyWith(
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
            ),
          ),
          if (f.categoryId != null)
            Padding(
              padding: const EdgeInsets.fromLTRB(12, 8, 12, 0),
              child: ref.watch(categoriesProvider).when(
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
                            color: WellPaidColors.navy.withValues(alpha: 0.85),
                          ),
                          title: Text(
                            title,
                            style: Theme.of(context).textTheme.titleSmall?.copyWith(
                                  fontWeight: FontWeight.w700,
                                  color: WellPaidColors.navy,
                                ),
                          ),
                          trailing: TextButton(
                            onPressed: () {
                              ref.read(expenseListFiltersProvider.notifier).state =
                                  ExpenseListFilters(
                                year: f.year,
                                month: f.month,
                                status: f.status,
                              );
                            },
                            child: Text(l10n.expenseListClearCategoryFilter),
                          ),
                        ),
                      );
                    },
                    loading: () => const SizedBox.shrink(),
                    error: (_, _) => const SizedBox.shrink(),
                  ),
            ),
          Padding(
            padding: const EdgeInsets.fromLTRB(12, 10, 12, 0),
            child: Card(
              elevation: 0,
              color: WellPaidColors.creamMuted.withValues(alpha: 0.9),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(14),
                side: BorderSide(
                  color: WellPaidColors.navy.withValues(alpha: 0.1),
                ),
              ),
              child: Padding(
                padding:
                    const EdgeInsets.symmetric(horizontal: 10, vertical: 10),
                child: Row(
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
                      icon: const Icon(PhosphorIconsRegular.arrowsClockwise),
                      color: WellPaidColors.navy,
                    ),
                  ],
                ),
              ),
            ),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
            child: Wrap(
              spacing: 8,
              children: [
                FilterChip(
                  label: Text(l10n.expensesFilterAll),
                  selected: f.status == null,
                  onSelected: (_) {
                    ref.read(expenseListFiltersProvider.notifier).state =
                        ExpenseListFilters(
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
                    ref.read(expenseListFiltersProvider.notifier).state =
                        ExpenseListFilters(
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
                    ref.read(expenseListFiltersProvider.notifier).state =
                        ExpenseListFilters(
                      year: f.year,
                      month: f.month,
                      status: 'paid',
                      categoryId: f.categoryId,
                    );
                  },
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
                    backgroundColor: WellPaidColors.navy.withValues(alpha: 0.08),
                  ),
                  const SizedBox(height: 20),
                  ...List.generate(
                    6,
                    (i) => Padding(
                      padding: const EdgeInsets.only(bottom: 14),
                      child: Container(
                        height: 56,
                        decoration: BoxDecoration(
                          color: WellPaidColors.navy.withValues(alpha: 0.06),
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
                  return ListView(
                    physics: const AlwaysScrollableScrollPhysics(),
                    children: [
                      const SizedBox(height: 48),
                      Center(child: Text(l10n.expensesEmpty)),
                    ],
                  );
                }
                return RefreshIndicator(
                  color: WellPaidColors.navy,
                  onRefresh: () async {
                    ref.invalidate(expensesListProvider);
                    await ref.read(expensesListProvider.future);
                  },
                  child: ListView.separated(
                    physics: const AlwaysScrollableScrollPhysics(),
                    padding: const EdgeInsets.fromLTRB(16, 0, 16, 24),
                    itemCount: items.length,
                    separatorBuilder: (context, _) => const Divider(height: 1),
                    itemBuilder: (context, i) {
                      final e = items[i];
                      return _ExpenseTile(
                        item: e,
                        onTap: () => context.push('/expenses/${e.id}'),
                        onPay: (e.isPending && e.isMine) ? () => _pay(e) : null,
                      );
                    },
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}

class _ExpenseTile extends StatelessWidget {
  const _ExpenseTile({
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
    final rec = expenseRecurringLabel(item, l10n);
    final catLine = item.isMine
        ? item.categoryName
        : l10n.expenseTileFamilyCategory(item.categoryName);
    final statusLabel =
        item.isPending ? l10n.expenseStatusPending : l10n.expenseStatusPaid;
    final today = DateTime.now();
    final anchorThisLine = item.dueDate ?? item.expenseDate;
    final pendingUrgency = item.isPending
        ? dueUrgencyFor(anchorThisLine, today)
        : null;
    final dateLineColor = pendingUrgency != null
        ? dueUrgencyOnLightBackground(pendingUrgency)
        : WellPaidColors.navy.withValues(alpha: 0.55);
    final DateTime? nextInstallmentDue = item.isInstallmentPlan &&
            item.installmentNumber < item.installmentTotal
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
                            style: Theme.of(context)
                                .textTheme
                                .bodyLarge
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
                          style: Theme.of(context)
                              .textTheme
                              .labelSmall
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
                      l10n.expenseTileDateLine(_dmY(item.expenseDate), statusLabel),
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
                    TextButton(
                      onPressed: onPay,
                      child: Text(l10n.expensePay),
                    ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
