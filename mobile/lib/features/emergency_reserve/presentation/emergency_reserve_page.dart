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
      ref.invalidate(emergencyReserveSnapshotProvider);
      ref.invalidate(emergencyReserveAccrualsProvider);
      final period = ref.read(dashboardPeriodProvider);
      ref.invalidate(dashboardOverviewByPeriodProvider(period));
      ref.invalidate(dashboardOverviewProvider);
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
                          trailing: Text(
                            formatBrlFromCents(item.amountCents),
                            style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                                  color: WellPaidColors.navy,
                                  fontWeight: FontWeight.w700,
                                ),
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
