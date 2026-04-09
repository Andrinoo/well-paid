import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/l10n/context_l10n.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/theme/well_paid_colors.dart';
import '../../auth/application/auth_notifier.dart';
import '../../dashboard/application/dashboard_providers.dart';
import '../../dashboard/presentation/dashboard_scroll_content.dart';
import '../../dashboard/presentation/period_selector_bar.dart';

enum _HomeMenuAction {
  settings,
  family,
  security,
  shoppingLists,
  refresh,
  logout,
}

class HomePage extends ConsumerWidget {
  const HomePage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final overview = ref.watch(dashboardOverviewProvider);
    final l10n = context.l10n;

    return Scaffold(
      appBar: AppBar(
        title: Text(l10n.homeDashboardTitle),
        actions: [
          PopupMenuButton<_HomeMenuAction>(
            tooltip: l10n.menuMoreTooltip,
            icon: const Icon(Icons.more_vert),
            onSelected: (action) async {
              switch (action) {
                case _HomeMenuAction.settings:
                  if (context.mounted) context.push('/settings');
                  break;
                case _HomeMenuAction.family:
                  if (context.mounted) context.push('/family');
                  break;
                case _HomeMenuAction.security:
                  if (context.mounted) context.push('/security');
                  break;
                case _HomeMenuAction.shoppingLists:
                  if (context.mounted) context.push('/shopping-lists');
                  break;
                case _HomeMenuAction.refresh:
                  ref.invalidate(dashboardOverviewProvider);
                  ref.invalidate(dashboardCashflowProvider);
                  break;
                case _HomeMenuAction.logout:
                  final ok = await showDialog<bool>(
                    context: context,
                    builder: (ctx) => AlertDialog(
                      title: Text(l10n.logoutConfirmTitle),
                      content: Text(l10n.logoutConfirmBody),
                      actions: [
                        TextButton(
                          onPressed: () => Navigator.of(ctx).pop(false),
                          child: Text(l10n.cancel),
                        ),
                        FilledButton(
                          onPressed: () => Navigator.of(ctx).pop(true),
                          child: Text(l10n.tooltipLogout),
                        ),
                      ],
                    ),
                  );
                  if (ok == true && context.mounted) {
                    await ref.read(authNotifierProvider.notifier).logout();
                  }
                  break;
              }
            },
            itemBuilder: (context) => [
              PopupMenuItem(
                value: _HomeMenuAction.settings,
                child: _MenuRow(
                  icon: Icons.settings_outlined,
                  label: l10n.settingsTitle,
                ),
              ),
              PopupMenuItem(
                value: _HomeMenuAction.family,
                child: _MenuRow(
                  icon: Icons.group_outlined,
                  label: l10n.tooltipFamily,
                ),
              ),
              PopupMenuItem(
                value: _HomeMenuAction.security,
                child: _MenuRow(
                  icon: Icons.lock_outline,
                  label: l10n.tooltipSecurity,
                ),
              ),
              PopupMenuItem(
                value: _HomeMenuAction.shoppingLists,
                child: _MenuRow(
                  icon: Icons.shopping_cart_outlined,
                  label: l10n.shoppingListsMenuLabel,
                ),
              ),
              PopupMenuItem(
                value: _HomeMenuAction.refresh,
                child: _MenuRow(
                  icon: Icons.refresh,
                  label: l10n.tooltipRefreshDashboard,
                ),
              ),
              const PopupMenuDivider(),
              PopupMenuItem(
                value: _HomeMenuAction.logout,
                child: _MenuRow(
                  icon: Icons.logout,
                  label: l10n.tooltipLogout,
                ),
              ),
            ],
          ),
        ],
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
            child: const Column(
              children: [
                PeriodSelectorBar(),
                SizedBox(height: 8),
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
                    messageFromDio(e, l10n) ?? l10n.homeDashboardError,
                    textAlign: TextAlign.center,
                  ),
                ),
              ),
              data: (d) => RefreshIndicator(
                color: WellPaidColors.navy,
                onRefresh: () async {
                  ref.invalidate(dashboardOverviewProvider);
                  ref.invalidate(dashboardCashflowProvider);
                  await ref.read(dashboardOverviewProvider.future);
                  await ref.read(dashboardCashflowProvider.future);
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

class _MenuRow extends StatelessWidget {
  const _MenuRow({required this.icon, required this.label});

  final IconData icon;
  final String label;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Icon(icon, size: 22, color: WellPaidColors.navy.withValues(alpha: 0.85)),
        const SizedBox(width: 12),
        Expanded(child: Text(label)),
      ],
    );
  }
}
