import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/authorized_dio_provider.dart';
import '../data/shopping_lists_repository.dart';
import '../domain/shopping_list_models.dart';

final shoppingListsRepositoryProvider = Provider<ShoppingListsRepository>(
  (ref) => ShoppingListsRepository(ref.watch(dioProvider)),
);

final shoppingListsProvider =
    FutureProvider.autoDispose<List<ShoppingListSummary>>((ref) async {
  final link = ref.keepAlive();
  final timer = Timer(const Duration(minutes: 3), link.close);
  ref.onDispose(timer.cancel);
  return ref.watch(shoppingListsRepositoryProvider).fetchLists();
});

/// Detalhe da lista.
///
/// Em **rascunho**, alterações de quantidade/valor por linha podem ser feitas só
/// em memória ([applyLocalItemLineDraft]); o servidor é atualizado em lote em
/// [flushDraftPatchesToServer] (ex.: ao fechar compra ou ao sair do ecrã).
/// Respostas da API usam [applyDetail] e limpam o rasto local.
class ShoppingListDetailNotifier
    extends AutoDisposeFamilyAsyncNotifier<ShoppingListDetail, String> {
  final Set<String> _dirtyItemIds = <String>{};

  @override
  Future<ShoppingListDetail> build(String listId) async {
    _dirtyItemIds.clear();
    final link = ref.keepAlive();
    final timer = Timer(const Duration(minutes: 3), link.close);
    ref.onDispose(timer.cancel);
    return ref.read(shoppingListsRepositoryProvider).fetchDetail(listId);
  }

  void applyDetail(ShoppingListDetail detail) {
    _dirtyItemIds.clear();
    state = AsyncData(detail);
  }

  /// Atualiza uma linha só no estado local (lista em rascunho).
  void applyLocalItemLineDraft({
    required String itemId,
    int? quantity,
    int? lineAmountCents,
    bool clearLineAmount = false,
  }) {
    final d = state.valueOrNull;
    if (d == null || !d.isDraft) return;
    final nextItems = d.items.map((e) {
      if (e.id != itemId) return e;
      return ShoppingListItemRow(
        id: e.id,
        sortOrder: e.sortOrder,
        label: e.label,
        quantity: quantity ?? e.quantity,
        lineAmountCents:
            clearLineAmount ? null : (lineAmountCents ?? e.lineAmountCents),
      );
    }).toList();
    _dirtyItemIds.add(itemId);
    state = AsyncData(d.copyWith(items: nextItems));
  }

  bool get hasPendingDraftPatches =>
      state.valueOrNull?.isDraft == true && _dirtyItemIds.isNotEmpty;

  /// Envia alterações pendentes das linhas para o servidor (rascunho).
  Future<bool> flushDraftPatchesToServer() async {
    final d = state.valueOrNull;
    if (d == null || !d.isDraft || _dirtyItemIds.isEmpty) return true;

    final repo = ref.read(shoppingListsRepositoryProvider);
    try {
      var current = d;
      final ids = List<String>.from(_dirtyItemIds);
      for (final itemId in ids) {
        ShoppingListItemRow? row;
        for (final e in current.items) {
          if (e.id == itemId) {
            row = e;
            break;
          }
        }
        if (row == null) continue;

        current = await repo.patchItem(
          current.id,
          itemId,
          quantity: row.quantity,
          lineAmountCents: row.lineAmountCents,
          clearLineAmount: row.lineAmountCents == null,
        );
        state = AsyncData(current);
      }
      _dirtyItemIds.clear();
      ref.invalidate(shoppingListsProvider);
      return true;
    } catch (_) {
      return false;
    }
  }
}

final shoppingListDetailProvider = AsyncNotifierProvider.autoDispose
    .family<ShoppingListDetailNotifier, ShoppingListDetail, String>(
  ShoppingListDetailNotifier.new,
);
