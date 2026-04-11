import 'dart:async' show unawaited;

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/format/brl_cents.dart';
import '../../../core/format/locale_dates.dart';
import '../../../core/format/parse_brl_input.dart';
import '../../../core/l10n/context_l10n.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/theme/well_paid_colors.dart';
import '../../dashboard/application/dashboard_providers.dart';
import '../application/emergency_reserve_providers.dart';
import '../domain/emergency_reserve_accrual_item.dart';

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
      text: (item.amountCents / 100).toStringAsFixed(2),
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
              (snap.monthlyTargetCents / 100).toStringAsFixed(2);
        }
      });
    });

    return Scaffold(
      appBar: AppBar(
        leading: Navigator.of(context).canPop()
            ? IconButton(
                icon: const Icon(Icons.arrow_back),
                onPressed: () => context.pop(),
              )
            : null,
        title: Text(l10n.emergencyReserveTitle),
        actions: [
          if (async.hasValue && async.value!.configured)
            PopupMenuButton<int>(
              icon: const Icon(Icons.more_vert),
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
        loading: () => const Center(child: CircularProgressIndicator()),
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
                Text(
                  l10n.emergencyReserveIntro,
                  style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                        color: WellPaidColors.navy.withValues(alpha: 0.72),
                        height: 1.35,
                      ),
                ),
                if (snap.configured) ...[
                  const SizedBox(height: 12),
                  Text(
                    '${l10n.dashEmergencyReserveBalance}: ${formatBrlFromCents(snap.balanceCents)}',
                    style: Theme.of(context).textTheme.bodySmall?.copyWith(
                          color: WellPaidColors.navy.withValues(alpha: 0.8),
                          fontWeight: FontWeight.w600,
                        ),
                  ),
                ],
                const SizedBox(height: 20),
                TextFormField(
                  controller: _amountCtrl,
                  decoration: InputDecoration(
                    labelText: l10n.emergencyReserveMonthlyLabel,
                  ),
                  keyboardType:
                      const TextInputType.numberWithOptions(decimal: true),
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
                        _amountCtrl.text = (cents / 100).toStringAsFixed(2);
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
                  loading: () => const Padding(
                    padding: EdgeInsets.symmetric(vertical: 12),
                    child: Center(child: CircularProgressIndicator()),
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
                                child: const Icon(Icons.more_vert, size: 22),
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
