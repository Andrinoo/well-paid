import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:phosphor_flutter/phosphor_flutter.dart';

import '../../../core/format/brl_cents.dart';
import '../../../core/l10n/context_l10n.dart';
import '../../../core/network/dio_client.dart';
import '../application/expenses_providers.dart';

class InstallmentPlanPage extends ConsumerWidget {
  const InstallmentPlanPage({super.key, required this.groupId});

  final String groupId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    final async = ref.watch(installmentPlanExpensesProvider(groupId));

    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(PhosphorIconsRegular.arrowLeft),
          onPressed: () => context.pop(),
        ),
        title: Text(l10n.pcInstallmentPlanTitle),
      ),
      body: async.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Text(messageFromDio(e, l10n) ?? '$e'),
          ),
        ),
        data: (items) {
          if (items.isEmpty) {
            return const Center(child: Text('Sem parcelas para este plano.'));
          }
          items.sort((a, b) => a.installmentNumber.compareTo(b.installmentNumber));
          return ListView.builder(
            padding: const EdgeInsets.all(16),
            itemCount: items.length,
            itemBuilder: (context, i) {
              final e = items[i];
              return Card(
                child: ListTile(
                  title: Text(e.description),
                  subtitle: Text(
                    'Parcela ${e.installmentNumber} de ${e.installmentTotal} · ${e.status}',
                  ),
                  trailing: Text(
                    formatBrlFromCents(e.amountCents),
                    style: const TextStyle(fontWeight: FontWeight.w700),
                  ),
                  onTap: () => context.push('/expenses/${e.id}'),
                ),
              );
            },
          );
        },
      ),
    );
  }
}
