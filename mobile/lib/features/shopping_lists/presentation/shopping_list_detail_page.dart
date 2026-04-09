import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/format/brl_cents.dart';
import '../../../core/format/parse_brl_input.dart';
import '../../../core/l10n/context_l10n.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/theme/well_paid_colors.dart';
import '../../expenses/application/expenses_providers.dart';
import '../../expenses/presentation/widgets/expense_category_dropdown.dart';
import '../../expenses/presentation/widgets/expense_share_form_section.dart';
import '../application/shopping_lists_providers.dart';
import '../domain/shopping_list_models.dart';

class ShoppingListDetailPage extends ConsumerWidget {
  const ShoppingListDetailPage({super.key, required this.listId});

  final String listId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    final async = ref.watch(shoppingListDetailProvider(listId));

    return async.when(
      loading: () => Scaffold(
        appBar: AppBar(title: Text(l10n.shoppingListsTitle)),
        body: const Center(child: CircularProgressIndicator()),
      ),
      error: (e, _) => Scaffold(
        appBar: AppBar(title: Text(l10n.shoppingListsTitle)),
        body: Center(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Text(
              messageFromDio(e, l10n) ?? l10n.shoppingListErrorLoad,
              textAlign: TextAlign.center,
            ),
          ),
        ),
      ),
      data: (detail) => _DetailScaffold(detail: detail),
    );
  }
}

class _DetailScaffold extends ConsumerWidget {
  const _DetailScaffold({required this.detail});

  final ShoppingListDetail detail;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    final title = (detail.title != null && detail.title!.trim().isNotEmpty)
        ? detail.title!.trim()
        : l10n.shoppingListUntitled;
    final canEdit = detail.isDraft && detail.isMine;

