import 'package:flutter/material.dart';

import '../../theme/well_paid_colors.dart';
import '../../theme/well_paid_radii.dart';

/// [AlertDialog] alinhado ao design system (cantos, acções).
class WellPaidAlertDialog extends StatelessWidget {
  const WellPaidAlertDialog({
    super.key,
    required this.title,
    this.content,
    this.actions = const [],
  });

  final Widget title;
  final Widget? content;
  final List<Widget> actions;

  @override
  Widget build(BuildContext context) {
    final r = context.wellPaidRadii;
    return AlertDialog(
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(r.lg),
      ),
      backgroundColor: WellPaidColors.cream,
      title: title,
      content: content,
      actions: actions,
    );
  }
}
