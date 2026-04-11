import 'dart:async' show unawaited;

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

void _scheduleShoppingListRefresh(WidgetRef ref, String listId) {
  WidgetsBinding.instance.addPostFrameCallback((_) {
    ref.invalidate(shoppingListDetailProvider(listId));
    ref.invalidate(shoppingListsProvider);
  });
}

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
                    padding: const EdgeInsets.fromLTRB(16, 8, 16, 8),
                    itemCount: detail.items.length,
                    separatorBuilder: (context, index) =>
                        const SizedBox(height: 8),
                    itemBuilder: (context, i) {
                      final it = detail.items[i];
                      if (canEdit) {
                        return _DraftItemLine(
                          listId: detail.id,
                          item: it,
                          onEditLabel: () =>
                              _editItemLabel(context, ref, detail, it),
                          onRequestRemove: () =>
                              _confirmRemoveItem(context, ref, detail, it),
                        );
                      }
                      return _ReadOnlyItemLine(item: it);
                    },
                  ),
          ),
          if (canEdit && detail.isDraft)
            _DraftListFooter(
              sumLineCents: detail.sumLineCents,
              hasItems: detail.items.isNotEmpty,
              onAdd: () => _addItem(context, ref, detail),
              onComplete: () => _openCompleteSheet(context, ref, detail),
            ),
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
    final result = await showDialog<_MetaDialogResult>(
      context: context,
      builder: (ctx) => _ShoppingListEditMetaDialog(
        initialTitle: detail.title ?? '',
        initialStore: detail.storeName ?? '',
      ),
    );
    if (result == null || !context.mounted) return;
    try {
      await ref.read(shoppingListsRepositoryProvider).patchList(
            detail.id,
            setTitle: true,
            title: result.title,
            setStore: true,
            storeName: result.storeName,
          );
      _scheduleShoppingListRefresh(ref, detail.id);
    } catch (e) {
      if (!context.mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(messageFromDio(e, l10n) ?? l10n.errorGeneric)),
      );
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
    final result = await showDialog<_AddItemDialogResult>(
      context: context,
      builder: (ctx) => const _ShoppingListAddItemDialog(),
    );
    if (result == null || !context.mounted) return;
    try {
      await ref.read(shoppingListsRepositoryProvider).addItem(
            detail.id,
            label: result.label,
            lineAmountCents: result.lineAmountCents,
          );
      _scheduleShoppingListRefresh(ref, detail.id);
    } catch (e) {
      if (!context.mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(messageFromDio(e, l10n) ?? l10n.errorGeneric)),
      );
    }
  }

  Future<void> _editItemLabel(
    BuildContext context,
    WidgetRef ref,
    ShoppingListDetail detail,
    ShoppingListItemRow item,
  ) async {
    final l10n = context.l10n;
    final newLabel = await showDialog<String>(
      context: context,
      builder: (ctx) => _EditItemLabelDialog(initialLabel: item.label),
    );
    if (newLabel == null || !context.mounted) return;
    final trimmed = newLabel.trim();
    if (trimmed.isEmpty || trimmed == item.label) return;
    try {
      await ref.read(shoppingListsRepositoryProvider).patchItem(
            detail.id,
            item.id,
            label: trimmed,
          );
      _scheduleShoppingListRefresh(ref, detail.id);
    } catch (e) {
      if (!context.mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(messageFromDio(e, l10n) ?? l10n.errorGeneric)),
      );
    }
  }

  Future<void> _confirmRemoveItem(
    BuildContext context,
    WidgetRef ref,
    ShoppingListDetail detail,
    ShoppingListItemRow item,
  ) async {
    final l10n = context.l10n;
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(l10n.shoppingListConfirmRemoveItemTitle),
        content: Text(l10n.shoppingListConfirmRemoveItemBody),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: Text(l10n.cancel),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(ctx, true),
            child: Text(l10n.shoppingListDeleteItem),
          ),
        ],
      ),
    );
    if (ok != true || !context.mounted) return;
    try {
      await ref.read(shoppingListsRepositoryProvider).deleteItem(detail.id, item.id);
      _scheduleShoppingListRefresh(ref, detail.id);
    } catch (e) {
      if (!context.mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(messageFromDio(e, l10n) ?? l10n.errorGeneric)),
      );
    }
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
    _scheduleShoppingListRefresh(ref, detail.id);
  }
}