    return Scaffold(
      appBar: AppBar(
        title: Text(title),
        actions: [
          if (canEdit) ...[
            IconButton(
              tooltip: l10n.shoppingListEditMetaTitle,
              icon: const Icon(Icons.tune_outlined),
              onPressed: () => _editMeta(context, ref, detail),
            ),
            IconButton(
              tooltip: l10n.delete,
              icon: const Icon(Icons.delete_outline),
              onPressed: () => _confirmDeleteDraft(context, ref, detail),
            ),
          ],
        ],
      ),
      body: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          if (detail.storeName != null && detail.storeName!.trim().isNotEmpty)
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 8, 16, 0),
              child: Text(
                detail.storeName!.trim(),
                style: TextStyle(
                  color: WellPaidColors.navy.withValues(alpha: 0.65),
                  fontSize: 14,
                ),
              ),
            ),
          if (detail.isDraft && detail.sumLineCents > 0)
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
              child: Text(
                l10n.shoppingListSubtotal(formatBrlFromCents(detail.sumLineCents)),
                style: const TextStyle(fontWeight: FontWeight.w600),
              ),
            ),
          if (detail.isCompleted) ...[
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  if (detail.totalCents != null)
                    Text(
                      formatBrlFromCents(detail.totalCents!),
                      style: const TextStyle(
                        fontSize: 22,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                  if (detail.completedAt != null)
                    Text(
                      l10n.shoppingListCompletedOn(_formatCompleted(detail.completedAt!)),
                      style: TextStyle(
                        fontSize: 13,
                        color: WellPaidColors.navy.withValues(alpha: 0.6),
                      ),
                    ),
                  if (detail.expenseId != null) ...[
                    const SizedBox(height: 12),
                    FilledButton.tonalIcon(
                      onPressed: () =>
                          context.push('/expenses/${detail.expenseId}'),
                      icon: const Icon(Icons.receipt_long_outlined),
                      label: Text(l10n.shoppingListViewExpense),
                    ),
                  ],
                ],
              ),
            ),
          ],
          if (!detail.isMine && detail.isDraft)
            Padding(
              padding: const EdgeInsets.all(16),
              child: Text(
                l10n.shoppingListReadOnlyDraft,
                style: TextStyle(color: WellPaidColors.navy.withValues(alpha: 0.7)),
              ),
            ),
          Expanded(
            child: detail.items.isEmpty
                ? Center(child: Text(l10n.shoppingListNoItems))
                : ListView.separated(
                    padding: const EdgeInsets.all(16),
                    itemCount: detail.items.length,
                    separatorBuilder: (context, index) =>
                        const SizedBox(height: 4),
                    itemBuilder: (context, i) {
                      final it = detail.items[i];
                      return _ItemTile(
                        item: it,
                        enabled: canEdit,
                        onTap: canEdit
                            ? () => _editItem(context, ref, detail, it)
                            : null,
                      );
                    },
                  ),
          ),
          if (canEdit) ...[
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 0, 16, 8),
              child: OutlinedButton.icon(
                onPressed: () => _addItem(context, ref, detail),
                icon: const Icon(Icons.add),
                label: Text(l10n.shoppingListAddItem),
              ),
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 0, 16, 24),
              child: FilledButton(
                onPressed: detail.items.isEmpty
                    ? null
                    : () => _openCompleteSheet(context, ref, detail),
                child: Text(l10n.shoppingListComplete),
              ),
            ),
          ],
        ],
      ),
    );
  }

  String _formatCompleted(DateTime d) {
    final local = d.toLocal();
    return '${local.day.toString().padLeft(2, '0')}/${local.month.toString().padLeft(2, '0')}/${local.year}';
  }

  Future<void> _editMeta(
    BuildContext context,
    WidgetRef ref,
    ShoppingListDetail detail,
  ) async {
    final l10n = context.l10n;
    final titleCtrl = TextEditingController(text: detail.title ?? '');
    final storeCtrl = TextEditingController(text: detail.storeName ?? '');
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(l10n.shoppingListEditMetaTitle),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: titleCtrl,
              decoration: InputDecoration(labelText: l10n.shoppingListTitleOptional),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: storeCtrl,
              decoration: InputDecoration(labelText: l10n.shoppingListStoreOptional),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: Text(l10n.cancel),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(ctx, true),
            child: Text(l10n.save),
          ),
        ],
      ),
    );
    if (ok != true || !context.mounted) return;
    try {
      await ref.read(shoppingListsRepositoryProvider).patchList(
            detail.id,
            setTitle: true,
            title: titleCtrl.text,
            setStore: true,
            storeName: storeCtrl.text,
          );
      ref.invalidate(shoppingListDetailProvider(detail.id));
      ref.invalidate(shoppingListsProvider);
    } catch (e) {
      if (!context.mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(messageFromDio(e, l10n) ?? l10n.errorGeneric)),
      );
    } finally {
      titleCtrl.dispose();
      storeCtrl.dispose();
    }
  }

  Future<void> _confirmDeleteDraft(
    BuildContext context,
    WidgetRef ref,
    ShoppingListDetail detail,
  ) async {
    final l10n = context.l10n;
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(l10n.shoppingListConfirmDeleteDraftTitle),
        content: Text(l10n.shoppingListConfirmDeleteDraftBody),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: Text(l10n.cancel),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(ctx, true),
            child: Text(l10n.delete),
          ),
        ],
      ),
    );
    if (ok != true || !context.mounted) return;
    try {
      await ref.read(shoppingListsRepositoryProvider).deleteList(detail.id);
      ref.invalidate(shoppingListsProvider);
      if (context.mounted) context.pop();
    } catch (e) {
      if (!context.mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(messageFromDio(e, l10n) ?? l10n.errorGeneric)),
      );
    }
  }

  Future<void> _addItem(
    BuildContext context,
    WidgetRef ref,
    ShoppingListDetail detail,
  ) async {
    final l10n = context.l10n;
    final labelCtrl = TextEditingController();
    final amountCtrl = TextEditingController();
    final formKey = GlobalKey<FormState>();
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(l10n.shoppingListAddItem),
        content: Form(
          key: formKey,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextFormField(
                controller: labelCtrl,
                decoration: InputDecoration(
                  labelText: l10n.shoppingListItemLabelHint,
                ),
                validator: (v) =>
                    v == null || v.trim().isEmpty ? l10n.requiredField : null,
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: amountCtrl,
                decoration: InputDecoration(
                  labelText: l10n.shoppingListItemAmountOptional,
                ),
                keyboardType: const TextInputType.numberWithOptions(decimal: true),
                inputFormatters: [
                  FilteringTextInputFormatter.allow(RegExp(r'[\d,]')),
                ],
                validator: (v) {
                  if (v == null || v.trim().isEmpty) return null;
                  final c = parseInputToCents(v);
                  if (c == null || c <= 0) return l10n.valueInvalid;
                  return null;
                },
              ),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: Text(l10n.cancel),
          ),
          FilledButton(
            onPressed: () {
              if (formKey.currentState?.validate() != true) return;
              Navigator.pop(ctx, true);
            },
            child: Text(l10n.confirm),
          ),
        ],
      ),
    );
    if (ok != true || !context.mounted) return;
    final label = labelCtrl.text.trim();
    int? cents;
    if (amountCtrl.text.trim().isNotEmpty) {
      cents = parseInputToCents(amountCtrl.text);
    }
    labelCtrl.dispose();
    amountCtrl.dispose();
    try {
      await ref.read(shoppingListsRepositoryProvider).addItem(
            detail.id,
            label: label,
            lineAmountCents: cents,
          );
      ref.invalidate(shoppingListDetailProvider(detail.id));
      ref.invalidate(shoppingListsProvider);
    } catch (e) {
      if (!context.mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(messageFromDio(e, l10n) ?? l10n.errorGeneric)),
      );
    }
  }

  Future<void> _editItem(
    BuildContext context,
    WidgetRef ref,
    ShoppingListDetail detail,
    ShoppingListItemRow item,
  ) async {
    final l10n = context.l10n;
    final labelCtrl = TextEditingController(text: item.label);
    final amountCtrl = TextEditingController(
      text: item.lineAmountCents != null
          ? formatBrlInputFromCents(item.lineAmountCents!)
          : '',
    );
    final formKey = GlobalKey<FormState>();
    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      builder: (ctx) {
        final pad = MediaQuery.viewInsetsOf(ctx).bottom;
        return Padding(
          padding: EdgeInsets.fromLTRB(16, 16, 16, 16 + pad),
          child: Form(
            key: formKey,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Text(
                  l10n.shoppingListEditItemTitle,
                  style: const TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.w700,
                  ),
                ),
                const SizedBox(height: 16),
                TextFormField(
                  controller: labelCtrl,
                  decoration: InputDecoration(
                    labelText: l10n.shoppingListItemLabelHint,
                  ),
                  validator: (v) =>
                      v == null || v.trim().isEmpty ? l10n.requiredField : null,
                ),
                const SizedBox(height: 12),
                TextFormField(
                  controller: amountCtrl,
                  decoration: InputDecoration(
                    labelText: l10n.shoppingListItemAmountOptional,
                  ),
                  keyboardType: const TextInputType.numberWithOptions(decimal: true),
                  inputFormatters: [
                    FilteringTextInputFormatter.allow(RegExp(r'[\d,]')),
                  ],
                  validator: (v) {
                    if (v == null || v.trim().isEmpty) return null;
                    final c = parseInputToCents(v);
                    if (c == null || c <= 0) return l10n.valueInvalid;
                    return null;
                  },
                ),
                const SizedBox(height: 16),
                FilledButton(
                  onPressed: () async {
                    if (formKey.currentState?.validate() != true) return;
                    final label = labelCtrl.text.trim();
                    int? cents;
                    bool clear = false;
                    if (amountCtrl.text.trim().isEmpty) {
                      clear = item.lineAmountCents != null;
                    } else {
                      cents = parseInputToCents(amountCtrl.text);
                    }
                    try {
                      await ref.read(shoppingListsRepositoryProvider).patchItem(
                            detail.id,
                            item.id,
                            label: label,
                            lineAmountCents: cents,
                            clearLineAmount: clear,
                          );
                      ref.invalidate(shoppingListDetailProvider(detail.id));
                      ref.invalidate(shoppingListsProvider);
                      if (ctx.mounted) Navigator.pop(ctx);
                    } catch (e) {
                      if (!ctx.mounted) return;
                      ScaffoldMessenger.of(ctx).showSnackBar(
                        SnackBar(
                          content: Text(
                            messageFromDio(e, l10n) ?? l10n.errorGeneric,
                          ),
                        ),
                      );
                    }
                  },
                  child: Text(l10n.save),
                ),
                TextButton(
                  onPressed: () async {
                    try {
                      await ref
                          .read(shoppingListsRepositoryProvider)
                          .deleteItem(detail.id, item.id);
                      ref.invalidate(shoppingListDetailProvider(detail.id));
                      ref.invalidate(shoppingListsProvider);
                      if (ctx.mounted) Navigator.pop(ctx);
                    } catch (e) {
                      if (!ctx.mounted) return;
                      ScaffoldMessenger.of(ctx).showSnackBar(
                        SnackBar(
                          content: Text(
                            messageFromDio(e, l10n) ?? l10n.errorGeneric,
                          ),
                        ),
                      );
                    }
                  },
                  child: Text(l10n.shoppingListDeleteItem),
                ),
              ],
            ),
          ),
        );
      },
    );
    labelCtrl.dispose();
    amountCtrl.dispose();
  }

  Future<void> _openCompleteSheet(
    BuildContext context,
    WidgetRef ref,
    ShoppingListDetail detail,
  ) async {
    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      builder: (ctx) => _CompleteShoppingSheet(
        listId: detail.id,
        sumLineCents: detail.sumLineCents,
      ),
    );
    ref.invalidate(shoppingListDetailProvider(detail.id));
    ref.invalidate(shoppingListsProvider);
  }
}

