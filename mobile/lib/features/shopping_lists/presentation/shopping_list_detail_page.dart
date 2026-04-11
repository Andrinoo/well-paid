import 'dart:async' show unawaited;

import 'package:flutter/material.dart';
import 'package:phosphor_flutter/phosphor_flutter.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/format/brl_cents.dart';
import '../../../core/format/brl_currency_input_formatter.dart';
import '../../../core/format/parse_brl_input.dart';
import '../../../core/l10n/context_l10n.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/theme/well_paid_colors.dart';
import '../../expenses/application/expenses_providers.dart';
import '../../expenses/presentation/widgets/expense_category_dropdown.dart';
import '../../expenses/presentation/widgets/expense_share_form_section.dart';
import '../application/shopping_lists_providers.dart';
import '../domain/shopping_list_models.dart';
import 'shopping_list_text_utils.dart';

void _applyListDetailAndRefreshIndex(WidgetRef ref, ShoppingListDetail detail) {
  ref.read(shoppingListDetailProvider(detail.id).notifier).applyDetail(detail);
  ref.invalidate(shoppingListsProvider);
}

class ShoppingListDetailPage extends ConsumerStatefulWidget {
  const ShoppingListDetailPage({super.key, required this.listId});

  final String listId;

  @override
  ConsumerState<ShoppingListDetailPage> createState() =>
      _ShoppingListDetailPageState();
}

