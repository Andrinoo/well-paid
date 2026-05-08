import 'package:flutter/material.dart';

import '../../theme/well_paid_colors.dart';
import '../../theme/well_paid_radii.dart';
import '../../theme/well_paid_shadows.dart';
import '../../theme/well_paid_spacing.dart';

/// Cartão de secção com título opcional e acção à direita (estilo dashboard).
class SectionCard extends StatelessWidget {
  const SectionCard({
    super.key,
    required this.child,
    this.title,
    this.subtitle,
    this.action,
    this.padding,
    this.margin,
  });

  final Widget child;
  final String? title;
  final String? subtitle;
  final Widget? action;
  final EdgeInsetsGeometry? padding;
  final EdgeInsetsGeometry? margin;

  @override
  Widget build(BuildContext context) {
    final r = context.wellPaidRadii;
    final sp = context.wellPaidSpacing;
    final shadows = context.wellPaidShadows;

    return Padding(
      padding: margin ?? EdgeInsets.zero,
      child: DecoratedBox(
        decoration: shadows.cardDecoration(
          color: Colors.white.withValues(alpha: 0.92),
          radius: r.lg,
        ),
        child: Padding(
          padding: padding ?? EdgeInsets.all(sp.md),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            mainAxisSize: MainAxisSize.min,
            children: [
              if (title != null || action != null)
                Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          if (title != null)
                            Text(
                              title!,
                              style: Theme.of(context).textTheme.titleMedium
                                  ?.copyWith(
                                    fontWeight: FontWeight.w700,
                                    color: WellPaidColors.navy,
                                    letterSpacing: -0.2,
                                  ),
                            ),
                          if (subtitle != null) ...[
                            SizedBox(height: sp.xs),
                            Text(
                              subtitle!,
                              style: Theme.of(context).textTheme.bodySmall
                                  ?.copyWith(
                                    color: WellPaidColors.navy.withValues(
                                      alpha: 0.58,
                                    ),
                                  ),
                            ),
                          ],
                        ],
                      ),
                    ),
                    if (action != null) action!,
                  ],
                ),
              if (title != null || action != null) SizedBox(height: sp.sm),
              child,
            ],
          ),
        ),
      ),
    );
  }
}
