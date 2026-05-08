import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../../../../core/l10n/context_l10n.dart';
import '../../../../core/theme/well_paid_colors.dart';
import 'well_paid_logo.dart';

/// Fluxo de autenticação: fundo escuro, conteúdo **centrado** (sem painel lateral).
/// Pensado para monitores largos: largura máxima do cartão + emblema quadrado.
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
    final layoutW = w;
    final sidePad = _sidePadding(layoutW);
    // Cartão relativamente estreito e centrado (estilo bancário em desktop).
    final cap = layoutW >= 1200
        ? 420.0
        : (layoutW >= 900 ? 440.0 : (layoutW >= 600 ? 404.0 : 380.0));
    final maxContent = math.min(cap, layoutW - 2 * sidePad).clamp(280.0, cap);
    final hInset = math.max(sidePad, (layoutW - maxContent) / 2);

    final logoH = logoMaxHeight ?? _authLogoHeight(layoutW, h);
    final emblemSide = math.min(logoH, maxContent - 24);
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
          cursorColor: WellPaidColors.gold,
          selectionColor: WellPaidColors.gold.withValues(alpha: 0.35),
          selectionHandleColor: WellPaidColors.gold,
        ),
      ),
      child: DefaultTextStyle(
        style: WellPaidColors.authInputTextStyle,
        child: IconTheme(
          data: IconThemeData(
            color: WellPaidColors.gold.withValues(alpha: 0.92),
            size: 20,
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(title, style: titleStyle),
              const SizedBox(height: 16),
              formBody,
              if (footer != null) ...[const SizedBox(height: 12), footer!],
            ],
          ),
        ),
      ),
    );

    final header = Padding(
      padding: EdgeInsets.fromLTRB(10, 8, 16, subtitle != null ? 18 : 10),
      child: Column(
        children: [
          if (leading != null)
            Align(alignment: Alignment.centerLeft, child: leading!)
          else
            const SizedBox(height: 4),
          Center(
            child: WellPaidLogo(maxHeight: emblemSide, maxWidth: emblemSide),
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
    );

    final card = Material(
      elevation: 10,
      shadowColor: Colors.black.withValues(alpha: 0.42),
      color: WellPaidColors.authCard,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(20),
        side: BorderSide(
          color: WellPaidColors.authCardBorder.withValues(alpha: 0.5),
        ),
      ),
      child: Padding(
        padding: const EdgeInsets.fromLTRB(22, 22, 22, 20),
        child: cardChild,
      ),
    );

    final scrollContent = CustomScrollView(
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
    );

    final gradientBox = DecoratedBox(
      decoration: const BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: [Color(0xFF0E1624), Color(0xFF06070A), Color(0xFF000000)],
          stops: [0.0, 0.45, 1.0],
        ),
      ),
      child: scrollContent,
    );

    return Scaffold(
      backgroundColor: WellPaidColors.loginBackground,
      resizeToAvoidBottomInset: true,
      body: gradientBox,
    );
  }
}

/// Título inspirado no contraste prata/dourado do logo (apenas tipografia).
class _BrandWordmark extends StatelessWidget {
  const _BrandWordmark({required this.text});

  final String text;

  @override
  Widget build(BuildContext context) {
    final w = MediaQuery.sizeOf(context).width;
    final fontSize = w >= 900 ? 34.0 : (w >= 600 ? 32.0 : 30.0);
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
          _gradientWord(first, const [
            Color(0xFFEEF1F8),
            Color(0xFFC8CDDA),
            Color(0xFF9AA3B8),
          ], fontSize),
          if (rest.isNotEmpty) ...[
            SizedBox(width: fontSize >= 32 ? 12 : 10),
            _gradientWord(rest, const [
              Color(0xFFF0E6B8),
              WellPaidColors.gold,
              Color(0xFFB8943D),
            ], fontSize),
          ],
        ],
      ),
    );
  }

  static Widget _gradientWord(
    String word,
    List<Color> colors,
    double fontSize,
  ) {
    return ShaderMask(
      blendMode: BlendMode.srcIn,
      shaderCallback: (bounds) => LinearGradient(
        begin: Alignment.topLeft,
        end: Alignment.bottomRight,
        colors: colors,
      ).createShader(bounds),
      child: Text(
        word,
        style: TextStyle(
          fontSize: fontSize,
          fontWeight: FontWeight.w800,
          letterSpacing: 0.55,
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

/// Altura do emblema (quadrado): proporcional mas sem dominar em monitores grandes.
double _authLogoHeight(double width, double height) {
  if (height < 700) {
    return width >= 600 ? 136 : 112;
  }
  if (height < 820) {
    return width >= 600 ? 152 : 128;
  }
  if (width >= 1400) return 160;
  if (width >= 900) return 168;
  if (width >= 600) return 160;
  return 144;
}
