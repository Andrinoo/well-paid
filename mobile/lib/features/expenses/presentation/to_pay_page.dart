import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/format/brl_cents.dart';
import '../../../core/l10n/context_l10n.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/theme/well_paid_colors.dart';
import '../../dashboard/presentation/due_urgency.dart';
import '../application/expenses_providers.dart';
import '../application/to_pay_filter.dart';
import '../domain/expense_item.dart';
import 'expense_recurring_label.dart';
import 'pay_expense_flow.dart';
import 'widgets/expense_type_tags.dart';

/// Contas a pagar: todas as despesas **pendentes** (inclui cada parcela), em **ordem
/// cronológica por vencimento**, com cor de alerta pela distância em dias ao vencimento.
class ToPayPage extends ConsumerStatefulWidget {
  const ToPayPage({super.key});

  @override
  ConsumerState<ToPayPage> createState() => _ToPayPageState();
}

class _ToPayPageState extends ConsumerState<ToPayPage> {
  ToPayQuickFilter _quickFilter = ToPayQuickFilter.all;

  void _showLegend(BuildContext context) {
    final l10n = context.l10n;
    showDialog<void>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(l10n.toPayLegendTitle),
        content: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              _legendRow(ctx, DueUrgency.overdue, l10n.toPayLegendOverdue),
              _legendRow(ctx, DueUrgency.dueToday, l10n.toPayLegendDueToday),
              _legendRow(ctx, DueUrgency.dueSoon, l10n.toPayLegendDueSoon),
              _legendRow(ctx, DueUrgency.upcoming, l10n.toPayLegendUpcoming),
              _legendRow(ctx, DueUrgency.safe, l10n.toPayLegendSafe),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(),
            child: Text(MaterialLocalizations.of(ctx).okButtonLabel),
          ),
        ],
      ),
    );
  }

  static Widget _legendRow(BuildContext context, DueUrgency u, String label) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 12,
            height: 12,
            margin: const EdgeInsets.only(top: 3, right: 10),
            decoration: BoxDecoration(
              color: dueUrgencyAccent(u),
              borderRadius: BorderRadius.circular(3),
            ),
          ),
          Expanded(
            child: Text(
              label,
              style: Theme.of(context).textTheme.bodyMedium,
            ),
          ),
        ],
      ),
    );
  }

  List<Widget> _tilesWithDividers(
    BuildContext context,
    List<ExpenseItem> items,
    DateTime today,
  ) {
    if (items.isEmpty) return const [];
    return [
      for (var i = 0; i < items.length; i++) ...[
        if (i > 0) const Divider(height: 1),
        _ToPayTile(
          item: items[i],
          urgency: dueUrgencyForExpense(items[i], today),
          onOpenDetail: () => context.push('/expenses/${items[i].id}'),
        ),
      ],
    ];
  }

  List<Widget> _sectionedChildren(
    BuildContext context,
    List<ExpenseItem> all,
    DateTime today,
  ) {
    final overdue = all.where((e) => isToPayOverdue(e, today)).toList()
      ..sort(compareToPayChronological);
    final week = all
        .where(
          (e) =>
              !isToPayOverdue(e, today) && isToPayDueThisCalendarWeek(e, today),
        )
        .toList()
      ..sort(compareToPayChronological);
    final later = all
        .where(
          (e) =>
              !isToPayOverdue(e, today) &&
              !isToPayDueThisCalendarWeek(e, today),
        )
        .toList()
      ..sort(compareToPayChronological);

    final l10n = context.l10n;
    final theme = Theme.of(context);
    final titleStyle = theme.textTheme.titleSmall?.copyWith(
      fontWeight: FontWeight.w800,
      color: WellPaidColors.navy.withValues(alpha: 0.85),
    );

    List<Widget> out = [];
    void addSection(String title, List<ExpenseItem> bucket) {
      if (bucket.isEmpty) return;
      if (out.isNotEmpty) {
        out.add(const SizedBox(height: 8));
      }
      out.add(
        Padding(
          padding: const EdgeInsets.only(top: 4, bottom: 8),
          child: Text(title, style: titleStyle),
        ),
      );
      out.addAll(_tilesWithDividers(context, bucket, today));
    }

    addSection(l10n.toPaySectionOverdue, overdue);
    addSection(l10n.toPaySectionThisWeek, week);
    addSection(l10n.toPaySectionLater, later);
    return out;
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final async = ref.watch(toPayListProvider);
    final theme = Theme.of(context);
    final showCacheBanner = switch (async) {
      AsyncData(:final value) => value.servedFromLocalCache,
      _ => false,
    };

    return Scaffold(
      appBar: AppBar(
        title: Text(l10n.dashToPay),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => context.pop(),
        ),
        actions: [
          IconButton(
            tooltip: l10n.toPayLegendTitle,
            onPressed: () => _showLegend(context),
            icon: const Icon(Icons.palette_outlined),
          ),
          IconButton(
            tooltip: l10n.expensesRefresh,
            onPressed: () => ref.invalidate(toPayListProvider),
            icon: const Icon(Icons.refresh),
          ),
        ],
      ),
      body: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          if (showCacheBanner)
            Material(
              color: WellPaidColors.navy.withValues(alpha: 0.08),
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
                child: Row(
                  children: [
                    Icon(
                      Icons.cloud_off_outlined,
                      size: 20,
                      color: WellPaidColors.navy.withValues(alpha: 0.75),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Text(
                        l10n.toPayOfflineCacheBanner,
                        style: theme.textTheme.bodySmall?.copyWith(
                          color: WellPaidColors.navy.withValues(alpha: 0.88),
                          height: 1.35,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
            child: Text(
              l10n.toPayScreenSubtitle,
              style: theme.textTheme.bodySmall?.copyWith(
                color: WellPaidColors.navy.withValues(alpha: 0.72),
                height: 1.35,
              ),
            ),
          ),
          Expanded(
            child: async.when(
              loading: () => const Center(child: CircularProgressIndicator()),
              error: (e, _) => Center(
                child: Padding(
                  padding: const EdgeInsets.all(24),
                  child: Text(
                    messageFromDio(e, l10n) ?? l10n.expensesLoadError,
                    textAlign: TextAlign.center,
                  ),
                ),
              ),
              data: (snapshot) {
                final today = DateTime.now();
                final all = snapshot.items;
                if (all.isEmpty) {
                  return ListView(
                    physics: const AlwaysScrollableScrollPhysics(),
                    padding: const EdgeInsets.all(24),
                    children: [
                      const SizedBox(height: 32),
                      Center(
                        child: Text(
                          l10n.dashNothingPending,
                          textAlign: TextAlign.center,
                          style: theme.textTheme.titleMedium?.copyWith(
                            color:
                                WellPaidColors.navy.withValues(alpha: 0.65),
                          ),
                        ),
                      ),
                      const SizedBox(height: 24),
                      Center(
                        child: TextButton.icon(
                          onPressed: () => context.push('/expenses'),
                          icon: const Icon(Icons.receipt_long_outlined),
                          label: Text(l10n.toPayViewAllExpenses),
                        ),
                      ),
                    ],
                  );
                }

                final filtered =
                    applyToPayQuickFilter(all, _quickFilter, today);
                final totalCents = sumAmountCents(filtered);

                return RefreshIndicator(
                  color: WellPaidColors.navy,
                  onRefresh: () async {
                    ref.invalidate(toPayListProvider);
                    await ref.read(toPayListProvider.future);
                  },
                  child: ListView(
                    physics: const AlwaysScrollableScrollPhysics(),
                    padding: const EdgeInsets.fromLTRB(16, 0, 16, 24),
                    children: [
                      SingleChildScrollView(
                        scrollDirection: Axis.horizontal,
                        child: Row(
                          children: [
                            ChoiceChip(
                              label: Text(l10n.toPayFilterAll),
                              selected: _quickFilter == ToPayQuickFilter.all,
                              onSelected: (v) {
                                if (v) {
                                  setState(
                                    () => _quickFilter = ToPayQuickFilter.all,
                                  );
                                }
                              },
                            ),
                            const SizedBox(width: 8),
                            ChoiceChip(
                              label: Text(l10n.toPayFilterOverdue),
                              selected:
                                  _quickFilter == ToPayQuickFilter.overdue,
                              onSelected: (v) {
                                if (v) {
                                  setState(
                                    () =>
                                        _quickFilter = ToPayQuickFilter.overdue,
                                  );
                                }
                              },
                            ),
                            const SizedBox(width: 8),
                            ChoiceChip(
                              label: Text(l10n.toPayFilterThisWeek),
                              selected:
                                  _quickFilter == ToPayQuickFilter.thisWeek,
                              onSelected: (v) {
                                if (v) {
                                  setState(
                                    () => _quickFilter =
                                        ToPayQuickFilter.thisWeek,
                                  );
                                }
                              },
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(height: 12),
                      Text(
                        l10n.toPayPendingTotal(
                          formatBrlFromCents(totalCents),
                        ),
                        style: theme.textTheme.titleSmall?.copyWith(
                          fontWeight: FontWeight.w700,
                          color: WellPaidColors.navy,
                        ),
                      ),
                      const SizedBox(height: 12),
                      if (filtered.isEmpty)
                        Padding(
                          padding: const EdgeInsets.only(top: 24),
                          child: Column(
                            children: [
                              Text(
                                l10n.toPayFilterEmpty,
                                textAlign: TextAlign.center,
                                style: theme.textTheme.bodyLarge?.copyWith(
                                  color: WellPaidColors.navy
                                      .withValues(alpha: 0.65),
                                ),
                              ),
                              const SizedBox(height: 16),
                              TextButton(
                                onPressed: () => setState(
                                  () => _quickFilter = ToPayQuickFilter.all,
                                ),
                                child: Text(l10n.toPayFilterAll),
                              ),
                            ],
                          ),
                        )
                      else if (_quickFilter == ToPayQuickFilter.all)
                        ..._sectionedChildren(context, all, today)
                      else
                        ..._tilesWithDividers(context, filtered, today),
                      const SizedBox(height: 16),
                      Center(
                        child: TextButton.icon(
                          onPressed: () => context.push('/expenses'),
                          icon: const Icon(Icons.receipt_long_outlined),
                          label: Text(l10n.toPayViewAllExpenses),
                        ),
                      ),
                    ],
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

class _ToPayTile extends ConsumerStatefulWidget {
  const _ToPayTile({
    required this.item,
    required this.urgency,
    required this.onOpenDetail,
  });

  final ExpenseItem item;
  final DueUrgency urgency;
  final VoidCallback onOpenDetail;

  @override
  ConsumerState<_ToPayTile> createState() => _ToPayTileState();
}

class _ToPayTileState extends ConsumerState<_ToPayTile> {
  bool _payBusy = false;

  String _dmY(DateTime d) =>
      '${d.day.toString().padLeft(2, '0')}/${d.month.toString().padLeft(2, '0')}/${d.year}';

  Future<void> _onPaySwitch(bool v) async {
    if (!v || _payBusy || !widget.item.isMine || !widget.item.isPending) return;
    setState(() => _payBusy = true);
    await confirmAndPayExpense(context, ref, expense: widget.item);
    if (mounted) setState(() => _payBusy = false);
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final item = widget.item;
    final accent = dueUrgencyAccent(widget.urgency);
    final dueTextColor = dueUrgencyOnLightBackground(widget.urgency);
    final dueTextWeight = dueUrgencyValueWeight(widget.urgency);
    final rec = expenseRecurringLabel(item, l10n);
    final catLine = item.isMine
        ? item.categoryName
        : l10n.expenseTileFamilyCategory(item.categoryName);

    final dueLine = item.dueDate != null
        ? l10n.toPayDueOn(_dmY(item.dueDate!))
        : l10n.toPayCompetenceOn(_dmY(item.expenseDate));

    final canQuickPay = item.isMine && item.isPending && !_payBusy;

    final leftColumn = InkWell(
      onTap: widget.onOpenDetail,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: Text(
                  item.description,
                  style: Theme.of(context).textTheme.bodyLarge?.copyWith(
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
          if (item.isProjected)
            Padding(
              padding: const EdgeInsets.only(top: 6),
              child: Text(
                l10n.toPayProjectedHint,
                style: Theme.of(context).textTheme.labelSmall?.copyWith(
                      color: WellPaidColors.navy.withValues(alpha: 0.55),
                      fontStyle: FontStyle.italic,
                    ),
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
                ],
              ),
            ),
          const SizedBox(height: 6),
          Text(
            dueLine,
            style: Theme.of(context).textTheme.labelMedium?.copyWith(
                  fontWeight: dueTextWeight,
                  color: dueTextColor,
                ),
          ),
        ],
      ),
    );

    final amountAndSwitch = Column(
      crossAxisAlignment: CrossAxisAlignment.end,
      children: [
        Text(
          formatBrlFromCents(item.amountCents),
          style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                fontWeight: FontWeight.w700,
                color: WellPaidColors.navy,
              ),
        ),
        const SizedBox(height: 4),
        Tooltip(
          message: l10n.toPayQuickPaySwitchTooltip,
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              if (_payBusy)
                Padding(
                  padding: const EdgeInsets.only(right: 8),
                  child: SizedBox(
                    width: 22,
                    height: 22,
                    child: CircularProgressIndicator(
                      strokeWidth: 2,
                      color: WellPaidColors.navy.withValues(alpha: 0.65),
                    ),
                  ),
                ),
              Switch.adaptive(
                value: false,
                onChanged: canQuickPay
                    ? (on) {
                        if (on) unawaited(_onPaySwitch(true));
                      }
                    : null,
              ),
            ],
          ),
        ),
      ],
    );

    return LayoutBuilder(
      builder: (context, constraints) {
        final narrow = constraints.maxWidth < 420;
        return Semantics(
          label: '${item.description}, ${formatBrlFromCents(item.amountCents)}',
          child: Padding(
            padding: const EdgeInsets.symmetric(vertical: 12),
            child: Row(
              crossAxisAlignment:
                  narrow ? CrossAxisAlignment.start : CrossAxisAlignment.stretch,
              children: [
                Container(
                  width: 4,
                  decoration: BoxDecoration(
                    color: accent,
                    borderRadius: BorderRadius.circular(3),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: narrow
                      ? Column(
                          crossAxisAlignment: CrossAxisAlignment.stretch,
                          children: [
                            leftColumn,
                            const SizedBox(height: 10),
                            Align(
                              alignment: Alignment.centerRight,
                              child: amountAndSwitch,
                            ),
                          ],
                        )
                      : Row(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Expanded(child: leftColumn),
                            const SizedBox(width: 8),
                            amountAndSwitch,
                          ],
                        ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}
