import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:phosphor_flutter/phosphor_flutter.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/format/brl_cents.dart';
import '../../../core/format/brl_currency_input_formatter.dart';
import '../../../core/format/locale_dates.dart';
import '../../../core/format/parse_brl_input.dart';
import '../../../core/l10n/context_l10n.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/theme/well_paid_colors.dart';
import '../../dashboard/application/dashboard_providers.dart';
import '../application/goals_providers.dart';
import '../domain/goal_item.dart';
import 'goal_linear_pace_section.dart';
import 'goal_milestone_widgets.dart';
import 'goal_progress_milestone.dart';

class GoalDetailPage extends ConsumerWidget {
  const GoalDetailPage({super.key, required this.goalId});

  final String goalId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    final asyncGoal = ref.watch(goalProvider(goalId));

    return asyncGoal.when(
      loading: () => Scaffold(
        appBar: AppBar(title: Text(l10n.goalDetailTitle)),
        body: const Center(child: CircularProgressIndicator()),
      ),
      error: (e, _) => Scaffold(
        appBar: AppBar(title: Text(l10n.goalDetailTitle)),
        body: Center(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Text(
              messageFromDio(e, l10n) ?? l10n.goalsLoadError,
              textAlign: TextAlign.center,
            ),
          ),
        ),
      ),
      data: (goal) => _GoalDetailBody(goalId: goalId, goal: goal),
    );
  }
}

class _GoalDetailBody extends ConsumerWidget {
  const _GoalDetailBody({required this.goalId, required this.goal});

  final String goalId;
  final GoalItem goal;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    final messenger = ScaffoldMessenger.of(context);
    final progress = goal.targetCents > 0
        ? (goal.currentCents / goal.targetCents).clamp(0.0, 1.0)
        : 0.0;
    final remaining = (goal.targetCents - goal.currentCents).clamp(0, 1 << 62);
    final completed = goal.currentCents >= goal.targetCents && goal.targetCents > 0;
    final milestone = resolveGoalProgressMilestone(
      currentCents: goal.currentCents,
      targetCents: goal.targetCents,
    );

