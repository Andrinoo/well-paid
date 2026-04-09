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

final shoppingListDetailProvider =
    FutureProvider.autoDispose.family<ShoppingListDetail, String>((ref, listId) async {
  final link = ref.keepAlive();
  final timer = Timer(const Duration(minutes: 3), link.close);
  ref.onDispose(timer.cancel);
  return ref.watch(shoppingListsRepositoryProvider).fetchDetail(listId);
});
