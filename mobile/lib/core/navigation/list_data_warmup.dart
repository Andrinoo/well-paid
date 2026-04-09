import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../features/dashboard/application/dashboard_providers.dart';
import '../../features/emergency_reserve/application/emergency_reserve_providers.dart';
import '../../features/expenses/application/expenses_providers.dart';
import '../../features/goals/application/goals_providers.dart';
import '../../features/incomes/application/incomes_providers.dart';

/// Mantém o mês das listas de despesas / proventos igual ao seletor do dashboard.
void syncListFiltersWithDashboardPeriod(WidgetRef ref) {
  final p = ref.read(dashboardPeriodProvider);
  final exp = ref.read(expenseListFiltersProvider);
  ref.read(expenseListFiltersProvider.notifier).state = ExpenseListFilters(
    year: p.year,
    month: p.month,
    status: exp.status,
  );
  ref.read(incomeListFiltersProvider.notifier).state = IncomeListFilters(
    year: p.year,
    month: p.month,
  );
}

/// Pré-carrega listas mensais para o mês atual do dashboard (cache Hive + Riverpod).
void warmMonthlyListsForDashboardPeriod(WidgetRef ref) {
  syncListFiltersWithDashboardPeriod(ref);
  unawaited(ref.read(expensesListProvider.future));
  unawaited(ref.read(incomesListProvider.future));
}

/// Dados sem filtro de mês (metas, categorias, reserva).
void warmGlobalReferenceData(WidgetRef ref) {
  unawaited(ref.read(goalsListProvider.future));
  unawaited(ref.read(categoriesProvider.future));
  unawaited(ref.read(incomeCategoriesProvider.future));
  unawaited(ref.read(emergencyReserveSnapshotProvider.future));
  unawaited(ref.read(emergencyReserveAccrualsProvider.future));
}

/// Chamado pelo shell após o primeiro frame — mesmo efeito que o warmup do dashboard.
void scheduleShellDataWarmup(WidgetRef ref) {
  warmGlobalReferenceData(ref);
  warmMonthlyListsForDashboardPeriod(ref);
}
