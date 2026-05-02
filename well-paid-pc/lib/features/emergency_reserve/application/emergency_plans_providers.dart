import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/authorized_dio_provider.dart';
import '../data/emergency_plans_repository.dart'
    show
        EmergencyPlansRepository,
        EmergencyReserveMonthRow,
        EmergencyReservePlanItem;

final emergencyPlansRepositoryProvider = Provider<EmergencyPlansRepository>(
  (ref) => EmergencyPlansRepository(ref.watch(dioProvider)),
);

final emergencyPlansListProvider =
    FutureProvider.autoDispose<List<EmergencyReservePlanItem>>((ref) async {
  return ref.watch(emergencyPlansRepositoryProvider).listPlans();
});

final emergencyPlanMonthsProvider = FutureProvider.autoDispose
    .family<List<EmergencyReserveMonthRow>, String>((ref, planId) async {
  return ref.watch(emergencyPlansRepositoryProvider).listPlanMonths(planId);
});
