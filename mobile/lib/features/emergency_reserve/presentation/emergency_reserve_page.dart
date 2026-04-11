import 'dart:async' show unawaited;

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
import '../../goals/presentation/goal_progress_milestone.dart';
import '../application/emergency_reserve_providers.dart';
import 'reserve_milestone_widgets.dart';
import '../domain/emergency_reserve_accrual_item.dart';
import '../domain/emergency_reserve_snapshot.dart';

class EmergencyReservePage extends ConsumerStatefulWidget {
  const EmergencyReservePage({super.key});

  @override
  ConsumerState<EmergencyReservePage> createState() =>
      _EmergencyReservePageState();
}

class _EmergencyReservePageState extends ConsumerState<EmergencyReservePage> {
  static const List<int> _presetCents = <int>[20000, 50000, 100000];
  final _formKey = GlobalKey<FormState>();
  final _amountCtrl = TextEditingController();
  bool _prefilled = false;

  @override
  void dispose() {
    _amountCtrl.dispose();
    super.dispose();
  }

  void _invalidateEmergencyAndDashboard() {
    ref.invalidate(emergencyReserveSnapshotProvider);
    ref.invalidate(emergencyReserveAccrualsProvider);
    final period = ref.read(dashboardPeriodProvider);
    ref.invalidate(dashboardOverviewByPeriodProvider(period));
    ref.invalidate(dashboardOverviewProvider);
  }

