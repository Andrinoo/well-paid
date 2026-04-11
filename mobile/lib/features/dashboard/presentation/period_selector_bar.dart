import 'dart:async';

import 'package:flutter/material.dart';
import 'package:phosphor_flutter/phosphor_flutter.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/l10n/context_l10n.dart';
import '../../../core/navigation/list_data_warmup.dart';
import '../../../core/theme/well_paid_colors.dart';
import '../../expenses/application/expenses_providers.dart';
import '../../incomes/application/incomes_providers.dart';
import '../application/dashboard_providers.dart';

/// Variante visual: [light] (fundo claro) ou [dark] (sobre header navy).
enum PeriodSelectorBarVariant { light, dark }

/// Mês civil do dashboard (setas anterior / seguinte).
class PeriodSelectorBar extends ConsumerStatefulWidget {
  const PeriodSelectorBar({
    super.key,
    this.variant = PeriodSelectorBarVariant.light,
    this.dense = false,
  });

  final PeriodSelectorBarVariant variant;
  /// Menos padding e ícones menores (ex.: header do dashboard).
  final bool dense;

  @override
  ConsumerState<PeriodSelectorBar> createState() => _PeriodSelectorBarState();
}

class _PeriodSelectorBarState extends ConsumerState<PeriodSelectorBar> {
  static const Duration _tapThrottle = Duration(milliseconds: 280);
  DateTime _lastShiftAt = DateTime.fromMillisecondsSinceEpoch(0);
  bool _bootstrappedPrefetch = false;

  @override
  Widget build(BuildContext context) {
    final ref = this.ref;
    final l10n = context.l10n;
    final p = ref.watch(dashboardPeriodProvider);
    final loading = ref.watch(dashboardOverviewProvider).isLoading;
    final label = '${p.month.toString().padLeft(2, '0')}/${p.year}';
    final dark = widget.variant == PeriodSelectorBarVariant.dark;
    final fg = dark ? WellPaidColors.cream : WellPaidColors.navy;
    final fgMuted = dark
        ? WellPaidColors.cream.withValues(alpha: 0.75)
        : WellPaidColors.navy.withValues(alpha: 0.7);
    final chevron = dark ? WellPaidColors.gold : WellPaidColors.navy;

    // Warm up nearby months once for immediate next interactions.
    if (!_bootstrappedPrefetch) {
      _bootstrappedPrefetch = true;
      _prefetchWindow(ref, p);
      warmGlobalReferenceData(ref);
      warmMonthlyListsForDashboardPeriod(ref);
    }

    final d = widget.dense;
    final iconSize = d ? 20.0 : 24.0;
    final pad = d
        ? const EdgeInsets.symmetric(horizontal: 2, vertical: 0)
        : const EdgeInsets.symmetric(horizontal: 8, vertical: 4);

    return Semantics(
      label: l10n.periodSummaryA11y(label),
      child: Material(
        color: dark ? Colors.transparent : WellPaidColors.creamMuted.withValues(alpha: 0.6),
        child: Padding(
          padding: pad,
          child: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            mainAxisSize: d ? MainAxisSize.min : MainAxisSize.max,
            children: [
              IconButton(
                tooltip: l10n.periodPrevMonth,
                onPressed: () => _shiftMonth(-1),
                icon: Icon(PhosphorIconsRegular.caretLeft, size: iconSize),
                color: chevron,
                style: IconButton.styleFrom(
                  padding: d ? const EdgeInsets.all(4) : null,
                  minimumSize: d ? const Size(32, 32) : null,
                  tapTargetSize: d ? MaterialTapTargetSize.shrinkWrap : null,
                  visualDensity: d ? VisualDensity.compact : null,
                ),
              ),
              Text(
                label,
                style: (d
                        ? Theme.of(context).textTheme.titleSmall
                        : Theme.of(context).textTheme.titleMedium)
                    ?.copyWith(
                      fontWeight: FontWeight.w700,
                      color: fg,
                      letterSpacing: d ? 0.2 : 0.3,
                    ),
              ),
              if (loading) ...[
                SizedBox(width: d ? 6 : 8),
                SizedBox(
                  width: d ? 12 : 14,
                  height: d ? 12 : 14,
                  child: CircularProgressIndicator(
                    strokeWidth: d ? 1.5 : 2,
                    color: fgMuted,
                  ),
                ),
              ],
              IconButton(
                tooltip: l10n.periodNextMonth,
                onPressed: () => _shiftMonth(1),
                icon: Icon(PhosphorIconsRegular.caretRight, size: iconSize),
                color: chevron,
                style: IconButton.styleFrom(
                  padding: d ? const EdgeInsets.all(4) : null,
                  minimumSize: d ? const Size(32, 32) : null,
                  tapTargetSize: d ? MaterialTapTargetSize.shrinkWrap : null,
                  visualDensity: d ? VisualDensity.compact : null,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  void _shiftMonth(int delta) {
    final now = DateTime.now();
    if (now.difference(_lastShiftAt) < _tapThrottle) {
      return;
    }
    _lastShiftAt = now;

    final p = ref.read(dashboardPeriodProvider);
    final next = _movePeriod(p, delta);
    ref.read(dashboardPeriodProvider.notifier).state = next;
    _prefetchWindow(ref, next);
    syncListFiltersWithDashboardPeriod(ref);
    unawaited(ref.read(expensesListProvider.future));
    unawaited(ref.read(incomesListProvider.future));
  }
}

DashboardPeriod _movePeriod(DashboardPeriod p, int delta) {
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
  return DashboardPeriod(year: y, month: m);
}

void _prefetchWindow(WidgetRef ref, DashboardPeriod center) {
  // Cache a small forward-looking window to keep month switching snappy
  // without overloading network: previous month + next 4 months.
  for (var d = -1; d <= 4; d++) {
    final period = _movePeriod(center, d);
    ref.read(dashboardOverviewByPeriodProvider(period).future);
  }
}
