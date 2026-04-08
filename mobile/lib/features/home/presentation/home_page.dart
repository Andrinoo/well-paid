import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/network/dio_client.dart';
import '../../../core/theme/well_paid_colors.dart';
import '../../auth/application/auth_notifier.dart';
import '../../dashboard/application/dashboard_providers.dart';
import '../../dashboard/presentation/dashboard_scroll_content.dart';
import '../../dashboard/presentation/period_selector_bar.dart';

class HomePage extends ConsumerWidget {
  const HomePage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final overview = ref.watch(dashboardOverviewProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Dashboard'),
        actions: [
          IconButton(
            tooltip: 'Atualizar dashboard',
            onPressed: () => ref.invalidate(dashboardOverviewProvider),
            icon: const Icon(Icons.refresh),
          ),
          IconButton(
            tooltip: 'Sair',
            onPressed: () async {
              await ref.read(authNotifierProvider.notifier).logout();
            },
            icon: const Icon(Icons.logout),
          ),
        ],
      ),
      floatingActionButton: Semantics(
        label: 'Nova despesa',
        button: true,
        child: FloatingActionButton(
          onPressed: () => context.push('/expenses/new'),
          tooltip: 'Nova despesa',
          backgroundColor: WellPaidColors.gold,
          foregroundColor: WellPaidColors.navy,
          child: const Icon(Icons.add),
        ),
      ),
      body: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Container(
            decoration: BoxDecoration(
              gradient: LinearGradient(
                colors: [
                  WellPaidColors.navy.withValues(alpha: 0.08),
                  WellPaidColors.gold.withValues(alpha: 0.1),
                ],
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
              ),
            ),
            child: Column(
              children: [
                const PeriodSelectorBar(),
                Padding(
                  padding: const EdgeInsets.fromLTRB(16, 10, 16, 10),
                  child: Row(
                    children: [
                      Expanded(
                        child: _QuickActionCard(
                          icon: Icons.receipt_long_outlined,
                          label: 'Despesas',
                          color: WellPaidColors.navy,
                          onTap: () => context.push('/expenses'),
                        ),
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: _QuickActionCard(
                          icon: Icons.savings_outlined,
                          label: 'Proventos',
                          color: WellPaidColors.goldPressed,
                          onTap: () {
                            ScaffoldMessenger.of(context).showSnackBar(
                              const SnackBar(
                                content: Text('Proventos será a próxima entrega.'),
                              ),
                            );
                          },
                        ),
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: _QuickActionCard(
                          icon: Icons.flag_outlined,
                          label: 'Metas',
                          color: WellPaidColors.navy,
                          onTap: () => context.push('/goals'),
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
          Expanded(
            child: overview.when(
              skipLoadingOnReload: true,
              loading: () => const Center(child: CircularProgressIndicator()),
              error: (e, _) => Center(
                child: Padding(
                  padding: const EdgeInsets.all(24),
                  child: Text(
                    messageFromDio(e) ?? 'Erro ao carregar o dashboard.',
                    textAlign: TextAlign.center,
                  ),
                ),
              ),
              data: (d) => RefreshIndicator(
                color: WellPaidColors.navy,
                onRefresh: () async {
                  ref.invalidate(dashboardOverviewProvider);
                  await ref.read(dashboardOverviewProvider.future);
                },
                child: DashboardScrollContent(data: d),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _QuickActionCard extends StatelessWidget {
  const _QuickActionCard({
    required this.icon,
    required this.label,
    required this.color,
    required this.onTap,
  });

  final IconData icon;
  final String label;
  final Color color;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      button: true,
      label: label,
      child: InkWell(
        borderRadius: BorderRadius.circular(14),
        onTap: onTap,
        child: TweenAnimationBuilder<double>(
          tween: Tween(begin: 0.96, end: 1),
          duration: const Duration(milliseconds: 240),
          curve: Curves.easeOutCubic,
          builder: (context, scale, _) => Transform.scale(
            scale: scale,
            child: Ink(
              padding: const EdgeInsets.symmetric(vertical: 10, horizontal: 10),
              decoration: BoxDecoration(
                color: WellPaidColors.creamMuted.withValues(alpha: 0.92),
                borderRadius: BorderRadius.circular(14),
                border: Border.all(color: color.withValues(alpha: 0.18)),
              ),
              child: Column(
                children: [
                  Icon(icon, color: color),
                  const SizedBox(height: 6),
                  Text(
                    label,
                    style: Theme.of(context).textTheme.labelLarge?.copyWith(
                          color: WellPaidColors.navy,
                          fontWeight: FontWeight.w700,
                        ),
                    textAlign: TextAlign.center,
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
