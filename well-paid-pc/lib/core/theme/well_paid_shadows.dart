import 'package:flutter/material.dart';

import 'well_paid_colors.dart';

/// Sombras discretas para cartões (níveis 0–2).
@immutable
class WellPaidShadows extends ThemeExtension<WellPaidShadows> {
  const WellPaidShadows({
    this.card = const [
      BoxShadow(
        color: Color(0x0D1B2C41),
        blurRadius: 12,
        offset: Offset(0, 4),
      ),
    ],
    this.raised = const [
      BoxShadow(
        color: Color(0x181B2C41),
        blurRadius: 20,
        offset: Offset(0, 8),
      ),
    ],
  });

  final List<BoxShadow> card;
  final List<BoxShadow> raised;

  BoxDecoration cardDecoration({
    required Color color,
    required double radius,
    Color? borderColor,
  }) {
    return BoxDecoration(
      color: color,
      borderRadius: BorderRadius.circular(radius),
      border: borderColor != null
          ? Border.all(color: borderColor)
          : Border.all(color: WellPaidColors.navy.withValues(alpha: 0.08)),
      boxShadow: card,
    );
  }

  @override
  WellPaidShadows copyWith({
    List<BoxShadow>? card,
    List<BoxShadow>? raised,
  }) {
    return WellPaidShadows(
      card: card ?? this.card,
      raised: raised ?? this.raised,
    );
  }

  @override
  WellPaidShadows lerp(ThemeExtension<WellPaidShadows>? other, double t) {
    if (other is! WellPaidShadows) return this;
    return WellPaidShadows(
      card: t < 0.5 ? card : other.card,
      raised: t < 0.5 ? raised : other.raised,
    );
  }
}

extension WellPaidShadowsX on BuildContext {
  WellPaidShadows get wellPaidShadows =>
      Theme.of(this).extension<WellPaidShadows>() ?? const WellPaidShadows();
}