  Future<void> _submit() async {
    final l10n = context.l10n;
    if (!_formKey.currentState!.validate()) return;
    final cents = parseInputToCents(_amountCtrl.text);
    if (cents == null || cents < 0) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(l10n.valueInvalid)),
      );
      return;
    }

    final messenger = ScaffoldMessenger.of(context);
    try {
      await ref.read(emergencyReserveRepositoryProvider).updateMonthlyTarget(cents);
      _invalidateEmergencyAndDashboard();
      if (mounted) {
        messenger.showSnackBar(
          SnackBar(content: Text(l10n.emergencyReserveSavedSnackbar)),
        );
        if (Navigator.of(context).canPop()) context.pop();
      }
    } catch (e) {
      if (mounted) {
        messenger.showSnackBar(
          SnackBar(
            content: Text(messageFromDio(e, l10n) ?? l10n.emergencyReserveError),
          ),
        );
      }
    }
  }

  Future<void> _confirmAndDeleteAccrual(
    EmergencyReserveAccrualItem item,
    String monthLabel,
  ) async {
    final l10n = context.l10n;
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(l10n.emergencyReserveAccrualDeleteTitle(monthLabel)),
        content: Text(l10n.emergencyReserveAccrualDeleteBody),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: Text(l10n.cancel),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(ctx, true),
            child: Text(l10n.emergencyReserveAccrualDeleteConfirm),
          ),
        ],
      ),
    );
    if (ok != true || !mounted) return;
    final messenger = ScaffoldMessenger.of(context);
    try {
      await ref.read(emergencyReserveRepositoryProvider).deleteAccrual(
            year: item.year,
            month: item.month,
          );
      _invalidateEmergencyAndDashboard();
      if (mounted) {
        messenger.showSnackBar(
          SnackBar(content: Text(l10n.emergencyReserveAccrualRemovedSnackbar)),
        );
      }
    } catch (e) {
      if (mounted) {
        messenger.showSnackBar(
          SnackBar(
            content: Text(messageFromDio(e, l10n) ?? l10n.emergencyReserveError),
          ),
        );
      }
    }
  }

  Future<void> _editAccrual(
    EmergencyReserveAccrualItem item,
    String monthLabel,
  ) async {
    final l10n = context.l10n;
    final ctrl = TextEditingController(
      text: formatBrlInputFromCents(item.amountCents),
    );
    final dialogFormKey = GlobalKey<FormState>();
    try {
      final submitted = await showDialog<bool>(
        context: context,
        builder: (ctx) => AlertDialog(
          title: Text(l10n.emergencyReserveAccrualEditTitle(monthLabel)),
          content: Form(
            key: dialogFormKey,
            child: TextFormField(
              controller: ctrl,
              decoration: InputDecoration(
                labelText: l10n.emergencyReserveMonthlyLabel,
              ),
              keyboardType:
                  const TextInputType.numberWithOptions(decimal: true),
              inputFormatters: kBrCurrencyInputFormatters,
              autofocus: true,
              validator: (v) {
                if (v == null || v.trim().isEmpty) {
                  return l10n.valueInvalid;
                }
                final c = parseInputToCents(v);
                if (c == null || c < 0) return l10n.valueInvalid;
                return null;
              },
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx, false),
              child: Text(l10n.cancel),
            ),
            FilledButton(
              onPressed: () {
                if (dialogFormKey.currentState?.validate() != true) return;
                Navigator.pop(ctx, true);
              },
              child: Text(l10n.save),
            ),
          ],
        ),
      );
      if (submitted != true || !mounted) return;
      final cents = parseInputToCents(ctrl.text);
      if (cents == null || cents < 0) return;
      final messenger = ScaffoldMessenger.of(context);
      try {
        await ref.read(emergencyReserveRepositoryProvider).patchAccrual(
              year: item.year,
              month: item.month,
              amountCents: cents,
            );
        _invalidateEmergencyAndDashboard();
        if (mounted) {
          messenger.showSnackBar(
            SnackBar(content: Text(l10n.emergencyReserveAccrualUpdatedSnackbar)),
          );
        }
      } catch (e) {
        if (mounted) {
          messenger.showSnackBar(
            SnackBar(
              content:
                  Text(messageFromDio(e, l10n) ?? l10n.emergencyReserveError),
            ),
          );
        }
      }
    } finally {
      ctrl.dispose();
    }
  }

  Future<void> _confirmResetEntireReserve() async {
    final l10n = context.l10n;
    final scheme = Theme.of(context).colorScheme;
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(l10n.emergencyReserveResetTitle),
        content: Text(l10n.emergencyReserveResetBody),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: Text(l10n.cancel),
          ),
          FilledButton(
            style: FilledButton.styleFrom(
              backgroundColor: scheme.error,
              foregroundColor: scheme.onError,
            ),
            onPressed: () => Navigator.pop(ctx, true),
            child: Text(l10n.emergencyReserveResetConfirm),
          ),
        ],
      ),
    );
    if (ok != true || !mounted) return;
    final messenger = ScaffoldMessenger.of(context);
    try {
      await ref.read(emergencyReserveRepositoryProvider).deleteEntireReserve();
      _invalidateEmergencyAndDashboard();
      setState(() => _prefilled = false);
      _amountCtrl.clear();
      if (mounted) {
        messenger.showSnackBar(
          SnackBar(content: Text(l10n.emergencyReserveResetSuccess)),
        );
      }
    } catch (e) {
      if (mounted) {
        messenger.showSnackBar(
          SnackBar(
            content: Text(messageFromDio(e, l10n) ?? l10n.emergencyReserveError),
          ),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final async = ref.watch(emergencyReserveSnapshotProvider);

    ref.listen(emergencyReserveSnapshotProvider, (prev, next) {
      next.whenData((snap) {
        if (!mounted || _prefilled) return;
        _prefilled = true;
        if (snap.monthlyTargetCents > 0) {
          _amountCtrl.text =
              formatBrlInputFromCents(snap.monthlyTargetCents);
        }
      });
    });

    return Scaffold(
      appBar: AppBar(
        leading: Navigator.of(context).canPop()
            ? IconButton(
                icon: const Icon(PhosphorIconsRegular.arrowLeft),
                onPressed: () => context.pop(),
              )
            : null,
        title: Text(l10n.emergencyReserveTitle),
        actions: [
          if (async.hasValue && async.value!.configured)
            PopupMenuButton<int>(
              icon: const Icon(PhosphorIconsRegular.dotsThreeVertical),
              onSelected: (v) {
                if (v == 0) unawaited(_confirmResetEntireReserve());
              },
              itemBuilder: (context) => [
                PopupMenuItem(
                  value: 0,
                  child: Text(l10n.emergencyReserveResetAction),
                ),
              ],
            ),
        ],
      ),
      body: async.when(
        skipLoadingOnReload: true,
        loading: () => ListView(
          padding: const EdgeInsets.all(20),
          children: [
            LinearProgressIndicator(
              minHeight: 3,
              color: WellPaidColors.gold,
              backgroundColor: WellPaidColors.navy.withValues(alpha: 0.08),
            ),
            const SizedBox(height: 20),
            Container(
              height: 140,
              decoration: BoxDecoration(
                color: WellPaidColors.navy.withValues(alpha: 0.08),
                borderRadius: BorderRadius.circular(16),
              ),
            ),
          ],
        ),
        error: (e, _) => Center(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Text(
              messageFromDio(e, l10n) ?? l10n.emergencyReserveError,
              textAlign: TextAlign.center,
            ),
          ),
        ),
        data: (snap) {
          final accrualsAsync = ref.watch(emergencyReserveAccrualsProvider);
          return Form(
            key: _formKey,
            child: ListView(
              padding: const EdgeInsets.all(20),
              children: [
                _EmergencyReserveHeroCard(snap: snap),
                const SizedBox(height: 16),
                Text(
                  l10n.emergencyReserveIntro,
                  style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                        color: WellPaidColors.navy.withValues(alpha: 0.72),
                        height: 1.35,
                      ),
                ),
                const SizedBox(height: 20),
                TextFormField(
                  controller: _amountCtrl,
                  decoration: InputDecoration(
                    labelText: l10n.emergencyReserveMonthlyLabel,
                  ),
                  keyboardType:
                      const TextInputType.numberWithOptions(decimal: true),
                  inputFormatters: kBrCurrencyInputFormatters,
                ),
                const SizedBox(height: 14),
                Text(
                  l10n.emergencyReserveQuickPickTitle,
                  style: Theme.of(context).textTheme.labelLarge?.copyWith(
                        color: WellPaidColors.navy.withValues(alpha: 0.82),
                        fontWeight: FontWeight.w700,
                      ),
                ),
                const SizedBox(height: 8),
                Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: _presetCents.map((cents) {
                    return ActionChip(
                      label: Text(formatBrlFromCents(cents)),
                      onPressed: () {
                        _amountCtrl.text = formatBrlInputFromCents(cents);
                      },
                    );
                  }).toList(),
                ),
                const SizedBox(height: 24),
                FilledButton(
                  onPressed: _submit,
                  child: Text(l10n.emergencyReserveSave),
                ),
                const SizedBox(height: 24),
                Text(
                  l10n.emergencyReserveAccrualListTitle,
                  style: Theme.of(context).textTheme.titleMedium?.copyWith(
                        color: WellPaidColors.navy,
                        fontWeight: FontWeight.w700,
                      ),
                ),
                const SizedBox(height: 8),
                accrualsAsync.when(
                  skipLoadingOnReload: true,
                  loading: () => Padding(
                    padding: const EdgeInsets.symmetric(vertical: 10),
                    child: LinearProgressIndicator(
                      minHeight: 2,
                      color: WellPaidColors.gold,
                      backgroundColor: WellPaidColors.navy.withValues(alpha: 0.08),
                    ),
                  ),
                  error: (e, _) => Padding(
                    padding: const EdgeInsets.symmetric(vertical: 8),
                    child: Text(
                      messageFromDio(e, l10n) ?? l10n.emergencyReserveError,
                      style: Theme.of(context).textTheme.bodySmall,
                    ),
                  ),
                  data: (items) {
                    if (items.isEmpty) {
                      return Padding(
                        padding: const EdgeInsets.symmetric(vertical: 4),
                        child: Text(
                          l10n.emergencyReserveAccrualListEmpty,
                          style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                                color: WellPaidColors.navy.withValues(alpha: 0.72),
                              ),
                        ),
                      );
                    }
                    return Column(
                      children: items.map((item) {
                        final monthLabel = formatMonthYearUi(
                          context,
                          DateTime(item.year, item.month),
                        );
                        return ListTile(
                          contentPadding: EdgeInsets.zero,
                          dense: true,
                          title: Text(monthLabel),
                          subtitle: Text(l10n.emergencyReserveAccrualListCredit),
                          trailing: Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              Text(
                                formatBrlFromCents(item.amountCents),
                                style: Theme.of(context)
                                    .textTheme
                                    .bodyMedium
                                    ?.copyWith(
                                      color: WellPaidColors.navy,
                                      fontWeight: FontWeight.w700,
                                    ),
                              ),
                              PopupMenuButton<String>(
                                padding: EdgeInsets.zero,
                                constraints: const BoxConstraints(
                                  minWidth: 40,
                                  minHeight: 40,
                                ),
                                child: const Icon(PhosphorIconsRegular.dotsThreeVertical, size: 22),
                                onSelected: (action) {
                                  if (action == 'edit') {
                                    unawaited(_editAccrual(item, monthLabel));
                                  } else if (action == 'delete') {
                                    unawaited(
                                      _confirmAndDeleteAccrual(item, monthLabel),
                                    );
                                  }
                                },
                                itemBuilder: (context) => [
                                  PopupMenuItem(
                                    value: 'edit',
                                    child: Text(l10n.emergencyReserveAccrualEdit),
                                  ),
                                  PopupMenuItem(
                                    value: 'delete',
                                    child:
                                        Text(l10n.emergencyReserveAccrualDelete),
                                  ),
                                ],
                              ),
                            ],
                          ),
                        );
                      }).toList(),
                    );
                  },
                ),
              ],
            ),
          );
        },
      ),
    );
  }
}

