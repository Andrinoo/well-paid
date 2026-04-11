import 'package:dio/dio.dart';

import '../../../core/network/dio_client.dart';
import '../domain/emergency_reserve_accrual_item.dart';
import '../domain/emergency_reserve_snapshot.dart';

class EmergencyReserveRepository {
  EmergencyReserveRepository(this._dio);

  final Dio _dio;

  Future<EmergencyReserveSnapshot> fetch() async {
    try {
      final res = await _dio.get<Map<String, dynamic>>('/emergency-reserve');
      return EmergencyReserveSnapshot.fromJson(res.data!);
    } on DioException catch (e, st) {
      logDioException(e, st);
      rethrow;
    }
  }

  Future<EmergencyReserveSnapshot> updateMonthlyTarget(int monthlyTargetCents) async {
    try {
      final res = await _dio.put<Map<String, dynamic>>(
        '/emergency-reserve',
        data: {'monthly_target_cents': monthlyTargetCents},
      );
      return EmergencyReserveSnapshot.fromJson(res.data!);
    } on DioException catch (e, st) {
      logDioException(e, st);
      rethrow;
    }
  }

  Future<List<EmergencyReserveAccrualItem>> fetchAccruals({
    int limit = 12,
  }) async {
    try {
      final res = await _dio.get<List<dynamic>>(
        '/emergency-reserve/accruals',
        queryParameters: {'limit': limit},
      );
      final raw = res.data ?? const [];
      return raw
          .map((e) => EmergencyReserveAccrualItem.fromJson(e as Map<String, dynamic>))
          .toList();
    } on DioException catch (e, st) {
      logDioException(e, st);
      rethrow;
    }
  }

  Future<EmergencyReserveSnapshot> patchAccrual({
    required int year,
    required int month,
    required int amountCents,
  }) async {
    try {
      final res = await _dio.patch<Map<String, dynamic>>(
        '/emergency-reserve/accruals/$year/$month',
        data: {'amount_cents': amountCents},
      );
      return EmergencyReserveSnapshot.fromJson(res.data!);
    } on DioException catch (e, st) {
      logDioException(e, st);
      rethrow;
    }
  }

  Future<EmergencyReserveSnapshot> deleteAccrual({
    required int year,
    required int month,
  }) async {
    try {
      final res = await _dio.delete<Map<String, dynamic>>(
        '/emergency-reserve/accruals/$year/$month',
      );
      return EmergencyReserveSnapshot.fromJson(res.data!);
    } on DioException catch (e, st) {
      logDioException(e, st);
      rethrow;
    }
  }

  Future<void> deleteEntireReserve() async {
    try {
      await _dio.delete<void>('/emergency-reserve');
    } on DioException catch (e, st) {
      logDioException(e, st);
      rethrow;
    }
  }
}
