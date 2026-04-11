import 'package:flutter/material.dart';
import 'package:phosphor_flutter/phosphor_flutter.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/format/brl_cents.dart';
import '../../../core/format/brl_currency_input_formatter.dart';
import '../../../core/format/parse_brl_input.dart';
import '../../../core/l10n/context_l10n.dart';
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
      loading: () {
        final l10n = context.l10n;
        return Scaffold(
          appBar: AppBar(
            leading: IconButton(
              icon: const Icon(PhosphorIconsRegular.x),
              onPressed: () => context.pop(),
            ),
            title: Text(l10n.editExpenseTitle),
          ),
          body: const Center(child: CircularProgressIndicator()),
        );
      },
      error: (e, _) {
        final l10n = context.l10n;
        return Scaffold(
          appBar: AppBar(
            leading: IconButton(
              icon: const Icon(PhosphorIconsRegular.x),
              onPressed: () => context.pop(),
            ),
            title: Text(l10n.editExpenseTitle),
          ),
          body: Center(
            child: Padding(
              padding: const EdgeInsets.all(24),
              child: Text(messageFromDio(e, l10n) ?? l10n.expenseLoadError),
            ),
          ),
        );
      },
      data: (expense) {
        if (!expense.isMine) {
          final l10n = context.l10n;
          return Scaffold(
            appBar: AppBar(
              leading: IconButton(
                icon: const Icon(PhosphorIconsRegular.x),
                onPressed: () => context.pop(),
              ),
              title: Text(l10n.editExpenseTitle),
            ),
            body: Center(
              child: Padding(
                padding: const EdgeInsets.all(24),
                child: Text(
                  l10n.expenseEditOwnerOnly,
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
    final l10n = context.l10n;
    if (!_formKey.currentState!.validate()) return;
    final cents = parseInputToCents(_amountCtrl.text);
    if (cents == null || cents <= 0) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(l10n.valueInvalid)),
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
          SnackBar(content: Text(l10n.expenseChangesSaved)),
        );
        context.pop();
      }
    } catch (e) {
      if (mounted) {
        messenger.showSnackBar(
          SnackBar(content: Text(messageFromDio(e, l10n) ?? l10n.expenseSaveError)),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final cats = ref.watch(categoriesProvider);

    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
                icon: const Icon(PhosphorIconsRegular.x),
          onPressed: () => context.pop(),
        ),
        title: Text(l10n.editExpenseTitle),
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
                        Icon(PhosphorIconsRegular.stackSimple, color: WellPaidColors.navy),
                        const SizedBox(width: 10),
                        Expanded(
                          child: Text(
                            l10n.expEditInstallmentBanner(
                              widget.expense.installmentNumber,
                              widget.expense.installmentTotal,
                            ),
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
            if (widget.expense.isRecurringAnchor &&
                !widget.expense.isInstallmentPlan)
              Padding(
                padding: const EdgeInsets.only(bottom: 16),
                child: Material(
                  color: WellPaidColors.navy.withValues(alpha: 0.08),
                  borderRadius: BorderRadius.circular(12),
                  child: Padding(
                    padding: const EdgeInsets.all(12),
                    child: Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Icon(
                          PhosphorIconsRegular.repeat,
                          color: WellPaidColors.navy.withValues(alpha: 0.75),
                        ),
                        const SizedBox(width: 10),
                        Expanded(
                          child: Text(
                            l10n.expEditRecurringAnchorBanner,
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
              decoration: InputDecoration(labelText: l10n.expEditDescription),
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
                labelText: l10n.expEditAmount,
                hintText: l10n.expFormAmountHint,
              ),
              keyboardType: const TextInputType.numberWithOptions(decimal: true),
              inputFormatters: kBrCurrencyInputFormatters,
              validator: (v) {
                if (v == null || v.trim().isEmpty) return l10n.requiredField;
                final c = parseInputToCents(v);
                if (c == null || c <= 0) return l10n.valueInvalid;
                return null;
              },
            ),
            const SizedBox(height: 16),
            ListTile(
              contentPadding: EdgeInsets.zero,
              title: Text(l10n.expEditExpenseDate),
              subtitle: Text(_dmY(_expenseDate)),
              trailing: const Icon(PhosphorIconsRegular.calendar),
              onTap: _pickExpenseDate,
            ),
            ListTile(
              contentPadding: EdgeInsets.zero,
              title: Text(l10n.expEditDueOptional),
              subtitle: Text(_dueDate == null ? l10n.noneDash : _dmY(_dueDate!)),
              trailing: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  if (_dueDate != null)
                    IconButton(
                      icon: const Icon(PhosphorIconsRegular.xCircle),
                      onPressed: () => setState(() => _dueDate = null),
                    ),
                  const Icon(PhosphorIconsRegular.calendarBlank),
                ],
              ),
              onTap: _pickDueDate,
            ),
            const SizedBox(height: 8),
            DropdownButtonFormField<String>(
              decoration: InputDecoration(labelText: l10n.expEditState),
              // ignore: deprecated_member_use
              value: _status,
              items: [
                DropdownMenuItem(
                  value: 'pending',
                  child: Text(l10n.expenseStatusPending),
                ),
                DropdownMenuItem(
                  value: 'paid',
                  child: Text(l10n.expenseStatusPaid),
                ),
              ],
              onChanged: (v) {
                if (v != null) setState(() => _status = v);
              },
            ),
            if (!widget.expense.isInstallmentPlan) ...[
              const SizedBox(height: 12),
              DropdownButtonFormField<String?>(
                decoration: InputDecoration(
                  labelText: l10n.expEditRecurrenceMeta,
                ),
                // ignore: deprecated_member_use
                value: _recurring,
                items: [
                  DropdownMenuItem<String?>(
                    value: null,
                    child: Text(l10n.expEditRecurrenceNone),
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
                  Text(messageFromDio(e, l10n) ?? l10n.expFormCategoriesLoadError),
                  TextButton(
                    onPressed: () => ref.invalidate(categoriesProvider),
                    child: Text(l10n.tryAgain),
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
              child: Text(l10n.saveChanges),
            ),
          ],
        ),
      ),
    );
  }
}
