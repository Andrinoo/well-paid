import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/format/parse_brl_input.dart';
import '../../../core/l10n/context_l10n.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/theme/well_paid_colors.dart';
import '../../dashboard/application/dashboard_providers.dart';
import '../application/incomes_providers.dart';
import 'widgets/income_category_dropdown.dart';

class NewIncomePage extends ConsumerStatefulWidget {
  const NewIncomePage({super.key});

  @override
  ConsumerState<NewIncomePage> createState() => _NewIncomePageState();
}

class _NewIncomePageState extends ConsumerState<NewIncomePage> {
  final _formKey = GlobalKey<FormState>();
  final _descCtrl = TextEditingController();
  final _amountCtrl = TextEditingController();
  final _notesCtrl = TextEditingController();
  DateTime _incomeDate = DateTime.now();
  String? _categoryId;

  @override
  void dispose() {
    _descCtrl.dispose();
    _amountCtrl.dispose();
    _notesCtrl.dispose();
    super.dispose();
  }

  String _dmY(DateTime d) =>
      '${d.day.toString().padLeft(2, '0')}/${d.month.toString().padLeft(2, '0')}/${d.year}';

  Future<void> _pickDate() async {
    final d = await showDatePicker(
      context: context,
      initialDate: _incomeDate,
      firstDate: DateTime(2000),
      lastDate: DateTime(2100),
    );
    if (d != null) setState(() => _incomeDate = d);
  }

  Future<void> _submit() async {
    final l10n = context.l10n;
    if (!_formKey.currentState!.validate()) return;
    if (_categoryId == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(l10n.incFormPickCategory)),
      );
      return;
    }
    final cents = parseInputToCents(_amountCtrl.text);
    if (cents == null || cents <= 0) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(l10n.valueInvalid)),
      );
      return;
    }

    final messenger = ScaffoldMessenger.of(context);
    try {
      await ref.read(incomesRepositoryProvider).createIncome(
            description: _descCtrl.text,
            amountCents: cents,
            incomeDate: _incomeDate,
            incomeCategoryId: _categoryId!,
            notes: _notesCtrl.text.trim().isEmpty ? null : _notesCtrl.text.trim(),
          );
      ref.invalidate(incomesListProvider);
      ref.invalidate(dashboardOverviewProvider);
      ref.invalidate(incomeCategoriesProvider);
      if (mounted) {
        messenger.showSnackBar(SnackBar(content: Text(l10n.incFormCreatedSnackbar)));
        context.pop();
      }
    } catch (e) {
      if (mounted) {
        messenger.showSnackBar(
          SnackBar(content: Text(messageFromDio(e, l10n) ?? l10n.incomeSaveError)),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final cats = ref.watch(incomeCategoriesProvider);

    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.close),
          onPressed: () => context.pop(),
        ),
        title: Text(l10n.newIncomeTitle),
      ),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: const EdgeInsets.all(20),
          children: [
            Text(
              l10n.incFormIntro,
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                    color: WellPaidColors.navy.withValues(alpha: 0.68),
                    height: 1.35,
                  ),
            ),
            const SizedBox(height: 16),
            TextFormField(
              controller: _descCtrl,
              decoration: InputDecoration(
                labelText: l10n.incFormDescription,
                hintText: l10n.incFormDescHint,
              ),
              textCapitalization: TextCapitalization.sentences,
              maxLength: 500,
              validator: (v) {
                if (v == null || v.trim().isEmpty) return l10n.requiredField;
                return null;
              },
            ),
            const SizedBox(height: 8),
            TextFormField(
              controller: _amountCtrl,
              decoration: InputDecoration(
                labelText: l10n.incFormAmount,
                hintText: l10n.incFormAmountHint,
              ),
              keyboardType: const TextInputType.numberWithOptions(decimal: true),
              validator: (v) {
                if (v == null || v.trim().isEmpty) return l10n.requiredField;
                final c = parseInputToCents(v);
                if (c == null || c <= 0) return l10n.valueInvalid;
                return null;
              },
            ),
            const SizedBox(height: 8),
            ListTile(
              contentPadding: EdgeInsets.zero,
              title: Text(l10n.incFormIncomeDateCompetence),
              subtitle: Text(_dmY(_incomeDate)),
              trailing: const Icon(Icons.calendar_today_outlined),
              onTap: _pickDate,
            ),
            const SizedBox(height: 8),
            cats.when(
              loading: () => const Center(
                child: Padding(
                  padding: EdgeInsets.all(24),
                  child: CircularProgressIndicator(),
                ),
              ),
              error: (e, _) => Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Text(messageFromDio(e, l10n) ?? l10n.incFormCategoriesLoadError),
                  TextButton(
                    onPressed: () => ref.invalidate(incomeCategoriesProvider),
                    child: Text(l10n.tryAgain),
                  ),
                ],
              ),
              data: (list) => IncomeCategoryDropdown(
                categories: list,
                value: _categoryId,
                onChanged: (id) => setState(() => _categoryId = id),
              ),
            ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _notesCtrl,
              decoration: InputDecoration(
                labelText: l10n.incFormNotes,
                hintText: l10n.incFormNotesHint,
                alignLabelWithHint: true,
              ),
              maxLines: 3,
              maxLength: 500,
            ),
            const SizedBox(height: 24),
            FilledButton(
              onPressed: _submit,
              child: Text(l10n.incFormSaveButton),
            ),
          ],
        ),
      ),
    );
  }
}
