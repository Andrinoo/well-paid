import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/format/brl_cents.dart';
import '../../../core/l10n/context_l10n.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/theme/well_paid_colors.dart';
import '../../../l10n/app_localizations.dart';
import '../../dashboard/application/dashboard_providers.dart';
import '../application/expenses_providers.dart';
import '../domain/expense_delete_options.dart';
import '../domain/expense_item.dart';
import 'expense_recurring_label.dart';
import 'pay_expense_flow.dart';

typedef _DeleteParams = ({
  ExpenseDeleteTarget target,
  ExpenseDeleteScope scope,
});

class ExpenseDetailPage extends ConsumerWidget {
  const ExpenseDetailPage({super.key, required this.expenseId});

  final String expenseId;

  static String _dmY(DateTime d) =>
      '${d.day.toString().padLeft(2, '0')}/${d.month.toString().padLeft(2, '0')}/${d.year}';

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    final async = ref.watch(expenseDetailProvider(expenseId));

    return async.when(
      loading: () => Scaffold(
        appBar: AppBar(
          leading: IconButton(
            icon: const Icon(Icons.arrow_back),
            onPressed: () => context.pop(),
          ),
          title: Text(l10n.expenseTitle),
        ),
        body: const Center(child: CircularProgressIndicator()),
      ),
      error: (e, _) => Scaffold(
        appBar: AppBar(
          leading: IconButton(
            icon: const Icon(Icons.arrow_back),
            onPressed: () => context.pop(),
          ),
          title: Text(l10n.expenseTitle),
        ),
        body: Center(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Text(
              messageFromDio(e, l10n) ?? l10n.expenseLoadError,
              textAlign: TextAlign.center,
            ),
          ),
        ),
      ),
      data: (e) => _DetailBody(expenseId: expenseId, e: e, readOnly: !e.isMine),
    );
  }
}

class _DetailBody extends ConsumerWidget {
  const _DetailBody({
    required this.expenseId,
    required this.e,
    required this.readOnly,
  });

  final String expenseId;
  final ExpenseItem e;
  final bool readOnly;

  Future<void> _pay(BuildContext context, WidgetRef ref) async {
    await confirmAndPayExpense(
      context,
      ref,
      expense: e,
      onPaid: (r) => r.invalidate(expenseDetailProvider(expenseId)),
    );
  }

