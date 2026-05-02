import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:phosphor_flutter/phosphor_flutter.dart';

import '../../../core/l10n/context_l10n.dart';
import '../../../core/theme/well_paid_colors.dart';
import '../../dashboard/application/dashboard_providers.dart';
import '../../dashboard/presentation/due_urgency.dart';

/// Atalhos extra: **A pagar** e **listas de compras** (Reserva fica só na barra inferior).
class ShellQuickPanel extends ConsumerWidget {
  const ShellQuickPanel({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    final overview = ref.watch(dashboardOverviewProvider).valueOrNull;
    final today = DateTime.now();
    final hasCritical = overview != null &&
        overview.pendingPreview.any((e) {
          final due = e.dueDate;
          if (due == null) return false;
          final u = dueUrgencyFor(due, today);
          return u == DueUrgency.overdue || u == DueUrgency.dueToday;
        });
    final hasPending = overview != null && overview.pendingPreview.isNotEmpty;

    return Padding(
      padding: const EdgeInsets.fromLTRB(12, 4, 12, 6),
      child: Row(
        children: [
          Expanded(
            child: _NavLikeShortcut(
              icon: PhosphorIconsRegular.invoice,
              label: l10n.dashToPay,
              dotColor: hasCritical
                  ? const Color(0xFFB00020)
                  : hasPending
                      ? const Color(0xFFF9A825)
                      : null,
              onTap: () => context.push('/to-pay'),
            ),
          ),
          Expanded(
            child: _NavLikeShortcut(
              icon: PhosphorIconsRegular.shoppingCartSimple,
              label: l10n.shoppingListsTitle,
              onTap: () => context.push('/shopping-lists'),
            ),
          ),
        ],
      ),
    );
  }
}

class _NavLikeShortcut extends StatelessWidget {
  const _NavLikeShortcut({
    required this.icon,
    required this.label,
    required this.onTap,
    this.dotColor,
  });

  final IconData icon;
  final String label;
  final VoidCallback onTap;
  final Color? dotColor;

  @override
  Widget build(BuildContext context) {
    final nav = NavigationBarTheme.of(context);
    const inactive = <WidgetState>{};
    final iconTheme = nav.iconTheme?.resolve(inactive) ??
        IconThemeData(
          size: 24,
          color: WellPaidColors.navy.withValues(alpha: 0.55),
        );
    final labelStyle = nav.labelTextStyle?.resolve(inactive) ??
        TextStyle(
          fontSize: 12,
          fontWeight: FontWeight.w500,
          color: WellPaidColors.navy.withValues(alpha: 0.65),
        );

    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(12),
        child: Semantics(
          button: true,
          label: label,
          child: Padding(
            padding: const EdgeInsets.symmetric(vertical: 6, horizontal: 4),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                SizedBox(
                  height: 24,
                  width: 40,
                  child: Stack(
                    clipBehavior: Clip.none,
                    alignment: Alignment.center,
                    children: [
                      IconTheme(
                        data: iconTheme,
                        child: Icon(icon),
                      ),
                      if (dotColor != null)
                        Positioned(
                          right: 2,
                          top: 0,
                          child: Container(
                            width: 8,
                            height: 8,
                            decoration: BoxDecoration(
                              color: dotColor,
                              shape: BoxShape.circle,
                              border: Border.all(
                                color: WellPaidColors.creamMuted,
                                width: 1.5,
                              ),
                            ),
                          ),
                        ),
                    ],
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  label,
                  maxLines: 2,
                  textAlign: TextAlign.center,
                  overflow: TextOverflow.ellipsis,
                  style: labelStyle,
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
