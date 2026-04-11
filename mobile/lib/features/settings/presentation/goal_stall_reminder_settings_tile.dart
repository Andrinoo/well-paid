import 'dart:async' show unawaited;

import 'package:flutter/material.dart';
import 'package:phosphor_flutter/phosphor_flutter.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/locale/app_locale_provider.dart';
import '../../../core/l10n/context_l10n.dart';
import '../../../core/notifications/goal_stall_reminder_service.dart';
import '../../../core/theme/well_paid_colors.dart';
import '../../goals/application/goals_providers.dart';

class GoalStallReminderSettingsTile extends ConsumerStatefulWidget {
  const GoalStallReminderSettingsTile({super.key});

  @override
  ConsumerState<GoalStallReminderSettingsTile> createState() =>
      _GoalStallReminderSettingsTileState();
}

class _GoalStallReminderSettingsTileState
    extends ConsumerState<GoalStallReminderSettingsTile> {
  bool? _enabled;
  bool _busy = false;

  @override
  void initState() {
    super.initState();
    unawaited(_load());
  }

  Future<void> _load() async {
    final v = await GoalStallReminderService.isEnabled();
    if (mounted) setState(() => _enabled = v);
  }

  Future<void> _onChanged(bool v) async {
    if (_busy) return;
    final l10n = context.l10n;
    final messenger = ScaffoldMessenger.maybeOf(context);
    setState(() => _busy = true);
    await GoalStallReminderService.setEnabled(v);
    if (v) {
      await GoalStallReminderService.requestPostNotificationsPermission();
      final loc =
          ref.read(appLocaleProvider).valueOrNull ?? const Locale('pt');
      try {
        final goals = await ref.read(goalsListProvider.future);
        await GoalStallReminderService.syncFromGoals(goals, locale: loc);
      } catch (_) {
        /* Sem lista: o shell volta a sincronizar quando houver dados. */
      }
      final granted = await GoalStallReminderService.androidNotificationsEnabled();
      if (mounted && granted == false && messenger != null) {
        messenger.showSnackBar(
          SnackBar(content: Text(l10n.settingsGoalStallPermissionDenied)),
        );
      }
    } else {
      await GoalStallReminderService.cancelScheduled();
    }
    if (mounted) {
      setState(() {
        _enabled = v;
        _busy = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final enabled = _enabled;
    return SwitchListTile(
      contentPadding: EdgeInsets.zero,
      secondary: Icon(
        PhosphorIconsRegular.bellRinging,
        color: WellPaidColors.navy.withValues(alpha: 0.85),
      ),
      title: Text(
        l10n.settingsGoalStallReminderTitle,
        style: const TextStyle(
          fontWeight: FontWeight.w600,
          color: WellPaidColors.navy,
        ),
      ),
      subtitle: Text(
        l10n.settingsGoalStallReminderSubtitle,
        style: Theme.of(context).textTheme.bodySmall?.copyWith(
              color: WellPaidColors.navy.withValues(alpha: 0.72),
              height: 1.35,
            ),
      ),
      value: enabled ?? false,
      onChanged: enabled == null || _busy ? null : _onChanged,
    );
  }
}
