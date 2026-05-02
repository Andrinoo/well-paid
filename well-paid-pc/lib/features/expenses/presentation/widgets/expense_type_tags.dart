import 'package:flutter/material.dart';

import '../../../../core/l10n/context_l10n.dart';
import '../../../../core/theme/well_paid_colors.dart';
import '../../domain/expense_item.dart';

/// Etiquetas de 3 letras: **PAR** (parcelamento), **REC** (recorrente).
class ExpenseTypeTags extends StatelessWidget {
  const ExpenseTypeTags({super.key, required this.item, this.compact = false});

  final ExpenseItem item;
  final bool compact;

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final tags = <Widget>[];

    if (item.isInstallmentPlan) {
      tags.add(_Tag(
        label: l10n.expenseTagPar,
        tooltip: l10n.expenseTagParA11y,
        background: const Color(0xFFE3F2FD),
        foreground: const Color(0xFF1565C0),
        compact: compact,
      ));
    }

    final isRecurringMember = !item.isInstallmentPlan &&
        item.recurringSeriesId != null &&
        item.recurringSeriesId!.isNotEmpty;
    if (isRecurringMember) {
      tags.add(_Tag(
        label: l10n.expenseTagRec,
        tooltip: l10n.expenseTagRecA11y,
        background: WellPaidColors.gold.withValues(alpha: 0.35),
        foreground: WellPaidColors.navy,
        compact: compact,
      ));
    }

    if (tags.isEmpty) return const SizedBox.shrink();

    return MergeSemantics(
      child: Wrap(
        spacing: compact ? 4 : 6,
        runSpacing: 4,
        children: tags,
      ),
    );
  }
}

class _Tag extends StatelessWidget {
  const _Tag({
    required this.label,
    required this.tooltip,
    required this.background,
    required this.foreground,
    required this.compact,
  });

  final String label;
  final String tooltip;
  final Color background;
  final Color foreground;
  final bool compact;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      label: tooltip,
      container: true,
      child: Tooltip(
        message: tooltip,
        child: Container(
          padding: EdgeInsets.symmetric(
            horizontal: compact ? 5 : 6,
            vertical: compact ? 2 : 3,
          ),
          decoration: BoxDecoration(
            color: background,
            borderRadius: BorderRadius.circular(6),
            border: Border.all(color: foreground.withValues(alpha: 0.35)),
          ),
          child: Text(
            label,
            style: TextStyle(
              fontSize: compact ? 9.5 : 10.5,
              fontWeight: FontWeight.w800,
              letterSpacing: 0.6,
              color: foreground,
            ),
          ),
        ),
      ),
    );
  }
}
