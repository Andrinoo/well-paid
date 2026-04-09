import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/l10n/context_l10n.dart';
import '../../../core/navigation/list_data_warmup.dart';
import '../../../core/theme/well_paid_colors.dart';

/// Shell with bottom [NavigationBar] for primary app sections.
class MainShell extends ConsumerStatefulWidget {
  const MainShell({super.key, required this.navigationShell});

  final StatefulNavigationShell navigationShell;

  @override
  ConsumerState<MainShell> createState() => _MainShellState();
}

class _MainShellState extends ConsumerState<MainShell> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      scheduleShellDataWarmup(ref);
    });
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final navigationShell = widget.navigationShell;
    return Scaffold(
      body: navigationShell,
      bottomNavigationBar: NavigationBarTheme(
        data: NavigationBarThemeData(
          indicatorColor: WellPaidColors.gold.withValues(alpha: 0.35),
          labelTextStyle: WidgetStateProperty.resolveWith((states) {
            final selected = states.contains(WidgetState.selected);
            return TextStyle(
              fontSize: 12,
              fontWeight: selected ? FontWeight.w700 : FontWeight.w500,
              color: selected ? WellPaidColors.navy : WellPaidColors.navy.withValues(alpha: 0.65),
            );
          }),
          iconTheme: WidgetStateProperty.resolveWith((states) {
            final selected = states.contains(WidgetState.selected);
            return IconThemeData(
              size: 24,
              color: selected ? WellPaidColors.navy : WellPaidColors.navy.withValues(alpha: 0.55),
            );
          }),
        ),
        child: NavigationBar(
          height: 64,
          backgroundColor: WellPaidColors.creamMuted.withValues(alpha: 0.98),
          surfaceTintColor: Colors.transparent,
          shadowColor: WellPaidColors.navy.withValues(alpha: 0.08),
          elevation: 3,
          selectedIndex: navigationShell.currentIndex,
          onDestinationSelected: (i) => _onDestinationSelected(navigationShell, i),
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
