import 'package:flutter/material.dart';

import '../../theme/well_paid_colors.dart';
import '../../theme/well_paid_money_typography.dart'
    show WellPaidMoneyTypographyX;
import '../../theme/well_paid_spacing.dart';

/// Linha estilo tabela para listas desktop (fracções flex).
class AppListRow extends StatelessWidget {
  const AppListRow({
    super.key,
    required this.children,
    this.onTap,
    this.minHeight = 52,
    this.leading,
    this.trailing,
  });

  final List<Widget> children;
  final VoidCallback? onTap;
  final double minHeight;
  final Widget? leading;
  final Widget? trailing;

  @override
  Widget build(BuildContext context) {
    final sp = context.wellPaidSpacing;
    final row = Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        child: ConstrainedBox(
          constraints: BoxConstraints(minHeight: minHeight),
          child: Padding(
            padding: EdgeInsets.symmetric(horizontal: sp.sm, vertical: sp.xs),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.center,
              children: [
                if (leading != null) ...[leading!, SizedBox(width: sp.sm)],
                ...children,
                if (trailing != null) trailing!,
              ],
            ),
          ),
        ),
      ),
    );
    return row;
  }
}

/// Célula alinhada para [AppListRow] com flex.
class AppListCell extends StatelessWidget {
  const AppListCell({
    super.key,
    required this.child,
    this.flex = 1,
    this.alignment = Alignment.centerLeft,
  });

  final Widget child;
  final int flex;
  final Alignment alignment;

  @override
  Widget build(BuildContext context) {
    return Expanded(
      flex: flex,
      child: Align(alignment: alignment, child: child),
    );
  }
}

/// Cabeçalho de colunas para listas tipo tabela.
class AppListHeaderRow extends StatelessWidget {
  const AppListHeaderRow({super.key, required this.cells, this.minHeight = 40});

  final List<Widget> cells;
  final double minHeight;

  @override
  Widget build(BuildContext context) {
    final sp = context.wellPaidSpacing;
    return Container(
      constraints: BoxConstraints(minHeight: minHeight),
      padding: EdgeInsets.symmetric(horizontal: sp.sm, vertical: sp.xs),
      decoration: BoxDecoration(
        color: WellPaidColors.creamMuted.withValues(alpha: 0.45),
        borderRadius: BorderRadius.vertical(top: Radius.circular(sp.sm)),
        border: Border(
          bottom: BorderSide(color: WellPaidColors.navy.withValues(alpha: 0.1)),
        ),
      ),
      child: Row(children: cells),
    );
  }
}

extension WellPaidMoneyTableStyle on BuildContext {
  TextStyle moneyTableStyle({Color? color}) {
    final base = wellPaidMoneyType.tableCell;
    return color != null ? base.copyWith(color: color) : base;
  }
}
