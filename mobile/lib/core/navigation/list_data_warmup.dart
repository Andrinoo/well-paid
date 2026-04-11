import 'dart:async';

import '../../features/dashboard/application/dashboard_providers.dart';
import '../../features/emergency_reserve/application/emergency_reserve_providers.dart';
import '../../features/expenses/application/expenses_providers.dart';
import '../../features/goals/application/goals_providers.dart';
import '../../features/incomes/application/incomes_providers.dart';
import '../../features/shopping_lists/application/shopping_lists_providers.dart';

/// [Ref] de qualquer origem (ex.: [WidgetRef] no shell, [Ref] no auth).
void syncListFiltersWithDashboardPeriod(dynamic ref) {
  final p = ref.read(dashboardPeriodProvider);
  final exp = ref.read(expenseListFiltersProvider);
  ref.read(expenseListFiltersProvider.notifier).state = ExpenseListFilters(
    year: p.year,
    month: p.month,
    status: exp.status,
    categoryId: null,
  );
  ref.read(incomeListFiltersProvider.notifier).state = IncomeListFilters(
    year: p.year,
    month: p.month,
  );
}

/// Pré-carrega listas mensais para o mês atual do dashboard (cache Hive + Riverpod).
void warmMonthlyListsForDashboardPeriod(dynamic ref) {
  syncListFiltersWithDashboardPeriod(ref);
  unawaited(ref.read(expensesListProvider.future));
  unawaited(ref.read(incomesListProvider.future));
}

/// Pré-carrega despesas e proventos para o mês do dashboard ±4 (cache local / API).
void warmNineMonthExpenseIncomeCaches(dynamic ref) {
  final p = ref.read(dashboardPeriodProvider);
  final repo = ref.read(expensesRepositoryProvider);
  final incRepo = ref.read(incomesRepositoryProvider);
  for (var i = -4; i <= 4; i++) {
    final d = DateTime(p.year, p.month + i);
    unawaited(repo.listExpenses(year: d.year, month: d.month));
    unawaited(incRepo.listIncomes(year: d.year, month: d.month));
  }
}

/// Metas, categorias, reserva, listas, dashboard e cashflow (pedido actual).
void warmGlobalReferenceData(dynamic ref) {
  unawaited(ref.read(goalsListProvider.future));
  unawaited(ref.read(categoriesProvider.future));
  unawaited(ref.read(incomeCategoriesProvider.future));
  unawaited(ref.read(emergencyReserveSnapshotProvider.future));
  unawaited(ref.read(emergencyReserveAccrualsProvider.future));
  unawaited(ref.read(shoppingListsProvider.future));
  unawaited(ref.read(dashboardOverviewProvider.future));
  unawaited(ref.read(dashboardCashflowProvider.future));
}

/// Chamado após sessão válida (hidratação ou login): aquece caches em background.
void scheduleShellDataWarmup(dynamic ref) {
  warmGlobalReferenceData(ref);
  warmMonthlyListsForDashboardPeriod(ref);
  warmNineMonthExpenseIncomeCaches(ref);
}
