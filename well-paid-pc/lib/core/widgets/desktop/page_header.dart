import 'package:flutter/material.dart';

import '../../theme/well_paid_colors.dart';
import '../../theme/well_paid_spacing.dart';

/// Cabeçalho de página: título, linha de navegação opcional, trailing.
class PageHeader extends StatelessWidget {
  const PageHeader({
    super.key,
    required this.title,
    this.subtitle,
    this.breadcrumb,
    this.trailing,
    this.dense = false,
  });

  final String title;
  final String? subtitle;
  final List<Widget>? breadcrumb;
  final Widget? trailing;
  final bool dense;

  @override
  Widget build(BuildContext context) {
    final sp = context.wellPaidSpacing;
    return Padding(
      padding: EdgeInsets.only(bottom: dense ? sp.sm : sp.md),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                if (breadcrumb != null && breadcrumb!.isNotEmpty)
                  Padding(
                    padding: EdgeInsets.only(bottom: sp.xs),
                    child: Wrap(
                      spacing: sp.xs,
                      crossAxisAlignment: WrapCrossAlignment.center,
                      children: _withSeparators(breadcrumb!),
                    ),
                  ),
                Text(
                  title,
                  style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                        fontWeight: FontWeight.w700,
                        color: WellPaidColors.navy,
                        letterSpacing: -0.35,
                      ),
                ),
                if (subtitle != null) ...[
                  SizedBox(height: sp.xs),
                  Text(
                    subtitle!,
                    style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                          color: WellPaidColors.navy.withValues(alpha: 0.62),
                        ),
                  ),
                ],
              ],
            ),
          ),
          if (trailing != null) trailing!,
        ],
      ),
    );
  }

  List<Widget> _withSeparators(List<Widget> items) {
    if (items.length <= 1) return items;
    final out = <Widget>[];
    for (var i = 0; i < items.length; i++) {
      out.add(items[i]);
      if (i < items.length - 1) {
        out.add(
          Text(
            '/',
            style: TextStyle(
              color: WellPaidColors.navy.withValues(alpha: 0.35),
              fontSize: 12,
            ),
          ),
        );
      }
    }
    return out;
  }
}
