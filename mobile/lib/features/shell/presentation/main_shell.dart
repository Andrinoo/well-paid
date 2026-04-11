import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/l10n/context_l10n.dart';
import '../../../core/navigation/list_data_warmup.dart';
import '../../../core/theme/well_paid_colors.dart';
import 'shell_quick_panel.dart';

/// Shell with bottom [NavigationBar] for primary app sections.
class MainShell extends ConsumerStatefulWidget {
  const MainShell({super.key, required this.navigationShell});

  final StatefulNavigationShell navigationShell;

  @override
  ConsumerState<MainShell> createState() => _MainShellState();
}

class _MainShellState extends ConsumerState<MainShell> {
  bool _quickPanelExpanded = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      scheduleShellDataWarmup(ref);
    });
  }

  void _setQuickExpanded(bool v) {
    if (_quickPanelExpanded == v) return;
    setState(() => _quickPanelExpanded = v);
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final navigationShell = widget.navigationShell;
    final reduceMotion = MediaQuery.maybeOf(context)?.disableAnimations ?? false;
    final expandDuration = Duration(milliseconds: reduceMotion ? 1 : 220);

    return Scaffold(
      body: navigationShell,
      bottomNavigationBar: Material(
        color: WellPaidColors.creamMuted.withValues(alpha: 0.98),
        elevation: 3,
        shadowColor: WellPaidColors.navy.withValues(alpha: 0.08),
        child: SafeArea(
          top: false,
          child: NavigationBarTheme(
            data: NavigationBarThemeData(
              indicatorColor: WellPaidColors.gold.withValues(alpha: 0.35),
              labelTextStyle: WidgetStateProperty.resolveWith((states) {
                final selected = states.contains(WidgetState.selected);
                return TextStyle(
                  fontSize: 12,
                  fontWeight: selected ? FontWeight.w700 : FontWeight.w500,
                  color: selected
                      ? WellPaidColors.navy
                      : WellPaidColors.navy.withValues(alpha: 0.65),
                );
              }),
              iconTheme: WidgetStateProperty.resolveWith((states) {
                final selected = states.contains(WidgetState.selected);
                return IconThemeData(
                  size: 24,
                  color: selected
                      ? WellPaidColors.navy
                      : WellPaidColors.navy.withValues(alpha: 0.55),
                );
              }),
            ),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                AnimatedSize(
                  duration: expandDuration,
                  curve: Curves.easeOutCubic,
                  alignment: Alignment.bottomCenter,
                  child: _quickPanelExpanded
                      ? const ShellQuickPanel()
                      : const SizedBox.shrink(),
                ),
                Material(
                  color: WellPaidColors.creamMuted.withValues(alpha: 0.98),
                  child: GestureDetector(
                    behavior: HitTestBehavior.opaque,
                    onVerticalDragEnd: (details) {
                      final v = details.primaryVelocity;
                      if (v == null) return;
                      if (v < -180) _setQuickExpanded(true);
                      if (v > 180) _setQuickExpanded(false);
                    },
                    child: InkWell(
                      onTap: () => _setQuickExpanded(!_quickPanelExpanded),
                      child: Semantics(
                        button: true,
                        label: l10n.navQuickPanelToggleHint,
                        child: SizedBox(
                          height: 28,
                          child: Center(
                            child: AnimatedRotation(
                              duration: expandDuration,
                              turns: _quickPanelExpanded ? 0.5 : 0,
                              child: Icon(
                                Icons.keyboard_arrow_up_rounded,
                                size: 22,
                                color: WellPaidColors.navy.withValues(alpha: 0.5),
                              ),
                            ),
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
                NavigationBar(
                  height: 64,
                  backgroundColor: Colors.transparent,
                  surfaceTintColor: Colors.transparent,
                  shadowColor: Colors.transparent,
                  elevation: 0,
                  selectedIndex: navigationShell.currentIndex,
                  onDestinationSelected: (i) =>
                      _onDestinationSelected(navigationShell, i),
                  destinations: [
                    NavigationDestination(
                      icon: const Icon(Icons.dashboard_outlined),
                      selectedIcon: const Icon(Icons.dashboard),
                      label: l10n.navHome,
                    ),
                    NavigationDestination(
                      icon: const Icon(Icons.receipt_long_outlined),
                      selectedIcon: const Icon(Icons.receipt_long),
                      label: l10n.navExpenses,
                    ),
                    NavigationDestination(
                      icon: const Icon(Icons.savings_outlined),
                      selectedIcon: const Icon(Icons.savings),
                      label: l10n.navIncomes,
                    ),
                    NavigationDestination(
                      icon: const Icon(Icons.flag_outlined),
                      selectedIcon: const Icon(Icons.flag),
                      label: l10n.navGoals,
                    ),
                    NavigationDestination(
                      icon: const Icon(Icons.shield_outlined),
                      selectedIcon: const Icon(Icons.shield),
                      label: l10n.navReserve,
                    ),
                  ],
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  void _onDestinationSelected(StatefulNavigationShell navigationShell, int index) {
    navigationShell.goBranch(
      index,
      initialLocation: index == navigationShell.currentIndex,
    );
    // Ao mudar de separador, reforça dados do mês do dashboard (já em cache se warmup correu).
    if (index == 1 || index == 2) {
      warmMonthlyListsForDashboardPeriod(ref);
    } else if (index == 3 || index == 4) {
      warmGlobalReferenceData(ref);
    }
  }
}
