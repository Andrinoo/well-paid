import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../../../../core/theme/well_paid_colors.dart';

/// Logo da tela de autenticação (PNG 2048×2048).
///
/// Usa [cacheWidth]/[cacheHeight] em função do [devicePixelRatio] para o decode
/// coincidir com os pixéis reais no ecrã (evita “blur” no web e em ecrãs HiDPI).
class WellPaidLogo extends StatelessWidget {
  const WellPaidLogo({super.key, this.maxHeight = 152, this.maxWidth = 320});

  final double maxHeight;
  final double maxWidth;

  static const String _assetPath = 'assets/images/well_paid_logo.png';
  static const int _sourceMaxPx = 2048;

  @override
  Widget build(BuildContext context) {
    final dpr = MediaQuery.devicePixelRatioOf(context);
    // O asset é quadrado. Passar cacheWidth e cacheHeight diferentes faz o decode
    // esticar o bitmap e achatar o emblema. Usar um único lado em pixéis.
    final logicalSide = math.min(maxWidth, maxHeight);
    final cacheSide = math.min(_sourceMaxPx, (logicalSide * dpr).round());

    return Semantics(
      label: 'Well Paid',
      child: ConstrainedBox(
        constraints: BoxConstraints(maxHeight: maxHeight, maxWidth: maxWidth),
        child: Image.asset(
          _assetPath,
          fit: BoxFit.contain,
          alignment: Alignment.center,
          filterQuality: FilterQuality.high,
          isAntiAlias: true,
          cacheWidth: cacheSide,
          cacheHeight: cacheSide,
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
        border: Border.all(color: WellPaidColors.gold.withValues(alpha: 0.45)),
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [WellPaidColors.navyMid, WellPaidColors.goldPressed],
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
