import 'package:flutter/material.dart';
import 'package:phosphor_flutter/phosphor_flutter.dart';
import 'package:flutter/scheduler.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/l10n/context_l10n.dart';
import '../../../l10n/app_localizations.dart';
import '../../../core/locale/app_locale_provider.dart';
import '../../../core/theme/well_paid_colors.dart';
import 'goal_stall_reminder_settings_tile.dart';

class SettingsPage extends ConsumerWidget {
  const SettingsPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    final localeAsync = ref.watch(appLocaleProvider);
    final lang = (localeAsync.valueOrNull ?? const Locale('pt')).languageCode;
    final group = lang == 'en' ? const Locale('en') : const Locale('pt');

    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(PhosphorIconsRegular.arrowLeft),
          onPressed: () => context.pop(),
        ),
        title: Text(l10n.settingsTitle),
      ),
      body: ListView(
        padding: const EdgeInsets.all(20),
        children: [
          ListTile(
            contentPadding: EdgeInsets.zero,
            leading: Icon(
              PhosphorIconsRegular.userCircle,
              color: WellPaidColors.navy.withValues(alpha: 0.85),
            ),
            title: Text(
              l10n.pcDisplayNameTitle,
              style: TextStyle(
                fontWeight: FontWeight.w600,
                color: WellPaidColors.navy,
              ),
            ),
            trailing: const Icon(PhosphorIconsRegular.caretRight),
            onTap: () => context.push('/display-name'),
          ),
          ListTile(
            contentPadding: EdgeInsets.zero,
            leading: Icon(
              PhosphorIconsRegular.tagSimple,
              color: WellPaidColors.navy.withValues(alpha: 0.85),
            ),
            title: Text(
              l10n.pcManageCategoriesTitle,
              style: TextStyle(
                fontWeight: FontWeight.w600,
                color: WellPaidColors.navy,
              ),
            ),
            trailing: const Icon(PhosphorIconsRegular.caretRight),
            onTap: () => context.push('/manage-categories'),
          ),
          ListTile(
            contentPadding: EdgeInsets.zero,
            leading: Icon(
              PhosphorIconsRegular.clipboardText,
              color: WellPaidColors.navy.withValues(alpha: 0.85),
            ),
            title: Text(
              l10n.pcPlansTitle,
              style: TextStyle(
                fontWeight: FontWeight.w600,
                color: WellPaidColors.navy,
              ),
            ),
            trailing: const Icon(PhosphorIconsRegular.caretRight),
            onTap: () => context.push('/emergency-plans'),
          ),
          ListTile(
            contentPadding: EdgeInsets.zero,
            leading: Icon(
              PhosphorIconsRegular.shield,
              color: WellPaidColors.navy.withValues(alpha: 0.85),
            ),
            title: Text(
              l10n.settingsEmergencyReserve,
              style: TextStyle(
                fontWeight: FontWeight.w600,
                color: WellPaidColors.navy,
              ),
            ),
            trailing: const Icon(PhosphorIconsRegular.caretRight),
            onTap: () => context.push('/emergency-reserve'),
          ),
          const SizedBox(height: 16),
          Divider(height: 1, color: WellPaidColors.navy.withValues(alpha: 0.12)),
          const SizedBox(height: 20),
          Text(
            l10n.settingsNotificationsSection,
            style: Theme.of(context).textTheme.titleMedium?.copyWith(
                  fontWeight: FontWeight.w800,
                  color: WellPaidColors.navy,
                ),
          ),
          const SizedBox(height: 6),
          const GoalStallReminderSettingsTile(),
          const SizedBox(height: 20),
          Divider(height: 1, color: WellPaidColors.navy.withValues(alpha: 0.12)),
          const SizedBox(height: 20),
          Text(
            l10n.settingsLanguageTitle,
            style: Theme.of(context).textTheme.titleMedium?.copyWith(
                  fontWeight: FontWeight.w800,
                  color: WellPaidColors.navy,
                ),
          ),
          const SizedBox(height: 6),
          Text(
            l10n.settingsLanguageSubtitle,
            style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                  color: WellPaidColors.navy.withValues(alpha: 0.72),
                  height: 1.35,
                ),
          ),
          const SizedBox(height: 16),
          RadioListTile<Locale>(
            contentPadding: EdgeInsets.zero,
            title: Text(l10n.langPortugueseBrazil),
            value: const Locale('pt'),
            groupValue: group,
            onChanged: localeAsync.isLoading
                ? null
                : (v) async {
                    if (v == null) return;
                    await ref.read(appLocaleProvider.notifier).setLocale(v);
                    if (!context.mounted) return;
                    SchedulerBinding.instance.addPostFrameCallback((_) {
                      if (!context.mounted) return;
                      final msg =
                          AppLocalizations.of(context)!.settingsLanguageUpdated;
                      ScaffoldMessenger.of(context)
                          .showSnackBar(SnackBar(content: Text(msg)));
                    });
                  },
          ),
          RadioListTile<Locale>(
            contentPadding: EdgeInsets.zero,
            title: Text(l10n.langEnglishUS),
            value: const Locale('en'),
            groupValue: group,
            onChanged: localeAsync.isLoading
                ? null
                : (v) async {
                    if (v == null) return;
                    await ref.read(appLocaleProvider.notifier).setLocale(v);
                    if (!context.mounted) return;
                    SchedulerBinding.instance.addPostFrameCallback((_) {
                      if (!context.mounted) return;
                      final msg =
                          AppLocalizations.of(context)!.settingsLanguageUpdated;
                      ScaffoldMessenger.of(context)
                          .showSnackBar(SnackBar(content: Text(msg)));
                    });
                  },
          ),
        ],
      ),
    );
  }
}
