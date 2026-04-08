import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:local_auth/local_auth.dart';

import '../../../core/theme/well_paid_colors.dart';
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
      builder: (ctx) => AlertDialog(
        title: Text(enabling ? 'Definir PIN da app' : 'Novo PIN'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: a,
              obscureText: true,
              keyboardType: TextInputType.number,
              maxLength: 6,
              inputFormatters: [FilteringTextInputFormatter.digitsOnly],
              decoration: const InputDecoration(
                labelText: 'PIN (4–6 dígitos)',
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
              decoration: const InputDecoration(
                labelText: 'Repetir PIN',
                counterText: '',
              ),
            ),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Cancelar')),
          FilledButton(
            onPressed: () {
              final p1 = a.text.trim();
              final p2 = b.text.trim();
              if (!_validPin(p1) || p1 != p2) {
                ScaffoldMessenger.of(ctx).showSnackBar(
                  const SnackBar(content: Text('PINs inválidos ou não coincidem.')),
                );
                return;
              }
              Navigator.pop(ctx, p1);
            },
            child: const Text('Guardar'),
          ),
        ],
      ),
    );
    a.dispose();
    b.dispose();
    if (chosen == null || chosen.isEmpty || !mounted) return;
    await ref.read(appLockNotifierProvider.notifier).setNewPin(chosen);
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('PIN da app guardado.')),
      );
    }
  }

  Future<void> _confirmDisablePin() async {
    final ctrl = TextEditingController();
    final entered = await showDialog<String>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Desativar PIN'),
        content: TextField(
          controller: ctrl,
          obscureText: true,
          keyboardType: TextInputType.number,
          maxLength: 6,
          inputFormatters: [FilteringTextInputFormatter.digitsOnly],
          decoration: const InputDecoration(labelText: 'PIN actual'),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Cancelar')),
          FilledButton(
            onPressed: () => Navigator.pop(ctx, ctrl.text.trim()),
            child: const Text('Confirmar'),
          ),
        ],
      ),
    );
    if (entered == null || entered.isEmpty || !mounted) return;
    final ok = await ref.read(appLockStorageProvider).verifyPin(entered);
    if (!ok) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('PIN incorreto.')),
        );
      }
      return;
    }
    await ref.read(appLockNotifierProvider.notifier).disablePinCompletely();
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Bloqueio por PIN desativado.')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final lock = ref.watch(appLockNotifierProvider);
    final pinOn = lock.pinEnabled;
    final bioOk = _hardwareBio == true;

    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => context.pop(),
        ),
        title: const Text('Segurança da app'),
      ),
      body: ListView(
        children: [
          SwitchListTile(
            title: const Text('Bloquear com PIN'),
            subtitle: Text(
              pinOn
                  ? 'Ao minimizar, a app pede o PIN ao voltar.'
                  : 'Desligado — só a sessão online (login) protege.',
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
            title: const Text('Oferecer biometria no desbloqueio'),
            subtitle: Text(
              bioOk
                  ? 'Impressão digital ou rosto, se o telemóvel permitir.'
                  : 'Este dispositivo não expõe biometria à app.',
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
              title: const Text('Alterar PIN'),
              leading: const Icon(Icons.pin_outlined),
              onTap: () => _promptNewPin(enabling: false),
            ),
        ],
      ),
    );
  }
}