class _EmergencyReserveHeroCard extends StatelessWidget {
  const _EmergencyReserveHeroCard({required this.snap});

  final EmergencyReserveSnapshot snap;

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final m = snap.monthlyTargetCents;
    final annual = m * 12;
    final pct = annual > 0
        ? ((snap.balanceCents / annual) * 100).round().clamp(0, 100)
        : 0;
    final ratio = m > 0 ? snap.balanceCents / m : 0.0;
    final milestone = m > 0
        ? resolveGoalProgressMilestone(
            currentCents: snap.balanceCents,
            targetCents: annual,
          )
        : null;

    return DecoratedBox(
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [
            WellPaidColors.navy,
            WellPaidColors.navyDeep,
          ],
        ),
        borderRadius: BorderRadius.circular(18),
        boxShadow: [
          BoxShadow(
            color: WellPaidColors.navy.withValues(alpha: 0.22),
            blurRadius: 14,
            offset: const Offset(0, 6),
          ),
        ],
      ),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Container(
                  width: 44,
                  height: 44,
                  decoration: BoxDecoration(
                    color: Colors.white.withValues(alpha: 0.18),
                    borderRadius: BorderRadius.circular(14),
                  ),
                  child: Icon(
                    PhosphorIconsRegular.moonStars,
                    color: WellPaidColors.gold,
                    size: 26,
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Text(
                    l10n.dashEmergencyReserve,
                    style: Theme.of(context).textTheme.titleMedium?.copyWith(
                          color: WellPaidColors.cream,
                          fontWeight: FontWeight.w800,
                        ),
                  ),
                ),
                if (milestone != null) ...[
                  const SizedBox(width: 8),
                  ReserveMilestoneChip(milestone: milestone),
                ],
              ],
            ),
            const SizedBox(height: 12),
            Text(
              formatBrlFromCents(snap.balanceCents),
              style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                    color: WellPaidColors.gold,
                    fontWeight: FontWeight.w800,
                    letterSpacing: -0.5,
                  ),
            ),
            const SizedBox(height: 4),
            Text(
              l10n.dashEmergencyReserveBalance,
              style: Theme.of(context).textTheme.labelMedium?.copyWith(
                    color: WellPaidColors.cream.withValues(alpha: 0.75),
                    fontWeight: FontWeight.w600,
                  ),
            ),
            if (milestone != null) ...[
              const SizedBox(height: 12),
              ReserveMilestoneBanner(milestone: milestone),
            ],
            if (m > 0) ...[
              const SizedBox(height: 12),
              Text(
                '${l10n.dashEmergencyReserveMonthly}: ${formatBrlFromCents(m)}',
                style: Theme.of(context).textTheme.bodySmall?.copyWith(
                      color: WellPaidColors.cream.withValues(alpha: 0.88),
                    ),
              ),
              const SizedBox(height: 8),
              ClipRRect(
                borderRadius: BorderRadius.circular(8),
                child: LinearProgressIndicator(
                  value: pct / 100,
                  minHeight: 10,
                  backgroundColor: Colors.white.withValues(alpha: 0.2),
                  color: WellPaidColors.gold,
                ),
              ),
              const SizedBox(height: 6),
              Text(
                l10n.dashEmergencyReserveAnnualProgress(pct),
                style: Theme.of(context).textTheme.labelMedium?.copyWith(
                      color: WellPaidColors.cream.withValues(alpha: 0.9),
                      fontWeight: FontWeight.w600,
                    ),
              ),
              if (ratio > 0)
                Text(
                  '${ratio.toStringAsFixed(1)}× ${l10n.dashEmergencyReserveTimesTarget}',
                  style: Theme.of(context).textTheme.labelSmall?.copyWith(
                        color: WellPaidColors.cream.withValues(alpha: 0.72),
                      ),
                ),
            ],
          ],
        ),
      ),
    );
  }
}

