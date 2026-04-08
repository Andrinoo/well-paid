import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/format/brl_cents.dart';
import '../../../core/network/dio_client.dart';
import '../../dashboard/application/dashboard_providers.dart';
import '../../expenses/application/expenses_providers.dart';
import '../../../core/theme/well_paid_colors.dart';
import '../domain/dashboard_overview.dart';
import 'category_donut_chart.dart';
import 'due_urgency.dart';

/// Secções §5.4: resumo, rosca, a pagar, vencimentos, metas (placeholder).
class DashboardScrollContent extends ConsumerWidget {
  const DashboardScrollContent({super.key, required this.data});

  final DashboardOverview data;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final today = DateTime.now();

    return ListView(
      physics: const AlwaysScrollableScrollPhysics(),
      padding: const EdgeInsets.fromLTRB(16, 8, 16, 100),
      children: [
        _SectionCard(
          title: 'Resumo do mês',
          icon: Icons.dashboard_outlined,
          accent: WellPaidColors.navy,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              _kv(
                context,
                'Receitas',
                formatBrlFromCents(data.monthIncomeCents),
              ),
              _kv(
                context,
                'Despesas',
                formatBrlFromCents(data.monthExpenseTotalCents),
              ),
              _kv(
                context,
                'Saldo',
                formatBrlFromCents(data.monthBalanceCents),
                emphasize: true,
              ),
            ],
          ),
        ),
        const SizedBox(height: 12),
        _SectionCard(
          title: 'Despesas por categoria',
          icon: Icons.pie_chart_outline,
          accent: WellPaidColors.goldPressed,
          child: CategoryDonutChart(
            categories: data.spendingByCategory,
            monthExpenseTotalCents: data.monthExpenseTotalCents,
          ),
        ),
        const SizedBox(height: 12),
        _SectionCard(
          title: 'A pagar',
          icon: Icons.credit_card_outlined,
          accent: const Color(0xFFCC8A00),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              if (data.pendingPreview.isEmpty)
                Text(
                  'Nada pendente.',
                  style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                        color: WellPaidColors.navy.withValues(alpha: 0.65),
                      ),
                )
              else
                ...data.pendingPreview.map(
                  (e) => _PendingTile(
                    item: e,
                    onPay: e.isMine
                        ? () async {
                            final messenger = ScaffoldMessenger.of(context);
                            try {
                              await ref
                                  .read(expensesRepositoryProvider)
                                  .payExpense(e.id);
                              ref.invalidate(dashboardOverviewProvider);
                              ref.invalidate(expensesListProvider);
                              messenger.showSnackBar(
                                const SnackBar(
                                  content: Text('Despesa marcada como paga.'),
                                ),
                              );
                            } catch (err) {
                              messenger.showSnackBar(
                                SnackBar(
                                  content: Text(
                                    messageFromDio(err) ?? 'Erro ao pagar.',
                                  ),
                                ),
                              );
                            }
                          }
                        : null,
                  ),
                ),
              const Divider(height: 24),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    'Total pendente',
                    style: Theme.of(context).textTheme.titleSmall?.copyWith(
                          fontWeight: FontWeight.w700,
                          color: WellPaidColors.navy,
                        ),
                  ),
                  Text(
                    formatBrlFromCents(data.pendingTotalCents),
                    style: Theme.of(context).textTheme.titleSmall?.copyWith(
                          fontWeight: FontWeight.w800,
                          color: WellPaidColors.navy,
                        ),
                  ),
                ],
              ),
              const SizedBox(height: 8),
              Align(
                alignment: Alignment.centerRight,
                child: TextButton(
                  onPressed: () => context.push('/expenses?status=pending'),
                  child: const Text('Ver todas'),
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 12),
        _SectionCard(
          title: 'Próximos vencimentos',
          icon: Icons.event_note_outlined,
          accent: const Color(0xFFB76E00),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              if (data.upcomingDue.isEmpty)
                Text(
                  'Sem contas com vencimento no próximo mês.',
                  style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                        color: WellPaidColors.navy.withValues(alpha: 0.65),
                      ),
                )
              else
                ...data.upcomingDue.map((e) {
                  final due = e.dueDate;
                  final u = due != null
                      ? dueUrgencyFor(due, today)
                      : DueUrgency.low;
                  return _UpcomingTile(item: e, urgency: u);
                }),
              const SizedBox(height: 4),
              Align(
                alignment: Alignment.centerRight,
                child: TextButton(
                  onPressed: () => context.push('/expenses'),
                  child: const Text('Ver mais'),
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 12),
        _SectionCard(
          title: 'Metas',
          icon: Icons.flag_outlined,
          accent: WellPaidColors.navy,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              if (data.goalsPreview.isEmpty) ...[
                Text(
                  'Sem metas ativas no momento.',
                  style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                        color: WellPaidColors.navy.withValues(alpha: 0.7),
                      ),
                ),
              ] else
                ...data.goalsPreview.map(
                  (g) => _GoalRow(goal: g),
                ),
              const SizedBox(height: 8),
              OutlinedButton(
                onPressed: () => context.push('/goals'),
                child: const Text('Ver metas'),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _SectionCard extends StatelessWidget {
  const _SectionCard({
    required this.title,
    required this.icon,
    required this.accent,
    required this.child,
  });

  final String title;
  final IconData icon;
  final Color accent;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      container: true,
      label: title,
      child: TweenAnimationBuilder<double>(
        tween: Tween(begin: 0.98, end: 1),
        duration: const Duration(milliseconds: 260),
        curve: Curves.easeOutCubic,
        builder: (context, scale, _) => Transform.scale(
          scale: scale,
          child: Card(
            elevation: 0,
            color: WellPaidColors.creamMuted.withValues(alpha: 0.85),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(16),
              side: BorderSide(
                color: accent.withValues(alpha: 0.18),
              ),
            ),
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Row(
                    children: [
                      Container(
                        width: 34,
                        height: 34,
                        decoration: BoxDecoration(
                          color: accent.withValues(alpha: 0.12),
                          borderRadius: BorderRadius.circular(10),
                        ),
                        child: Icon(icon, size: 20, color: accent),
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: Text(
                          title,
                          style:
                              Theme.of(context).textTheme.titleMedium?.copyWith(
                                    fontWeight: FontWeight.w800,
                                    color: WellPaidColors.navy,
                                  ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  child,
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

Widget _kv(
  BuildContext context,
  String k,
  String v, {
  bool emphasize = false,
}) {
  return Padding(
    padding: const EdgeInsets.symmetric(vertical: 6),
    child: Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(
          k,
          style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                color: WellPaidColors.navy.withValues(alpha: 0.85),
              ),
        ),
        Text(
          v,
          style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                fontWeight: emphasize ? FontWeight.w800 : FontWeight.w600,
                color: WellPaidColors.navy,
              ),
        ),
      ],
    ),
  );
}

class _PendingTile extends StatelessWidget {
  const _PendingTile({required this.item, this.onPay});

  final PendingExpenseItem item;
  final Future<void> Function()? onPay;

  @override
  Widget build(BuildContext context) {
    final due = item.dueDate;
    final dueStr = due == null
        ? '—'
        : '${due.day.toString().padLeft(2, '0')}/${due.month.toString().padLeft(2, '0')}/${due.year}';

    return Semantics(
      label:
          '${item.description}, ${formatBrlFromCents(item.amountCents)}, vencimento $dueStr',
      child: Padding(
        padding: const EdgeInsets.only(bottom: 10),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    item.description,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                          fontWeight: FontWeight.w600,
                          color: WellPaidColors.navy,
                        ),
                  ),
                  Text(
                    'Venc. $dueStr${item.isMine ? '' : ' · Família'}',
                    style: Theme.of(context).textTheme.bodySmall?.copyWith(
                          color: WellPaidColors.navy.withValues(alpha: 0.6),
                        ),
                  ),
                ],
              ),
            ),
            Text(
              formatBrlFromCents(item.amountCents),
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    fontWeight: FontWeight.w700,
                    color: WellPaidColors.navy,
                  ),
            ),
            if (onPay != null) ...[
              const SizedBox(width: 4),
              IconButton(
                tooltip: 'Marcar como paga',
                onPressed: () => unawaited(onPay!()),
                icon: const Icon(Icons.payment_outlined, size: 22),
                color: WellPaidColors.goldPressed,
                constraints: const BoxConstraints(minWidth: 40, minHeight: 40),
                padding: EdgeInsets.zero,
              ),
            ],
          ],
        ),
      ),
    );
  }
}

