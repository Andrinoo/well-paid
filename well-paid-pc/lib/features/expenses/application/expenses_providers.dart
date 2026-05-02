import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:hive/hive.dart';

import '../../../core/network/authorized_dio_provider.dart';
import '../data/expenses_local_store.dart';
import '../data/expenses_repository.dart';
import '../domain/category_option.dart';
import '../domain/expense_item.dart';
import 'to_pay_filter.dart';

final expensesLocalStoreProvider = Provider<ExpensesLocalStore>((ref) {
  return ExpensesLocalStore(
    Hive.box<dynamic>('expenses_cache'),
    Hive.box<dynamic>('expenses_sync_queue'),
    Hive.box<dynamic>('expenses_categories_cache'),
  );
});

final expensesRepositoryProvider = Provider<ExpensesRepository>(
  (ref) => ExpensesRepository(
    ref.watch(dioProvider),
    ref.watch(expensesLocalStoreProvider),
  ),
);

final categoriesProvider = FutureProvider.autoDispose<List<CategoryOption>>(
  (ref) async {
    final link = ref.keepAlive();
    final timer = Timer(const Duration(minutes: 10), link.close);
    ref.onDispose(timer.cancel);
    return ref.watch(expensesRepositoryProvider).fetchCategories();
  },
);

/// Filtros da lista de despesas (mês + status opcional + categoria opcional).
class ExpenseListFilters {
  const ExpenseListFilters({
    required this.year,
    required this.month,
    this.status,
    this.categoryId,
  });

  final int year;
  final int month;
  /// `null` = todas; `pending` | `paid`
  final String? status;
  /// `null` = todas as categorias (pedido à API com `category_id` quando definido).
  final String? categoryId;
}

final expenseListFiltersProvider =
    StateProvider<ExpenseListFilters>((ref) {
  final now = DateTime.now();
  return ExpenseListFilters(year: now.year, month: now.month);
});

final expensesListProvider =
    FutureProvider.autoDispose<List<ExpenseItem>>((ref) async {
  final link = ref.keepAlive();
  final timer = Timer(const Duration(minutes: 3), link.close);
  ref.onDispose(timer.cancel);
  final f = ref.watch(expenseListFiltersProvider);
  final r = await ref.watch(expensesRepositoryProvider).listExpenses(
        year: f.year,
        month: f.month,
        status: f.status,
        categoryId: f.categoryId,
      );
  return r.items;
});

/// Todas as despesas pendentes (cada parcela é uma linha), por ordem cronológica de vencimento.
final toPayListProvider =
    FutureProvider.autoDispose<ToPaySnapshot>((ref) async {
  final link = ref.keepAlive();
  final timer = Timer(const Duration(minutes: 3), link.close);
  ref.onDispose(timer.cancel);
  final result = await ref
      .watch(expensesRepositoryProvider)
      .listExpenses(status: 'pending');
  final sorted = [...result.items]..sort(compareToPayChronological);
  return ToPaySnapshot(
    items: sorted,
    servedFromLocalCache: result.servedFromLocalCache,
  );
});

final expenseDetailProvider =
    FutureProvider.autoDispose.family<ExpenseItem, String>(
  (ref, id) => ref.watch(expensesRepositoryProvider).getExpense(id),
);

final expenseAdvanceQuoteProvider =
    FutureProvider.autoDispose.family<ExpenseAdvanceQuote, String>(
  (ref, id) => ref.watch(expensesRepositoryProvider).quoteAdvancePayment(id),
);

/// Parcelas do mesmo grupo (plano de prestações).
final installmentPlanExpensesProvider =
    FutureProvider.autoDispose.family<List<ExpenseItem>, String>(
  (ref, groupId) async {
    final r = await ref.watch(expensesRepositoryProvider).listExpenses(
          installmentGroupId: groupId,
        );
    return r.items;
  },
);
