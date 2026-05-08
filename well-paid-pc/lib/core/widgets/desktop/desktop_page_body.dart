import 'package:flutter/material.dart';

import '../../theme/well_paid_breakpoints.dart';
import '../../theme/well_paid_colors.dart';
import '../../theme/well_paid_spacing.dart';

/// Corpo de página centrado com largura máxima e padding responsivo.
class DesktopPageBody extends StatelessWidget {
  const DesktopPageBody({
    super.key,
    required this.child,
    this.maxWidth = WellPaidBreakpoints.pageMaxWidth,
    this.padding,
    this.backgroundWash = true,
  });

  final Widget child;
  final double maxWidth;
  final EdgeInsetsGeometry? padding;
  final bool backgroundWash;

  @override
  Widget build(BuildContext context) {
    final sp = context.wellPaidSpacing;
    final media = MediaQuery.sizeOf(context);
    final hPad = media.width >= WellPaidBreakpoints.medium ? sp.lg : sp.md;

    Widget body = child;
    if (backgroundWash) {
      body = DecoratedBox(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [
              WellPaidColors.cream,
              WellPaidColors.creamMuted,
            ],
          ),
        ),
        child: child,
      );
    }

    return Align(
      alignment: Alignment.topCenter,
      child: ConstrainedBox(
        constraints: BoxConstraints(maxWidth: maxWidth),
        child: Padding(
          padding: padding ?? EdgeInsets.fromLTRB(hPad, sp.sm, hPad, sp.lg),
          child: body,
        ),
      ),
    );
  }
}