class _MetaDialogResult {
  const _MetaDialogResult({required this.title, required this.storeName});

  final String title;
  final String storeName;
}

class _ShoppingListEditMetaDialog extends StatefulWidget {
  const _ShoppingListEditMetaDialog({
    required this.initialTitle,
    required this.initialStore,
  });

  final String initialTitle;
  final String initialStore;

  @override
  State<_ShoppingListEditMetaDialog> createState() =>
      _ShoppingListEditMetaDialogState();
}

class _ShoppingListEditMetaDialogState extends State<_ShoppingListEditMetaDialog> {
  late final TextEditingController _titleCtrl;
  late final TextEditingController _storeCtrl;

  @override
  void initState() {
    super.initState();
    _titleCtrl = TextEditingController(text: widget.initialTitle);
    _storeCtrl = TextEditingController(text: widget.initialStore);
  }

  @override
  void dispose() {
    _titleCtrl.dispose();
    _storeCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    return AlertDialog(
      title: Text(l10n.shoppingListEditMetaTitle),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          TextField(
            controller: _titleCtrl,
            decoration: InputDecoration(labelText: l10n.shoppingListTitleOptional),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _storeCtrl,
            decoration: InputDecoration(labelText: l10n.shoppingListStoreOptional),
          ),
        ],
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context),
          child: Text(l10n.cancel),
        ),
        FilledButton(
          onPressed: () => Navigator.pop(
            context,
            _MetaDialogResult(
              title: _titleCtrl.text,
              storeName: _storeCtrl.text,
            ),
          ),
          child: Text(l10n.save),
        ),
      ],
    );
  }
}

class _EditItemLabelDialog extends StatefulWidget {
  const _EditItemLabelDialog({required this.initialLabel});

  final String initialLabel;

  @override
  State<_EditItemLabelDialog> createState() => _EditItemLabelDialogState();
}

class _EditItemLabelDialogState extends State<_EditItemLabelDialog> {
  late final TextEditingController _ctrl;

  @override
  void initState() {
    super.initState();
    _ctrl = TextEditingController(text: widget.initialLabel);
  }

  @override
  void dispose() {
    _ctrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    return AlertDialog(
      title: Text(l10n.shoppingListEditLabelTitle),
      content: TextField(
        controller: _ctrl,
        autofocus: true,
        decoration: InputDecoration(labelText: l10n.shoppingListItemLabelHint),
        onSubmitted: (_) {
          if (_ctrl.text.trim().isNotEmpty) {
            Navigator.pop(context, _ctrl.text.trim());
          }
        },
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context),
          child: Text(l10n.cancel),
        ),
        FilledButton(
          onPressed: () {
            if (_ctrl.text.trim().isEmpty) return;
            Navigator.pop(context, _ctrl.text.trim());
          },
          child: Text(l10n.save),
        ),
      ],
    );
  }
}

class _AddItemDialogResult {
  const _AddItemDialogResult({
    required this.label,
    this.lineAmountCents,
  });

  final String label;
  final int? lineAmountCents;
}

class _ShoppingListAddItemDialog extends StatefulWidget {
  const _ShoppingListAddItemDialog();

  @override
  State<_ShoppingListAddItemDialog> createState() =>
      _ShoppingListAddItemDialogState();
}

class _ShoppingListAddItemDialogState extends State<_ShoppingListAddItemDialog> {
  final _formKey = GlobalKey<FormState>();
  final _labelCtrl = TextEditingController();
  final _amountCtrl = TextEditingController();

