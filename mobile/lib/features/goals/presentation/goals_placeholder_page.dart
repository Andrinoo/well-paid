import 'package:flutter/material.dart';
import 'package:phosphor_flutter/phosphor_flutter.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/format/brl_cents.dart';
import '../../../core/format/locale_dates.dart';
import '../../../core/l10n/context_l10n.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/theme/well_paid_colors.dart';
import '../application/goals_providers.dart';
import '../domain/goal_item.dart';
import '../domain/goal_linear_pace.dart';
import 'goal_milestone_widgets.dart';
import 'goal_progress_milestone.dart';

/// Metas — listagem com resumo agregado (padrão comum em apps de poupança: totais + progresso por meta).
class GoalsPlaceholderPage extends ConsumerWidget {
  const GoalsPlaceholderPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    final async = ref.watch(goalsListProvider);
    return Scaffold(
      appBar: AppBar(
        leading: Navigator.of(context).canPop()
            ? IconButton(
                icon: const Icon(PhosphorIconsRegular.arrowLeft),
                onPressed: () => context.pop(),
              )
            : null,
        title: Text(l10n.goalsTitle),
        actions: [
          IconButton(
            tooltip: l10n.goalsAddTooltip,
            onPressed: () => context.push('/goals/new'),
            icon: const Icon(PhosphorIconsRegular.plusCircle),
          ),
          IconButton(
            tooltip: l10n.goalsRefresh,
            onPressed: () => ref.invalidate(goalsListProvider),
            icon: const Icon(PhosphorIconsRegular.arrowsClockwise),
          ),
        ],
      ),
      body: async.when(
        skipLoadingOnReload: true,
        loading: () => CustomScrollView(
          physics: const AlwaysScrollableScrollPhysics(),
          slivers: [
            SliverPadding(
              padding: const EdgeInsets.fromLTRB(16, 16, 16, 8),
              sliver: SliverToBoxAdapter(
                child: LinearProgressIndicator(
                  minHeight: 3,
                  color: WellPaidColors.gold,
                  backgroundColor: WellPaidColors.navy.withValues(alpha: 0.08),
                ),
              ),
            ),
            SliverPadding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              sliver: SliverList(
                delegate: SliverChildBuilderDelegate(
                  (_, i) => Padding(
                    padding: const EdgeInsets.only(bottom: 12),
                    child: Container(
                      height: 96,
                      decoration: BoxDecoration(
                        color: WellPaidColors.navy.withValues(alpha: 0.06),
                        borderRadius: BorderRadius.circular(16),
                      ),
                    ),
                  ),
                  childCount: 4,
                ),
              ),
            ),
          ],
        ),
        error: (e, _) => Center(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Text(
              messageFromDio(e, l10n) ?? l10n.goalsLoadError,
              textAlign: TextAlign.center,
            ),
          ),
        ),
        data: (items) {
          if (items.isEmpty) {
            return Center(
              child: Padding(
                padding: const EdgeInsets.all(24),
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Icon(
                      PhosphorIconsRegular.flag,
                      size: 56,
                      color: WellPaidColors.navy.withValues(alpha: 0.35),
                    ),
                    const SizedBox(height: 16),
                    Text(
                      l10n.goalsEmpty,
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        color: WellPaidColors.navy.withValues(alpha: 0.8),
                        fontSize: 16,
                      ),
                    ),
                    const SizedBox(height: 20),
                    FilledButton.tonalIcon(
                      onPressed: () => context.push('/goals/new'),
                      icon: const Icon(PhosphorIconsRegular.plus),
                      label: Text(l10n.goalFormSave),
                      style: FilledButton.styleFrom(
                        foregroundColor: WellPaidColors.navy,
                      ),
                    ),
                  ],
                ),
              ),
            );
          }
          final active = items.where((g) => g.isActive).toList();
          var sumCur = 0;
          var sumTgt = 0;
          for (final g in active) {
            sumCur += g.currentCents;
            sumTgt += g.targetCents;
          }
          final agg = sumTgt > 0 ? (sumCur / sumTgt).clamp(0.0, 1.0) : 0.0;

          return RefreshIndicator(
            color: WellPaidColors.navy,
            onRefresh: () async {
              ref.invalidate(goalsListProvider);
              await ref.read(goalsListProvider.future);
            },
            child: CustomScrollView(
              physics: const AlwaysScrollableScrollPhysics(),
              slivers: [
                SliverPadding(
                  padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
                  sliver: SliverToBoxAdapter(
                    child: Text(
                      l10n.goalsScreenHint,
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(
                            color: WellPaidColors.navy.withValues(alpha: 0.62),
                            height: 1.35,
                          ),
                    ),
                  ),
                ),
                if (active.isNotEmpty)
                  SliverPadding(
                    padding: const EdgeInsets.symmetric(horizontal: 16),
                    sliver: SliverToBoxAdapter(
                      child: _GoalsAggregateCard(
                        title: l10n.goalsAggregateTitle,
                        line: l10n.goalsAggregateLine(
                          active.length,
                          formatBrlFromCents(sumCur),
                          formatBrlFromCents(sumTgt),
                        ),
                        progress: agg,
                      ),
                    ),
                  ),
                if (active.isNotEmpty)
                  const SliverPadding(
                    padding: EdgeInsets.only(bottom: 8),
                    sliver: SliverToBoxAdapter(child: SizedBox.shrink()),
                  ),
                SliverPadding(
                  padding: const EdgeInsets.fromLTRB(16, 0, 16, 24),
                  sliver: SliverList.separated(
                    itemCount: items.length,
                    separatorBuilder: (_, _) => const SizedBox(height: 12),
                    itemBuilder: (context, i) {
                      return _GoalListTile(
                        goal: items[i],
                        onTap: () => context.push('/goals/${items[i].id}'),
                      );
                    },
                  ),
                ),
              ],
            ),
          );
        },
      ),
    );
  }
}

