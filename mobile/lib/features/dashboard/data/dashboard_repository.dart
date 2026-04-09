import 'package:dio/dio.dart';

import '../../../core/network/dio_client.dart';
import '../domain/dashboard_cashflow.dart';
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

  Future<DashboardCashflow> fetchCashflow(DashboardCashflowRequest request) async {
    try {
      final q = <String, dynamic>{
        'dynamic': request.isDynamicWindow,
        'forecast_months': request.forecastMonths,
      };
      if (!request.isDynamicWindow) {
        q['start_year'] = request.startYear!;
        q['start_month'] = request.startMonth!;
        q['end_year'] = request.endYear!;
        q['end_month'] = request.endMonth!;
      }
      final res = await _dio.get<Map<String, dynamic>>(
        '/dashboard/cashflow',
        queryParameters: q,
      );
      return DashboardCashflow.fromJson(res.data!);
    } on DioException catch (e, st) {
      logDioException(e, st);
      rethrow;
    }
  }
}
