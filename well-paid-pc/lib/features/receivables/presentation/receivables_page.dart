import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:phosphor_flutter/phosphor_flutter.dart';

import '../../../core/format/brl_cents.dart';
import '../../../core/l10n/context_l10n.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/theme/well_paid_colors.dart';
import '../application/receivables_providers.dart';
import '../domain/receivable_item.dart';

class ReceivablesPage extends ConsumerWidget {
  const ReceivablesPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    final async = ref.watch(receivablesBundleProvider);

    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(PhosphorIconsRegular.arrowLeft),
          onPressed: () => context.pop(),
        ),
        title: Text(l10n.pcNavReceivables),
      ),
      body: async.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Text(messageFromDio(e, l10n) ?? '$e'),
          ),
        ),
        data: (bundle) {
          final pendingCreditor =
              bundle.asCreditor.where((r) => r.settledAt == null).toList();
          final pendingDebtor =
              bundle.asDebtor.where((r) => r.settledAt == null).toList();
          if (pendingCreditor.isEmpty && pendingDebtor.isEmpty) {
            return Center(child: Text(l10n.pcReceivablesEmpty));
          }
          return ListView(
            padding: const EdgeInsets.all(16),
            children: [
              if (pendingCreditor.isNotEmpty) ...[
                Text(
                  'A receber (credor)',
                  style: Theme.of(context).textTheme.titleMedium?.copyWith(
                        fontWeight: FontWeight.w800,
                        color: WellPaidColors.navy,
                      ),
                ),
                const SizedBox(height: 8),
                ...pendingCreditor.map(
                  (r) => _ReceivableTile(
                    item: r,
                    subtitle: r.debtorDisplayName ?? '—',
                    onSettle: () => _confirmSettle(context, ref, r.id),
                  ),
                ),
                const SizedBox(height: 24),
              ],
              if (pendingDebtor.isNotEmpty) ...[
                Text(
                  'A pagar (devedor)',
                  style: Theme.of(context).textTheme.titleMedium?.copyWith(
                        fontWeight: FontWeight.w800,
                        color: WellPaidColors.navy,
                      ),
                ),
                const SizedBox(height: 8),
                ...pendingDebtor.map(
                  (r) => _ReceivableTile(
                    item: r,
                    subtitle: r.creditorDisplayName ?? '—',
                    onSettle: () => _confirmSettle(context, ref, r.id),
                  ),
                ),
              ],
            ],
          );
        },
      ),
    );
  }

  Future<void> _confirmSettle(
    BuildContext context,
    WidgetRef ref,
    String id,
  ) async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Liquidar'),
        content: const Text(
          'Marcar este valor como liquidado?',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: const Text('Cancelar'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(ctx, true),
            child: const Text('Liquidar'),
          ),
        ],
      ),
    );
    if (ok != true || !context.mounted) return;
    try {
      await ref.read(receivablesRepositoryProvider).settle(id);
      ref.invalidate(receivablesBundleProvider);
    } catch (e) {
      if (!context.mounted) return;
      final loc = context.l10n;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(messageFromDio(e, loc) ?? '$e')),
      );
    }
  }
}

class _ReceivableTile extends StatelessWidget {
  const _ReceivableTile({
    required this.item,
    required this.subtitle,
    required this.onSettle,
  });

  final ReceivableItem item;
  final String subtitle;
  final VoidCallback onSettle;

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      child: ListTile(
        title: Text(
          formatBrlFromCents(item.amountCents),
          style: const TextStyle(fontWeight: FontWeight.w700),
        ),
        subtitle: Text(
          '$subtitle · até ${_isoDate(item.settleBy)} · ${item.status}',
        ),
        trailing: item.settledAt == null
            ? TextButton(onPressed: onSettle, child: const Text('Liquidar'))
            : null,
      ),
    );
  }

  String _isoDate(DateTime d) =>
      '${d.day.toString().padLeft(2, '0')}/${d.month.toString().padLeft(2, '0')}/${d.year}';
}
