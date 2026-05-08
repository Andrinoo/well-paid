import 'package:flutter/material.dart';

import 'well_paid_colors.dart';

/// Cores semânticas para estados, badges e alertas.
@immutable
class WellPaidSemanticColors extends ThemeExtension<WellPaidSemanticColors> {
  const WellPaidSemanticColors({
    required this.success,
    required this.onSuccess,
    required this.warning,
    required this.onWarning,
    required this.danger,
    required this.onDanger,
    required this.info,
    required this.onInfo,
  });

  final Color success;
  final Color onSuccess;
  final Color warning;
  final Color onWarning;
  final Color danger;
  final Color onDanger;
  final Color info;
  final Color onInfo;

  static WellPaidSemanticColors light() {
    return WellPaidSemanticColors(
      success: const Color(0xFF1B5E20),
      onSuccess: Colors.white,
      warning: const Color(0xFFF9A825),
      onWarning: const Color(0xFF1B2C41),
      danger: const Color(0xFFB00020),
      onDanger: Colors.white,
      info: WellPaidColors.navy.withValues(alpha: 0.75),
      onInfo: Colors.white,
    );
  }

  @override
  WellPaidSemanticColors copyWith({
    Color? success,
    Color? onSuccess,
    Color? warning,
    Color? onWarning,
    Color? danger,
    Color? onDanger,
    Color? info,
    Color? onInfo,
  }) {
    return WellPaidSemanticColors(
      success: success ?? this.success,
      onSuccess: onSuccess ?? this.onSuccess,
      warning: warning ?? this.warning,
      onWarning: onWarning ?? this.onWarning,
      danger: danger ?? this.danger,
      onDanger: onDanger ?? this.onDanger,
      info: info ?? this.info,
      onInfo: onInfo ?? this.onInfo,
    );
  }

  @override
  WellPaidSemanticColors lerp(
    ThemeExtension<WellPaidSemanticColors>? other,
    double t,
  ) {
    if (other is! WellPaidSemanticColors) return this;
    return WellPaidSemanticColors(
      success: Color.lerp(success, other.success, t)!,
      onSuccess: Color.lerp(onSuccess, other.onSuccess, t)!,
      warning: Color.lerp(warning, other.warning, t)!,
      onWarning: Color.lerp(onWarning, other.onWarning, t)!,
      danger: Color.lerp(danger, other.danger, t)!,
      onDanger: Color.lerp(onDanger, other.onDanger, t)!,
      info: Color.lerp(info, other.info, t)!,
      onInfo: Color.lerp(onInfo, other.onInfo, t)!,
    );
  }
}

extension WellPaidSemanticColorsX on BuildContext {
  WellPaidSemanticColors get wellPaidSemantic =>
      Theme.of(this).extension<WellPaidSemanticColors>() ??
      WellPaidSemanticColors.light();
}
