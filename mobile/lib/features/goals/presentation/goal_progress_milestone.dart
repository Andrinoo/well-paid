import '../../../l10n/app_localizations.dart';

/// Marco derivado só de `current` / `target` (sem backend).
enum GoalProgressMilestone {
  quarter,
  half,
  almost,
  complete,
}

extension GoalProgressMilestoneL10n on GoalProgressMilestone {
  String bannerMessage(AppLocalizations l) {
    switch (this) {
      case GoalProgressMilestone.quarter:
        return l.goalMilestoneBannerQuarter;
      case GoalProgressMilestone.half:
        return l.goalMilestoneBannerHalf;
      case GoalProgressMilestone.almost:
        return l.goalMilestoneBannerAlmost;
      case GoalProgressMilestone.complete:
        return l.goalMilestoneBannerComplete;
    }
  }

  String chipLabel(AppLocalizations l) {
    switch (this) {
      case GoalProgressMilestone.quarter:
        return l.goalMilestoneChipQuarter;
      case GoalProgressMilestone.half:
        return l.goalMilestoneChipHalf;
      case GoalProgressMilestone.almost:
        return l.goalMilestoneChipAlmost;
      case GoalProgressMilestone.complete:
        return l.goalMilestoneChipComplete;
    }
  }
}

/// `null` se ainda abaixo de 25% ou `target` inválido.
GoalProgressMilestone? resolveGoalProgressMilestone({
  required int currentCents,
  required int targetCents,
}) {
  if (targetCents <= 0) return null;
  if (currentCents >= targetCents) return GoalProgressMilestone.complete;
  final p = currentCents / targetCents;
  if (p >= 0.9) return GoalProgressMilestone.almost;
  if (p >= 0.5) return GoalProgressMilestone.half;
  if (p >= 0.25) return GoalProgressMilestone.quarter;
  return null;
}