    Future<void> onArchive() async {
      final ok = await showDialog<bool>(
        context: context,
        builder: (ctx) => AlertDialog(
          title: Text(l10n.goalArchiveTitle),
          content: Text(l10n.goalArchiveBody),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx, false),
              child: Text(l10n.cancel),
            ),
            FilledButton(
              onPressed: () => Navigator.pop(ctx, true),
              child: Text(l10n.goalArchive),
            ),
          ],
        ),
      );
      if (ok != true || !context.mounted) return;
      try {
        await ref.read(goalsRepositoryProvider).updateGoalFields(
              id: goalId,
              isActive: false,
            );
        ref.invalidate(goalProvider(goalId));
        ref.invalidate(goalsListProvider);
        ref.invalidate(dashboardOverviewProvider);
        if (context.mounted) {
          messenger.showSnackBar(SnackBar(content: Text(l10n.goalArchivedSnackbar)));
        }
      } catch (e) {
        if (context.mounted) {
          messenger.showSnackBar(
            SnackBar(content: Text(messageFromDio(e, l10n) ?? l10n.goalSaveError)),
          );
        }
      }
    }

    Future<void> onReactivate() async {
      try {
        await ref.read(goalsRepositoryProvider).updateGoalFields(
              id: goalId,
              isActive: true,
            );
        ref.invalidate(goalProvider(goalId));
        ref.invalidate(goalsListProvider);
        ref.invalidate(dashboardOverviewProvider);
        if (context.mounted) {
          messenger.showSnackBar(SnackBar(content: Text(l10n.goalReactivatedSnackbar)));
        }
      } catch (e) {
        if (context.mounted) {
          messenger.showSnackBar(
            SnackBar(content: Text(messageFromDio(e, l10n) ?? l10n.goalSaveError)),
          );
        }
      }
    }

    Future<void> onDelete() async {
      final ok = await showDialog<bool>(
        context: context,
        builder: (ctx) => AlertDialog(
          title: Text(l10n.goalDeleteTitle),
          content: Text(l10n.goalDeleteBody),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx, false),
              child: Text(l10n.cancel),
            ),
            FilledButton(
              onPressed: () => Navigator.pop(ctx, true),
              child: Text(l10n.goalDelete),
            ),
          ],
        ),
      );
      if (ok != true || !context.mounted) return;
      try {
        await ref.read(goalsRepositoryProvider).deleteGoal(goalId);
        ref.invalidate(goalsListProvider);
        ref.invalidate(dashboardOverviewProvider);
        if (context.mounted) {
          messenger.showSnackBar(SnackBar(content: Text(l10n.goalDeletedSnackbar)));
          context.pop();
        }
      } catch (e) {
        if (context.mounted) {
          final msg = e is DioException && e.response?.statusCode == 409
              ? (messageFromDio(e, l10n) ?? l10n.goalSaveError)
              : (messageFromDio(e, l10n) ?? l10n.goalSaveError);
          messenger.showSnackBar(SnackBar(content: Text(msg)));
        }
      }
    }

    return Scaffold(
      appBar: AppBar(
        title: Text(
          goal.isMine ? goal.title : '${goal.title}${l10n.dashGoalFamilySuffix}',
        ),
        actions: [
          if (goal.isMine)
            PopupMenuButton<String>(
              onSelected: (v) {
                switch (v) {
                  case 'archive':
                    onArchive();
                    break;
                  case 'reactivate':
                    onReactivate();
                    break;
                  case 'delete':
                    onDelete();
                    break;
                }
              },
              itemBuilder: (ctx) => [
                if (goal.isActive)
                  PopupMenuItem(value: 'archive', child: Text(l10n.goalArchive)),
                if (!goal.isActive)
                  PopupMenuItem(value: 'reactivate', child: Text(l10n.goalReactivate)),
                PopupMenuItem(value: 'delete', child: Text(l10n.goalDelete)),
              ],
            ),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.all(20),
        children: [
          if (!goal.isActive)
            Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: Chip(
                label: Text(l10n.goalInactiveBadge),
                backgroundColor: WellPaidColors.navy.withValues(alpha: 0.08),
              ),
            ),
          if (milestone != null) ...[
            GoalMilestoneBanner(milestone: milestone),
            const SizedBox(height: 16),
          ],
          ClipRRect(
            borderRadius: BorderRadius.circular(8),
            child: LinearProgressIndicator(
              value: progress,
              minHeight: 10,
              backgroundColor: WellPaidColors.navy.withValues(alpha: 0.1),
              color: WellPaidColors.gold,
            ),
          ),
          const SizedBox(height: 12),
          Text(
            '${formatBrlFromCents(goal.currentCents)} / ${formatBrlFromCents(goal.targetCents)}',
            style: Theme.of(context).textTheme.titleMedium?.copyWith(
                  fontWeight: FontWeight.w800,
                  color: WellPaidColors.navy,
                ),
          ),
          const SizedBox(height: 6),
          Text(
            completed ? l10n.goalCompleted : '${l10n.goalRemaining}: ${formatBrlFromCents(remaining)}',
            style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                  color: WellPaidColors.navy.withValues(alpha: 0.72),
                ),
          ),
          if (!completed && goal.isActive) ...[
            const SizedBox(height: 16),
            GoalLinearPaceSection(goal: goal),
          ],
          if (goal.isMine && goal.isActive) ...[
            const SizedBox(height: 20),
            FilledButton.icon(
              onPressed: () => _openContributeSheet(context, ref, goalId),
              icon: const Icon(PhosphorIconsRegular.plus),
              label: Text(l10n.goalContribute),
            ),
          ],
          const SizedBox(height: 28),
          Text(
            l10n.goalContributionsTitle,
            style: Theme.of(context).textTheme.titleSmall?.copyWith(
                  fontWeight: FontWeight.w700,
                  color: WellPaidColors.navy,
                ),
          ),
          const SizedBox(height: 8),
          if (!goal.isMine)
            Text(
              l10n.goalContributionsOwnerOnly,
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: WellPaidColors.navy.withValues(alpha: 0.7),
                  ),
            )
          else
            ref.watch(goalContributionsProvider(goalId)).when(
                  loading: () => const Padding(
                    padding: EdgeInsets.symmetric(vertical: 16),
                    child: Center(child: CircularProgressIndicator()),
                  ),
                  error: (e, _) => Text(
                    messageFromDio(e, l10n) ?? l10n.goalsLoadError,
                    style: Theme.of(context).textTheme.bodySmall,
                  ),
                  data: (items) {
                    if (items.isEmpty) {
                      return Text(
                        l10n.goalContributionsEmpty,
                        style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                              color: WellPaidColors.navy.withValues(alpha: 0.65),
                            ),
                      );
                    }
                    return Column(
                      children: items.map((c) {
                        return ListTile(
                          contentPadding: EdgeInsets.zero,
                          title: Text(
                            '+ ${formatBrlFromCents(c.amountCents)}',
                            style: const TextStyle(fontWeight: FontWeight.w700),
                          ),
                          subtitle: Text(
                            [
                              formatDateTimeUi(context, c.recordedAt),
                              if (c.note != null && c.note!.trim().isNotEmpty)
                                c.note!.trim(),
                            ].join(' · '),
                          ),
                        );
                      }).toList(),
                    );
                  },
                ),
        ],
      ),
    );
  }
}