class _UpcomingTile extends StatelessWidget {
  const _UpcomingTile({required this.item, required this.urgency});

  final PendingExpenseItem item;
  final DueUrgency urgency;

  @override
  Widget build(BuildContext context) {
    final due = item.dueDate!;
    final dueStr =
        '${due.day.toString().padLeft(2, '0')}/${due.month.toString().padLeft(2, '0')}/${due.year}';
    final accent = dueUrgencyAccent(urgency);

    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: DecoratedBox(
        decoration: BoxDecoration(
          border: Border(
            left: BorderSide(color: accent, width: 4),
          ),
          color: accent.withValues(alpha: 0.06),
          borderRadius: BorderRadius.circular(8),
        ),
        child: Padding(
          padding: const EdgeInsets.fromLTRB(12, 10, 10, 10),
          child: Row(
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      item.description,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                            fontWeight: FontWeight.w600,
                            color: WellPaidColors.navy,
                          ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      'Vence $dueStr',
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(
                            color: accent,
                            fontWeight: FontWeight.w600,
                          ),
                    ),
                  ],
                ),
              ),
              Text(
                formatBrlFromCents(item.amountCents),
                style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                      fontWeight: FontWeight.w700,
                      color: WellPaidColors.navy,
                    ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _GoalRow extends StatelessWidget {
  const _GoalRow({required this.goal});

  final GoalSummaryItem goal;

  @override
  Widget build(BuildContext context) {
    final p = goal.targetCents > 0
        ? (goal.currentCents / goal.targetCents).clamp(0.0, 1.0)
        : 0.0;
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            goal.isMine ? goal.title : '${goal.title} (família)',
            style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                  fontWeight: FontWeight.w600,
                ),
          ),
          const SizedBox(height: 6),
          ClipRRect(
            borderRadius: BorderRadius.circular(6),
            child: LinearProgressIndicator(
              value: p,
              minHeight: 8,
              backgroundColor: WellPaidColors.navy.withValues(alpha: 0.1),
              color: WellPaidColors.gold,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            '${formatBrlFromCents(goal.currentCents)} / ${formatBrlFromCents(goal.targetCents)}',
            style: Theme.of(context).textTheme.labelSmall,
          ),
        ],
      ),
    );
  }
}
