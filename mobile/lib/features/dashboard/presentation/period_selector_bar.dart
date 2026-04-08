import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/theme/well_paid_colors.dart';
import '../application/dashboard_providers.dart';

/// Mês civil do dashboard (setas anterior / seguinte).
class PeriodSelectorBar extends ConsumerWidget {
  const PeriodSelectorBar({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final p = ref.watch(dashboardPeriodProvider);
    final label =
        '${p.month.toString().padLeft(2, '0')}/${p.year}';

    return Semantics(
      label: 'Período do resumo, $label',
      child: Material(
        color: WellPaidColors.creamMuted.withValues(alpha: 0.6),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              IconButton(
                tooltip: 'Mês anterior',
                onPressed: () => _shiftMonth(ref, -1),
                icon: const Icon(Icons.chevron_left),
                color: WellPaidColors.navy,
              ),
              Text(
                label,
                style: Theme.of(context).textTheme.titleMedium?.copyWith(
                      fontWeight: FontWeight.w700,
                      color: WellPaidColors.navy,
                    ),
              ),
              IconButton(
                tooltip: 'Próximo mês',
                onPressed: () => _shiftMonth(ref, 1),
                icon: const Icon(Icons.chevron_right),
                color: WellPaidColors.navy,
              ),
            ],
          ),
        ),
      ),
    );
  }
}

void _shiftMonth(WidgetRef ref, int delta) {
  final p = ref.read(dashboardPeriodProvider);
  var y = p.year;
  var m = p.month + delta;
  while (m > 12) {
    m -= 12;
    y++;
  }
  while (m < 1) {
    m += 12;
    y--;
  }
  ref.read(dashboardPeriodProvider.notifier).state =
      DashboardPeriod(year: y, month: m);
}