void _openContributeSheet(BuildContext context, WidgetRef ref, String goalId) {
  showModalBottomSheet<void>(
    context: context,
    isScrollControlled: true,
    builder: (ctx) => Padding(
      padding: EdgeInsets.only(
        bottom: MediaQuery.viewInsetsOf(ctx).bottom,
      ),
      child: _ContributeSheet(goalId: goalId),
    ),
  );
}

class _ContributeSheet extends ConsumerStatefulWidget {
  const _ContributeSheet({required this.goalId});

  final String goalId;

  @override
  ConsumerState<_ContributeSheet> createState() => _ContributeSheetState();
}

class _ContributeSheetState extends ConsumerState<_ContributeSheet> {
  final _formKey = GlobalKey<FormState>();
  final _amountCtrl = TextEditingController();
  final _noteCtrl = TextEditingController();
  bool _busy = false;

  @override
  void dispose() {
    _amountCtrl.dispose();
    _noteCtrl.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    final l10n = context.l10n;
    if (!_formKey.currentState!.validate()) return;
    final cents = parseInputToCents(_amountCtrl.text);
    if (cents == null || cents <= 0) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(l10n.valueInvalid)),
      );
      return;
    }
    setState(() => _busy = true);
    try {
      await ref.read(goalsRepositoryProvider).contributeToGoal(
            goalId: widget.goalId,
            amountCents: cents,
            note: _noteCtrl.text.trim().isEmpty ? null : _noteCtrl.text.trim(),
          );
      ref.invalidate(goalProvider(widget.goalId));
      ref.invalidate(goalContributionsProvider(widget.goalId));
      ref.invalidate(goalsListProvider);
      ref.invalidate(dashboardOverviewProvider);
      if (mounted) {
        Navigator.pop(context);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(l10n.goalContributeSaved)),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(messageFromDio(e, l10n) ?? l10n.goalContributeError),
          ),
        );
      }
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    return SafeArea(
      child: SingleChildScrollView(
        padding: const EdgeInsets.fromLTRB(20, 16, 20, 24),
        child: Form(
          key: _formKey,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(
                l10n.goalContributeTitle,
                style: Theme.of(context).textTheme.titleMedium?.copyWith(
                      fontWeight: FontWeight.w800,
                      color: WellPaidColors.navy,
                    ),
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _amountCtrl,
                enabled: !_busy,
                decoration: InputDecoration(
                  labelText: l10n.goalContributeAmountLabel,
                ),
                keyboardType: const TextInputType.numberWithOptions(decimal: true),
                inputFormatters: kBrCurrencyInputFormatters,
                validator: (v) {
                  if (v == null || v.trim().isEmpty) {
                    return l10n.requiredField;
                  }
                  final c = parseInputToCents(v);
                  if (c == null || c <= 0) return l10n.valueInvalid;
                  return null;
                },
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: _noteCtrl,
                enabled: !_busy,
                decoration: InputDecoration(
                  labelText: l10n.goalContributeNoteLabel,
                ),
                maxLines: 2,
                maxLength: 500,
              ),
              const SizedBox(height: 16),
              FilledButton(
                onPressed: _busy ? null : _submit,
                child: _busy
                    ? const SizedBox(
                        height: 22,
                        width: 22,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : Text(l10n.save),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
