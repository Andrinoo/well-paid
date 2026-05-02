import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:phosphor_flutter/phosphor_flutter.dart';

import '../../../core/l10n/context_l10n.dart';
import '../../../core/theme/well_paid_colors.dart';
import '../../dashboard/application/dashboard_providers.dart';
import '../../dashboard/presentation/due_urgency.dart';

/// Atalhos para shell desktop: mesmo conceito que [ShellQuickPanel], com mais entradas.
class ShellQuickPanelDesktop extends ConsumerWidget {
  const ShellQuickPanelDesktop({super.key});

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
    final hasPending =
        overview != null && overview.pendingPreview.isNotEmpty;

    return Padding(
      padding: const EdgeInsets.fromLTRB(12, 8, 12, 10),
      child: Wrap(
        spacing: 8,
        runSpacing: 8,
        children: [
          _ShortcutChip(
            icon: PhosphorIconsRegular.invoice,
            label: l10n.dashToPay,
            dotColor: hasCritical
                ? const Color(0xFFB00020)
                : hasPending
                    ? const Color(0xFFF9A825)
                    : null,
            onTap: () => context.push('/to-pay'),
          ),
          _ShortcutChip(
            icon: PhosphorIconsRegular.shoppingCartSimple,
            label: l10n.shoppingListsTitle,
            onTap: () => context.push('/shopping-lists'),
          ),
          _ShortcutChip(
            icon: PhosphorIconsRegular.megaphone,
            label: l10n.pcNavAnnouncements,
            onTap: () => context.push('/announcements'),
          ),
          _ShortcutChip(
            icon: PhosphorIconsRegular.handCoins,
            label: l10n.pcNavReceivables,
            onTap: () => context.push('/receivables'),
          ),
          _ShortcutChip(
            icon: PhosphorIconsRegular.chartLineUp,
            label: l10n.pcNavInvestments,
            onTap: () => context.push('/investments'),
          ),
        ],
      ),
    );
  }
}

class _ShortcutChip extends StatelessWidget {
  const _ShortcutChip({
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
    return Material(
      color: WellPaidColors.creamMuted.withValues(alpha: 0.95),
      borderRadius: BorderRadius.circular(12),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(12),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              SizedBox(
                width: 28,
                height: 24,
                child: Stack(
                  clipBehavior: Clip.none,
                  alignment: Alignment.center,
                  children: [
                    Icon(
                      icon,
                      size: 22,
                      color: WellPaidColors.navy.withValues(alpha: 0.75),
                    ),
                    if (dotColor != null)
                      Positioned(
                        right: -2,
                        top: -2,
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
              const SizedBox(width: 8),
              Text(
                label,
                style: TextStyle(
                  fontWeight: FontWeight.w600,
                  fontSize: 13,
                  color: WellPaidColors.navy.withValues(alpha: 0.88),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