  @override
  void dispose() {
    _labelCtrl.dispose();
    _amountCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    return AlertDialog(
      title: Text(l10n.shoppingListAddItem),
      content: Form(
        key: _formKey,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextFormField(
              controller: _labelCtrl,
              decoration: InputDecoration(
                labelText: l10n.shoppingListItemLabelHint,
              ),
              validator: (v) =>
                  v == null || v.trim().isEmpty ? l10n.requiredField : null,
            ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _amountCtrl,
              decoration: InputDecoration(
                labelText: l10n.shoppingListItemAmountOptional,
              ),
              keyboardType:
                  const TextInputType.numberWithOptions(decimal: true),
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
          onPressed: () => Navigator.pop(context),
          child: Text(l10n.cancel),
        ),
        FilledButton(
          onPressed: () {
            if (_formKey.currentState?.validate() != true) return;
            int? cents;
            if (_amountCtrl.text.trim().isNotEmpty) {
              cents = parseInputToCents(_amountCtrl.text);
            }
            Navigator.pop(
              context,
              _AddItemDialogResult(
                label: _labelCtrl.text.trim(),
                lineAmountCents: cents,
              ),
            );
          },
          child: Text(l10n.confirm),
        ),
      ],
    );
  }
}

class _ReadOnlyItemLine extends StatelessWidget {
  const _ReadOnlyItemLine({required this.item});

  final ShoppingListItemRow item;

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final sub = item.lineAmountCents != null
        ? formatBrlFromCents(item.lineAmountCents!)
        : l10n.shoppingListNoPriceYet;
    return Material(
      color: WellPaidColors.creamMuted.withValues(alpha: 0.35),
      borderRadius: BorderRadius.circular(12),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
        child: Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    item.label,
                    style: const TextStyle(fontWeight: FontWeight.w600),
                  ),
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
          ],
        ),
      ),
    );
  }
}

class _DraftItemLine extends ConsumerStatefulWidget {
  const _DraftItemLine({
    required this.listId,
    required this.item,
    required this.onEditLabel,
    required this.onRequestRemove,
  });

  final String listId;
  final ShoppingListItemRow item;
  final VoidCallback onEditLabel;
  final VoidCallback onRequestRemove;

  @override
  ConsumerState<_DraftItemLine> createState() => _DraftItemLineState();
}

class _DraftItemLineState extends ConsumerState<_DraftItemLine> {
  late final TextEditingController _amountCtrl;
  final FocusNode _amountFocus = FocusNode();

  @override
  void initState() {
    super.initState();
    _amountCtrl = TextEditingController();
    _syncAmountFromItem();
    _amountFocus.addListener(_onAmountFocusChange);
  }

