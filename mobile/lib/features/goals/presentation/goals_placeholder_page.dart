import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/format/brl_cents.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/theme/well_paid_colors.dart';
import '../application/goals_providers.dart';

/// Metas §5.7 — listagem mínima real com GET /goals.
class GoalsPlaceholderPage extends ConsumerWidget {
  const GoalsPlaceholderPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final async = ref.watch(goalsListProvider);
    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => context.pop(),
        ),
        title: const Text('Metas'),
        actions: [
          IconButton(
            tooltip: 'Atualizar',
            onPressed: () => ref.invalidate(goalsListProvider),
            icon: const Icon(Icons.refresh),
          ),
        ],
      ),
      body: async.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Text(
              messageFromDio(e) ?? 'Erro ao carregar metas.',
              textAlign: TextAlign.center,
            ),
          ),
        ),
        data: (items) {
          if (items.isEmpty) {
            return Center(
              child: Padding(
                padding: const EdgeInsets.all(24),
                child: Text(
                  'Sem metas ativas ainda.',
                  style: TextStyle(
                    color: WellPaidColors.navy.withValues(alpha: 0.8),
                  ),
                ),
              ),
            );
          }
          return ListView.separated(
            padding: const EdgeInsets.all(16),
            itemCount: items.length,
            separatorBuilder: (_, index) => const SizedBox(height: 10),
            itemBuilder: (context, i) {
              final g = items[i];
              final p = g.targetCents > 0
                  ? (g.currentCents / g.targetCents).clamp(0.0, 1.0)
                  : 0.0;
              return Card(
                elevation: 0,
                color: WellPaidColors.creamMuted.withValues(alpha: 0.9),
                child: Padding(
                  padding: const EdgeInsets.all(14),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        g.isMine ? g.title : '${g.title} (família)',
                        style: Theme.of(context).textTheme.titleMedium?.copyWith(
                              color: WellPaidColors.navy,
                              fontWeight: FontWeight.w700,
                            ),
                      ),
                      const SizedBox(height: 8),
                      ClipRRect(
                        borderRadius: BorderRadius.circular(6),
                        child: LinearProgressIndicator(
                          value: p,
                          minHeight: 8,
                          backgroundColor: WellPaidColors.navy.withValues(alpha: 0.1),
                          color: WellPaidColors.gold,
                        ),
                      ),
                      const SizedBox(height: 6),
                      Text(
                        '${formatBrlFromCents(g.currentCents)} / ${formatBrlFromCents(g.targetCents)}',
                        style: Theme.of(context).textTheme.bodySmall?.copyWith(
                              color: WellPaidColors.navy.withValues(alpha: 0.75),
                            ),
                      ),
                    ],
                  ),
                ),
              );
            },
          );
        },
      ),
    );
  }
}
