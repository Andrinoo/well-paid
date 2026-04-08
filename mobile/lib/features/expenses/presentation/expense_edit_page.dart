import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/format/brl_cents.dart';
import '../../../core/format/parse_brl_input.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/theme/well_paid_colors.dart';
import '../../dashboard/application/dashboard_providers.dart';
import '../application/expenses_providers.dart';
import '../domain/expense_item.dart';
import 'widgets/expense_category_dropdown.dart';
import 'widgets/expense_share_form_section.dart';

class ExpenseEditPage extends ConsumerWidget {
  const ExpenseEditPage({super.key, required this.expenseId});

  final String expenseId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final async = ref.watch(expenseDetailProvider(expenseId));

    return async.when(
      loading: () => Scaffold(
        appBar: AppBar(
          leading: IconButton(
            icon: const Icon(Icons.close),
            onPressed: () => context.pop(),
          ),
          title: const Text('Editar despesa'),
        ),
        body: const Center(child: CircularProgressIndicator()),
      ),
      error: (e, _) => Scaffold(
        appBar: AppBar(
          leading: IconButton(
            icon: const Icon(Icons.close),
            onPressed: () => context.pop(),
          ),
          title: const Text('Editar despesa'),
        ),
        body: Center(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Text(messageFromDio(e) ?? 'Erro.'),
          ),
        ),
      ),
      data: (expense) {
        if (!expense.isMine) {
          return Scaffold(
            appBar: AppBar(
              leading: IconButton(
                icon: const Icon(Icons.close),
                onPressed: () => context.pop(),
              ),
              title: const Text('Editar despesa'),
            ),
            body: Center(
              child: Padding(
                padding: const EdgeInsets.all(24),
                child: Text(
                  'Só o titular desta despesa a pode editar.',
                  textAlign: TextAlign.center,
                  style: TextStyle(color: WellPaidColors.navy.withValues(alpha: 0.85)),
                ),
              ),
            ),
          );
        }
        return _ExpenseEditForm(
          key: ValueKey('${expense.id}-${expense.updatedAt.toIso8601String()}'),
          expenseId: expenseId,
          expense: expense,
        );
      },
    );
  }
}

class _ExpenseEditForm extends ConsumerStatefulWidget {
  const _ExpenseEditForm({
    super.key,
    required this.expenseId,
    required this.expense,
  });

  final String expenseId;
  final ExpenseItem expense;

  @override
  ConsumerState<_ExpenseEditForm> createState() => _ExpenseEditFormState();
}

class _ExpenseEditFormState extends ConsumerState<_ExpenseEditForm> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _descCtrl;
  late final TextEditingController _amountCtrl;
  late DateTime _expenseDate;
  DateTime? _dueDate;
  late String _categoryId;
  late String _status;
  String? _recurring;
  late bool _isShared;
  String? _sharedWithUserId;

  @override
  void initState() {
    super.initState();
    final e = widget.expense;
    _descCtrl = TextEditingController(text: e.description);
    _amountCtrl = TextEditingController(
      text: formatBrlInputFromCents(e.amountCents),
    );
    _expenseDate = e.expenseDate;
    _dueDate = e.dueDate;
    _categoryId = e.categoryId;
    _status = e.status;
    _recurring = e.recurringFrequency;
    _isShared = e.isShared;
    _sharedWithUserId = e.sharedWithUserId;
  }

  @override
  void dispose() {
    _descCtrl.dispose();
    _amountCtrl.dispose();
    super.dispose();
  }

  String _dmY(DateTime d) =>
      '${d.day.toString().padLeft(2, '0')}/${d.month.toString().padLeft(2, '0')}/${d.year}';

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
      await ref.read(expensesRepositoryProvider).updateExpense(
            widget.expenseId,
            description: _descCtrl.text,
            amountCents: cents,
            expenseDate: _expenseDate,
            dueDate: _dueDate,
            categoryId: _categoryId,
            status: _status,
            recurringFrequency:
                widget.expense.isInstallmentPlan ? null : _recurring,
            isShared: _isShared,
            sharedWithUserId: _isShared ? _sharedWithUserId : null,
          );
      ref.invalidate(expenseDetailProvider(widget.expenseId));
      ref.invalidate(expensesListProvider);
      ref.invalidate(dashboardOverviewProvider);
      if (mounted) {
        messenger.showSnackBar(
          const SnackBar(content: Text('Alterações guardadas.')),
        );
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
    final cats = ref.watch(categoriesProvider);

    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.close),
          onPressed: () => context.pop(),
        ),
        title: const Text('Editar despesa'),
      ),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: const EdgeInsets.all(20),
          children: [
            if (widget.expense.isInstallmentPlan)
              Padding(
                padding: const EdgeInsets.only(bottom: 16),
                child: Material(
                  color: WellPaidColors.gold.withValues(alpha: 0.22),
                  borderRadius: BorderRadius.circular(12),
                  child: Padding(
                    padding: const EdgeInsets.all(12),
                    child: Row(
                      children: [
                        Icon(Icons.layers_outlined, color: WellPaidColors.navy),
                        const SizedBox(width: 10),
                        Expanded(
                          child: Text(
                            'Parcela ${widget.expense.installmentNumber} de '
                            '${widget.expense.installmentTotal}. '
                            'Alterações aplicam-se só a esta linha.',
                            style: Theme.of(context).textTheme.bodySmall?.copyWith(
                                  color: WellPaidColors.navy,
                                  height: 1.35,
                                ),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            TextFormField(
              controller: _descCtrl,
              decoration: const InputDecoration(labelText: 'Descrição'),
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
                hintText: 'ex. 12,50',
              ),
              keyboardType: const TextInputType.numberWithOptions(decimal: true),
              validator: (v) {
                if (v == null || v.trim().isEmpty) return 'Obrigatório';
                final c = parseInputToCents(v);
                if (c == null || c <= 0) return 'Valor inválido';
                return null;
              },
            ),
            const SizedBox(height: 16),
            ListTile(
              contentPadding: EdgeInsets.zero,
              title: const Text('Data da despesa'),
              subtitle: Text(_dmY(_expenseDate)),
              trailing: const Icon(Icons.calendar_today_outlined),
              onTap: _pickExpenseDate,
            ),
            ListTile(
              contentPadding: EdgeInsets.zero,
              title: const Text('Vencimento (opcional)'),
              subtitle: Text(_dueDate == null ? '—' : _dmY(_dueDate!)),
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
            DropdownButtonFormField<String>(
              decoration: const InputDecoration(labelText: 'Estado'),
              // ignore: deprecated_member_use
              value: _status,
              items: const [
                DropdownMenuItem(value: 'pending', child: Text('Pendente')),
                DropdownMenuItem(value: 'paid', child: Text('Paga')),
              ],
              onChanged: (v) {
                if (v != null) setState(() => _status = v);
              },
            ),
            if (!widget.expense.isInstallmentPlan) ...[
              const SizedBox(height: 12),
              DropdownButtonFormField<String?>(
                decoration: const InputDecoration(
                  labelText: 'Recorrência (metadado)',
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
            ],
            const SizedBox(height: 16),
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
                  Text(messageFromDio(e) ?? 'Erro nas categorias.'),
                  TextButton(
                    onPressed: () => ref.invalidate(categoriesProvider),
                    child: const Text('Tentar novamente'),
                  ),
                ],
              ),
              data: (list) => ExpenseCategoryDropdown(
                categories: list,
                value: _categoryId,
                onChanged: (id) {
                  if (id != null) setState(() => _categoryId = id);
                },
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
              child: const Text('Guardar alterações'),
            ),
          ],
        ),
      ),
    );
  }
}
