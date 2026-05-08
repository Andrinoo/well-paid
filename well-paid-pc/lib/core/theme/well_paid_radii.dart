import 'dart:ui' show lerpDouble;

import 'package:flutter/material.dart';

@immutable
class WellPaidRadii extends ThemeExtension<WellPaidRadii> {
  const WellPaidRadii({
    this.sm = 8,
    this.md = 12,
    this.lg = 16,
    this.xl = 20,
    this.input = 14,
  });

  final double sm;
  final double md;
  final double lg;
  final double xl;
  final double input;

  @override
  WellPaidRadii copyWith({
    double? sm,
    double? md,
    double? lg,
    double? xl,
    double? input,
  }) {
    return WellPaidRadii(
      sm: sm ?? this.sm,
      md: md ?? this.md,
      lg: lg ?? this.lg,
      xl: xl ?? this.xl,
      input: input ?? this.input,
    );
  }

  @override
  WellPaidRadii lerp(ThemeExtension<WellPaidRadii>? other, double t) {
    if (other is! WellPaidRadii) return this;
    return WellPaidRadii(
      sm: lerpDouble(sm, other.sm, t)!,
      md: lerpDouble(md, other.md, t)!,
      lg: lerpDouble(lg, other.lg, t)!,
      xl: lerpDouble(xl, other.xl, t)!,
      input: lerpDouble(input, other.input, t)!,
    );
  }
}

extension WellPaidRadiiX on BuildContext {
  WellPaidRadii get wellPaidRadii =>
      Theme.of(this).extension<WellPaidRadii>() ?? const WellPaidRadii();
}
