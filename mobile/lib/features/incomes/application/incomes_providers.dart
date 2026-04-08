import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/authorized_dio_provider.dart';
import '../data/incomes_repository.dart';
import '../domain/income_category_option.dart';
import '../domain/income_item.dart';

final incomesRepositoryProvider = Provider<IncomesRepository>(
  (ref) => IncomesRepository(ref.watch(dioProvider)),
);

final incomeCategoriesProvider =
    FutureProvider.autoDispose<List<IncomeCategoryOption>>(
  (ref) => ref.watch(incomesRepositoryProvider).fetchIncomeCategories(),
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
