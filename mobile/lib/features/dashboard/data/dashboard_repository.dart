import 'package:dio/dio.dart';

import '../../../core/network/dio_client.dart';
import '../domain/dashboard_overview.dart';

class DashboardRepository {
  DashboardRepository(this._dio);

  final Dio _dio;

  Future<DashboardOverview> fetchOverview({
    required int year,
    required int month,
  }) async {
    try {
      final res = await _dio.get<Map<String, dynamic>>(
        '/dashboard/overview',
        queryParameters: {'year': year, 'month': month},
      );
      return DashboardOverview.fromJson(res.data!);
    } on DioException catch (e, st) {
      logDioException(e, st);
      rethrow;
    }
  }
}
