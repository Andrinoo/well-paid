import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/format/brl_cents.dart';
import '../../../core/l10n/context_l10n.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/theme/well_paid_colors.dart';
import '../../dashboard/application/dashboard_providers.dart';
import '../application/incomes_providers.dart';
import '../domain/income_item.dart';

class IncomeDetailPage extends ConsumerWidget {
  const IncomeDetailPage({super.key, required this.incomeId});

  final String incomeId;

  static String _dmY(DateTime d) =>
      '${d.day.toString().padLeft(2, '0')}/${d.month.toString().padLeft(2, '0')}/${d.year}';

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    final async = ref.watch(incomeDetailProvider(incomeId));

    return async.when(
      loading: () => Scaffold(
        appBar: AppBar(
          leading: IconButton(
            icon: const Icon(Icons.arrow_back),
            onPressed: () => context.pop(),
          ),
          title: Text(l10n.incomeDetailTitle),
        ),
        body: const Center(child: CircularProgressIndicator()),
      ),
      error: (e, _) => Scaffold(
        appBar: AppBar(
          leading: IconButton(
            icon: const Icon(Icons.arrow_back),
            onPressed: () => context.pop(),
          ),
          title: Text(l10n.incomeDetailTitle),
        ),
        body: Center(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Text(
              messageFromDio(e, l10n) ?? l10n.incomesLoadError,
              textAlign: TextAlign.center,
            ),
          ),
        ),
      ),
      data: (item) => _DetailScaffold(incomeId: incomeId, item: item),
    );
  }
}

class _DetailScaffold extends ConsumerWidget {
  const _DetailScaffold({required this.incomeId, required this.item});

  final String incomeId;
  final IncomeItem item;

  Future<void> _delete(BuildContext context, WidgetRef ref) async {
    final l10n = context.l10n;
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(l10n.incomeDeleteTitle),
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
    if (ok != true || !context.mounted) return;
    final messenger = ScaffoldMessenger.of(context);
    try {
      await ref.read(incomesRepositoryProvider).deleteIncome(incomeId);
      ref.invalidate(incomesListProvider);
      ref.invalidate(dashboardOverviewProvider);
      if (context.mounted) {
        messenger.showSnackBar(SnackBar(content: Text(l10n.incomeDeletedSnackbar)));
        context.pop();
      }
    } catch (err) {
      if (context.mounted) {
        messenger.showSnackBar(
          SnackBar(content: Text(messageFromDio(err, l10n) ?? l10n.incomeDeleteError)),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    final readOnly = !item.isMine;

    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => context.pop(),
        ),
        title: Text(l10n.incomeDetailTitle),
        actions: [
          if (!readOnly)
            IconButton(
              tooltip: l10n.expenseEdit,
              icon: const Icon(Icons.edit_outlined),
              onPressed: () => context.push('/incomes/$incomeId/edit'),
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
                      Icon(Icons.info_outline,
                          color: WellPaidColors.navy.withValues(alpha: 0.8)),
                      const SizedBox(width: 10),
                      Expanded(
                        child: Text(
                          l10n.incomeReadOnlyBanner,
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
            formatBrlFromCents(item.amountCents),
            style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                  fontWeight: FontWeight.w800,
                  color: const Color(0xFF2E7D32),
                ),
          ),
          const SizedBox(height: 16),
          Text(
            item.description,
            style: Theme.of(context).textTheme.titleMedium?.copyWith(
                  color: WellPaidColors.navy,
                ),
          ),
          const SizedBox(height: 12),
          _row(context, l10n.incomeDetailTypeLabel, item.categoryName),
          _row(
            context,
            l10n.incomeDetailDateCompetenceLabel,
            IncomeDetailPage._dmY(item.incomeDate),
          ),
          if (item.notes != null && item.notes!.trim().isNotEmpty)
            _row(context, l10n.incomeDetailNotesLabel, item.notes!.trim()),
          const SizedBox(height: 32),
          if (!readOnly) ...[
            OutlinedButton.icon(
              onPressed: () => context.push('/incomes/$incomeId/edit'),
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
            width: 120,
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
