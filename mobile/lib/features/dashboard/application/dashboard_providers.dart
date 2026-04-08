import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/authorized_dio_provider.dart';
import '../data/dashboard_repository.dart';
import '../domain/dashboard_overview.dart';

final dashboardRepositoryProvider = Provider<DashboardRepository>(
  (ref) => DashboardRepository(ref.watch(dioProvider)),
);

/// Período do dashboard (mês civil). Alterar invalida o overview.
class DashboardPeriod {
  const DashboardPeriod({required this.year, required this.month});

  final int year;
  final int month;
}

final dashboardPeriodProvider = StateProvider<DashboardPeriod>((ref) {
  final now = DateTime.now();
  return DashboardPeriod(year: now.year, month: now.month);
});

final dashboardOverviewProvider =
    FutureProvider.autoDispose<DashboardOverview>((ref) async {
  final period = ref.watch(dashboardPeriodProvider);
  final repo = ref.watch(dashboardRepositoryProvider);
  return repo.fetchOverview(year: period.year, month: period.month);
});
