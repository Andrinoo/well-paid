import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:hive/hive.dart';

import '../../../core/network/authorized_dio_provider.dart';
import '../data/expenses_local_store.dart';
import '../data/expenses_repository.dart';
import '../domain/category_option.dart';
import '../domain/expense_item.dart';

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
  (ref) => ref.watch(expensesRepositoryProvider).fetchCategories(),
);

/// Filtros da lista de despesas (mês + status opcional).
class ExpenseListFilters {
  const ExpenseListFilters({
    required this.year,
    required this.month,
    this.status,
  });

  final int year;
  final int month;
  /// `null` = todas; `pending` | `paid`
  final String? status;
}

final expenseListFiltersProvider =
    StateProvider<ExpenseListFilters>((ref) {
  final now = DateTime.now();
  return ExpenseListFilters(year: now.year, month: now.month);
});

final expensesListProvider =
    FutureProvider.autoDispose<List<ExpenseItem>>((ref) async {
  final f = ref.watch(expenseListFiltersProvider);
  return ref.watch(expensesRepositoryProvider).listExpenses(
        year: f.year,
        month: f.month,
        status: f.status,
      );
});

final expenseDetailProvider =
    FutureProvider.autoDispose.family<ExpenseItem, String>(
  (ref, id) => ref.watch(expensesRepositoryProvider).getExpense(id),
);
