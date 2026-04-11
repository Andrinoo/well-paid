import 'package:flutter/material.dart';
import 'package:phosphor_flutter/phosphor_flutter.dart';

import '../../../core/format/brl_cents.dart';
import '../../../core/format/locale_dates.dart';
import '../../../core/l10n/context_l10n.dart';
import '../../../core/theme/well_paid_colors.dart';
import '../domain/goal_item.dart';
import '../domain/goal_linear_pace.dart';

/// Ritmo linear estimado (cliente) + mês alvo; com aviso de que é aproximado.
class GoalLinearPaceSection extends StatelessWidget {
  const GoalLinearPaceSection({super.key, required this.goal});

  final GoalItem goal;

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    if (!goal.isActive || goal.targetCents <= 0) {
      return const SizedBox.shrink();
    }
    final remaining = goal.targetCents - goal.currentCents;
    if (remaining <= 0) {
      return const SizedBox.shrink();
    }

    final est = computeGoalLinearPaceEstimate(goal);
    if (est != null) {
      final monthYear =
          formatMonthYearUi(context, est.estimatedCompletionMonthEnd);
      final avgStr = formatBrlFromCents(est.avgCentsPerMonth);
      return Card(
        elevation: 0,
        color: WellPaidColors.navy.withValues(alpha: 0.06),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16),
          side: BorderSide(color: WellPaidColors.navy.withValues(alpha: 0.08)),
        ),
        child: Padding(
          padding: const EdgeInsets.all(14),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Icon(
                    PhosphorIconsRegular.chartLine,
                    size: 22,
                    color: WellPaidColors.navy.withValues(alpha: 0.85),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      l10n.goalLinearPaceCardTitle,
                      style: Theme.of(context).textTheme.titleSmall?.copyWith(
                            fontWeight: FontWeight.w800,
                            color: WellPaidColors.navy,
                          ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 10),
              Text(
                l10n.goalLinearPaceAvgPerMonth(avgStr),
                style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                      color: WellPaidColors.navy.withValues(alpha: 0.82),
                      fontWeight: FontWeight.w600,
                      height: 1.35,
                    ),
              ),
              const SizedBox(height: 6),
              Text(
                l10n.goalLinearPaceEta(monthYear),
                style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                      color: WellPaidColors.navy.withValues(alpha: 0.82),
                      fontWeight: FontWeight.w600,
                      height: 1.35,
                    ),
              ),
              const SizedBox(height: 10),
              Text(
                l10n.goalLinearPaceDisclaimer,
                style: Theme.of(context).textTheme.bodySmall?.copyWith(
                      color: WellPaidColors.navy.withValues(alpha: 0.55),
                      height: 1.35,
                    ),
              ),
            ],
          ),
        ),
      );
    }

    if (goal.currentCents <= 0) {
      return Text(
        l10n.goalLinearPaceInsufficientHistory,
        style: Theme.of(context).textTheme.bodySmall?.copyWith(
              color: WellPaidColors.navy.withValues(alpha: 0.58),
              height: 1.35,
            ),
      );
    }

    return const SizedBox.shrink();
  }
}
