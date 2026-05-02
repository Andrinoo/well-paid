import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:hive/hive.dart';

import '../../../core/network/authorized_dio_provider.dart';
import '../data/incomes_local_store.dart';
import '../data/incomes_repository.dart';
import '../domain/income_category_option.dart';
import '../domain/income_item.dart';

final incomesLocalStoreProvider = Provider<IncomesLocalStore>((ref) {
  return IncomesLocalStore(
    Hive.box<dynamic>('incomes_cache'),
    Hive.box<dynamic>('incomes_sync_queue'),
    Hive.box<dynamic>('incomes_categories_cache'),
  );
});

final incomesRepositoryProvider = Provider<IncomesRepository>(
  (ref) => IncomesRepository(
    ref.watch(dioProvider),
    ref.watch(incomesLocalStoreProvider),
  ),
);

final incomeCategoriesProvider =
    FutureProvider.autoDispose<List<IncomeCategoryOption>>(
  (ref) async {
    final link = ref.keepAlive();
    final timer = Timer(const Duration(minutes: 10), link.close);
    ref.onDispose(timer.cancel);
    return ref.watch(incomesRepositoryProvider).fetchIncomeCategories();
  },
);

class IncomeListFilters {
  const IncomeListFilters({required this.year, required this.month});

  final int year;
  final int month;
}

final incomeListFiltersProvider = StateProvider<IncomeListFilters>((ref) {
  final now = DateTime.now();
  return IncomeListFilters(year: now.year, month: now.month);
});

final incomesListProvider = FutureProvider.autoDispose<List<IncomeItem>>(
  (ref) async {
    final link = ref.keepAlive();
    final timer = Timer(const Duration(minutes: 3), link.close);
    ref.onDispose(timer.cancel);
    final f = ref.watch(incomeListFiltersProvider);
    return ref.watch(incomesRepositoryProvider).listIncomes(
          year: f.year,
          month: f.month,
        );
  },
);

final incomeDetailProvider =
    FutureProvider.autoDispose.family<IncomeItem, String>(
  (ref, id) => ref.watch(incomesRepositoryProvider).getIncome(id),
);