class _ItemTile extends StatelessWidget {
  const _ItemTile({
    required this.item,
    required this.enabled,
    this.onTap,
  });

  final ShoppingListItemRow item;
  final bool enabled;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final sub = item.lineAmountCents != null
        ? formatBrlFromCents(item.lineAmountCents!)
        : l10n.shoppingListNoPriceYet;
    return Material(
      color: WellPaidColors.creamMuted.withValues(alpha: 0.35),
      borderRadius: BorderRadius.circular(12),
      child: InkWell(
        borderRadius: BorderRadius.circular(12),
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
          child: Row(
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(item.label, style: const TextStyle(fontWeight: FontWeight.w600)),
                    Text(
                      sub,
                      style: TextStyle(
                        fontSize: 13,
                        color: WellPaidColors.navy.withValues(alpha: 0.65),
                      ),
                    ),
                  ],
                ),
              ),
              if (enabled)
                Icon(Icons.chevron_right, color: WellPaidColors.navy.withValues(alpha: 0.35)),
            ],
          ),
        ),
      ),
    );
  }
}

class _CompleteShoppingSheet extends ConsumerStatefulWidget {
  const _CompleteShoppingSheet({
    required this.listId,
    required this.sumLineCents,
  });

  final String listId;
  final int sumLineCents;

