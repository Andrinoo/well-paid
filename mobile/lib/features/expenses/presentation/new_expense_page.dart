import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/format/brl_cents.dart';
import '../../../core/format/parse_brl_input.dart';
import '../../../core/l10n/context_l10n.dart';
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

enum _ExpenseKind { single, installments, recurring }

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
  _ExpenseKind _kind = _ExpenseKind.single;
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
    final l10n = context.l10n;
    if (!_formKey.currentState!.validate()) return;
    final installmentTotal = _kind == _ExpenseKind.installments
        ? int.parse(_installmentsCtrl.text.trim())
        : 1;
    if (_kind == _ExpenseKind.installments) {
      _installments = installmentTotal;
    }
    final recurringFrequency =
        _kind == _ExpenseKind.recurring ? _recurring : null;
    if (_categoryId == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(l10n.expFormPickCategory)),
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
    if (_hasDueDate && _dueDate == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(l10n.expFormPickDue)),
      );
      return;
    }
    if (_kind == _ExpenseKind.recurring &&
        (recurringFrequency == null || recurringFrequency.isEmpty)) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(l10n.expFormRecurringFreqRequired)),
      );
      return;
    }

    final messenger = ScaffoldMessenger.of(context);
    try {
      final created = await ref.read(expensesRepositoryProvider).createExpense(
            description: _descCtrl.text,
            amountCents: cents,
            expenseDate: _expenseDate,
            dueDate: _hasDueDate ? _dueDate : null,
            categoryId: _categoryId!,
            status: _markPaid ? 'paid' : 'pending',
            installmentTotal: installmentTotal,
            recurringFrequency: recurringFrequency,
            isShared: _isShared,
            sharedWithUserId: _isShared ? _sharedWithUserId : null,
          );
      ref.invalidate(expensesListProvider);
      ref.invalidate(dashboardOverviewProvider);
      ref.invalidate(categoriesProvider);
      if (mounted) {
        final n = created.length;
        final planRef = n > 1
            ? (created.first.installmentGroupId != null &&
                    created.first.installmentGroupId!.length >= 8
                ? '${created.first.installmentGroupId!.substring(0, 8)}…'
                : null)
            : null;
        final msg = n > 1
            ? (planRef != null
                ? l10n.expFormPlanCreatedRef(n, planRef)
                : l10n.expFormPlanCreated(n))
            : l10n.expFormCreated;
        messenger.showSnackBar(SnackBar(content: Text(msg)));
        context.pop();
      }
    } catch (e) {
      if (mounted) {
        messenger.showSnackBar(
          SnackBar(content: Text(messageFromDio(e, l10n) ?? l10n.expFormCreateError)),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final cats = ref.watch(categoriesProvider);
    final installmentAmountCents = parseInputToCents(_amountCtrl.text);
    final totalForPlanCents = installmentAmountCents == null
        ? null
        : installmentAmountCents *
            (_kind == _ExpenseKind.installments ? _installments : 1);

    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.close),
          onPressed: () => context.pop(),
        ),
        title: Text(l10n.newExpenseTitle),
      ),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: const EdgeInsets.all(20),
          children: [
            TextFormField(
              controller: _descCtrl,
              decoration: InputDecoration(
                labelText: l10n.expFormDescription,
              ),
              textCapitalization: TextCapitalization.sentences,
              maxLength: 500,
              validator: (v) {
                if (v == null || v.trim().isEmpty) {
                  return l10n.requiredField;
                }
                return null;
              },
            ),
            const SizedBox(height: 8),
            SegmentedButton<_ExpenseKind>(
              segments: [
                ButtonSegment<_ExpenseKind>(
                  value: _ExpenseKind.single,
                  label: Text(l10n.expFormKindSingle),
                  icon: const Icon(Icons.receipt_long_outlined),
                ),
                ButtonSegment<_ExpenseKind>(
                  value: _ExpenseKind.installments,
                  label: Text(l10n.expFormKindInstallments),
                  icon: const Icon(Icons.calendar_view_month_outlined),
                ),
                ButtonSegment<_ExpenseKind>(
                  value: _ExpenseKind.recurring,
                  label: Text(l10n.expFormKindRecurring),
                  icon: const Icon(Icons.autorenew_outlined),
                ),
              ],
              selected: {_kind},
              onSelectionChanged: (s) {
                if (s.isEmpty) return;
                setState(() {
                  _kind = s.first;
                  if (_kind != _ExpenseKind.installments) {
                    _installmentsCtrl.text = '1';
                    _installments = 1;
                  }
                  if (_kind != _ExpenseKind.recurring) {
                    _recurring = null;
                  }
                });
              },
            ),
            const SizedBox(height: 16),
            TextFormField(
              controller: _amountCtrl,
              decoration: InputDecoration(
                labelText: _kind == _ExpenseKind.installments
                    ? l10n.expFormAmountInstallment
                    : l10n.expFormAmount,
                hintText: l10n.expFormAmountHint,
              ),
              keyboardType: const TextInputType.numberWithOptions(decimal: true),
              onChanged: (_) => setState(() {}),
              validator: (v) {
                if (v == null || v.trim().isEmpty) return l10n.requiredField;
                final c = parseInputToCents(v);
                if (c == null || c <= 0) return l10n.valueInvalid;
                return null;
              },
            ),
            const SizedBox(height: 8),
            if (_kind == _ExpenseKind.installments)
              TextFormField(
                controller: _installmentsCtrl,
                decoration: InputDecoration(
                  labelText: l10n.expFormInstallmentsLabel,
                  hintText: l10n.expFormInstallmentsHint,
                ),
                keyboardType: TextInputType.number,
                inputFormatters: [
                  FilteringTextInputFormatter.digitsOnly,
                  LengthLimitingTextInputFormatter(2),
                ],
                validator: (v) {
                  if (v == null || v.trim().isEmpty) return l10n.requiredField;
                  final n = int.tryParse(v.trim());
                  if (n == null) return l10n.expFormInstallmentInvalid;
                  if (n < 1 || n > 24) return l10n.expFormInstallmentRangeError;
                  return null;
                },
                onChanged: (v) {
                  final n = int.tryParse(v.trim());
                  if (n == null || n < 1 || n > 24) return;
                  setState(() => _installments = n);
                },
              ),
            if (_kind == _ExpenseKind.installments)
              Padding(
                padding: const EdgeInsets.only(top: 8, bottom: 4),
                child: Text(
                  totalForPlanCents == null
                      ? l10n.expFormInstallmentNeedAmountLine(_installments)
                      : l10n.expFormInstallmentPlanTotalLine(
                          formatBrlFromCents(totalForPlanCents),
                          _installments,
                          formatBrlFromCents(installmentAmountCents!),
                        ),
                  style: Theme.of(context).textTheme.bodySmall?.copyWith(
                        color: WellPaidColors.navy.withValues(alpha: 0.72),
                        height: 1.35,
                      ),
                ),
              ),
            if (_kind == _ExpenseKind.recurring) ...[
              const SizedBox(height: 8),
              DropdownButtonFormField<String?>(
                decoration: InputDecoration(
                  labelText: l10n.expFormRecurringFrequency,
                ),
                // ignore: deprecated_member_use
                value: _recurring,
                items: [
                  DropdownMenuItem<String?>(
                    value: null,
                    child: Text(l10n.expFormRecurringChoose),
                  ),
                  DropdownMenuItem(
                    value: 'monthly',
                    child: Text(l10n.expFormFreqMonthly),
                  ),
                  DropdownMenuItem(
                    value: 'weekly',
                    child: Text(l10n.expFormFreqWeekly),
                  ),
                  DropdownMenuItem(
                    value: 'yearly',
                    child: Text(l10n.expFormFreqYearly),
                  ),
                ],
                onChanged: (v) => setState(() => _recurring = v),
              ),
              Padding(
                padding: const EdgeInsets.only(top: 6),
                child: Text(
                  l10n.expFormRecurringHelp,
                  style: Theme.of(context).textTheme.labelSmall?.copyWith(
                        color: WellPaidColors.navy.withValues(alpha: 0.55),
                        height: 1.35,
                      ),
                ),
              ),
            ],
            const SizedBox(height: 12),
            SwitchListTile(
              contentPadding: EdgeInsets.zero,
              title: Text(l10n.expFormMarkPaid),
              value: _markPaid,
              onChanged: (v) => setState(() => _markPaid = v),
            ),
            SwitchListTile(
              contentPadding: EdgeInsets.zero,
              title: Text(l10n.expFormHasDueDate),
              subtitle: Text(l10n.expFormHasDueDateSub),
              value: _hasDueDate,
              onChanged: (v) => setState(() {
                _hasDueDate = v;
                if (!v) _dueDate = null;
              }),
            ),
            const SizedBox(height: 8),
            ListTile(
              contentPadding: EdgeInsets.zero,
              title: Text(l10n.expFormExpenseDate),
              subtitle: Text(_dmY(_expenseDate)),
              trailing: const Icon(Icons.calendar_today_outlined),
              onTap: _pickExpenseDate,
            ),
            if (_hasDueDate)
              ListTile(
                contentPadding: EdgeInsets.zero,
                title: Text(l10n.expFormDueDate),
                subtitle: Text(
                  _dueDate == null ? l10n.expFormChooseDate : _dmY(_dueDate!),
                ),
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
                    messageFromDio(e, l10n) ?? l10n.expFormCategoriesLoadError,
                    style: TextStyle(
                      color: WellPaidColors.navy.withValues(alpha: 0.8),
                    ),
                  ),
                  TextButton(
                    onPressed: () => ref.invalidate(categoriesProvider),
                    child: Text(l10n.tryAgain),
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
              child: Text(l10n.save),
            ),
          ],
        ),
      ),
    );
  }
}
