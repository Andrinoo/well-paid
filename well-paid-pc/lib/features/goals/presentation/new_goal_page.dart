import 'package:flutter/material.dart';
import 'package:phosphor_flutter/phosphor_flutter.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/format/brl_currency_input_formatter.dart';
import '../../../core/format/parse_brl_input.dart';
import '../../../core/l10n/context_l10n.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/theme/well_paid_colors.dart';
import '../../dashboard/application/dashboard_providers.dart';
import '../application/goals_providers.dart';

class NewGoalPage extends ConsumerStatefulWidget {
  const NewGoalPage({super.key});

  @override
  ConsumerState<NewGoalPage> createState() => _NewGoalPageState();
}

class _NewGoalPageState extends ConsumerState<NewGoalPage> {
  final _formKey = GlobalKey<FormState>();
  final _titleCtrl = TextEditingController();
  final _targetCtrl = TextEditingController();
  final _initialCtrl = TextEditingController();

  @override
  void dispose() {
    _titleCtrl.dispose();
    _targetCtrl.dispose();
    _initialCtrl.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    final l10n = context.l10n;
    if (!_formKey.currentState!.validate()) return;
    final targetCents = parseInputToCents(_targetCtrl.text);
    if (targetCents == null || targetCents <= 0) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(l10n.valueInvalid)),
      );
      return;
    }
    final initialRaw = _initialCtrl.text.trim();
    final initialCents =
        initialRaw.isEmpty ? 0 : (parseInputToCents(initialRaw) ?? -1);
    if (initialCents < 0) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(l10n.valueInvalid)),
      );
      return;
    }

    final messenger = ScaffoldMessenger.of(context);
    try {
      await ref.read(goalsRepositoryProvider).createGoal(
            title: _titleCtrl.text,
            targetCents: targetCents,
            currentCents: initialCents,
          );
      ref.invalidate(goalsListProvider);
      ref.invalidate(dashboardOverviewProvider);
      if (mounted) {
        messenger.showSnackBar(
          SnackBar(content: Text(l10n.goalFormCreatedSnackbar)),
        );
        context.pop();
      }
    } catch (e) {
      if (mounted) {
        messenger.showSnackBar(
          SnackBar(
            content: Text(messageFromDio(e, l10n) ?? l10n.goalSaveError),
          ),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;

    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(PhosphorIconsRegular.x),
          onPressed: () => context.pop(),
        ),
        title: Text(l10n.newGoalTitle),
      ),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: const EdgeInsets.all(20),
          children: [
            Text(
              l10n.goalFormIntro,
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                    color: WellPaidColors.navy.withValues(alpha: 0.68),
                    height: 1.35,
                  ),
            ),
            const SizedBox(height: 16),
            TextFormField(
              controller: _titleCtrl,
              decoration: InputDecoration(
                labelText: l10n.goalFormTitleLabel,
              ),
              textCapitalization: TextCapitalization.sentences,
              maxLength: 200,
              validator: (v) {
                if (v == null || v.trim().isEmpty) {
                  return l10n.requiredField;
                }
                return null;
              },
            ),
            const SizedBox(height: 8),
            TextFormField(
              controller: _targetCtrl,
              decoration: InputDecoration(
                labelText: l10n.goalFormTargetLabel,
              ),
              keyboardType: const TextInputType.numberWithOptions(decimal: true),
              inputFormatters: kBrCurrencyInputFormatters,
            ),
            const SizedBox(height: 8),
            TextFormField(
              controller: _initialCtrl,
              decoration: InputDecoration(
                labelText: l10n.goalFormInitialLabel,
                helperText: l10n.goalFormInitialHint,
              ),
              keyboardType: const TextInputType.numberWithOptions(decimal: true),
              inputFormatters: kBrCurrencyInputFormatters,
            ),
            const SizedBox(height: 24),
            FilledButton(
              onPressed: _submit,
              child: Text(l10n.goalFormSave),
            ),
          ],
        ),
      ),
    );
  }
}