class _GoalsAggregateCard extends StatelessWidget {
  const _GoalsAggregateCard({
    required this.title,
    required this.line,
    required this.progress,
  });

  final String title;
  final String line;
  final double progress;

  @override
  Widget build(BuildContext context) {
    return Card(
      elevation: 0,
      color: WellPaidColors.navy.withValues(alpha: 0.06),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: BorderSide(color: WellPaidColors.navy.withValues(alpha: 0.08)),
      ),
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(
                  PhosphorIconsRegular.chartPie,
                  size: 22,
                  color: WellPaidColors.navy.withValues(alpha: 0.85),
                ),
                const SizedBox(width: 8),
                Text(
                  title,
                  style: Theme.of(context).textTheme.titleSmall?.copyWith(
                        fontWeight: FontWeight.w800,
                        color: WellPaidColors.navy,
                      ),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Text(
              line,
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: WellPaidColors.navy.withValues(alpha: 0.78),
                    fontWeight: FontWeight.w600,
                  ),
            ),
            const SizedBox(height: 10),
            ClipRRect(
              borderRadius: BorderRadius.circular(8),
              child: LinearProgressIndicator(
                value: progress,
                minHeight: 10,
                backgroundColor: WellPaidColors.navy.withValues(alpha: 0.1),
                color: WellPaidColors.gold,
              ),
            ),
            const SizedBox(height: 6),
            Text(
              '${(progress * 100).round()}%',
              style: Theme.of(context).textTheme.labelLarge?.copyWith(
                    color: WellPaidColors.navy.withValues(alpha: 0.55),
                    fontWeight: FontWeight.w700,
                  ),
            ),
          ],
        ),
      ),
    );
  }
}

class _GoalListTile extends StatelessWidget {
  const _GoalListTile({
    required this.goal,
    required this.onTap,
  });

