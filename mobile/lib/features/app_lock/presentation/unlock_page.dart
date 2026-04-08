import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:local_auth/local_auth.dart';

import '../../../core/theme/well_paid_colors.dart';
import '../application/app_lock_notifier.dart';

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
  bool? _canBio;

  @override
  void initState() {
    super.initState();
    _checkBio();
  }

  @override
  void dispose() {
    _pinCtrl.dispose();
    super.dispose();
  }

  Future<void> _checkBio() async {
    try {
      final supported = await _auth.isDeviceSupported();
      final can = supported && await _auth.canCheckBiometrics;
      if (mounted) setState(() => _canBio = can);
    } catch (_) {
      if (mounted) setState(() => _canBio = false);
    }
  }

  Future<void> _tryPin() async {
    final pin = _pinCtrl.text.trim();
    if (pin.length < 4) {
      setState(() => _error = 'PIN com pelo menos 4 dígitos');
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
      setState(() => _error = 'PIN incorreto');
      _pinCtrl.clear();
    }
  }

  Future<void> _tryBio() async {
    final prefer = ref.read(appLockNotifierProvider).biometricPreferred;
    if (!prefer) return;
    setState(() {
      _error = null;
      _busy = true;
    });
    try {
      final ok = await _auth.authenticate(
        localizedReason: 'Desbloquear o Well Paid',
        options: const AuthenticationOptions(biometricOnly: true),
      );
      if (!mounted) return;
      if (ok) {
        ref.read(appLockNotifierProvider.notifier).unlockSession();
        context.go('/home');
      }
    } on PlatformException catch (e) {
      if (mounted) {
        setState(() => _error = e.message ?? 'Biometria indisponível');
      }
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final lock = ref.watch(appLockNotifierProvider);
    final showBio = lock.biometricPreferred && (_canBio == true);

    return PopScope(
      canPop: false,
      child: Scaffold(
        appBar: AppBar(
          automaticallyImplyLeading: false,
          title: const Text('Desbloquear'),
        ),
        body: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(
                'Introduz o PIN da app para continuar.',
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
                decoration: const InputDecoration(
                  labelText: 'PIN',
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
                child: const Text('Confirmar'),
              ),
              if (showBio) ...[
                const SizedBox(height: 16),
                OutlinedButton.icon(
                  onPressed: _busy ? null : _tryBio,
                  icon: const Icon(Icons.fingerprint),
                  label: const Text('Usar biometria'),
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
