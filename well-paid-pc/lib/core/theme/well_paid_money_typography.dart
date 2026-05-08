import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

import 'well_paid_colors.dart';

/// Estilos para montantes e métricas (tabular onde suportado).
@immutable
class WellPaidMoneyTypography extends ThemeExtension<WellPaidMoneyTypography> {
  const WellPaidMoneyTypography({
    required this.displayLarge,
    required this.displayMedium,
    required this.tableCell,
  });

  final TextStyle displayLarge;
  final TextStyle displayMedium;
  final TextStyle tableCell;

  static WellPaidMoneyTypography fromTextTheme(TextTheme t) {
    return WellPaidMoneyTypography(
      displayLarge: GoogleFonts.inter(
        fontSize: 28,
        fontWeight: FontWeight.w800,
        letterSpacing: -0.5,
        color: WellPaidColors.navy,
        height: 1.2,
      ),
      displayMedium: GoogleFonts.inter(
        fontSize: 20,
        fontWeight: FontWeight.w700,
        letterSpacing: -0.35,
        color: WellPaidColors.navy,
        height: 1.25,
      ),
      tableCell: GoogleFonts.inter(
        fontSize: 14,
        fontWeight: FontWeight.w600,
        letterSpacing: -0.1,
        color: WellPaidColors.navy,
        height: 1.2,
      ),
    );
  }

  @override
  WellPaidMoneyTypography copyWith({
    TextStyle? displayLarge,
    TextStyle? displayMedium,
    TextStyle? tableCell,
  }) {
    return WellPaidMoneyTypography(
      displayLarge: displayLarge ?? this.displayLarge,
      displayMedium: displayMedium ?? this.displayMedium,
      tableCell: tableCell ?? this.tableCell,
    );
  }

  @override
  WellPaidMoneyTypography lerp(
    ThemeExtension<WellPaidMoneyTypography>? other,
    double t,
  ) {
    if (other is! WellPaidMoneyTypography) return this;
    return WellPaidMoneyTypography(
      displayLarge: TextStyle.lerp(displayLarge, other.displayLarge, t)!,
      displayMedium: TextStyle.lerp(displayMedium, other.displayMedium, t)!,
      tableCell: TextStyle.lerp(tableCell, other.tableCell, t)!,
    );
  }
}

extension WellPaidMoneyTypographyX on BuildContext {
  WellPaidMoneyTypography get wellPaidMoneyType {
    return Theme.of(this).extension<WellPaidMoneyTypography>() ??
        WellPaidMoneyTypography.fromTextTheme(Theme.of(this).textTheme);
  }
}
