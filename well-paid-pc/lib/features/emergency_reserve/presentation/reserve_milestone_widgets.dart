import 'package:flutter/material.dart';
import 'package:phosphor_flutter/phosphor_flutter.dart';

import '../../../core/l10n/context_l10n.dart';
import '../../../core/theme/well_paid_colors.dart';
import '../../../l10n/app_localizations.dart';
import '../../goals/presentation/goal_progress_milestone.dart';

extension ReserveMilestoneL10n on GoalProgressMilestone {
  String reserveBannerMessage(AppLocalizations l) {
    switch (this) {
      case GoalProgressMilestone.quarter:
        return l.reserveMilestoneBannerQuarter;
      case GoalProgressMilestone.half:
        return l.reserveMilestoneBannerHalf;
      case GoalProgressMilestone.almost:
        return l.reserveMilestoneBannerAlmost;
      case GoalProgressMilestone.complete:
        return l.reserveMilestoneBannerComplete;
    }
  }
}

/// Faixa no cartão escuro da reserva (eco das metas, texto específico da reserva).
class ReserveMilestoneBanner extends StatelessWidget {
  const ReserveMilestoneBanner({super.key, required this.milestone});

  final GoalProgressMilestone milestone;

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final done = milestone == GoalProgressMilestone.complete;
    return Material(
      color: Colors.white.withValues(alpha: 0.14),
      borderRadius: BorderRadius.circular(14),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Icon(
              done
                  ? PhosphorIconsRegular.checkCircle
                  : PhosphorIconsRegular.trendUp,
              color: WellPaidColors.gold,
              size: 24,
            ),
            const SizedBox(width: 10),
            Expanded(
              child: Text(
                milestone.reserveBannerMessage(l10n),
                style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                      color: WellPaidColors.cream.withValues(alpha: 0.92),
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

/// Selo compacto na linha do título (mesmos rótulos % que as metas).
class ReserveMilestoneChip extends StatelessWidget {
  const ReserveMilestoneChip({super.key, required this.milestone});

  final GoalProgressMilestone milestone;

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final done = milestone == GoalProgressMilestone.complete;
    return Tooltip(
      message: milestone.reserveBannerMessage(l10n),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 3),
        decoration: BoxDecoration(
          color: done
              ? const Color(0xFF5DBEAA).withValues(alpha: 0.22)
              : WellPaidColors.gold.withValues(alpha: 0.22),
          borderRadius: BorderRadius.circular(8),
          border: Border.all(
            color: Colors.white.withValues(alpha: 0.22),
          ),
        ),
        child: Text(
          milestone.chipLabel(l10n),
          style: Theme.of(context).textTheme.labelSmall?.copyWith(
                color: WellPaidColors.cream.withValues(alpha: 0.95),
                fontWeight: FontWeight.w800,
                letterSpacing: 0.2,
              ),
        ),
      ),
    );
  }
}
