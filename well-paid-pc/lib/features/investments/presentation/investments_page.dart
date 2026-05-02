import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:phosphor_flutter/phosphor_flutter.dart';

import '../../../core/format/brl_cents.dart';
import '../../../core/l10n/context_l10n.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/theme/well_paid_colors.dart';
import '../application/investments_providers.dart';

class InvestmentsPage extends ConsumerWidget {
  const InvestmentsPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    final overview = ref.watch(investmentOverviewProvider);
    final positions = ref.watch(investmentPositionsProvider);

    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(PhosphorIconsRegular.arrowLeft),
          onPressed: () => context.pop(),
        ),
        title: Text(l10n.pcInvestmentsTitle),
      ),
      body: RefreshIndicator(
        color: WellPaidColors.navy,
        onRefresh: () async {
          ref.invalidate(investmentOverviewProvider);
          ref.invalidate(investmentPositionsProvider);
          await ref.read(investmentOverviewProvider.future);
          await ref.read(investmentPositionsProvider.future);
        },
        child: CustomScrollView(
          physics: const AlwaysScrollableScrollPhysics(),
          slivers: [
            SliverPadding(
              padding: const EdgeInsets.all(16),
              sliver: SliverToBoxAdapter(
                child: overview.when(
                  loading: () => const LinearProgressIndicator(),
                  error: (e, _) => Text(
                    messageFromDio(e, l10n) ?? '$e',
                  ),
                  data: (o) => Card(
                    color: WellPaidColors.creamMuted.withValues(alpha: 0.95),
                    child: Padding(
                      padding: const EdgeInsets.all(16),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            'Total alocado',
                            style: TextStyle(
                              color: WellPaidColors.navy.withValues(alpha: 0.65),
                            ),
                          ),
                          Text(
                            formatBrlFromCents(o.totalAllocatedCents),
                            style: const TextStyle(
                              fontSize: 22,
                              fontWeight: FontWeight.w800,
                            ),
                          ),
                          const SizedBox(height: 12),
                          Text(
                            'Rendimento acumulado (est.)',
                            style: TextStyle(
                              color: WellPaidColors.navy.withValues(alpha: 0.65),
                            ),
                          ),
                          Text(formatBrlFromCents(o.totalYieldCents)),
                          const SizedBox(height: 8),
                          Text(
                            'Yield mensal est.: ${formatBrlFromCents(o.estimatedMonthlyYieldCents)}',
                            style: const TextStyle(fontSize: 13),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
              ),
            ),
            SliverPadding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              sliver: positions.when(
                loading: () => const SliverToBoxAdapter(
                  child: Center(child: CircularProgressIndicator()),
                ),
                error: (e, _) => SliverToBoxAdapter(
                  child: Text(messageFromDio(e, l10n) ?? '$e'),
                ),
                data: (list) {
                  if (list.isEmpty) {
                    return const SliverToBoxAdapter(
                      child: Padding(
                        padding: EdgeInsets.all(24),
                        child: Text('Sem posições. Cria posições na API ou app móvel.'),
                      ),
                    );
                  }
                  return SliverList(
                    delegate: SliverChildBuilderDelegate(
                      (context, i) {
                        final p = list[i];
                        return Card(
                          child: ListTile(
                            title: Text(p.name),
                            subtitle: Text(
                              '${p.instrumentType} · ${(p.annualRateBps / 100).toStringAsFixed(2)}% a.a.',
                            ),
                            trailing: Text(
                              formatBrlFromCents(p.principalCents),
                              style:
                                  const TextStyle(fontWeight: FontWeight.w700),
                            ),
                            onTap: () => context.push('/investments/${p.id}/aporte'),
                          ),
                        );
                      },
                      childCount: list.length,
                    ),
                  );
                },
              ),
            ),
          ],
        ),
      ),
    );
  }
}