  final GoalItem goal;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final p = goal.targetCents > 0
        ? (goal.currentCents / goal.targetCents).clamp(0.0, 1.0)
        : 0.0;
    final title = goal.isMine
        ? goal.title
        : '${goal.title}${l10n.dashGoalFamilySuffix}';
    final milestone = resolveGoalProgressMilestone(
      currentCents: goal.currentCents,
      targetCents: goal.targetCents,
    );
    final linearPace = goal.isActive && goal.currentCents < goal.targetCents
        ? computeGoalLinearPaceEstimate(goal)
        : null;

    return Material(
      color: Colors.white,
      elevation: 1,
      shadowColor: WellPaidColors.navy.withValues(alpha: 0.06),
      borderRadius: BorderRadius.circular(16),
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                width: 44,
                height: 44,
                decoration: BoxDecoration(
                  color: WellPaidColors.gold.withValues(alpha: 0.22),
                  borderRadius: BorderRadius.circular(14),
                ),
                child: Icon(
                  PhosphorIconsRegular.coins,
                  color: WellPaidColors.navy.withValues(alpha: 0.88),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Expanded(
                          child: Text(
                            title,
                            style: Theme.of(context)
                                .textTheme
                                .titleSmall
                                ?.copyWith(
                                  color: WellPaidColors.navy,
                                  fontWeight: FontWeight.w800,
                                ),
                          ),
                        ),
                        if (milestone != null) ...[
                          const SizedBox(width: 6),
                          GoalMilestoneChip(milestone: milestone),
                        ],
                        if (!goal.isActive) ...[
                          const SizedBox(width: 6),
                          Container(
                            padding: const EdgeInsets.symmetric(
                              horizontal: 8,
                              vertical: 2,
                            ),
                            decoration: BoxDecoration(
                              color: WellPaidColors.navy.withValues(alpha: 0.08),
                              borderRadius: BorderRadius.circular(8),
                            ),
                            child: Text(
                              l10n.goalInactiveBadge,
                              style: Theme.of(context)
                                  .textTheme
                                  .labelSmall
                                  ?.copyWith(
                                    color: WellPaidColors.navy
                                        .withValues(alpha: 0.65),
                                    fontWeight: FontWeight.w700,
                                  ),
                            ),
                          ),
                        ],
                      ],
                    ),
                    const SizedBox(height: 8),
                    ClipRRect(
                      borderRadius: BorderRadius.circular(6),
                      child: LinearProgressIndicator(
                        value: p,
                        minHeight: 8,
                        backgroundColor:
                            WellPaidColors.navy.withValues(alpha: 0.1),
                        color: WellPaidColors.gold,
                      ),
                    ),
                    const SizedBox(height: 6),
                    Row(
                      children: [
                        Expanded(
                          child: Text(
                            '${formatBrlFromCents(goal.currentCents)} / ${formatBrlFromCents(goal.targetCents)}',
                            style: Theme.of(context)
                                .textTheme
                                .bodySmall
                                ?.copyWith(
                                  color: WellPaidColors.navy
                                      .withValues(alpha: 0.72),
                                  fontWeight: FontWeight.w600,
                                ),
                          ),
                        ),
                        Text(
                          '${(p * 100).round()}%',
                          style: Theme.of(context)
                              .textTheme
                              .labelLarge
                              ?.copyWith(
                                color: WellPaidColors.navy
                                    .withValues(alpha: 0.55),
                                fontWeight: FontWeight.w800,
                              ),
                        ),
                      ],
                    ),
                    if (linearPace != null) ...[
                      const SizedBox(height: 6),
                      Text(
                        l10n.goalLinearPaceListHint(
                          formatBrlFromCents(linearPace.avgCentsPerMonth),
                          formatMonthYearUi(
                            context,
                            linearPace.estimatedCompletionMonthEnd,
                          ),
                        ),
                        style: Theme.of(context).textTheme.labelMedium?.copyWith(
                              color:
                                  WellPaidColors.navy.withValues(alpha: 0.52),
                              fontWeight: FontWeight.w600,
                              height: 1.25,
                            ),
                      ),
                    ],
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
