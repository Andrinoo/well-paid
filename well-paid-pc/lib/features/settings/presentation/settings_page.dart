import 'package:flutter/material.dart';
import 'package:flutter/scheduler.dart';
import 'package:phosphor_flutter/phosphor_flutter.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/l10n/context_l10n.dart';
import '../../../core/locale/app_locale_provider.dart';
import '../../../core/theme/well_paid_breakpoints.dart';
import '../../../core/theme/well_paid_colors.dart';
import '../../../core/widgets/desktop/desktop.dart';
import '../../../l10n/app_localizations.dart';
import 'goal_stall_reminder_settings_tile.dart';

class SettingsPage extends ConsumerWidget {
  const SettingsPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    final localeAsync = ref.watch(appLocaleProvider);
    final lang = (localeAsync.valueOrNull ?? const Locale('pt')).languageCode;
    final group = lang == 'en' ? const Locale('en') : const Locale('pt');
    final accountSection = SectionCard(
      child: Column(
        children: [
          _SettingsLink(
            icon: PhosphorIconsRegular.userCircle,
            label: l10n.pcDisplayNameTitle,
            onTap: () => context.push('/display-name'),
          ),
          _SettingsLink(
            icon: PhosphorIconsRegular.tagSimple,
            label: l10n.pcManageCategoriesTitle,
            onTap: () => context.push('/manage-categories'),
          ),
          _SettingsLink(
            icon: PhosphorIconsRegular.clipboardText,
            label: l10n.pcPlansTitle,
            onTap: () => context.push('/emergency-plans'),
          ),
          _SettingsLink(
            icon: PhosphorIconsRegular.shield,
            label: l10n.settingsEmergencyReserve,
            onTap: () => context.push('/emergency-reserve'),
          ),
        ],
      ),
    );

    final notificationSection = SectionCard(
      title: l10n.settingsNotificationsSection,
      child: const GoalStallReminderSettingsTile(),
    );

    final languageSection = SectionCard(
      title: l10n.settingsLanguageTitle,
      subtitle: l10n.settingsLanguageSubtitle,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
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
                      final msg = AppLocalizations.of(context)!
                          .settingsLanguageUpdated;
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
                      final msg = AppLocalizations.of(context)!
                          .settingsLanguageUpdated;
                      ScaffoldMessenger.of(context)
                          .showSnackBar(SnackBar(content: Text(msg)));
                    });
                  },
          ),
        ],
      ),
    );

    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(PhosphorIconsRegular.arrowLeft),
          onPressed: () => context.pop(),
        ),
        title: Text(l10n.settingsTitle),
      ),
      body: LayoutBuilder(
        builder: (context, constraints) {
          final wide =
              constraints.maxWidth >= WellPaidBreakpoints.medium;
          return wide
              ? Align(
                  alignment: Alignment.topCenter,
                  child: ConstrainedBox(
                    constraints: const BoxConstraints(
                      maxWidth: WellPaidBreakpoints.pageMaxWidth,
                    ),
                    child: ListView(
                      padding: const EdgeInsets.fromLTRB(24, 20, 24, 32),
                      children: [
                        accountSection,
                        const SizedBox(height: 16),
                        notificationSection,
                        const SizedBox(height: 16),
                        languageSection,
                      ],
                    ),
                  ),
                )
              : ListView(
                  padding: const EdgeInsets.all(20),
                  children: [
                    accountSection,
                    const SizedBox(height: 12),
                    notificationSection,
                    const SizedBox(height: 12),
                    languageSection,
                  ],
                );
        },
      ),
    );
  }
}

class _SettingsLink extends StatelessWidget {
  const _SettingsLink({
    required this.icon,
    required this.label,
    required this.onTap,
  });

  final IconData icon;
  final String label;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Material(
        color: WellPaidColors.creamMuted.withValues(alpha: 0.5),
        borderRadius: BorderRadius.circular(12),
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(12),
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
            child: Row(
              children: [
                Container(
                  width: 40,
                  height: 40,
                  decoration: BoxDecoration(
                    color: WellPaidColors.navy.withValues(alpha: 0.08),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Icon(
                    icon,
                    size: 22,
                    color: WellPaidColors.navy.withValues(alpha: 0.88),
                  ),
                ),
                const SizedBox(width: 14),
                Expanded(
                  child: Text(
                    label,
                    style: Theme.of(context).textTheme.titleSmall?.copyWith(
                          fontWeight: FontWeight.w600,
                          color: WellPaidColors.navy,
                        ),
                  ),
                ),
                Icon(
                  PhosphorIconsRegular.caretRight,
                  color: WellPaidColors.navy.withValues(alpha: 0.45),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
