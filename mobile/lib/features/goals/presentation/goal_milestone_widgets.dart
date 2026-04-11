import 'package:flutter/material.dart';
import 'package:phosphor_flutter/phosphor_flutter.dart';

import '../../../core/l10n/context_l10n.dart';
import '../../../core/theme/well_paid_colors.dart';
import 'goal_progress_milestone.dart';

class GoalMilestoneBanner extends StatelessWidget {
  const GoalMilestoneBanner({super.key, required this.milestone});

  final GoalProgressMilestone milestone;

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    return Material(
      color: WellPaidColors.gold.withValues(alpha: 0.16),
      borderRadius: BorderRadius.circular(14),
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Icon(
              milestone == GoalProgressMilestone.complete
                  ? PhosphorIconsRegular.checkCircle
                  : PhosphorIconsRegular.trendUp,
              color: WellPaidColors.navy.withValues(alpha: 0.88),
              size: 26,
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Text(
                milestone.bannerMessage(l10n),
                style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                      color: WellPaidColors.navy.withValues(alpha: 0.88),
                      height: 1.35,
                      fontWeight: FontWeight.w600,
                    ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

/// Selo compacto para a lista de metas.
class GoalMilestoneChip extends StatelessWidget {
  const GoalMilestoneChip({super.key, required this.milestone});

  final GoalProgressMilestone milestone;

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final done = milestone == GoalProgressMilestone.complete;
    return Tooltip(
      message: milestone.bannerMessage(l10n),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
        decoration: BoxDecoration(
          color: done
              ? const Color(0xFF2A7A6E).withValues(alpha: 0.12)
              : WellPaidColors.gold.withValues(alpha: 0.22),
          borderRadius: BorderRadius.circular(8),
          border: Border.all(
            color: WellPaidColors.navy.withValues(alpha: 0.1),
          ),
        ),
        child: Text(
          milestone.chipLabel(l10n),
          style: Theme.of(context).textTheme.labelSmall?.copyWith(
                color: WellPaidColors.navy.withValues(alpha: 0.85),
                fontWeight: FontWeight.w800,
                letterSpacing: 0.2,
              ),
        ),
      ),
    );
  }
}
