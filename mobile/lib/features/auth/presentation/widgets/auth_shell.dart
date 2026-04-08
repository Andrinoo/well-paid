import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../../../../core/theme/well_paid_colors.dart';
import 'well_paid_logo.dart';

/// Fluxo de autenticação: fundo alinhado ao logo, cartão compacto (~20% menor).
class AuthShell extends StatelessWidget {
  const AuthShell({
    super.key,
    required this.title,
    required this.formBody,
    this.subtitle,
    this.leading,
    this.footer,
    this.logoMaxHeight = 152,
  });

  final String title;
  final String? subtitle;
  final Widget formBody;
  final Widget? leading;
  final Widget? footer;
  final double logoMaxHeight;

  @override
  Widget build(BuildContext context) {
    final media = MediaQuery.of(context);
    final w = media.size.width;
    final sidePad = _sidePadding(w);
    final maxContent = math.min(352.0, w - 2 * sidePad).clamp(260.0, 352.0);
    final hInset = math.max(sidePad, (w - maxContent) / 2);

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
              const SizedBox(height: 14),
              formBody,
              if (footer != null) ...[
                const SizedBox(height: 9),
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
        padding: EdgeInsets.fromLTRB(10, 8, 16, subtitle != null ? 20 : 12),
        child: Column(
          children: [
            if (leading != null)
              Align(
                alignment: Alignment.centerLeft,
                child: leading!,
              )
            else
              const SizedBox(height: 8),
            Center(
              child: WellPaidLogo(
                maxHeight: logoMaxHeight,
                maxWidth: maxContent - 8,
              ),
            ),
            if (subtitle != null) ...[
              const SizedBox(height: 10),
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

    final card = Transform.translate(
      offset: const Offset(0, -14),
      child: Material(
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
          padding: const EdgeInsets.fromLTRB(16, 16, 16, 15),
          child: cardChild,
        ),
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
                math.max(20.0, media.padding.bottom + 12),
              ),
              sliver: SliverToBoxAdapter(
                child: SizedBox(
                  width: maxContent,
                  child: _AuthEntranceColumn(
                    topPadding: media.padding.top + 6,
                    header: RepaintBoundary(child: header),
                    card: RepaintBoundary(child: card),
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

/// Entrada única: header e cartão com fade + slide escalonados (leve, sem loops).
class _AuthEntranceColumn extends StatefulWidget {
  const _AuthEntranceColumn({
    required this.topPadding,
    required this.header,
    required this.card,
  });

  final double topPadding;
  final Widget header;
  final Widget card;

  @override
  State<_AuthEntranceColumn> createState() => _AuthEntranceColumnState();
}

class _AuthEntranceColumnState extends State<_AuthEntranceColumn>
    with SingleTickerProviderStateMixin {
  late final AnimationController _c;
  late final Animation<double> _headerOpacity;
  late final Animation<Offset> _headerSlide;
  late final Animation<double> _cardOpacity;
  late final Animation<Offset> _cardSlide;

  @override
  void initState() {
    super.initState();
    _c = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 700),
    );
    _headerOpacity = Tween<double>(begin: 0, end: 1).animate(
      CurvedAnimation(
        parent: _c,
        curve: const Interval(0.0, 0.44, curve: Curves.easeOutCubic),
      ),
    );
    _headerSlide = Tween<Offset>(
      begin: const Offset(0, 0.055),
      end: Offset.zero,
    ).animate(
      CurvedAnimation(
        parent: _c,
        curve: const Interval(0.0, 0.5, curve: Curves.easeOutCubic),
      ),
    );
    _cardOpacity = Tween<double>(begin: 0, end: 1).animate(
      CurvedAnimation(
        parent: _c,
        curve: const Interval(0.2, 0.9, curve: Curves.easeOutCubic),
      ),
    );
    _cardSlide = Tween<Offset>(
      begin: const Offset(0, 0.045),
      end: Offset.zero,
    ).animate(
      CurvedAnimation(
        parent: _c,
        curve: const Interval(0.2, 1.0, curve: Curves.easeOutCubic),
      ),
    );
    _c.forward();
  }

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        SizedBox(height: widget.topPadding),
        FadeTransition(
          opacity: _headerOpacity,
          child: SlideTransition(
            position: _headerSlide,
            child: widget.header,
          ),
        ),
        FadeTransition(
          opacity: _cardOpacity,
          child: SlideTransition(
            position: _cardSlide,
            child: widget.card,
          ),
        ),
      ],
    );
  }
}

double _sidePadding(double width) {
  if (width < 360) return 18;
  if (width < 520) return 22;
  if (width < 900) return 28;
  return 36;
}
