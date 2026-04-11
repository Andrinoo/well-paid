import 'package:flutter/material.dart';
import 'package:phosphor_flutter/phosphor_flutter.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:local_auth/local_auth.dart';

import '../../../core/l10n/context_l10n.dart';
import '../../../core/theme/well_paid_colors.dart';
import '../../../l10n/app_localizations.dart';
import '../application/app_lock_notifier.dart';

bool _validPin(String p) {
  if (p.length < 4 || p.length > 6) return false;
  return RegExp(r'^\d+$').hasMatch(p);
}

/// Ativar PIN local, biometria e desativar bloqueio — §5.1 / Etapa 13.
class SecuritySettingsPage extends ConsumerStatefulWidget {
  const SecuritySettingsPage({super.key});

  @override
  ConsumerState<SecuritySettingsPage> createState() => _SecuritySettingsPageState();
}

class _SecuritySettingsPageState extends ConsumerState<SecuritySettingsPage> {
  final _la = LocalAuthentication();
  bool? _hardwareBio;

  @override
  void initState() {
    super.initState();
    _checkHardware();
  }

  Future<void> _checkHardware() async {
    try {
      final s = await _la.isDeviceSupported();
      final c = s && await _la.canCheckBiometrics;
      if (mounted) setState(() => _hardwareBio = c);
    } catch (_) {
      if (mounted) setState(() => _hardwareBio = false);
    }
  }

  Future<void> _promptNewPin({required bool enabling}) async {
    final a = TextEditingController();
    final b = TextEditingController();
    final chosen = await showDialog<String>(
      context: context,
      barrierDismissible: false,
      builder: (ctx) {
        final l10n = AppLocalizations.of(ctx)!;
        return AlertDialog(
          title: Text(enabling ? l10n.secSetPinTitle : l10n.secNewPinTitle),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                controller: a,
                obscureText: true,
                keyboardType: TextInputType.number,
                maxLength: 6,
                inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                decoration: InputDecoration(
                  labelText: l10n.secPinField,
                  counterText: '',
                ),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: b,
                obscureText: true,
                keyboardType: TextInputType.number,
                maxLength: 6,
                inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                decoration: InputDecoration(
                  labelText: l10n.secRepeatPinField,
                  counterText: '',
                ),
              ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: Text(l10n.cancel),
            ),
            FilledButton(
              onPressed: () {
                final p1 = a.text.trim();
                final p2 = b.text.trim();
                if (!_validPin(p1) || p1 != p2) {
                  ScaffoldMessenger.of(ctx).showSnackBar(
                    SnackBar(content: Text(l10n.secPinInvalidOrMismatch)),
                  );
                  return;
                }
                Navigator.pop(ctx, p1);
              },
              child: Text(l10n.save),
            ),
          ],
        );
      },
    );
    a.dispose();
    b.dispose();
    if (chosen == null || chosen.isEmpty || !mounted) return;
    await ref.read(appLockNotifierProvider.notifier).setNewPin(chosen);
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(context.l10n.secPinSavedSnackbar)),
      );
    }
  }

  Future<void> _confirmDisablePin() async {
    final ctrl = TextEditingController();
    final entered = await showDialog<String>(
      context: context,
      builder: (ctx) {
        final l10n = AppLocalizations.of(ctx)!;
        return AlertDialog(
          title: Text(l10n.secDisablePinTitle),
          content: TextField(
            controller: ctrl,
            obscureText: true,
            keyboardType: TextInputType.number,
            maxLength: 6,
            inputFormatters: [FilteringTextInputFormatter.digitsOnly],
            decoration: InputDecoration(labelText: l10n.secCurrentPinField),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: Text(l10n.cancel),
            ),
            FilledButton(
              onPressed: () => Navigator.pop(ctx, ctrl.text.trim()),
              child: Text(l10n.confirm),
            ),
          ],
        );
      },
    );
    if (entered == null || entered.isEmpty || !mounted) return;
    final ok = await ref.read(appLockStorageProvider).verifyPin(entered);
    if (!ok) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(context.l10n.secWrongPin)),
        );
      }
      return;
    }
    await ref.read(appLockNotifierProvider.notifier).disablePinCompletely();
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(context.l10n.secPinDisabledSnackbar)),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final lock = ref.watch(appLockNotifierProvider);
    final pinOn = lock.pinEnabled;
    final bioOk = _hardwareBio == true;

    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(PhosphorIconsRegular.arrowLeft),
          onPressed: () => context.pop(),
        ),
        title: Text(l10n.secAppTitle),
      ),
      body: ListView(
        children: [
          SwitchListTile(
            title: Text(l10n.secLockWithPin),
            subtitle: Text(
              pinOn ? l10n.secLockPinOnSub : l10n.secLockPinOffSub,
              style: TextStyle(color: WellPaidColors.navy.withValues(alpha: 0.7), fontSize: 13),
            ),
            value: pinOn,
            onChanged: (v) async {
              if (v) {
                await _promptNewPin(enabling: true);
              } else {
                await _confirmDisablePin();
              }
            },
          ),
          SwitchListTile(
            title: Text(l10n.secBiometricTitle),
            subtitle: Text(
              bioOk ? l10n.secBiometricOnSub : l10n.secBiometricOffSub,
              style: TextStyle(color: WellPaidColors.navy.withValues(alpha: 0.7), fontSize: 13),
            ),
            value: lock.biometricPreferred && bioOk,
            onChanged: (!pinOn || !bioOk)
                ? null
                : (v) {
                    ref.read(appLockNotifierProvider.notifier).setBiometricPreferred(v);
                  },
          ),
          if (pinOn)
            ListTile(
              title: Text(l10n.secChangePin),
              leading: const Icon(PhosphorIconsRegular.keyboard),
              onTap: () => _promptNewPin(enabling: false),
            ),
        ],
      ),
    );
  }
}
