import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:phosphor_flutter/phosphor_flutter.dart';

import '../../../core/l10n/context_l10n.dart';
import '../../../core/network/dio_client.dart';
import '../application/emergency_plans_providers.dart';

class EmergencyReservePlanCreatePage extends ConsumerStatefulWidget {
  const EmergencyReservePlanCreatePage({super.key});

  @override
  ConsumerState<EmergencyReservePlanCreatePage> createState() =>
      _EmergencyReservePlanCreatePageState();
}

class _EmergencyReservePlanCreatePageState
    extends ConsumerState<EmergencyReservePlanCreatePage> {
  final _title = TextEditingController();
  final _monthly = TextEditingController();
  final _months = TextEditingController();
  bool _busy = false;

  @override
  void dispose() {
    _title.dispose();
    _monthly.dispose();
    _months.dispose();
    super.dispose();
  }

  int? _parseCents(String raw) {
    final n = raw.trim().replaceAll(',', '.');
    final v = double.tryParse(n);
    if (v == null) return null;
    return (v * 100).round();
  }

  Future<void> _save() async {
    final l10n = context.l10n;
    final title = _title.text.trim();
    if (title.length < 2) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Informe um título.')),
      );
      return;
    }
    final cents = _parseCents(_monthly.text);
    if (cents == null || cents < 0) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Meta mensal inválida.')),
      );
      return;
    }
    int? dur;
    final mt = _months.text.trim();
    if (mt.isNotEmpty) {
      dur = int.tryParse(mt);
      if (dur == null || dur < 1) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Duração em meses inválida.')),
        );
        return;
      }
    }

    setState(() => _busy = true);
    try {
      final created = await ref.read(emergencyPlansRepositoryProvider).createPlan(
            title: title,
            monthlyTargetCents: cents,
            planDurationMonths: dur,
          );
      ref.invalidate(emergencyPlansListProvider);
      if (!mounted) return;
      context.go('/emergency-plan/${created.id}');
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(messageFromDio(e, l10n) ?? '$e')),
      );
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;

    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(PhosphorIconsRegular.arrowLeft),
          onPressed: () => context.pop(),
        ),
        title: const Text('Novo plano de reserva'),
      ),
      body: ListView(
        padding: const EdgeInsets.all(24),
        children: [
          TextField(
            controller: _title,
            decoration: const InputDecoration(labelText: 'Título'),
          ),
          const SizedBox(height: 16),
          TextField(
            controller: _monthly,
            keyboardType: const TextInputType.numberWithOptions(decimal: true),
            inputFormatters: [
              FilteringTextInputFormatter.allow(RegExp(r'[0-9.,]')),
            ],
            decoration: const InputDecoration(
              labelText: 'Meta mensal (R\$)',
              prefixText: 'R\$ ',
            ),
          ),
          const SizedBox(height: 16),
          TextField(
            controller: _months,
            keyboardType: TextInputType.number,
            decoration: const InputDecoration(
              labelText: 'Duração (meses, opcional)',
            ),
          ),
          const SizedBox(height: 28),
          FilledButton(
            onPressed: _busy ? null : _save,
            child: _busy
                ? const SizedBox(
                    height: 22,
                    width: 22,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : Text(l10n.emergencyReserveSave),
          ),
        ],
      ),
    );
  }
}
