import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/format/brl_cents.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/theme/well_paid_colors.dart';
import '../../dashboard/application/dashboard_providers.dart';
import '../application/expenses_providers.dart';
import '../domain/expense_item.dart';

class ExpenseDetailPage extends ConsumerWidget {
  const ExpenseDetailPage({super.key, required this.expenseId});

  final String expenseId;

  static String _dmY(DateTime d) =>
      '${d.day.toString().padLeft(2, '0')}/${d.month.toString().padLeft(2, '0')}/${d.year}';

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final async = ref.watch(expenseDetailProvider(expenseId));

    return async.when(
      loading: () => Scaffold(
        appBar: AppBar(
          leading: IconButton(
            icon: const Icon(Icons.arrow_back),
            onPressed: () => context.pop(),
          ),
          title: const Text('Despesa'),
        ),
        body: const Center(child: CircularProgressIndicator()),
      ),
      error: (e, _) => Scaffold(
        appBar: AppBar(
          leading: IconButton(
            icon: const Icon(Icons.arrow_back),
            onPressed: () => context.pop(),
          ),
          title: const Text('Despesa'),
        ),
        body: Center(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Text(
              messageFromDio(e) ?? 'Erro ao carregar.',
              textAlign: TextAlign.center,
            ),
          ),
        ),
      ),
      data: (e) => _DetailBody(expenseId: expenseId, e: e),
    );
  }
}

class _DetailBody extends ConsumerWidget {
  const _DetailBody({required this.expenseId, required this.e});

  final String expenseId;
  final ExpenseItem e;

  Future<void> _pay(BuildContext context, WidgetRef ref) async {
    final messenger = ScaffoldMessenger.of(context);
    try {
      await ref.read(expensesRepositoryProvider).payExpense(e.id);
      ref.invalidate(expenseDetailProvider(expenseId));
      ref.invalidate(expensesListProvider);
      ref.invalidate(dashboardOverviewProvider);
      if (context.mounted) {
        messenger.showSnackBar(
          const SnackBar(content: Text('Marcada como paga.')),
        );
      }
    } catch (err) {
      if (context.mounted) {
        messenger.showSnackBar(
          SnackBar(content: Text(messageFromDio(err) ?? 'Erro.')),
        );
      }
    }
  }

  Future<void> _delete(BuildContext context, WidgetRef ref) async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Eliminar despesa?'),
        content: const Text('Esta ação não pode ser anulada.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: const Text('Cancelar'),
          ),
          FilledButton(
            style: FilledButton.styleFrom(
              backgroundColor: const Color(0xFFB71C1C),
            ),
            onPressed: () => Navigator.pop(ctx, true),
            child: const Text('Eliminar'),
          ),
        ],
      ),
    );
    if (ok != true || !context.mounted) return;

    final messenger = ScaffoldMessenger.of(context);
    try {
      await ref.read(expensesRepositoryProvider).deleteExpense(e.id);
      ref.invalidate(expensesListProvider);
      ref.invalidate(dashboardOverviewProvider);
      ref.invalidate(expenseDetailProvider(expenseId));
      if (context.mounted) {
        messenger.showSnackBar(
          const SnackBar(content: Text('Despesa eliminada.')),
        );
        context.pop();
      }
    } catch (err) {
      if (context.mounted) {
        messenger.showSnackBar(
          SnackBar(content: Text(messageFromDio(err) ?? 'Erro ao eliminar.')),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => context.pop(),
        ),
        title: const Text('Despesa'),
        actions: [
          IconButton(
            tooltip: 'Editar',
            icon: const Icon(Icons.edit_outlined),
            onPressed: () => context.push('/expenses/$expenseId/edit'),
          ),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.all(20),
        children: [
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
          _row(context, 'Data (competência)', ExpenseDetailPage._dmY(e.expenseDate)),
          _row(
            context,
            'Vencimento',
            e.dueDate == null ? '—' : ExpenseDetailPage._dmY(e.dueDate!),
          ),
          _row(
            context,
            'Estado',
            e.isPending ? 'Pendente' : 'Paga',
          ),
          if (e.isInstallmentPlan)
            _row(
              context,
              'Parcelas',
              '${e.installmentNumber} de ${e.installmentTotal}',
            ),
          if (e.recurringLabelPt != null)
            _row(context, 'Recorrência', e.recurringLabelPt!),
          const SizedBox(height: 32),
          if (e.isPending) ...[
            FilledButton.icon(
              onPressed: () => unawaited(_pay(context, ref)),
              icon: const Icon(Icons.payment_outlined),
              label: const Text('Marcar como paga'),
            ),
            const SizedBox(height: 12),
          ],
          OutlinedButton.icon(
            onPressed: () => context.push('/expenses/$expenseId/edit'),
            icon: const Icon(Icons.edit_outlined),
            label: const Text('Editar'),
          ),
          const SizedBox(height: 12),
          OutlinedButton.icon(
            onPressed: () => unawaited(_delete(context, ref)),
            style: OutlinedButton.styleFrom(
              foregroundColor: const Color(0xFFB71C1C),
              side: const BorderSide(color: Color(0xFFB71C1C)),
            ),
            icon: const Icon(Icons.delete_outline),
            label: const Text('Eliminar'),
          ),
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
