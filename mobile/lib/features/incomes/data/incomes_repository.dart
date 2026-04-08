import 'package:dio/dio.dart';

import '../../../core/network/dio_client.dart';
import '../domain/income_category_option.dart';
import '../domain/income_item.dart';

class IncomesRepository {
  IncomesRepository(this._dio);

  final Dio _dio;

  static String _isoDate(DateTime d) =>
      '${d.year.toString().padLeft(4, '0')}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';

  Future<List<IncomeCategoryOption>> fetchIncomeCategories() async {
    try {
      final res = await _dio.get<List<dynamic>>('/income-categories');
      final list = res.data ?? [];
      return list
          .map((e) => IncomeCategoryOption.fromJson(e as Map<String, dynamic>))
          .toList();
    } on DioException catch (e, st) {
      logDioException(e, st);
      rethrow;
    }
  }

  Future<List<IncomeItem>> listIncomes({required int year, required int month}) async {
    try {
      final res = await _dio.get<List<dynamic>>(
        '/incomes',
        queryParameters: {'year': year, 'month': month},
      );
      final list = res.data ?? [];
      return list
          .map((e) => IncomeItem.fromJson(e as Map<String, dynamic>))
          .toList();
    } on DioException catch (e, st) {
      logDioException(e, st);
      rethrow;
    }
  }

  Future<IncomeItem> getIncome(String id) async {
    try {
      final res = await _dio.get<Map<String, dynamic>>('/incomes/$id');
      return IncomeItem.fromJson(res.data!);
    } on DioException catch (e, st) {
      logDioException(e, st);
      rethrow;
    }
  }

  Future<IncomeItem> createIncome({
    required String description,
    required int amountCents,
    required DateTime incomeDate,
    required String incomeCategoryId,
    String? notes,
  }) async {
    try {
      final body = <String, dynamic>{
        'description': description.trim(),
        'amount_cents': amountCents,
        'income_date': _isoDate(incomeDate),
        'income_category_id': incomeCategoryId,
        if (notes != null && notes.trim().isNotEmpty) 'notes': notes.trim(),
      };
      final res = await _dio.post<Map<String, dynamic>>('/incomes', data: body);
      return IncomeItem.fromJson(res.data!);
    } on DioException catch (e, st) {
      logDioException(e, st);
      rethrow;
    }
  }

  Future<IncomeItem> updateIncome(
    String id, {
    required String description,
    required int amountCents,
    required DateTime incomeDate,
    required String incomeCategoryId,
    String? notes,
  }) async {
    try {
      final body = <String, dynamic>{
        'description': description.trim(),
        'amount_cents': amountCents,
        'income_date': _isoDate(incomeDate),
        'income_category_id': incomeCategoryId,
        'notes': notes == null || notes.trim().isEmpty ? null : notes.trim(),
      };
      final res = await _dio.put<Map<String, dynamic>>('/incomes/$id', data: body);
      return IncomeItem.fromJson(res.data!);
    } on DioException catch (e, st) {
      logDioException(e, st);
      rethrow;
    }
  }

  Future<void> deleteIncome(String id) async {
    try {
      await _dio.delete<void>('/incomes/$id');
    } on DioException catch (e, st) {
      logDioException(e, st);
      rethrow;
    }
  }
}
