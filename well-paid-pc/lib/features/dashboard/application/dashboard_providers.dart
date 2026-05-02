import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/authorized_dio_provider.dart';
import '../data/dashboard_repository.dart';
import '../domain/dashboard_cashflow.dart';
import '../domain/dashboard_overview.dart';

final dashboardRepositoryProvider = Provider<DashboardRepository>(
  (ref) => DashboardRepository(ref.watch(dioProvider)),
);

/// Período do dashboard (mês civil). Alterar invalida o overview.
class DashboardPeriod {
  const DashboardPeriod({required this.year, required this.month});

  final int year;
  final int month;

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is DashboardPeriod && other.year == year && other.month == month;

  @override
  int get hashCode => Object.hash(year, month);
}

final dashboardPeriodProvider = StateProvider<DashboardPeriod>((ref) {
  final now = DateTime.now();
  return DashboardPeriod(year: now.year, month: now.month);
});

final dashboardOverviewByPeriodProvider =
    FutureProvider.autoDispose.family<DashboardOverview, DashboardPeriod>(
  (ref, period) async {
    // Keep each month cached for a short window to speed navigation
    // without keeping an unbounded number of months alive.
    final link = ref.keepAlive();
    final timer = Timer(const Duration(minutes: 5), link.close);
    ref.onDispose(timer.cancel);

    final repo = ref.watch(dashboardRepositoryProvider);
    return repo.fetchOverview(year: period.year, month: period.month);
  },
);

final dashboardOverviewProvider = FutureProvider<DashboardOverview>((ref) async {
  final period = ref.watch(dashboardPeriodProvider);
  return ref.watch(dashboardOverviewByPeriodProvider(period).future);
});

/// Pedido atual do gráfico Histórico mensal (F2+). Por defeito: janela dinâmica do servidor.
final dashboardCashflowRequestProvider =
    StateProvider<DashboardCashflowRequest>((ref) {
  return const DashboardCashflowRequest(
    isDynamicWindow: true,
    forecastMonths: 3,
  );
});

/// Dados de `/dashboard/cashflow` para o pedido em [dashboardCashflowRequestProvider].
final dashboardCashflowProvider =
    FutureProvider.autoDispose<DashboardCashflow>((ref) async {
  final link = ref.keepAlive();
  final timer = Timer(const Duration(minutes: 5), link.close);
  ref.onDispose(timer.cancel);

  final req = ref.watch(dashboardCashflowRequestProvider);
  final repo = ref.watch(dashboardRepositoryProvider);
  return repo.fetchCashflow(req);
});

/// Variante com parâmetros explícitos (testes, ecrãs que não usem o StateProvider).
final dashboardCashflowByRequestProvider = FutureProvider.autoDispose
    .family<DashboardCashflow, DashboardCashflowRequest>((ref, request) async {
  final link = ref.keepAlive();
  final timer = Timer(const Duration(minutes: 5), link.close);
  ref.onDispose(timer.cancel);

  final repo = ref.watch(dashboardRepositoryProvider);
  return repo.fetchCashflow(request);
});

Future<int> refreshDashboardData(ProviderContainer container) async {
  final sw = Stopwatch()..start();
  container.invalidate(dashboardOverviewProvider);
  container.invalidate(dashboardCashflowProvider);
  await Future.wait([
    container.read(dashboardOverviewProvider.future),
    container.read(dashboardCashflowProvider.future),
  ]);
  sw.stop();
  return sw.elapsedMilliseconds;
}