  Future<_DeleteParams?> _pickDeleteParams(
    BuildContext context,
    AppLocalizations l10n,
  ) async {
    if (e.isInstallmentPlan && e.installmentGroupId != null) {
      if (e.installmentPlanHasPaid != false) {
        return showDialog<_DeleteParams>(
          context: context,
          builder: (ctx) => AlertDialog(
            title: Text(l10n.expDelInstallmentTitle),
            content: Text(
              e.installmentPlanHasPaid == true
                  ? l10n.expDelInstallmentPaidBody
                  : l10n.expDelInstallmentMaybePaidBody,
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(ctx),
                child: Text(l10n.cancel),
              ),
              TextButton(
                onPressed: () => Navigator.pop(
                  ctx,
                  (
                    target: ExpenseDeleteTarget.series,
                    scope: ExpenseDeleteScope.futureUnpaid,
                  ),
                ),
                child: Text(l10n.expDelFutureOnly),
              ),
              FilledButton(
                style: FilledButton.styleFrom(
                  backgroundColor: const Color(0xFFB71C1C),
                ),
                onPressed: () => Navigator.pop(
                  ctx,
                  (
                    target: ExpenseDeleteTarget.series,
                    scope: ExpenseDeleteScope.all,
                  ),
                ),
                child: Text(l10n.expDelAllIncludingPaid),
              ),
            ],
          ),
        );
      }
      final ok = await showDialog<bool>(
        context: context,
        builder: (ctx) => AlertDialog(
          title: Text(l10n.expDelInstallmentSimpleTitle),
          content: Text(l10n.expDelInstallmentSimpleBody(e.installmentTotal)),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx, false),
              child: Text(l10n.cancel),
            ),
            FilledButton(
              style: FilledButton.styleFrom(
                backgroundColor: const Color(0xFFB71C1C),
              ),
              onPressed: () => Navigator.pop(ctx, true),
              child: Text(l10n.delete),
            ),
          ],
        ),
      );
      if (ok != true) return null;
      return (
        target: ExpenseDeleteTarget.series,
        scope: ExpenseDeleteScope.all,
      );
    }

    if (e.belongsToRecurringSeries) {
      if (e.isRecurringAnchor) {
        return showDialog<_DeleteParams>(
          context: context,
          builder: (ctx) => AlertDialog(
            title: Text(l10n.expDelRecurringTitle),
            content: Text(l10n.expDelRecurringBody),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(ctx),
                child: Text(l10n.cancel),
              ),
              TextButton(
                onPressed: () => Navigator.pop(
                  ctx,
                  (
                    target: ExpenseDeleteTarget.series,
                    scope: ExpenseDeleteScope.futureUnpaid,
                  ),
                ),
                child: Text(l10n.expDelFutureOnly),
              ),
              FilledButton(
                style: FilledButton.styleFrom(
                  backgroundColor: const Color(0xFFB71C1C),
                ),
                onPressed: () => Navigator.pop(
                  ctx,
                  (
                    target: ExpenseDeleteTarget.series,
                    scope: ExpenseDeleteScope.all,
                  ),
                ),
                child: Text(l10n.expDelCloseSeries),
              ),
            ],
          ),
        );
      }

      final area = await showDialog<String>(
        context: context,
        builder: (ctx) => AlertDialog(
          title: Text(l10n.expDelRecurringOccurrenceTitle),
          content: Text(l10n.expDelRecurringOccurrenceBody),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: Text(l10n.cancel),
            ),
            TextButton(
              onPressed: () => Navigator.pop(ctx, 'one'),
              child: Text(l10n.expDelThisOnly),
            ),
            FilledButton(
              onPressed: () => Navigator.pop(ctx, 'series'),
              child: Text(l10n.expDelWholeSeries),
            ),
          ],
        ),
      );
      if (area == null || !context.mounted) return null;
      if (area == 'one') {
        final paid = !e.isPending;
        final ok = await showDialog<bool>(
          context: context,
          builder: (ctx) => AlertDialog(
            title: Text(
              paid
                  ? l10n.expDelRemoveFromRecurrenceTitle
                  : l10n.expDelRemoveOccurrenceTitle,
            ),
            content: Text(
              paid ? l10n.expDelPaidUnlinkBody : l10n.expDelPendingDeleteBody,
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(ctx, false),
                child: Text(l10n.cancel),
              ),
              FilledButton(
                onPressed: () => Navigator.pop(ctx, true),
                child: Text(l10n.confirm),
              ),
            ],
          ),
        );
        if (ok != true) return null;
        return (
          target: ExpenseDeleteTarget.occurrence,
          scope: ExpenseDeleteScope.all,
        );
      }

      return showDialog<_DeleteParams>(
        context: context,
        builder: (ctx) => AlertDialog(
          title: Text(l10n.expDelSeriesScopeTitle),
          content: Text(l10n.expDelSeriesScopeBody),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: Text(l10n.cancel),
            ),
            TextButton(
              onPressed: () => Navigator.pop(
                ctx,
                (
                  target: ExpenseDeleteTarget.series,
                  scope: ExpenseDeleteScope.futureUnpaid,
                ),
              ),
              child: Text(l10n.expDelFutureOnly),
            ),
            FilledButton(
              style: FilledButton.styleFrom(
                backgroundColor: const Color(0xFFB71C1C),
              ),
              onPressed: () => Navigator.pop(
                ctx,
                (
                  target: ExpenseDeleteTarget.series,
                  scope: ExpenseDeleteScope.all,
                ),
              ),
              child: Text(l10n.expDelCloseSeries),
            ),
          ],
        ),
      );
    }

    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(l10n.expDelSingleTitle),
        content: Text(l10n.expDelSingleBody),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: Text(l10n.cancel),
          ),
          FilledButton(
            style: FilledButton.styleFrom(
              backgroundColor: const Color(0xFFB71C1C),
            ),
            onPressed: () => Navigator.pop(ctx, true),
            child: Text(l10n.delete),
          ),
        ],
      ),
    );
    if (ok != true) return null;
    return (target: ExpenseDeleteTarget.series, scope: ExpenseDeleteScope.all);
  }

  Future<void> _delete(BuildContext context, WidgetRef ref) async {
    final l10n = context.l10n;
    final params = await _pickDeleteParams(context, l10n);
    if (params == null || !context.mounted) return;

    final messenger = ScaffoldMessenger.of(context);
    try {
      await ref.read(expensesRepositoryProvider).deleteExpense(
            e.id,
            target: params.target,
            scope: params.scope,
          );
      ref.invalidate(expensesListProvider);
      ref.invalidate(dashboardOverviewProvider);
      ref.invalidate(expenseDetailProvider(expenseId));
      if (context.mounted) {
        final msg = _deleteSuccessMessage(params, l10n);
        messenger.showSnackBar(SnackBar(content: Text(msg)));
        context.pop();
      }
    } catch (err) {
      if (context.mounted) {
        messenger.showSnackBar(
          SnackBar(
            content: Text(messageFromDio(err, l10n) ?? l10n.expenseDeleteError),
          ),
        );
      }
    }
  }

  String _deleteSuccessMessage(_DeleteParams p, AppLocalizations l10n) {
    if (e.isInstallmentPlan && e.installmentGroupId != null) {
      return p.scope == ExpenseDeleteScope.futureUnpaid
          ? l10n.expDelSuccessInstallmentFuture
          : l10n.expDelSuccessInstallmentAll;
    }
    if (e.belongsToRecurringSeries) {
      if (p.target == ExpenseDeleteTarget.occurrence) {
        return e.isPending
            ? l10n.expDelSuccessOccurrence
            : l10n.expDelSuccessOccurrenceUnlink;
      }
      return p.scope == ExpenseDeleteScope.futureUnpaid
          ? l10n.expDelSuccessRecurringFuture
          : l10n.expDelSuccessRecurringClose;
    }
    return l10n.expDelSuccessSingle;
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    final rec = expenseRecurringLabel(e, l10n);
    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => context.pop(),
        ),
        title: Text(l10n.expenseTitle),
        actions: [
          if (!readOnly)
            IconButton(
              tooltip: l10n.expenseEdit,
              icon: const Icon(Icons.edit_outlined),
              onPressed: () => context.push('/expenses/$expenseId/edit'),
            ),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.all(20),
        children: [
          if (readOnly)
            Padding(
              padding: const EdgeInsets.only(bottom: 16),
              child: Material(
                color: WellPaidColors.navy.withValues(alpha: 0.08),
                borderRadius: BorderRadius.circular(12),
                child: Padding(
                  padding: const EdgeInsets.all(12),
                  child: Row(
                    children: [
                      Icon(Icons.info_outline, color: WellPaidColors.navy.withValues(alpha: 0.8)),
                      const SizedBox(width: 10),
                      Expanded(
                        child: Text(
                          l10n.expenseReadOnlyBanner,
                          style: TextStyle(
                            color: WellPaidColors.navy.withValues(alpha: 0.85),
                            fontSize: 13,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          Text(
            formatBrlFromCents(e.amountCents),
            style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                  fontWeight: FontWeight.w800,
                  color: WellPaidColors.navy,
                ),
          ),
          const SizedBox(height: 16),
          Text(
            e.description,
            style: Theme.of(context).textTheme.titleMedium?.copyWith(
                  color: WellPaidColors.navy,
                ),
          ),
          const SizedBox(height: 8),
          Chip(
            label: Text(e.categoryName),
            backgroundColor: WellPaidColors.creamMuted,
          ),
          const SizedBox(height: 16),
          _row(context, l10n.expenseCompetence, ExpenseDetailPage._dmY(e.expenseDate)),
          _row(
            context,
            l10n.expenseDue,
            e.dueDate == null ? l10n.noneDash : ExpenseDetailPage._dmY(e.dueDate!),
          ),
          _row(
            context,
            l10n.expenseStatusLabel,
            e.isPending ? l10n.expenseStatusPending : l10n.expenseStatusPaid,
          ),
          if (e.isInstallmentPlan)
            _row(
              context,
              l10n.expenseInstallmentsRow,
              '${e.installmentNumber} de ${e.installmentTotal}',
            ),
          if (rec != null)
            _row(context, l10n.expenseRecurrence, rec),
          if (e.isShared)
            _row(
              context,
              l10n.expenseShare,
              e.sharedWithLabel != null && e.sharedWithLabel!.isNotEmpty
                  ? l10n.expenseShareWith(e.sharedWithLabel!)
                  : l10n.expenseShareFamily,
            ),
          const SizedBox(height: 32),
          if (!readOnly && e.isPending) ...[
            FilledButton.icon(
              onPressed: () => unawaited(_pay(context, ref)),
              icon: const Icon(Icons.payment_outlined),
              label: Text(l10n.expenseMarkPaid),
            ),
            const SizedBox(height: 12),
          ],
          if (!readOnly) ...[
            OutlinedButton.icon(
              onPressed: () => context.push('/expenses/$expenseId/edit'),
              icon: const Icon(Icons.edit_outlined),
              label: Text(l10n.expenseEdit),
            ),
            const SizedBox(height: 12),
            OutlinedButton.icon(
              onPressed: () => unawaited(_delete(context, ref)),
              style: OutlinedButton.styleFrom(
                foregroundColor: const Color(0xFFB71C1C),
                side: const BorderSide(color: Color(0xFFB71C1C)),
              ),
              icon: const Icon(Icons.delete_outline),
              label: Text(l10n.expenseDelete),
            ),
          ],
        ],
      ),
    );
  }

  static Widget _row(BuildContext context, String k, String v) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 140,
            child: Text(
              k,
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: WellPaidColors.navy.withValues(alpha: 0.65),
                  ),
            ),
          ),
          Expanded(
            child: Text(
              v,
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    fontWeight: FontWeight.w600,
                    color: WellPaidColors.navy,
                  ),
            ),
          ),
        ],
      ),
    );
  }
}
