import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:phosphor_flutter/phosphor_flutter.dart';

import '../../../core/l10n/context_l10n.dart';
import '../../../core/network/dio_client.dart';
import '../application/investments_providers.dart';

class InvestmentAportePage extends ConsumerStatefulWidget {
  const InvestmentAportePage({super.key, required this.positionId});

  final String positionId;

  @override
  ConsumerState<InvestmentAportePage> createState() =>
      _InvestmentAportePageState();
}

class _InvestmentAportePageState extends ConsumerState<InvestmentAportePage> {
  final _amount = TextEditingController();
  bool _busy = false;

  @override
  void dispose() {
    _amount.dispose();
    super.dispose();
  }

  int? _parseCents(String raw) {
    final normalized = raw.trim().replaceAll(',', '.');
    final v = double.tryParse(normalized);
    if (v == null) return null;
    return (v * 100).round();
  }

  Future<void> _submit() async {
    final l10n = context.l10n;
    final cents = _parseCents(_amount.text);
    if (cents == null || cents <= 0) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Valor inválido')),
      );
      return;
    }
    setState(() => _busy = true);
    try {
      await ref.read(investmentsRepositoryProvider).addPrincipal(
            widget.positionId,
            cents,
          );
      ref.invalidate(investmentPositionsProvider);
      ref.invalidate(investmentOverviewProvider);
      if (!mounted) return;
      context.pop();
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Aporte registado.')),
      );
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(messageFromDio(e, l10n) ?? '$e'),
        ),
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
        title: Text(l10n.pcInvestmentsAddPrincipal),
      ),
      body: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(l10n.pcInvestmentsPrincipalHint),
            const SizedBox(height: 8),
            TextField(
              controller: _amount,
              keyboardType: const TextInputType.numberWithOptions(decimal: true),
              inputFormatters: [
                FilteringTextInputFormatter.allow(RegExp(r'[0-9.,]')),
              ],
              decoration: const InputDecoration(
                prefixText: 'R\$ ',
              ),
            ),
            const SizedBox(height: 24),
            FilledButton(
              onPressed: _busy ? null : _submit,
              child: _busy
                  ? const SizedBox(
                      height: 22,
                      width: 22,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : Text(l10n.pcInvestmentsAddPrincipal),
            ),
          ],
        ),
      ),
    );
  }
}
