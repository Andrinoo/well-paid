import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/format/brl_cents.dart';
import '../../../core/format/parse_brl_input.dart';
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
    if (!_formKey.currentState!.validate()) return;
    if (_categoryId == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Escolhe o tipo de provento.')),
      );
      return;
    }
    final cents = parseInputToCents(_amountCtrl.text);
    if (cents == null || cents <= 0) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Valor inválido.')),
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
        messenger.showSnackBar(const SnackBar(content: Text('Provento registado.')));
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
        title: const Text('Novo provento'),
      ),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: const EdgeInsets.all(20),
          children: [
            Text(
              'Regista entradas reais (salário, extras, etc.). O saldo do mês no '
              'dashboard usa a soma dos proventos deste período.',
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                    color: WellPaidColors.navy.withValues(alpha: 0.68),
                    height: 1.35,
                  ),
            ),
            const SizedBox(height: 16),
            TextFormField(
              controller: _descCtrl,
              decoration: const InputDecoration(
                labelText: 'Descrição',
                hintText: 'ex. Salário abril, Honorários cliente X',
              ),
              textCapitalization: TextCapitalization.sentences,
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
                hintText: 'ex. 3.500,00',
              ),
              keyboardType: const TextInputType.numberWithOptions(decimal: true),
              validator: (v) {
                if (v == null || v.trim().isEmpty) return 'Obrigatório';
                final c = parseInputToCents(v);
                if (c == null || c <= 0) return 'Valor inválido';
                return null;
              },
            ),
            const SizedBox(height: 8),
            ListTile(
              contentPadding: EdgeInsets.zero,
              title: const Text('Data do provento (competência)'),
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
                  Text(messageFromDio(e) ?? 'Erro ao carregar tipos.'),
                  TextButton(
                    onPressed: () => ref.invalidate(incomeCategoriesProvider),
                    child: const Text('Tentar novamente'),
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
              decoration: const InputDecoration(
                labelText: 'Notas (opcional)',
                hintText: 'Origem, referência, NIF… — só para o teu agregado',
                alignLabelWithHint: true,
              ),
              maxLines: 3,
              maxLength: 500,
            ),
            const SizedBox(height: 24),
            FilledButton(
              onPressed: _submit,
              child: const Text('Guardar provento'),
            ),
          ],
        ),
      ),
    );
  }
}
