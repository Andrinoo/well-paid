import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../../../../core/l10n/context_l10n.dart';
import '../../../../core/theme/well_paid_colors.dart';
import 'well_paid_logo.dart';

/// Fluxo de autenticação: fundo alinhado ao logo, cartão com respiro vertical.
class AuthShell extends StatelessWidget {
  const AuthShell({
    super.key,
    required this.title,
    required this.formBody,
    this.subtitle,
    this.leading,
    this.footer,
    this.logoMaxHeight,
  });

  final String title;
  final String? subtitle;
  final Widget formBody;
  final Widget? leading;
  final Widget? footer;
  final double? logoMaxHeight;

  @override
  Widget build(BuildContext context) {
    final media = MediaQuery.of(context);
    final w = media.size.width;
    final h = media.size.height;
    final sidePad = _sidePadding(w);
    final maxContent = math.min(380.0, w - 2 * sidePad).clamp(280.0, 380.0);
    final hInset = math.max(sidePad, (w - maxContent) / 2);

    final logoH = logoMaxHeight ??
        (h < 700 ? 128.0 : (h < 820 ? 144.0 : 158.0));
    final gapLogoToWordmark = h < 700 ? 14.0 : 18.0;
    final gapWordmarkToCard = h < 640 ? 28.0 : (h < 780 ? 36.0 : 44.0);

    final titleStyle = Theme.of(context).textTheme.titleMedium?.copyWith(
          color: WellPaidColors.authOnCard,
          fontWeight: FontWeight.w700,
          letterSpacing: -0.12,
          fontSize: 16,
        );

    final cardChild = Theme(
      data: Theme.of(context).copyWith(
        textTheme: Theme.of(context).textTheme.apply(
          bodyColor: WellPaidColors.authOnCard,
          displayColor: WellPaidColors.authOnCard,
        ),
        textSelectionTheme: TextSelectionThemeData(
          cursorColor: WellPaidColors.brandBlue,
          selectionColor: WellPaidColors.brandBlue.withValues(alpha: 0.35),
          selectionHandleColor: WellPaidColors.brandBlue,
        ),
      ),
      child: DefaultTextStyle(
        style: WellPaidColors.authInputTextStyle,
        child: IconTheme(
          data: IconThemeData(
            color: WellPaidColors.brandBlue.withValues(alpha: 0.92),
            size: 20,
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(title, style: titleStyle),
              const SizedBox(height: 16),
              formBody,
              if (footer != null) ...[
                const SizedBox(height: 12),
                footer!,
              ],
            ],
          ),
        ),
      ),
    );

    final header = ColoredBox(
      color: WellPaidColors.loginBackground,
      child: Padding(
        padding: EdgeInsets.fromLTRB(10, 8, 16, subtitle != null ? 18 : 10),
        child: Column(
          children: [
            if (leading != null)
              Align(
                alignment: Alignment.centerLeft,
                child: leading!,
              )
            else
              const SizedBox(height: 4),
            Center(
              child: WellPaidLogo(
                maxHeight: logoH,
                maxWidth: maxContent - 8,
              ),
            ),
            SizedBox(height: gapLogoToWordmark),
            _BrandWordmark(text: context.l10n.appTitle),
            if (subtitle != null) ...[
              const SizedBox(height: 12),
              Text(
                subtitle!,
                textAlign: TextAlign.center,
                style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                      color: Colors.white.withValues(alpha: 0.78),
                      height: 1.35,
                    ),
              ),
            ],
          ],
        ),
      ),
    );

    final card = Material(
      elevation: 6,
      shadowColor: Colors.black54,
      color: WellPaidColors.authCard,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(18),
        side: BorderSide(
          color: WellPaidColors.brandBlue.withValues(alpha: 0.32),
        ),
      ),
      child: Padding(
        padding: const EdgeInsets.fromLTRB(18, 18, 18, 17),
        child: cardChild,
      ),
    );

    return Scaffold(
      backgroundColor: WellPaidColors.loginBackground,
      resizeToAvoidBottomInset: true,
      body: ColoredBox(
        color: WellPaidColors.loginBackground,
        child: CustomScrollView(
          keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
          physics: const ClampingScrollPhysics(),
          slivers: [
            SliverPadding(
              padding: EdgeInsets.fromLTRB(
                hInset,
                0,
                hInset,
                math.max(24.0, media.padding.bottom + 16),
              ),
              sliver: SliverToBoxAdapter(
                child: SizedBox(
                  width: maxContent,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      SizedBox(height: media.padding.top + (h < 700 ? 8 : 16)),
                      RepaintBoundary(child: header),
                      SizedBox(height: gapWordmarkToCard),
                      RepaintBoundary(child: card),
                    ],
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

/// Título inspirado no contraste prata/dourado do logo (apenas tipografia).
class _BrandWordmark extends StatelessWidget {
  const _BrandWordmark({required this.text});

  final String text;

  @override
  Widget build(BuildContext context) {
    final parts = text.trim().split(RegExp(r'\s+'));
    final first = parts.isNotEmpty ? parts.first : text;
    final rest = parts.length > 1 ? parts.sublist(1).join(' ') : '';

    return FittedBox(
      fit: BoxFit.scaleDown,
      child: Row(
        mainAxisSize: MainAxisSize.min,
        mainAxisAlignment: MainAxisAlignment.center,
        crossAxisAlignment: CrossAxisAlignment.baseline,
        textBaseline: TextBaseline.alphabetic,
        children: [
          _gradientWord(
            first,
            const [
              Color(0xFFEEF1F8),
              Color(0xFFC8CDDA),
              Color(0xFF9AA3B8),
            ],
          ),
          if (rest.isNotEmpty) ...[
            const SizedBox(width: 10),
            _gradientWord(
              rest,
              const [
                Color(0xFFF0E6B8),
                WellPaidColors.gold,
                Color(0xFFB8943D),
              ],
            ),
          ],
        ],
      ),
    );
  }

  static Widget _gradientWord(String word, List<Color> colors) {
    return ShaderMask(
      blendMode: BlendMode.srcIn,
      shaderCallback: (bounds) => LinearGradient(
        begin: Alignment.topLeft,
        end: Alignment.bottomRight,
        colors: colors,
      ).createShader(bounds),
      child: Text(
        word,
        style: const TextStyle(
          fontSize: 30,
          fontWeight: FontWeight.w800,
          letterSpacing: 0.6,
          height: 1.05,
          color: Colors.white,
        ),
      ),
    );
  }
}

double _sidePadding(double width) {
  if (width < 360) return 18;
  if (width < 520) return 22;
  if (width < 900) return 28;
  return 36;
}