  @override
  ConsumerState<_CompleteShoppingSheet> createState() =>
      _CompleteShoppingSheetState();
}

class _CompleteShoppingSheetState extends ConsumerState<_CompleteShoppingSheet> {
  final _formKey = GlobalKey<FormState>();
  final _totalOverrideCtrl = TextEditingController();
  final _descCtrl = TextEditingController();
  DateTime _expenseDate = DateTime.now();
  bool _markPaid = false;
  String? _categoryId;
  bool _isShared = false;
  String? _sharedWithUserId;

  @override
  void dispose() {
    _totalOverrideCtrl.dispose();
    _descCtrl.dispose();
    super.dispose();
  }

  String _dmY(DateTime d) =>
      '${d.day.toString().padLeft(2, '0')}/${d.month.toString().padLeft(2, '0')}/${d.year}';

  Future<void> _pickDate() async {
    final d = await showDatePicker(
      context: context,
      initialDate: _expenseDate,
      firstDate: DateTime(2000),
      lastDate: DateTime(2100),
    );
    if (d != null) setState(() => _expenseDate = d);
  }

  Future<void> _submit() async {
    final l10n = context.l10n;
    if (!_formKey.currentState!.validate()) return;
    if (_categoryId == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(l10n.expFormPickCategory)),
      );
      return;
    }
    int? totalOverride;
    if (_totalOverrideCtrl.text.trim().isNotEmpty) {
      totalOverride = parseInputToCents(_totalOverrideCtrl.text);
      if (totalOverride == null || totalOverride <= 0) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(l10n.valueInvalid)),
        );
        return;
      }
    }
    final desc = _descCtrl.text.trim();
    try {
      await ref.read(shoppingListsRepositoryProvider).completeList(
            widget.listId,
            categoryId: _categoryId!,
            expenseDate: _expenseDate,
            markPaid: _markPaid,
            description: desc.isEmpty ? null : desc,
            totalCents: totalOverride,
            isShared: _isShared,
            sharedWithUserId: _sharedWithUserId,
          );
      ref.invalidate(expensesListProvider);
      ref.invalidate(shoppingListDetailProvider(widget.listId));
      ref.invalidate(shoppingListsProvider);
      if (mounted) {
        Navigator.pop(context);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(l10n.shoppingListCompleteSuccess)),
        );
      }
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(messageFromDio(e, l10n) ?? l10n.errorGeneric)),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final cats = ref.watch(categoriesProvider);
    final pad = MediaQuery.viewInsetsOf(context).bottom;

    return Padding(
      padding: EdgeInsets.fromLTRB(16, 16, 16, 16 + pad),
      child: Form(
        key: _formKey,
        child: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                l10n.shoppingListCompleteTitle,
                style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w700),
              ),
              if (widget.sumLineCents > 0) ...[
                const SizedBox(height: 8),
                Text(
                  l10n.shoppingListSubtotal(formatBrlFromCents(widget.sumLineCents)),
                  style: TextStyle(
                    color: WellPaidColors.navy.withValues(alpha: 0.7),
                  ),
                ),
              ],
              const SizedBox(height: 16),
              cats.when(
                loading: () => const LinearProgressIndicator(),
                error: (_, _) => Text(l10n.errorGeneric),
                data: (list) => ExpenseCategoryDropdown(
                  categories: list,
                  value: _categoryId,
                  onChanged: (v) => setState(() => _categoryId = v),
                ),
              ),
              const SizedBox(height: 12),
              ListTile(
                contentPadding: EdgeInsets.zero,
                title: Text(l10n.shoppingListExpenseDate),
                subtitle: Text(_dmY(_expenseDate)),
                trailing: const Icon(Icons.calendar_today_outlined),
                onTap: _pickDate,
              ),
              SwitchListTile(
                contentPadding: EdgeInsets.zero,
                title: Text(l10n.shoppingListMarkPaid),
                value: _markPaid,
                onChanged: (v) => setState(() => _markPaid = v),
              ),
              TextFormField(
                controller: _totalOverrideCtrl,
                decoration: InputDecoration(
                  labelText: l10n.shoppingListTotalOverrideHint,
                ),
                keyboardType: const TextInputType.numberWithOptions(decimal: true),
                inputFormatters: [
                  FilteringTextInputFormatter.allow(RegExp(r'[\d,]')),
                ],
                validator: (v) {
                  if (v == null || v.trim().isEmpty) return null;
                  final c = parseInputToCents(v);
                  if (c == null || c <= 0) return l10n.valueInvalid;
                  return null;
                },
              ),
              const SizedBox(height: 8),
              TextFormField(
                controller: _descCtrl,
                decoration: InputDecoration(
                  labelText: l10n.shoppingListDescriptionOptional,
                ),
                maxLines: 2,
              ),
              const SizedBox(height: 8),
              ExpenseShareFormSection(
                isShared: _isShared,
                sharedWithUserId: _sharedWithUserId,
                onSharedChanged: (v) => setState(() => _isShared = v),
                onPeerChanged: (v) => setState(() => _sharedWithUserId = v),
              ),
              const SizedBox(height: 16),
              FilledButton(
                onPressed: _submit,
                child: Text(l10n.shoppingListComplete),
              ),
              const SizedBox(height: 8),
            ],
          ),
        ),
      ),
    );
  }
}
