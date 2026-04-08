import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/format/brl_cents.dart';
import '../../../core/format/parse_brl_input.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/theme/well_paid_colors.dart';
import '../../dashboard/application/dashboard_providers.dart';
import '../application/expenses_providers.dart';
import 'widgets/expense_category_dropdown.dart';
import 'widgets/expense_share_form_section.dart';

class NewExpensePage extends ConsumerStatefulWidget {
  const NewExpensePage({super.key});

  @override
  ConsumerState<NewExpensePage> createState() => _NewExpensePageState();
}

class _NewExpensePageState extends ConsumerState<NewExpensePage> {
  final _formKey = GlobalKey<FormState>();
  final _descCtrl = TextEditingController();
  final _amountCtrl = TextEditingController();
  final _installmentsCtrl = TextEditingController(text: '1');
  DateTime _expenseDate = DateTime.now();
  DateTime? _dueDate;
  String? _categoryId;
  bool _hasDueDate = false;
  bool _markPaid = false;
  int _installments = 1;
  String? _recurring;
  bool _isShared = false;
  String? _sharedWithUserId;

  @override
  void dispose() {
    _descCtrl.dispose();
    _amountCtrl.dispose();
    _installmentsCtrl.dispose();
    super.dispose();
  }

  Future<void> _pickExpenseDate() async {
    final d = await showDatePicker(
      context: context,
      initialDate: _expenseDate,
      firstDate: DateTime(2000),
      lastDate: DateTime(2100),
    );
    if (d != null) setState(() => _expenseDate = d);
  }

  Future<void> _pickDueDate() async {
    final d = await showDatePicker(
      context: context,
      initialDate: _dueDate ?? DateTime.now(),
      firstDate: DateTime(2000),
      lastDate: DateTime(2100),
    );
    if (d != null) setState(() => _dueDate = d);
  }

  String _dmY(DateTime d) =>
      '${d.day.toString().padLeft(2, '0')}/${d.month.toString().padLeft(2, '0')}/${d.year}';

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    _installments = int.parse(_installmentsCtrl.text.trim());
    if (_categoryId == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Escolhe uma categoria.')),
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
    if (_hasDueDate && _dueDate == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Escolhe a data de vencimento.')),
      );
      return;
    }