  @override
  void didUpdateWidget(covariant _DraftItemLine oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.item.lineAmountCents != widget.item.lineAmountCents &&
        !_amountFocus.hasFocus) {
      _syncAmountFromItem();
    }
  }

  void _syncAmountFromItem() {
    _amountCtrl.text = widget.item.lineAmountCents != null
        ? formatBrlInputFromCents(widget.item.lineAmountCents!)
        : '';
  }

  void _onAmountFocusChange() {
    if (!_amountFocus.hasFocus) {
      unawaited(_commitAmountIfChanged());
    }
  }

  Future<void> _commitAmountIfChanged() async {
    final l10n = context.l10n;
    final trimmed = _amountCtrl.text.trim();
    final current = widget.item.lineAmountCents;
    int? newCents;
    var clear = false;
    if (trimmed.isEmpty) {
      if (current == null) return;
      clear = true;
    } else {
      newCents = parseInputToCents(trimmed);
      if (newCents == null || newCents <= 0) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text(l10n.valueInvalid)),
          );
        }
        _syncAmountFromItem();
        return;
      }
      if (newCents == current) return;
    }
    try {
      await ref.read(shoppingListsRepositoryProvider).patchItem(
            widget.listId,
            widget.item.id,
            lineAmountCents: newCents,
            clearLineAmount: clear,
          );
      if (!mounted) return;
      _scheduleShoppingListRefresh(ref, widget.listId);
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(messageFromDio(e, l10n) ?? l10n.errorGeneric)),
      );
      _syncAmountFromItem();
    }
  }

  @override
  void dispose() {
    _amountFocus.removeListener(_onAmountFocusChange);
    _amountFocus.dispose();
    _amountCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    return Material(
      color: WellPaidColors.creamMuted.withValues(alpha: 0.35),
      borderRadius: BorderRadius.circular(12),
      child: Padding(
        padding: const EdgeInsets.fromLTRB(10, 8, 4, 8),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            Expanded(
              child: InkWell(
                borderRadius: BorderRadius.circular(8),
                onTap: widget.onEditLabel,
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 4),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        widget.item.label,
                        style: const TextStyle(fontWeight: FontWeight.w600),
                      ),
                      if (widget.item.lineAmountCents == null)
                        Text(
                          l10n.shoppingListNoPriceYet,
                          style: TextStyle(
                            fontSize: 12,
                            color: WellPaidColors.navy.withValues(alpha: 0.55),
                          ),
                        ),
                    ],
                  ),
                ),
              ),
            ),
            SizedBox(
              width: 100,
              child: TextField(
                controller: _amountCtrl,
                focusNode: _amountFocus,
                decoration: InputDecoration(
                  isDense: true,
                  contentPadding:
                      const EdgeInsets.symmetric(horizontal: 8, vertical: 10),
                  hintText: l10n.shoppingListInlineAmountHint,
                  border: const OutlineInputBorder(),
                ),
                style: const TextStyle(fontSize: 14),
                keyboardType:
                    const TextInputType.numberWithOptions(decimal: true),
                inputFormatters: [
                  FilteringTextInputFormatter.allow(RegExp(r'[\d,]')),
                ],
                textInputAction: TextInputAction.done,
                onEditingComplete: () {
                  _amountFocus.unfocus();
                },
              ),
            ),
            PopupMenuButton<String>(
              icon: Icon(
                Icons.more_vert,
                color: WellPaidColors.navy.withValues(alpha: 0.45),
              ),
              onSelected: (v) {
                if (v == 'label') widget.onEditLabel();
                if (v == 'remove') widget.onRequestRemove();
              },
              itemBuilder: (ctx) => [
                PopupMenuItem(
                  value: 'label',
                  child: Text(l10n.shoppingListEditLabelTitle),
                ),
                PopupMenuItem(
                  value: 'remove',
                  child: Text(l10n.shoppingListDeleteItem),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _DraftListFooter extends StatelessWidget {
  const _DraftListFooter({
    required this.sumLineCents,
    required this.hasItems,
    required this.onAdd,
    required this.onComplete,
  });

  final int sumLineCents;
  final bool hasItems;
  final VoidCallback onAdd;
  final VoidCallback onComplete;

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    return Material(
      elevation: 8,
      shadowColor: Colors.black26,
      color: Theme.of(context).colorScheme.surface,
      child: SafeArea(
        top: false,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(16, 10, 16, 12),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            mainAxisSize: MainAxisSize.min,
            children: [
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          l10n.shoppingListFooterEstimatedTotal,
                          style: Theme.of(context).textTheme.titleSmall?.copyWith(
                                fontWeight: FontWeight.w700,
                                color: WellPaidColors.navy,
                              ),
                        ),
                        const SizedBox(height: 2),
                        Text(
                          l10n.shoppingListFooterEstimatedNote,
                          style: Theme.of(context).textTheme.bodySmall?.copyWith(
                                color: WellPaidColors.navy.withValues(alpha: 0.6),
                              ),
                        ),
                      ],
                    ),
                  ),
                  Text(
                    formatBrlFromCents(sumLineCents),
                    style: Theme.of(context).textTheme.titleMedium?.copyWith(
                          fontWeight: FontWeight.w800,
                          color: WellPaidColors.navy,
                        ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              Row(
                children: [
                  Expanded(
                    child: OutlinedButton.icon(
                      onPressed: onAdd,
                      icon: const Icon(Icons.add, size: 20),
                      label: Text(l10n.shoppingListAddItem),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: FilledButton(
                      onPressed: hasItems ? onComplete : null,
                      child: Text(l10n.shoppingListComplete),
                    ),
                  ),
                ],
              ),
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
      if (!mounted) return;
      final messenger = ScaffoldMessenger.of(context);
      Navigator.pop(context);
      WidgetsBinding.instance.addPostFrameCallback((_) {
        ref.invalidate(expensesListProvider);
        ref.invalidate(shoppingListDetailProvider(widget.listId));
        ref.invalidate(shoppingListsProvider);
      });
      messenger.showSnackBar(
        SnackBar(content: Text(l10n.shoppingListCompleteSuccess)),
      );
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
