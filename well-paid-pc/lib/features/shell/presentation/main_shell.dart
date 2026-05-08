import 'dart:async' show unawaited;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
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
/// Atalhos: Alt+1 … Alt+5 para Home, Despesas, Rendimentos, Metas, Reserva.
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

  void _goBranchByShortcut(int index) {
    if (index < 0 || index > 4) return;
    _onDestinationSelected(widget.navigationShell, index);
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
    final reduceMotion =
        MediaQuery.maybeOf(context)?.disableAnimations ?? false;
    final expandDuration = Duration(milliseconds: reduceMotion ? 1 : 220);

    final wideLabels = MediaQuery.sizeOf(context).width >= 520
        ? NavigationRailLabelType.all
        : NavigationRailLabelType.selected;

    return CallbackShortcuts(
      bindings: <ShortcutActivator, VoidCallback>{
        const SingleActivator(LogicalKeyboardKey.digit1, alt: true): () =>
            _goBranchByShortcut(0),
        const SingleActivator(LogicalKeyboardKey.digit2, alt: true): () =>
            _goBranchByShortcut(1),
        const SingleActivator(LogicalKeyboardKey.digit3, alt: true): () =>
            _goBranchByShortcut(2),
        const SingleActivator(LogicalKeyboardKey.digit4, alt: true): () =>
            _goBranchByShortcut(3),
        const SingleActivator(LogicalKeyboardKey.digit5, alt: true): () =>
            _goBranchByShortcut(4),
      },
      child: Scaffold(
        backgroundColor: WellPaidColors.cream,
        body: Row(
          children: [
            NavigationRail(
              selectedIndex: navigationShell.currentIndex,
              onDestinationSelected: (i) =>
                  _onDestinationSelected(navigationShell, i),
              labelType: wideLabels,
              minWidth: 72,
              groupAlignment: -0.85,
              backgroundColor: WellPaidColors.creamMuted.withValues(
                alpha: 0.98,
              ),
              indicatorColor: WellPaidColors.gold.withValues(alpha: 0.42),
              leading: Column(
                children: [
                  const SizedBox(height: 10),
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 8),
                    child: Tooltip(
                      message: l10n.pcNavKeyboardSections,
                      child: Material(
                        color: WellPaidColors.navy.withValues(alpha: 0.06),
                        borderRadius: BorderRadius.circular(12),
                        child: InkWell(
                          onTap: () => navigationShell.goBranch(0),
                          borderRadius: BorderRadius.circular(12),
                          hoverColor: WellPaidColors.navy.withValues(
                            alpha: 0.1,
                          ),
                          child: Padding(
                            padding: const EdgeInsets.symmetric(
                              vertical: 8,
                              horizontal: 6,
                            ),
                            child: Text(
                              'WP',
                              textAlign: TextAlign.center,
                              style: Theme.of(context).textTheme.labelLarge
                                  ?.copyWith(
                                    fontWeight: FontWeight.w800,
                                    letterSpacing: 0.5,
                                    color: WellPaidColors.navy,
                                  ),
                            ),
                          ),
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(height: 10),
                  IconButton(
                    tooltip: l10n.settingsTitle,
                    icon: Icon(
                      PhosphorIconsRegular.gear,
                      color: WellPaidColors.navy.withValues(alpha: 0.85),
                    ),
                    onPressed: () => context.push('/settings'),
                  ),
                  const SizedBox(height: 2),
                  IconButton(
                    tooltip: l10n.navQuickPanelToggleHint,
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
            VerticalDivider(
              width: 1,
              thickness: 1,
              color: WellPaidColors.navy.withValues(alpha: 0.08),
            ),
            Expanded(
              child: DecoratedBox(
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    begin: Alignment.topLeft,
                    end: Alignment.bottomRight,
                    colors: [
                      WellPaidColors.cream,
                      WellPaidColors.creamMuted.withValues(alpha: 0.72),
                    ],
                  ),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    AnimatedSize(
                      duration: expandDuration,
                      curve: Curves.easeOutCubic,
                      alignment: Alignment.topCenter,
                      child: _quickPanelExpanded
                          ? Material(
                              color: Colors.transparent,
                              child: const ShellQuickPanelDesktop(),
                            )
                          : const SizedBox.shrink(),
                    ),
                    Expanded(child: navigationShell),
                  ],
                ),
              ),
            ),
          ],
        ),
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
