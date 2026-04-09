import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/authorized_dio_provider.dart';
import '../data/emergency_reserve_repository.dart';
import '../domain/emergency_reserve_accrual_item.dart';
import '../domain/emergency_reserve_snapshot.dart';

final emergencyReserveRepositoryProvider = Provider<EmergencyReserveRepository>(
  (ref) => EmergencyReserveRepository(ref.watch(dioProvider)),
);

final emergencyReserveSnapshotProvider =
    FutureProvider.autoDispose<EmergencyReserveSnapshot>(
  (ref) async {
    final link = ref.keepAlive();
    final timer = Timer(const Duration(minutes: 3), link.close);
    ref.onDispose(timer.cancel);
    return ref.watch(emergencyReserveRepositoryProvider).fetch();
  },
);

final emergencyReserveAccrualsProvider =
    FutureProvider.autoDispose<List<EmergencyReserveAccrualItem>>(
  (ref) async {
    final link = ref.keepAlive();
    final timer = Timer(const Duration(minutes: 3), link.close);
    ref.onDispose(timer.cancel);
    return ref.watch(emergencyReserveRepositoryProvider).fetchAccruals(limit: 12);
  },
);
