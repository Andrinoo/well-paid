import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:phosphor_flutter/phosphor_flutter.dart';

import '../../../core/format/brl_cents.dart';
import '../../../core/l10n/context_l10n.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/theme/well_paid_colors.dart';
import '../application/emergency_plans_providers.dart';

class EmergencyPlansListPage extends ConsumerWidget {
  const EmergencyPlansListPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    final async = ref.watch(emergencyPlansListProvider);

    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(PhosphorIconsRegular.arrowLeft),
          onPressed: () => context.pop(),
        ),
        title: Text(l10n.pcPlansTitle),
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => context.push('/emergency-reserve/plan-new'),
        icon: const Icon(PhosphorIconsRegular.plus),
        label: const Text('Novo plano'),
      ),
      body: async.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(
          child: Text(messageFromDio(e, l10n) ?? '$e'),
        ),
        data: (plans) {
          if (plans.isEmpty) {
            return Center(
              child: Padding(
                padding: const EdgeInsets.all(24),
                child: Text(
                  l10n.pcPlansEmpty,
                  textAlign: TextAlign.center,
                ),
              ),
            );
          }
          return ListView.builder(
            padding: const EdgeInsets.all(16),
            itemCount: plans.length,
            itemBuilder: (context, i) {
              final p = plans[i];
              return Card(
                color: WellPaidColors.creamMuted.withValues(alpha: 0.9),
                child: ListTile(
                  title: Text(p.title),
                  subtitle: Text(
                    '${p.status} · meta mensal ${formatBrlFromCents(p.monthlyTargetCents)}',
                  ),
                  trailing: Text(
                    formatBrlFromCents(p.balanceCents),
                    style: const TextStyle(fontWeight: FontWeight.w700),
                  ),
                  onTap: () =>
                      context.push('/emergency-plan/${p.id}'),
                ),
              );
            },
          );
        },
      ),
    );
  }
}