class _ShoppingListDetailPageState extends ConsumerState<ShoppingListDetailPage> {
  @override
  void dispose() {
    unawaited(
      ref
          .read(shoppingListDetailProvider(widget.listId).notifier)
          .flushDraftPatchesToServer(),
    );
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final async = ref.watch(shoppingListDetailProvider(widget.listId));

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
    final canEditDraft = detail.isDraft && detail.isMine;
    final canEditCompleted = detail.isCompleted && detail.isMine;

    return Scaffold(
      appBar: AppBar(
        title: Text(title),
        actions: [
          if (canEditDraft || canEditCompleted)
            IconButton(
              tooltip: l10n.shoppingListEditMetaTitle,
              icon: const Icon(PhosphorIconsRegular.slidersHorizontal),
              onPressed: () => _editMeta(context, ref, detail),
            ),
          if (canEditDraft)
            IconButton(
              tooltip: l10n.delete,
              icon: const Icon(PhosphorIconsRegular.trash),
              onPressed: () => _confirmDeleteDraft(context, ref, detail),
            ),
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
                      icon: const Icon(PhosphorIconsRegular.receipt),
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
          if (canEditCompleted)
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 8, 16, 0),
              child: Text(
                l10n.shoppingListCompletedEditHint,
                style: TextStyle(
                  fontSize: 13,
                  color: WellPaidColors.navy.withValues(alpha: 0.72),
                ),
              ),
            ),
          Expanded(
            child: detail.items.isEmpty
                ? Center(child: Text(l10n.shoppingListNoItems))
                : GestureDetector(
                    behavior: HitTestBehavior.deferToChild,
                    onTap: () => FocusManager.instance.primaryFocus?.unfocus(),
                    child: ListView.separated(
                    padding: const EdgeInsets.fromLTRB(16, 8, 16, 8),
                    itemCount: detail.items.length,
                    separatorBuilder: (context, index) =>
                        const SizedBox(height: 8),
                    itemBuilder: (context, i) {
                      final it = detail.items[i];
                      if (canEditDraft || canEditCompleted) {
                        return _DraftItemLine(
                          listId: detail.id,
                          item: it,
                          useLocalDraftEdits: canEditDraft,
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
          ),
          if (canEditDraft && detail.isDraft)
            _DraftListFooter(
              sumLineCents: detail.sumLineCents,
              hasItems: detail.items.isNotEmpty,
              onAdd: () => _addItem(context, ref, detail),
              onComplete: () => _openCompleteSheet(context, ref, detail),
            ),
          if (canEditCompleted)
            _CompletedOwnerFooter(
              onAdd: () => _addItem(context, ref, detail),
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
      final updated = await ref.read(shoppingListsRepositoryProvider).patchList(
            detail.id,
            setTitle: true,
            title: result.title,
            setStore: true,
            storeName: result.storeName,
          );
      _applyListDetailAndRefreshIndex(ref, updated);
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
    final seeds = <String>{...kShoppingListLabelSeeds};
    for (final it in detail.items) {
      if (it.label.trim().isNotEmpty) seeds.add(it.label.trim());
    }
    final sorted = seeds.toList()..sort((a, b) => a.toLowerCase().compareTo(b.toLowerCase()));
    final result = await showDialog<_AddItemDialogResult>(
      context: context,
      builder: (ctx) => _ShoppingListAddItemDialog(suggestions: sorted),
    );
    if (result == null || !context.mounted) return;
    try {
      final updated = await ref.read(shoppingListsRepositoryProvider).addItem(
            detail.id,
            label: result.label,
            quantity: result.quantity,
            lineAmountCents: result.lineAmountCents,
          );
      _applyListDetailAndRefreshIndex(ref, updated);
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
    final trimmed = shoppingListFormatSentenceCase(newLabel.trim());
    if (trimmed.isEmpty || trimmed == item.label) return;
    try {
      final updated = await ref.read(shoppingListsRepositoryProvider).patchItem(
            detail.id,
            item.id,
            label: trimmed,
          );
      _applyListDetailAndRefreshIndex(ref, updated);
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
      final updated =
          await ref.read(shoppingListsRepositoryProvider).deleteItem(detail.id, item.id);
      _applyListDetailAndRefreshIndex(ref, updated);
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
      builder: (ctx) => _CompleteShoppingSheet(listId: detail.id),
    );
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
            textCapitalization: TextCapitalization.sentences,
            decoration: InputDecoration(labelText: l10n.shoppingListTitleOptional),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _storeCtrl,
            textCapitalization: TextCapitalization.sentences,
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
              title: shoppingListFormatSentenceCase(_titleCtrl.text.trim()),
              storeName: shoppingListFormatSentenceCase(_storeCtrl.text.trim()),
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
        textCapitalization: TextCapitalization.sentences,
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
    required this.quantity,
    this.lineAmountCents,
  });

  final String label;
  final int quantity;
  final int? lineAmountCents;
}

class _ShoppingListAddItemDialog extends StatefulWidget {
  const _ShoppingListAddItemDialog({required this.suggestions});

  final List<String> suggestions;

  @override
  State<_ShoppingListAddItemDialog> createState() =>
      _ShoppingListAddItemDialogState();
}

class _ShoppingListAddItemDialogState extends State<_ShoppingListAddItemDialog> {
  final _formKey = GlobalKey<FormState>();
  final _amountCtrl = TextEditingController();
  final _qtyCtrl = TextEditingController(text: '1');
  /// Preenchido pelo [Autocomplete.fieldViewBuilder]; não fazer dispose (pertence ao Autocomplete).
  TextEditingController? _labelFieldController;

  @override
  void dispose() {
    _amountCtrl.dispose();
    _qtyCtrl.dispose();
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
            Autocomplete<String>(
              displayStringForOption: (s) => s,
              optionsBuilder: (value) {
                final q = value.text.trim().toLowerCase();
                if (q.isEmpty) return const Iterable<String>.empty();
                return widget.suggestions
                    .where((s) => s.toLowerCase().startsWith(q))
                    .take(12);
              },
              fieldViewBuilder: (context, textController, focusNode, onSubmit) {
                _labelFieldController = textController;
                return TextFormField(
                  controller: textController,
                  focusNode: focusNode,
                  textCapitalization: TextCapitalization.sentences,
                  decoration: InputDecoration(
                    labelText: l10n.shoppingListItemLabelHint,
                  ),
                  validator: (v) =>
                      v == null || v.trim().isEmpty ? l10n.requiredField : null,
                  onFieldSubmitted: (_) => onSubmit(),
                );
              },
              onSelected: (s) {},
            ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _qtyCtrl,
              decoration: InputDecoration(
                labelText: l10n.shoppingListItemQuantity,
              ),
              keyboardType: TextInputType.number,
              inputFormatters: [FilteringTextInputFormatter.digitsOnly],
              validator: (v) {
                if (v == null || v.trim().isEmpty) return l10n.requiredField;
                final n = int.tryParse(v.trim());
                if (n == null || n < 1) return l10n.valueInvalid;
                return null;
              },
            ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _amountCtrl,
              decoration: InputDecoration(
                labelText: l10n.shoppingListItemAmountOptional,
              ),
              keyboardType:
                  const TextInputType.numberWithOptions(decimal: true),
              inputFormatters: kBrCurrencyInputFormatters,
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
            final labelText = _labelFieldController?.text.trim() ?? '';
            if (labelText.isEmpty) return;
            int? cents;
            if (_amountCtrl.text.trim().isNotEmpty) {
              cents = parseInputToCents(_amountCtrl.text);
            }
            final qty = int.parse(_qtyCtrl.text.trim());
            Navigator.pop(
              context,
              _AddItemDialogResult(
                label: shoppingListFormatSentenceCase(labelText.trim()),
                quantity: qty,
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
    final unit = item.lineAmountCents;
    final lineTotal = item.lineTotalCents;
    final String sub;
    if (unit == null) {
      sub = l10n.shoppingListNoPriceYet;
    } else if (item.quantity > 1 && lineTotal != null) {
      sub =
          '${formatBrlFromCents(unit)} × ${item.quantity} = ${formatBrlFromCents(lineTotal)}';
    } else {
      sub = formatBrlFromCents(unit);
    }
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
                    item.quantity > 1
                        ? '${item.label}  ×${item.quantity}'
                        : item.label,
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
    required this.useLocalDraftEdits,
    required this.onEditLabel,
    required this.onRequestRemove,
  });

  final String listId;
  final ShoppingListItemRow item;
  /// Rascunho: quantidade/valor só em memória até flush (fechar compra / sair).
  final bool useLocalDraftEdits;
  final VoidCallback onEditLabel;
  final VoidCallback onRequestRemove;

  @override
  ConsumerState<_DraftItemLine> createState() => _DraftItemLineState();
}

class _DraftItemLineState extends ConsumerState<_DraftItemLine> {
  late final TextEditingController _amountCtrl;
  late final TextEditingController _qtyCtrl;
  final FocusNode _amountFocus = FocusNode();
  final FocusNode _qtyFocus = FocusNode();

  @override
  void initState() {
    super.initState();
    _amountCtrl = TextEditingController();
    _qtyCtrl = TextEditingController(text: '${widget.item.quantity}');
    _syncAmountFromItem();
    _amountFocus.addListener(_onAmountFocusChange);
    _qtyFocus.addListener(_onQtyFocusChange);
  }

  @override
  void didUpdateWidget(covariant _DraftItemLine oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.item.lineAmountCents != widget.item.lineAmountCents &&
        !_amountFocus.hasFocus) {
      _syncAmountFromItem();
    }
    if (oldWidget.item.quantity != widget.item.quantity && !_qtyFocus.hasFocus) {
      _qtyCtrl.text = '${widget.item.quantity}';
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

  void _onQtyFocusChange() {
    if (!_qtyFocus.hasFocus) {
      unawaited(_commitQuantityIfChanged());
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
    if (widget.useLocalDraftEdits) {
      ref.read(shoppingListDetailProvider(widget.listId).notifier).applyLocalItemLineDraft(
            itemId: widget.item.id,
            lineAmountCents: clear ? null : newCents,
            clearLineAmount: clear,
          );
      return;
    }
    try {
      final updated = await ref.read(shoppingListsRepositoryProvider).patchItem(
            widget.listId,
            widget.item.id,
            lineAmountCents: newCents,
            clearLineAmount: clear,
          );
      if (!mounted) return;
      _applyListDetailAndRefreshIndex(ref, updated);
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(messageFromDio(e, l10n) ?? l10n.errorGeneric)),
      );
      _syncAmountFromItem();
    }
  }

  Future<void> _commitQuantityIfChanged() async {
    final l10n = context.l10n;
    final trimmed = _qtyCtrl.text.trim();
    final n = int.tryParse(trimmed);
    if (n == null || n < 1) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(l10n.valueInvalid)),
        );
      }
      _qtyCtrl.text = '${widget.item.quantity}';
      return;
    }
    if (n == widget.item.quantity) return;
    if (widget.useLocalDraftEdits) {
      ref.read(shoppingListDetailProvider(widget.listId).notifier).applyLocalItemLineDraft(
            itemId: widget.item.id,
            quantity: n,
          );
      return;
    }
    try {
      final updated = await ref.read(shoppingListsRepositoryProvider).patchItem(
            widget.listId,
            widget.item.id,
            quantity: n,
          );
      if (!mounted) return;
      _applyListDetailAndRefreshIndex(ref, updated);
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(messageFromDio(e, l10n) ?? l10n.errorGeneric)),
      );
      _qtyCtrl.text = '${widget.item.quantity}';
    }
  }

  @override
  void dispose() {
    _amountFocus.removeListener(_onAmountFocusChange);
    _qtyFocus.removeListener(_onQtyFocusChange);
    _amountFocus.dispose();
    _qtyFocus.dispose();
    _amountCtrl.dispose();
    _qtyCtrl.dispose();
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
                        widget.item.quantity > 1
                            ? '${widget.item.label}  ×${widget.item.quantity}'
                            : widget.item.label,
                        style: const TextStyle(fontWeight: FontWeight.w600),
                      ),
                      if (widget.item.lineAmountCents == null)
                        Text(
                          l10n.shoppingListNoPriceYet,
                          style: TextStyle(
                            fontSize: 12,
                            color: WellPaidColors.navy.withValues(alpha: 0.55),
                          ),
                        )
                      else if (widget.item.quantity > 1 &&
                          widget.item.lineTotalCents != null)
                        Text(
                          '${formatBrlFromCents(widget.item.lineAmountCents!)} × ${widget.item.quantity} = ${formatBrlFromCents(widget.item.lineTotalCents!)}',
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
              width: 44,
              child: TextField(
                controller: _qtyCtrl,
                focusNode: _qtyFocus,
                decoration: InputDecoration(
                  isDense: true,
                  contentPadding:
                      const EdgeInsets.symmetric(horizontal: 6, vertical: 10),
                  hintText: '1',
                  border: const OutlineInputBorder(),
                ),
                style: const TextStyle(fontSize: 14),
                keyboardType: TextInputType.number,
                inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                textAlign: TextAlign.center,
                textInputAction: TextInputAction.next,
                onTapOutside: (_) =>
                    FocusManager.instance.primaryFocus?.unfocus(),
                onEditingComplete: () {
                  _qtyFocus.unfocus();
                  _amountFocus.requestFocus();
                },
              ),
            ),
            const SizedBox(width: 6),
            SizedBox(
              width: 92,
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
                inputFormatters: kBrCurrencyInputFormatters,
                textInputAction: TextInputAction.done,
                onTapOutside: (_) =>
                    FocusManager.instance.primaryFocus?.unfocus(),
                onEditingComplete: () {
                  _amountFocus.unfocus();
                },
              ),
            ),
            PopupMenuButton<String>(
              icon: Icon(
                PhosphorIconsRegular.dotsThreeVertical,
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
                      icon: const Icon(PhosphorIconsRegular.plus, size: 20),
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

class _CompletedOwnerFooter extends StatelessWidget {
  const _CompletedOwnerFooter({required this.onAdd});

  final VoidCallback onAdd;

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
              Text(
                l10n.shoppingListFooterAddItemCompleted,
                style: Theme.of(context).textTheme.bodySmall?.copyWith(
                      color: WellPaidColors.navy.withValues(alpha: 0.65),
                    ),
              ),
              const SizedBox(height: 10),
              OutlinedButton.icon(
                onPressed: onAdd,
                icon: const Icon(PhosphorIconsRegular.plus, size: 20),
                label: Text(l10n.shoppingListAddItem),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _CompleteShoppingSheet extends ConsumerStatefulWidget {
  const _CompleteShoppingSheet({required this.listId});

  final String listId;

  @override
  ConsumerState<_CompleteShoppingSheet> createState() =>
      _CompleteShoppingSheetState();
}

class _CompleteShoppingSheetState extends ConsumerState<_CompleteShoppingSheet> {
  final _formKey = GlobalKey<FormState>();
  final _totalOverrideCtrl = TextEditingController();
  late DateTime _expenseDate;
  String? _categoryId;
  bool _isShared = false;
  String? _sharedWithUserId;
  bool _submitting = false;

  @override
  void initState() {
    super.initState();
    final n = DateTime.now();
    _expenseDate = DateTime(n.year, n.month, n.day);
  }

  @override
  void dispose() {
    _totalOverrideCtrl.dispose();
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
    setState(() => _submitting = true);
    final notifier = ref.read(shoppingListDetailProvider(widget.listId).notifier);
    final flushed = await notifier.flushDraftPatchesToServer();
    if (!flushed) {
      if (mounted) {
        setState(() => _submitting = false);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(l10n.shoppingListFlushDraftError)),
        );
      }
      return;
    }
    try {
      final updated = await ref.read(shoppingListsRepositoryProvider).completeList(
            widget.listId,
            categoryId: _categoryId!,
            expenseDate: _expenseDate,
            totalCents: totalOverride,
            isShared: _isShared,
            sharedWithUserId: _sharedWithUserId,
          );
      if (!mounted) return;
      ref.read(shoppingListDetailProvider(widget.listId).notifier).applyDetail(updated);
      ref.invalidate(expensesListProvider);
      ref.invalidate(shoppingListsProvider);
      final messenger = ScaffoldMessenger.of(context);
      Navigator.pop(context);
      messenger.showSnackBar(
        SnackBar(content: Text(l10n.shoppingListCompleteSuccess)),
      );
    } catch (e) {
      if (!mounted) return;
      setState(() => _submitting = false);
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
    final sumLineCents = ref
            .watch(shoppingListDetailProvider(widget.listId))
            .valueOrNull
            ?.sumLineCents ??
        0;

    final scheme = Theme.of(context).colorScheme;

    return Padding(
      padding: EdgeInsets.fromLTRB(16, 16, 16, 16 + pad),
      child: Form(
        key: _formKey,
        child: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            mainAxisSize: MainAxisSize.min,
            children: [
              AbsorbPointer(
                absorbing: _submitting,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      l10n.shoppingListCompleteTitle,
                      style: const TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                    if (sumLineCents > 0) ...[
                      const SizedBox(height: 8),
                      Text(
                        l10n.shoppingListSubtotal(
                          formatBrlFromCents(sumLineCents),
                        ),
                        style: TextStyle(
                          color: WellPaidColors.navy.withValues(alpha: 0.7),
                        ),
                      ),
                    ],
                    const SizedBox(height: 8),
                    Text(
                      l10n.shoppingListExpenseFromListPaidNote,
                      style: TextStyle(
                        fontSize: 13,
                        color: WellPaidColors.navy.withValues(alpha: 0.65),
                      ),
                    ),
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
                      trailing: const Icon(PhosphorIconsRegular.calendar),
                      onTap: _pickDate,
                    ),
                    TextFormField(
                      controller: _totalOverrideCtrl,
                      decoration: InputDecoration(
                        labelText: l10n.shoppingListTotalOverrideHint,
                      ),
                      keyboardType:
                          const TextInputType.numberWithOptions(decimal: true),
                      inputFormatters: kBrCurrencyInputFormatters,
                      validator: (v) {
                        if (v == null || v.trim().isEmpty) return null;
                        final c = parseInputToCents(v);
                        if (c == null || c <= 0) return l10n.valueInvalid;
                        return null;
                      },
                    ),
                    const SizedBox(height: 8),
                    ExpenseShareFormSection(
                      isShared: _isShared,
                      sharedWithUserId: _sharedWithUserId,
                      onSharedChanged: (v) => setState(() => _isShared = v),
                      onPeerChanged: (v) =>
                          setState(() => _sharedWithUserId = v),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 16),
              if (_submitting) ...[
                ClipRRect(
                  borderRadius: BorderRadius.circular(4),
                  child: LinearProgressIndicator(
                    minHeight: 3,
                    backgroundColor:
                        scheme.primaryContainer.withValues(alpha: 0.35),
                  ),
                ),
                const SizedBox(height: 10),
                Text(
                  l10n.shoppingListCompleteInProgress,
                  textAlign: TextAlign.center,
                  style: Theme.of(context).textTheme.bodySmall?.copyWith(
                        color: WellPaidColors.navy.withValues(alpha: 0.72),
                      ),
                ),
                const SizedBox(height: 12),
              ],
              FilledButton(
                onPressed: _submitting ? null : _submit,
                child: _submitting
                    ? SizedBox(
                        height: 22,
                        width: 22,
                        child: CircularProgressIndicator(
                          strokeWidth: 2,
                          color: scheme.onPrimary,
                        ),
                      )
                    : Text(l10n.shoppingListComplete),
              ),
              const SizedBox(height: 8),
            ],
          ),
        ),
      ),
    );
  }
}
