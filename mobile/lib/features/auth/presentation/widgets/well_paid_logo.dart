import 'package:flutter/material.dart';

import '../../../../core/theme/well_paid_colors.dart';

/// Logo da tela de autenticação.
class WellPaidLogo extends StatelessWidget {
  const WellPaidLogo({
    super.key,
    this.maxHeight = 152,
    this.maxWidth = 320,
  });

  final double maxHeight;
  final double maxWidth;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      label: 'Well Paid',
      child: ConstrainedBox(
        constraints: BoxConstraints(
          maxHeight: maxHeight,
          maxWidth: maxWidth,
        ),
        child: Image.asset(
          'assets/images/login_logo.png',
          fit: BoxFit.contain,
          alignment: Alignment.center,
          filterQuality: FilterQuality.high,
          errorBuilder: (context, error, stackTrace) =>
              _FallbackMark(maxHeight: maxHeight, maxWidth: maxWidth),
        ),
      ),
    );
  }
}

class _FallbackMark extends StatelessWidget {
  const _FallbackMark({required this.maxHeight, required this.maxWidth});

  final double maxHeight;
  final double maxWidth;

  @override
  Widget build(BuildContext context) {
    final w = maxWidth.clamp(120.0, 280.0);
    final h = maxHeight.clamp(56.0, 120.0);
    return Container(
      width: w,
      height: h,
      alignment: Alignment.center,
      decoration: BoxDecoration(
        color: WellPaidColors.loginBackground,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(
          color: WellPaidColors.brandBlue.withValues(alpha: 0.4),
        ),
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [
            WellPaidColors.brandBlue,
            WellPaidColors.brandPurple,
          ],
        ),
      ),
      child: Text(
        'well paid',
        style: TextStyle(
          color: Colors.white.withValues(alpha: 0.95),
          fontWeight: FontWeight.w700,
          fontSize: h * 0.22,
          letterSpacing: 0.5,
        ),
      ),
    );
  }
}
