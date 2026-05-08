import 'dart:ui' show lerpDouble;

import 'package:flutter/material.dart';

/// Espaçamentos (base 4px) — [ThemeExtension].
@immutable
class WellPaidSpacing extends ThemeExtension<WellPaidSpacing> {
  const WellPaidSpacing({
    this.xs = 4,
    this.sm = 8,
    this.md = 16,
    this.lg = 24,
    this.xl = 32,
    this.xxl = 40,
  });

  final double xs;
  final double sm;
  final double md;
  final double lg;
  final double xl;
  final double xxl;

  @override
  WellPaidSpacing copyWith({
    double? xs,
    double? sm,
    double? md,
    double? lg,
    double? xl,
    double? xxl,
  }) {
    return WellPaidSpacing(
      xs: xs ?? this.xs,
      sm: sm ?? this.sm,
      md: md ?? this.md,
      lg: lg ?? this.lg,
      xl: xl ?? this.xl,
      xxl: xxl ?? this.xxl,
    );
  }

  @override
  WellPaidSpacing lerp(ThemeExtension<WellPaidSpacing>? other, double t) {
    if (other is! WellPaidSpacing) return this;
    return WellPaidSpacing(
      xs: lerpDouble(xs, other.xs, t)!,
      sm: lerpDouble(sm, other.sm, t)!,
      md: lerpDouble(md, other.md, t)!,
      lg: lerpDouble(lg, other.lg, t)!,
      xl: lerpDouble(xl, other.xl, t)!,
      xxl: lerpDouble(xxl, other.xxl, t)!,
    );
  }
}

extension WellPaidSpacingX on BuildContext {
  WellPaidSpacing get wellPaidSpacing =>
      Theme.of(this).extension<WellPaidSpacing>() ?? const WellPaidSpacing();
}
