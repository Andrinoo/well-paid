import 'package:flutter/material.dart';
import 'package:phosphor_flutter/phosphor_flutter.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/format/brl_cents.dart';
import '../../../core/l10n/context_l10n.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/theme/well_paid_colors.dart';
import '../../auth/application/auth_notifier.dart';
import '../../dashboard/application/dashboard_providers.dart';
import '../../dashboard/presentation/dashboard_scroll_content.dart';
import '../../dashboard/presentation/period_selector_bar.dart';

enum _HomeMenuAction { settings, family, security, refresh, logout }

class HomePage extends ConsumerWidget {
  const HomePage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final overview = ref.watch(dashboardOverviewProvider);
    final l10n = context.l10n;
    final headerOverview = overview.valueOrNull;
    final balanceLine = headerOverview != null
        ? formatBrlFromCents(headerOverview.monthBalanceCents)
        : null;

    return Scaffold(
      backgroundColor: WellPaidColors.cream,
      body: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          DecoratedBox(
            decoration: const BoxDecoration(
              gradient: LinearGradient(
                begin: Alignment.topCenter,
                end: Alignment.bottomCenter,
                colors: [WellPaidColors.navyDeep, WellPaidColors.navy],
              ),
            ),
            child: SafeArea(
              bottom: false,
              child: Padding(
                padding: const EdgeInsets.fromLTRB(0, 0, 0, 2),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Align(
                      alignment: Alignment.centerRight,
                      child: PopupMenuButton<_HomeMenuAction>(
                        tooltip: l10n.menuMoreTooltip,
                        icon: Icon(
                          PhosphorIconsRegular.dotsThreeVertical,
                          size: 22,
                          color: WellPaidColors.cream.withValues(alpha: 0.95),
                        ),
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
                                      onPressed: () =>
                                          Navigator.of(ctx).pop(false),
                                      child: Text(l10n.cancel),
                                    ),
                                    FilledButton(
                                      onPressed: () =>
                                          Navigator.of(ctx).pop(true),
                                      child: Text(l10n.tooltipLogout),
                                    ),
                                  ],
                                ),
                              );
                              if (ok == true && context.mounted) {
                                await ref
                                    .read(authNotifierProvider.notifier)
                                    .logout();
                              }
                              break;
                          }
                        },
                        itemBuilder: (context) => [
                          PopupMenuItem(
                            value: _HomeMenuAction.settings,
                            child: _MenuRow(
                              icon: PhosphorIconsRegular.gearSix,
                              label: l10n.settingsTitle,
                            ),
                          ),
                          PopupMenuItem(
                            value: _HomeMenuAction.family,
                            child: _MenuRow(
                              icon: PhosphorIconsRegular.usersThree,
                              label: l10n.tooltipFamily,
                            ),
                          ),
                          PopupMenuItem(
                            value: _HomeMenuAction.security,
                            child: _MenuRow(
                              icon: PhosphorIconsRegular.lock,
                              label: l10n.tooltipSecurity,
                            ),
                          ),
                          PopupMenuItem(
                            value: _HomeMenuAction.refresh,
                            child: _MenuRow(
                              icon: PhosphorIconsRegular.arrowsClockwise,
                              label: l10n.tooltipRefreshDashboard,
                            ),
                          ),
                          const PopupMenuDivider(),
                          PopupMenuItem(
                            value: _HomeMenuAction.logout,
                            child: _MenuRow(
                              icon: PhosphorIconsRegular.signOut,
                              label: l10n.tooltipLogout,
                            ),
                          ),
                        ],
                      ),
                    ),
                    Padding(
                      padding: const EdgeInsets.fromLTRB(6, 2, 6, 0),
                      child: Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Expanded(
                            child: headerOverview != null
                                ? _HomeHeaderMoneyCol(
                                    label: l10n.dashIncome,
                                    value: formatBrlFromCents(
                                      headerOverview.monthIncomeCents,
                                    ),
                                    emphasize: false,
                                    crossAxisAlignment:
                                        CrossAxisAlignment.start,
                                    compact: true,
                                  )
                                : const SizedBox.shrink(),
                          ),
                          Expanded(
                            flex: 2,
                            child: Column(
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                Text(
                                  l10n.dashBalance,
                                  textAlign: TextAlign.center,
                                  style: Theme.of(context).textTheme.labelSmall
                                      ?.copyWith(
                                        color: WellPaidColors.cream.withValues(
                                          alpha: 0.68,
                                        ),
                                        fontWeight: FontWeight.w600,
                                        fontSize: 10,
                                      ),
                                ),
                                AnimatedSwitcher(
                                  duration: const Duration(milliseconds: 280),
                                  switchInCurve: Curves.easeOutCubic,
                                  switchOutCurve: Curves.easeInCubic,
                                  child: balanceLine != null
                                      ? Text(
                                          key: ValueKey<String>(balanceLine),
                                          balanceLine,
                                          textAlign: TextAlign.center,
                                          maxLines: 1,
                                          overflow: TextOverflow.ellipsis,
                                          style: Theme.of(context)
                                              .textTheme
                                              .titleSmall
                                              ?.copyWith(
                                                fontWeight: FontWeight.w800,
                                                color: WellPaidColors.gold,
                                                letterSpacing: -0.3,
                                                fontSize: 15,
                                              ),
                                        )
                                      : SizedBox(
                                          key: const ValueKey<String>(
                                            'balance-loading',
                                          ),
                                          height: 17,
                                          child: Center(
                                            child: SizedBox(
                                              width: 15,
                                              height: 15,
                                              child: CircularProgressIndicator(
                                                strokeWidth: 1.75,
                                                color: WellPaidColors.cream
                                                    .withValues(alpha: 0.85),
                                              ),
                                            ),
                                          ),
                                        ),
                                ),
                              ],
                            ),
                          ),
                          Expanded(
                            child: headerOverview != null
                                ? _HomeHeaderMoneyCol(
                                    label: l10n.dashExpenses,
                                    value: formatBrlFromCents(
                                      headerOverview.monthExpenseTotalCents,
                                    ),
                                    emphasize: true,
                                    crossAxisAlignment: CrossAxisAlignment.end,
                                    compact: true,
                                  )
                                : const SizedBox.shrink(),
                          ),
                        ],
                      ),
                    ),
                    Center(
                      child: PeriodSelectorBar(
                        variant: PeriodSelectorBarVariant.dark,
                        dense: true,
                        ultraDense: true,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
          Expanded(
            child: overview.when(
              skipLoadingOnReload: true,
              loading: () => const _HomeDashboardLoadingSkeleton(),
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

class _HomeHeaderMoneyCol extends StatelessWidget {
  const _HomeHeaderMoneyCol({
    required this.label,
    required this.value,
    required this.emphasize,
    this.crossAxisAlignment = CrossAxisAlignment.start,
    this.compact = false,
  });

  final String label;
  final String value;
  final bool emphasize;
  final CrossAxisAlignment crossAxisAlignment;
  final bool compact;

  @override
  Widget build(BuildContext context) {
    final valueColor = emphasize
        ? WellPaidColors.gold
        : WellPaidColors.cream.withValues(alpha: 0.95);
    final alignEnd = crossAxisAlignment == CrossAxisAlignment.end;
    return Column(
      crossAxisAlignment: crossAxisAlignment,
      mainAxisSize: MainAxisSize.min,
      children: [
        Text(
          label,
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
          textAlign: alignEnd ? TextAlign.right : TextAlign.left,
          style: Theme.of(context).textTheme.labelSmall?.copyWith(
            color: WellPaidColors.cream.withValues(alpha: 0.65),
            fontWeight: FontWeight.w600,
            fontSize: compact ? 9.5 : null,
          ),
        ),
        Text(
          value,
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
          textAlign: alignEnd ? TextAlign.right : TextAlign.left,
          style: Theme.of(context).textTheme.labelLarge?.copyWith(
            fontWeight: FontWeight.w800,
            color: valueColor,
            letterSpacing: -0.15,
            fontSize: compact ? 11 : 13,
          ),
        ),
      ],
    );
  }
}

/// Placeholders alinhados ao layout real do dashboard (carregamento inicial).
class _HomeDashboardLoadingSkeleton extends StatelessWidget {
  const _HomeDashboardLoadingSkeleton();

  @override
  Widget build(BuildContext context) {
    final base = WellPaidColors.navy.withValues(alpha: 0.07);
    Widget bar({required double h, double r = 16}) {
      return Container(
        height: h,
        decoration: BoxDecoration(
          color: base,
          borderRadius: BorderRadius.circular(r),
        ),
      );
    }

    return ListView(
      physics: const AlwaysScrollableScrollPhysics(),
      padding: const EdgeInsets.fromLTRB(10, 2, 10, 120),
      children: [
        bar(h: 40, r: 14),
        const SizedBox(height: 6),
        bar(h: 48, r: 12),
        const SizedBox(height: 8),
        bar(h: 300, r: 20),
      ],
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
        Icon(
          icon,
          size: 22,
          color: WellPaidColors.navy.withValues(alpha: 0.85),
        ),
        const SizedBox(width: 12),
        Expanded(child: Text(label)),
      ],
    );
  }
}
