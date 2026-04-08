import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/format/brl_cents.dart';
import '../../../core/format/parse_brl_input.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/theme/well_paid_colors.dart';
import '../../dashboard/application/dashboard_providers.dart';
import '../application/incomes_providers.dart';
import '../domain/income_item.dart';
import 'widgets/income_category_dropdown.dart';

class IncomeEditPage extends ConsumerWidget {
  const IncomeEditPage({super.key, required this.incomeId});

  final String incomeId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final async = ref.watch(incomeDetailProvider(incomeId));

    return async.when(
      loading: () => Scaffold(
        appBar: AppBar(
          leading: IconButton(
            icon: const Icon(Icons.close),
            onPressed: () => context.pop(),
          ),
          title: const Text('Editar provento'),
        ),
        body: const Center(child: CircularProgressIndicator()),
      ),
      error: (e, _) => Scaffold(
        appBar: AppBar(
          leading: IconButton(
            icon: const Icon(Icons.close),
            onPressed: () => context.pop(),
          ),
          title: const Text('Editar provento'),
        ),
        body: Center(
          child: Text(messageFromDio(e) ?? 'Erro.'),
        ),
      ),
      data: (income) {
        if (!income.isMine) {
          return Scaffold(
            appBar: AppBar(
              leading: IconButton(
                icon: const Icon(Icons.close),
                onPressed: () => context.pop(),
              ),
              title: const Text('Editar provento'),
            ),
            body: Center(
              child: Padding(
                padding: const EdgeInsets.all(24),
                child: Text(
                  'Só o titular deste provento o pode editar.',
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    color: WellPaidColors.navy.withValues(alpha: 0.85),
                  ),
                ),
              ),
            ),
          );
        }
        return _IncomeEditForm(
          key: ValueKey('${income.id}-${income.updatedAt.toIso8601String()}'),
          incomeId: incomeId,
          income: income,
        );
      },
    );
  }
}

class _IncomeEditForm extends ConsumerStatefulWidget {
  const _IncomeEditForm({
    super.key,
    required this.incomeId,
    required this.income,
  });

  final String incomeId;
  final IncomeItem income;

  @override
  ConsumerState<_IncomeEditForm> createState() => _IncomeEditFormState();
}

class _IncomeEditFormState extends ConsumerState<_IncomeEditForm> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _descCtrl;
  late final TextEditingController _amountCtrl;
  late final TextEditingController _notesCtrl;
  late DateTime _incomeDate;
  late String _categoryId;

  @override
  void initState() {
    super.initState();
    final i = widget.income;
    _descCtrl = TextEditingController(text: i.description);
    _amountCtrl = TextEditingController(
      text: formatBrlInputFromCents(i.amountCents),
    );
    _notesCtrl = TextEditingController(text: i.notes ?? '');
    _incomeDate = i.incomeDate;
    _categoryId = i.incomeCategoryId;
  }

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
    if (!_formKey.currentState!.validate()) return;
    final cents = parseInputToCents(_amountCtrl.text);
    if (cents == null || cents <= 0) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Valor inválido.')),
      );
      return;
    }
    final messenger = ScaffoldMessenger.of(context);
    try {
      await ref.read(incomesRepositoryProvider).updateIncome(
            widget.incomeId,
            description: _descCtrl.text,
            amountCents: cents,
            incomeDate: _incomeDate,
            incomeCategoryId: _categoryId,
            notes: _notesCtrl.text.trim().isEmpty ? null : _notesCtrl.text.trim(),
          );
      ref.invalidate(incomeDetailProvider(widget.incomeId));
      ref.invalidate(incomesListProvider);
      ref.invalidate(dashboardOverviewProvider);
      if (mounted) {
        messenger.showSnackBar(const SnackBar(content: Text('Guardado.')));
        context.pop();
      }
    } catch (e) {
      if (mounted) {
        messenger.showSnackBar(
          SnackBar(content: Text(messageFromDio(e) ?? 'Erro ao guardar.')),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final cats = ref.watch(incomeCategoriesProvider);

    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.close),
          onPressed: () => context.pop(),
        ),
        title: const Text('Editar provento'),
      ),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: const EdgeInsets.all(20),
          children: [
            TextFormField(
              controller: _descCtrl,
              decoration: const InputDecoration(labelText: 'Descrição'),
              maxLength: 500,
              validator: (v) {
                if (v == null || v.trim().isEmpty) return 'Obrigatório';
                return null;
              },
            ),
            const SizedBox(height: 8),
            TextFormField(
              controller: _amountCtrl,
              decoration: const InputDecoration(
                labelText: 'Valor (R\$)',
              ),
              keyboardType: const TextInputType.numberWithOptions(decimal: true),
              validator: (v) {
                if (v == null || v.trim().isEmpty) return 'Obrigatório';
                final c = parseInputToCents(v);
                if (c == null || c <= 0) return 'Valor inválido';
                return null;
              },
            ),
            ListTile(
              contentPadding: EdgeInsets.zero,
              title: const Text('Data do provento'),
              subtitle: Text(_dmY(_incomeDate)),
              trailing: const Icon(Icons.calendar_today_outlined),
              onTap: _pickDate,
            ),
            cats.when(
              loading: () => const Padding(
                padding: EdgeInsets.all(24),
                child: Center(child: CircularProgressIndicator()),
              ),
              error: (e, _) => Text(messageFromDio(e) ?? 'Erro'),
              data: (list) => IncomeCategoryDropdown(
                categories: list,
                value: _categoryId,
                onChanged: (id) {
                  if (id != null) setState(() => _categoryId = id);
                },
              ),
            ),
            const SizedBox(height: 8),
            TextFormField(
              controller: _notesCtrl,
              decoration: const InputDecoration(
                labelText: 'Notas (opcional)',
              ),
              maxLines: 3,
              maxLength: 500,
            ),
            const SizedBox(height: 24),
            FilledButton(
              onPressed: _submit,
              child: const Text('Guardar alterações'),
            ),
          ],
        ),
      ),
    );
  }
}
