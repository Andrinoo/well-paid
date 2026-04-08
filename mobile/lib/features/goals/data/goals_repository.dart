import 'package:dio/dio.dart';

import '../../../core/network/dio_client.dart';
import '../domain/goal_item.dart';

class GoalsRepository {
  GoalsRepository(this._dio);

  final Dio _dio;

  Future<List<GoalItem>> listGoals() async {
    try {
      final res = await _dio.get<List<dynamic>>('/goals');
      final list = res.data ?? [];
      return list
          .map((e) => GoalItem.fromJson(e as Map<String, dynamic>))
          .toList();
    } on DioException catch (e, st) {
      logDioException(e, st);
      rethrow;
    }
  }
}
