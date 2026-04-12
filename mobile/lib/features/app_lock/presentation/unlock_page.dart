import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:local_auth/local_auth.dart';

import '../../../core/l10n/context_l10n.dart';
import '../../../l10n/app_localizations.dart';
import '../../../core/theme/well_paid_colors.dart';
import '../application/app_lock_notifier.dart';
import '../application/biometric_ui_kind.dart';

/// Desbloqueio local (PIN / biometria) após bloqueio de sessão — Etapa 13.
class UnlockPage extends ConsumerStatefulWidget {
  const UnlockPage({super.key});

  @override
  ConsumerState<UnlockPage> createState() => _UnlockPageState();
}

class _UnlockPageState extends ConsumerState<UnlockPage> {
  final _pinCtrl = TextEditingController();
  final _auth = LocalAuthentication();
  String? _error;
  bool _busy = false;
  AppBiometricUiKind? _bioKind;
  bool _autoBioScheduled = false;

  @override
  void initState() {
    super.initState();
    _checkBioAndMaybeAutoBiometric();
  }

  @override
  void dispose() {
    _pinCtrl.dispose();
    super.dispose();
  }

  Future<void> _checkBioAndMaybeAutoBiometric() async {
    try {
      final kind = await detectBiometricUiKind(_auth);
      if (!mounted) return;
      setState(() => _bioKind = kind);
      final can = kind != AppBiometricUiKind.unavailable;
      if (!can || _autoBioScheduled) return;
      final prefer = ref.read(appLockNotifierProvider).biometricPreferred;
      if (!prefer) return;
      _autoBioScheduled = true;
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (!mounted || _busy) return;
        _tryBio();
      });
    } catch (_) {
      if (mounted) {
        setState(() => _bioKind = AppBiometricUiKind.unavailable);
      }
    }
  }

  Future<void> _tryPin() async {
    final l10n = context.l10n;
    final pin = _pinCtrl.text.trim();
    if (pin.length < 4) {
      setState(() => _error = l10n.unlockPinTooShort);
      return;
    }
    setState(() {
      _error = null;
      _busy = true;
    });
    final ok = await ref.read(appLockNotifierProvider.notifier).verifyAndUnlock(pin);
    if (!mounted) return;
    setState(() => _busy = false);
    if (ok) {
      context.go('/home');
    } else {
      setState(() => _error = context.l10n.secWrongPin);
      _pinCtrl.clear();
    }
  }

  String _unlockBioButtonLabel(AppLocalizations l10n, AppBiometricUiKind kind) {
    switch (kind) {
      case AppBiometricUiKind.face:
        return l10n.unlockUseFaceRecognition;
      case AppBiometricUiKind.fingerprint:
        return l10n.unlockUseFingerprint;
      case AppBiometricUiKind.mixed:
        return l10n.unlockUseBiometricMixed;
      case AppBiometricUiKind.generic:
      case AppBiometricUiKind.unavailable:
        return l10n.unlockUseBiometric;
    }
  }

  Future<void> _tryBio() async {
    final l10n = context.l10n;
    final prefer = ref.read(appLockNotifierProvider).biometricPreferred;
    if (!prefer) return;
    setState(() {
      _error = null;
      _busy = true;
    });
    try {
      final ok = await _auth.authenticate(
        localizedReason: l10n.unlockBioReason,
        options: const AuthenticationOptions(biometricOnly: true),
      );
      if (!mounted) return;
      if (ok) {
        ref.read(appLockNotifierProvider.notifier).unlockSession();
        context.go('/home');
      }
    } on PlatformException catch (e) {
      if (mounted) {
        setState(
          () => _error = e.message ?? context.l10n.unlockBioUnavailable,
        );
      }
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final lock = ref.watch(appLockNotifierProvider);
    final k = _bioKind;

    return PopScope(
      canPop: false,
      child: Scaffold(
        appBar: AppBar(
          automaticallyImplyLeading: false,
          title: Text(l10n.unlockTitle),
        ),
        body: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(
                l10n.unlockIntro,
                style: TextStyle(
                  color: WellPaidColors.navy.withValues(alpha: 0.85),
                  fontSize: 16,
                ),
              ),
              const SizedBox(height: 24),
              TextField(
                controller: _pinCtrl,
                keyboardType: TextInputType.number,
                obscureText: true,
                maxLength: 6,
                inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                decoration: InputDecoration(
                  labelText: l10n.unlockPinLabel,
                  counterText: '',
                ),
                onSubmitted: (_) {
                  if (!_busy) _tryPin();
                },
              ),
              if (_error != null) ...[
                const SizedBox(height: 8),
                Text(
                  _error!,
                  style: const TextStyle(color: Colors.redAccent),
                ),
              ],
              const SizedBox(height: 20),
              FilledButton(
                onPressed: _busy ? null : _tryPin,
                child: Text(l10n.confirm),
              ),
              if (lock.biometricPreferred &&
                  k != null &&
                  k != AppBiometricUiKind.unavailable) ...[
                const SizedBox(height: 16),
                OutlinedButton.icon(
                  onPressed: _busy ? null : _tryBio,
                  icon: Icon(biometricPhosphorIcon(k)),
                  label: Text(_unlockBioButtonLabel(l10n, k)),
                ),
              ],
              if (_busy)
                const Padding(
                  padding: EdgeInsets.only(top: 24),
                  child: Center(child: CircularProgressIndicator()),
                ),
            ],
          ),
        ),
      ),
    );
  }
}
