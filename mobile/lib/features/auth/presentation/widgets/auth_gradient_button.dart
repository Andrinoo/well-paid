import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../../core/theme/well_paid_colors.dart';

/// CTA principal do fluxo auth (gradiente azul → roxo), com micro-escala e háptica.
class AuthGradientButton extends StatefulWidget {
  const AuthGradientButton({
    super.key,
    required this.label,
    required this.onPressed,
    this.busy = false,
  });

  final String label;
  final VoidCallback? onPressed;
  final bool busy;

  @override
  State<AuthGradientButton> createState() => _AuthGradientButtonState();
}

class _AuthGradientButtonState extends State<AuthGradientButton> {
  bool _pressed = false;

  @override
  void didUpdateWidget(AuthGradientButton oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.busy && !oldWidget.busy) {
      _pressed = false;
    }
  }

  @override
  Widget build(BuildContext context) {
    final disabled = widget.onPressed == null || widget.busy;

    final child = Material(
      color: Colors.transparent,
      clipBehavior: Clip.antiAlias,
      borderRadius: BorderRadius.circular(12),
      child: InkWell(
        onTap: disabled
            ? null
            : () {
                HapticFeedback.lightImpact();
                widget.onPressed?.call();
              },
        onHighlightChanged: disabled
            ? null
            : (highlighted) => setState(() => _pressed = highlighted),
        borderRadius: BorderRadius.circular(12),
        splashColor: Colors.white.withValues(alpha: 0.18),
        highlightColor: Colors.white.withValues(alpha: 0.08),
        child: Ink(
          decoration: BoxDecoration(
            gradient: LinearGradient(
              colors: disabled
                  ? [
                      WellPaidColors.navyMid.withValues(alpha: 0.55),
                      WellPaidColors.gold.withValues(alpha: 0.45),
                    ]
                  : const [
                      WellPaidColors.navyMid,
                      WellPaidColors.gold,
                    ],
            ),
            borderRadius: BorderRadius.circular(12),
            boxShadow: disabled
                ? null
                : [
                    BoxShadow(
                      color: WellPaidColors.gold.withValues(alpha: 0.28),
                      blurRadius: 14,
                      offset: const Offset(0, 6),
                    ),
                  ],
          ),
          child: Container(
            width: double.infinity,
            padding: const EdgeInsets.symmetric(vertical: 10),
            alignment: Alignment.center,
            child: widget.busy
                ? const SizedBox(
                    height: 22,
                    width: 22,
                    child: CircularProgressIndicator(
                      strokeWidth: 2,
                      color: Colors.white,
                    ),
                  )
                : Text(
                    widget.label,
                    style: const TextStyle(
                      color: Colors.white,
                      fontWeight: FontWeight.w700,
                      letterSpacing: 0.3,
                      fontSize: 14,
                    ),
                  ),
          ),
        ),
      ),
    );

    return MouseRegion(
      cursor: disabled ? SystemMouseCursors.basic : SystemMouseCursors.click,
      child: AnimatedScale(
        scale: _pressed && !disabled ? 0.985 : 1.0,
        duration: const Duration(milliseconds: 100),
        curve: Curves.easeOutCubic,
        child: child,
      ),
    );
  }
}
