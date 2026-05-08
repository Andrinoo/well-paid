import 'package:flutter/material.dart';

import '../../theme/well_paid_colors.dart';
import '../../theme/well_paid_semantic_colors.dart';
import '../../theme/well_paid_spacing.dart';

class WellPaidEmptyState extends StatelessWidget {
  const WellPaidEmptyState({
    super.key,
    required this.title,
    this.message,
    this.icon,
    this.action,
  });

  final String title;
  final String? message;
  final IconData? icon;
  final Widget? action;

  @override
  Widget build(BuildContext context) {
    final sp = context.wellPaidSpacing;
    return Center(
      child: Padding(
        padding: EdgeInsets.all(sp.xl),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            if (icon != null)
              Icon(
                icon,
                size: 56,
                color: WellPaidColors.navy.withValues(alpha: 0.22),
              ),
            if (icon != null) SizedBox(height: sp.md),
            Text(
              title,
              textAlign: TextAlign.center,
              style: Theme.of(context).textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.w700,
                    color: WellPaidColors.navy,
                  ),
            ),
            if (message != null) ...[
              SizedBox(height: sp.sm),
              Text(
                message!,
                textAlign: TextAlign.center,
                style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                      color: WellPaidColors.navy.withValues(alpha: 0.62),
                    ),
              ),
            ],
            if (action != null) ...[
              SizedBox(height: sp.lg),
              action!,
            ],
          ],
        ),
      ),
    );
  }
}

class WellPaidErrorState extends StatelessWidget {
  const WellPaidErrorState({
    super.key,
    required this.message,
    this.onRetry,
    this.retryLabel,
  });

  final String message;
  final VoidCallback? onRetry;
  final String? retryLabel;

  @override
  Widget build(BuildContext context) {
    final sp = context.wellPaidSpacing;
    return Center(
      child: Padding(
        padding: EdgeInsets.all(sp.xl),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              Icons.error_outline_rounded,
              size: 48,
              color: context.wellPaidSemantic.danger.withValues(alpha: 0.85),
            ),
            SizedBox(height: sp.md),
            Text(
              message,
              textAlign: TextAlign.center,
              style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                    color: WellPaidColors.navy.withValues(alpha: 0.75),
                  ),
            ),
            if (onRetry != null) ...[
              SizedBox(height: sp.lg),
              FilledButton(
                onPressed: onRetry,
                child: Text(retryLabel ?? 'Retry'),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
