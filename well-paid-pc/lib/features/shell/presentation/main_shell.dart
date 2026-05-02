import 'dart:async' show unawaited;

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:phosphor_flutter/phosphor_flutter.dart';

import '../../../core/locale/app_locale_provider.dart';
import '../../../core/l10n/context_l10n.dart';
import '../../../core/navigation/list_data_warmup.dart';
import '../../../core/notifications/goal_stall_reminder_service.dart';
import '../../../core/theme/well_paid_colors.dart';
import '../../goals/application/goals_providers.dart';
import '../../goals/domain/goal_item.dart';
import 'shell_quick_panel_desktop.dart';

/// Shell desktop com [NavigationRail] e painel de atalhos (Well Paid PC).
class MainShell extends ConsumerStatefulWidget {
  const MainShell({super.key, required this.navigationShell});

  final StatefulNavigationShell navigationShell;

  @override
  ConsumerState<MainShell> createState() => _MainShellState();
}

class _MainShellState extends ConsumerState<MainShell> {
  bool _quickPanelExpanded = true;

  void _setQuickExpanded(bool v) {
    if (_quickPanelExpanded == v) return;
    setState(() => _quickPanelExpanded = v);
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    ref.listen<AsyncValue<List<GoalItem>>>(goalsListProvider, (prev, next) {
      next.whenData((goals) {
        final loc =
            ref.read(appLocaleProvider).valueOrNull ?? const Locale('pt');
        unawaited(GoalStallReminderService.syncFromGoals(goals, locale: loc));
      });
    });

    final navigationShell = widget.navigationShell;
    final reduceMotion = MediaQuery.maybeOf(context)?.disableAnimations ?? false;
    final expandDuration = Duration(milliseconds: reduceMotion ? 1 : 220);

    final wideLabels =
        MediaQuery.sizeOf(context).width >= 520 ? NavigationRailLabelType.all : NavigationRailLabelType.selected;

    return Scaffold(
      body: Row(
        children: [
          NavigationRail(
            selectedIndex: navigationShell.currentIndex,
            onDestinationSelected: (i) =>
                _onDestinationSelected(navigationShell, i),
            labelType: wideLabels,
            backgroundColor: WellPaidColors.creamMuted.withValues(alpha: 0.98),
            indicatorColor: WellPaidColors.gold.withValues(alpha: 0.35),
            leading: Column(
              children: [
                const SizedBox(height: 12),
                IconButton(
                  tooltip: l10n.settingsTitle,
                  icon: Icon(
                    PhosphorIconsRegular.gear,
                    color: WellPaidColors.navy.withValues(alpha: 0.85),
                  ),
                  onPressed: () => context.push('/settings'),
                ),
                const SizedBox(height: 4),
                IconButton(
                  tooltip: _quickPanelExpanded
                      ? l10n.navQuickPanelToggleHint
                      : l10n.navQuickPanelToggleHint,
                  icon: Icon(
                    _quickPanelExpanded
                        ? PhosphorIconsRegular.caretUp
                        : PhosphorIconsRegular.caretDown,
                    color: WellPaidColors.navy.withValues(alpha: 0.55),
                  ),
                  onPressed: () => _setQuickExpanded(!_quickPanelExpanded),
                ),
                const SizedBox(height: 8),
              ],
            ),
            destinations: [
              NavigationRailDestination(
                icon: const Icon(PhosphorIconsRegular.house),
                selectedIcon: const Icon(PhosphorIconsFill.house),
                label: Text(l10n.navHome),
              ),
              NavigationRailDestination(
                icon: const Icon(PhosphorIconsRegular.receipt),
                selectedIcon: const Icon(PhosphorIconsFill.receipt),
                label: Text(l10n.navExpenses),
              ),
              NavigationRailDestination(
                icon: const Icon(PhosphorIconsRegular.coins),
                selectedIcon: const Icon(PhosphorIconsFill.coins),
                label: Text(l10n.navIncomes),
              ),
              NavigationRailDestination(
                icon: const Icon(PhosphorIconsRegular.flag),
                selectedIcon: const Icon(PhosphorIconsFill.flag),
                label: Text(l10n.navGoals),
              ),
              NavigationRailDestination(
                icon: const Icon(PhosphorIconsRegular.shield),
                selectedIcon: const Icon(PhosphorIconsFill.shield),
                label: Text(l10n.navReserve),
              ),
            ],
          ),
          const VerticalDivider(width: 1, thickness: 1),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                AnimatedSize(
                  duration: expandDuration,
                  curve: Curves.easeOutCubic,
                  alignment: Alignment.topCenter,
                  child: _quickPanelExpanded
                      ? Material(
                          color: WellPaidColors.cream.withValues(alpha: 0.65),
                          child: const ShellQuickPanelDesktop(),
                        )
                      : const SizedBox.shrink(),
                ),
                Expanded(child: navigationShell),
              ],
            ),
          ),
        ],
      ),
    );
  }

  void _onDestinationSelected(
    StatefulNavigationShell navigationShell,
    int index,
  ) {
    navigationShell.goBranch(
      index,
      initialLocation: index == navigationShell.currentIndex,
    );
    if (index == 1 || index == 2) {
      warmMonthlyListsForDashboardPeriod(ref);
    } else if (index == 3 || index == 4) {
      warmGlobalReferenceData(ref);
    }
  }
}
