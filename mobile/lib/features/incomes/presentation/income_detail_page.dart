import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/format/brl_cents.dart';
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
    final async = ref.watch(incomeDetailProvider(incomeId));

    return async.when(
      loading: () => Scaffold(
        appBar: AppBar(
          leading: IconButton(
            icon: const Icon(Icons.arrow_back),
            onPressed: () => context.pop(),
          ),
          title: const Text('Provento'),
        ),
        body: const Center(child: CircularProgressIndicator()),
      ),
      error: (e, _) => Scaffold(
        appBar: AppBar(
          leading: IconButton(
            icon: const Icon(Icons.arrow_back),
            onPressed: () => context.pop(),
          ),
          title: const Text('Provento'),
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
      data: (item) => _DetailScaffold(incomeId: incomeId, item: item),
    );
  }
}

class _DetailScaffold extends ConsumerWidget {
  const _DetailScaffold({required this.incomeId, required this.item});

  final String incomeId;
  final IncomeItem item;

  Future<void> _delete(BuildContext context, WidgetRef ref) async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Eliminar provento?'),
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
      await ref.read(incomesRepositoryProvider).deleteIncome(incomeId);
      ref.invalidate(incomesListProvider);
      ref.invalidate(dashboardOverviewProvider);
      if (context.mounted) {
        messenger.showSnackBar(const SnackBar(content: Text('Eliminado.')));
        context.pop();
      }
    } catch (err) {
      if (context.mounted) {
        messenger.showSnackBar(
          SnackBar(content: Text(messageFromDio(err) ?? 'Erro.')),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final readOnly = !item.isMine;

    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => context.pop(),
        ),
        title: const Text('Provento'),
        actions: [
          if (!readOnly)
            IconButton(
              tooltip: 'Editar',
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
                          'Provento de outro membro da família — só podes ver.',
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
          _row(context, 'Tipo', item.categoryName),
          _row(context, 'Data (competência)', IncomeDetailPage._dmY(item.incomeDate)),
          if (item.notes != null && item.notes!.trim().isNotEmpty)
            _row(context, 'Notas', item.notes!.trim()),
          const SizedBox(height: 32),
          if (!readOnly) ...[
            OutlinedButton.icon(
              onPressed: () => context.push('/incomes/$incomeId/edit'),
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