    final messenger = ScaffoldMessenger.of(context);
    try {
      await ref.read(expensesRepositoryProvider).createExpense(
            description: _descCtrl.text,
            amountCents: cents,
            expenseDate: _expenseDate,
            dueDate: _hasDueDate ? _dueDate : null,
            categoryId: _categoryId!,
            status: _markPaid ? 'paid' : 'pending',
            installmentTotal: _installments,
            recurringFrequency: _installments == 1 ? _recurring : null,
            isShared: _isShared,
            sharedWithUserId: _isShared ? _sharedWithUserId : null,
          );
      ref.invalidate(expensesListProvider);
      ref.invalidate(dashboardOverviewProvider);
      ref.invalidate(categoriesProvider);
      if (mounted) {
        messenger.showSnackBar(
          SnackBar(
            content: Text(
              _installments > 1
                  ? 'Criadas $_installments despesas (parcelas mensais).'
                  : 'Despesa criada.',
            ),
          ),
        );
        context.pop();
      }
    } catch (e) {
      if (mounted) {
        messenger.showSnackBar(
          SnackBar(content: Text(messageFromDio(e) ?? 'Erro ao criar.')),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final cats = ref.watch(categoriesProvider);
    final installmentAmountCents = parseInputToCents(_amountCtrl.text);
    final totalForPlanCents = installmentAmountCents == null
        ? null
        : installmentAmountCents * _installments;

    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.close),
          onPressed: () => context.pop(),
        ),
        title: const Text('Nova despesa'),
      ),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: const EdgeInsets.all(20),
          children: [
            TextFormField(
              controller: _descCtrl,
              decoration: const InputDecoration(
                labelText: 'Descrição',
              ),
              textCapitalization: TextCapitalization.sentences,
              maxLength: 500,
              validator: (v) {
                if (v == null || v.trim().isEmpty) {
                  return 'Obrigatório';
                }
                return null;
              },
            ),
            const SizedBox(height: 8),
            TextFormField(
              controller: _amountCtrl,
              decoration: InputDecoration(
                labelText: _installments > 1
                    ? 'Valor da parcela (R\$)'
                    : 'Valor (R\$)',
                hintText: 'ex. 12,50',
              ),
              keyboardType: const TextInputType.numberWithOptions(decimal: true),
              onChanged: (_) => setState(() {}),
              validator: (v) {
                if (v == null || v.trim().isEmpty) return 'Obrigatório';
                final c = parseInputToCents(v);
                if (c == null || c <= 0) return 'Valor inválido';
                return null;
              },
            ),
            const SizedBox(height: 8),
            TextFormField(
              controller: _installmentsCtrl,
              decoration: const InputDecoration(
                labelText: 'Parcelas (competência mensal)',
                hintText: '1 a 24',
              ),
              keyboardType: TextInputType.number,
              inputFormatters: [
                FilteringTextInputFormatter.digitsOnly,
                LengthLimitingTextInputFormatter(2),
              ],
              validator: (v) {
                if (v == null || v.trim().isEmpty) return 'Obrigatório';
                final n = int.tryParse(v.trim());
                if (n == null) return 'Número inválido';
                if (n < 1 || n > 24) return 'Use de 1 a 24';
                return null;
              },
              onChanged: (v) {
                final n = int.tryParse(v.trim());
                if (n == null || n < 1 || n > 24) return;
                setState(() {
                  _installments = n;
                  if (n > 1) _recurring = null;
                });
              },
            ),
            if (_installments > 1)
              Padding(
                padding: const EdgeInsets.only(top: 8, bottom: 4),
                child: Text(
                  totalForPlanCents == null
                      ? 'Informe o valor de cada parcela. '
                          'Total = parcela × $_installments.'
                      : 'Total do plano: ${formatBrlFromCents(totalForPlanCents)} '
                          '($_installments × ${formatBrlFromCents(installmentAmountCents!)}).',
                  style: Theme.of(context).textTheme.bodySmall?.copyWith(
                        color: WellPaidColors.navy.withValues(alpha: 0.72),
                        height: 1.35,
                      ),
                ),
              ),
            if (_installments == 1) ...[
              const SizedBox(height: 8),
              DropdownButtonFormField<String?>(
                decoration: const InputDecoration(
                  labelText: 'Recorrência (opcional)',
                ),
                // ignore: deprecated_member_use
                value: _recurring,
                items: const [
                  DropdownMenuItem<String?>(
                    value: null,
                    child: Text('Nenhuma'),
                  ),
                  DropdownMenuItem(value: 'monthly', child: Text('Mensal')),
                  DropdownMenuItem(value: 'weekly', child: Text('Semanal')),
                  DropdownMenuItem(value: 'yearly', child: Text('Anual')),
                ],
                onChanged: (v) => setState(() => _recurring = v),
              ),
              Padding(
                padding: const EdgeInsets.only(top: 6),
                child: Text(
                  'Recorrência regista a frequência; geração automática de '
                  'futuras despesas virá numa fase seguinte.',
                  style: Theme.of(context).textTheme.labelSmall?.copyWith(
                        color: WellPaidColors.navy.withValues(alpha: 0.55),
                        fontStyle: FontStyle.italic,
                      ),
                ),
              ),
            ],
            const SizedBox(height: 12),
            SwitchListTile(
              contentPadding: EdgeInsets.zero,
              title: const Text('Já está paga'),
              value: _markPaid,
              onChanged: (v) => setState(() => _markPaid = v),
            ),
            SwitchListTile(
              contentPadding: EdgeInsets.zero,
              title: const Text('Tem data de vencimento'),
              subtitle: const Text('Contas a pagar / alertas no dashboard'),
              value: _hasDueDate,
              onChanged: (v) => setState(() {
                _hasDueDate = v;
                if (!v) _dueDate = null;
              }),
            ),
            const SizedBox(height: 8),
            ListTile(
              contentPadding: EdgeInsets.zero,
              title: const Text('Data da despesa (competência)'),
              subtitle: Text(_dmY(_expenseDate)),
              trailing: const Icon(Icons.calendar_today_outlined),
              onTap: _pickExpenseDate,
            ),
            if (_hasDueDate)
              ListTile(
                contentPadding: EdgeInsets.zero,
                title: const Text('Data de vencimento'),
                subtitle: Text(_dueDate == null ? 'Escolher…' : _dmY(_dueDate!)),
                trailing: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    if (_dueDate != null)
                      IconButton(
                        icon: const Icon(Icons.clear),
                        onPressed: () => setState(() => _dueDate = null),
                      ),
                    const Icon(Icons.event_outlined),
                  ],
                ),
                onTap: _pickDueDate,
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
                  Text(
                    messageFromDio(e) ?? 'Erro ao carregar categorias.',
                    style: TextStyle(
                      color: WellPaidColors.navy.withValues(alpha: 0.8),
                    ),
                  ),
                  TextButton(
                    onPressed: () => ref.invalidate(categoriesProvider),
                    child: const Text('Tentar novamente'),
                  ),
                ],
              ),
              data: (list) => ExpenseCategoryDropdown(
                categories: list,
                value: _categoryId,
                onChanged: (id) => setState(() => _categoryId = id),
              ),
            ),
            const SizedBox(height: 16),
            ExpenseShareFormSection(
              isShared: _isShared,
              sharedWithUserId: _sharedWithUserId,
              onSharedChanged: (v) => setState(() => _isShared = v),
              onPeerChanged: (v) => setState(() => _sharedWithUserId = v),
            ),
            const SizedBox(height: 28),
            FilledButton(
              onPressed: _submit,
              child: const Text('Guardar'),
            ),
          ],
        ),
      ),
    );
  }
}
