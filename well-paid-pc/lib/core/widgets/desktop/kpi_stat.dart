import 'package:flutter/material.dart';

import '../../theme/well_paid_colors.dart';
import '../../theme/well_paid_money_typography.dart';
import '../../theme/well_paid_radii.dart';
import '../../theme/well_paid_shadows.dart';
import '../../theme/well_paid_spacing.dart';

/// Métrica compacta (valor + rótulo).
class KpiStat extends StatelessWidget {
  const KpiStat({
    super.key,
    required this.label,
    required this.value,
    this.icon,
    this.emphasize = false,
  });

  final String label;
  final String value;
  final IconData? icon;
  final bool emphasize;

  @override
  Widget build(BuildContext context) {
    final sp = context.wellPaidSpacing;
    final r = context.wellPaidRadii;
    final shadows = context.wellPaidShadows;
    final money = context.wellPaidMoneyType;

    return DecoratedBox(
      decoration: shadows.cardDecoration(
        color: Colors.white.withValues(alpha: 0.95),
        radius: r.md,
      ),
      child: Padding(
        padding: EdgeInsets.symmetric(horizontal: sp.md, vertical: sp.sm + 2),
        child: Row(
          children: [
            if (icon != null) ...[
              Icon(
                icon,
                size: 22,
                color: WellPaidColors.navy.withValues(alpha: 0.72),
              ),
              SizedBox(width: sp.sm),
            ],
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    label,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: Theme.of(context).textTheme.labelSmall?.copyWith(
                          color: WellPaidColors.navy.withValues(alpha: 0.58),
                          fontWeight: FontWeight.w600,
                        ),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    value,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: emphasize
                        ? money.displayMedium.copyWith(color: WellPaidColors.gold)
                        : money.displayMedium,
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
