import 'package:dio/dio.dart';

import 'investment_models.dart';

class InvestmentsRepository {
  InvestmentsRepository(this._dio);

  final Dio _dio;

  Future<InvestmentOverview> fetchOverview() async {
    final res = await _dio.get<Map<String, dynamic>>('/investments/overview');
    return InvestmentOverview.fromJson(res.data!);
  }

  Future<List<InvestmentPosition>> listPositions() async {
    final res = await _dio.get<List<dynamic>>('/investments/positions');
    final list = res.data ?? [];
    return list
        .map((e) => InvestmentPosition.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<InvestmentPosition> addPrincipal(
    String positionId,
    int addPrincipalCents,
  ) async {
    final res = await _dio.patch<Map<String, dynamic>>(
      '/investments/positions/$positionId',
      data: {'add_principal_cents': addPrincipalCents},
    );
    return InvestmentPosition.fromJson(res.data!);
  }
}
