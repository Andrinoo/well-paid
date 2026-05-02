import 'package:dio/dio.dart';

import '../domain/receivable_item.dart';

class ReceivablesParseResult {
  const ReceivablesParseResult({
    required this.asCreditor,
    required this.asDebtor,
  });

  final List<ReceivableItem> asCreditor;
  final List<ReceivableItem> asDebtor;
}

class ReceivablesRepository {
  ReceivablesRepository(this._dio);

  final Dio _dio;

  Future<ReceivablesParseResult> listAll() async {
    final res = await _dio.get<Map<String, dynamic>>('/receivables');
    final data = res.data ?? {};
    List<ReceivableItem> parseList(dynamic raw) {
      if (raw is! List<dynamic>) return [];
      return raw
          .map((e) => ReceivableItem.fromJson(e as Map<String, dynamic>))
          .toList();
    }

    return ReceivablesParseResult(
      asCreditor: parseList(data['as_creditor']),
      asDebtor: parseList(data['as_debtor']),
    );
  }

  Future<ReceivableItem> settle(
    String id, {
    bool createIncome = false,
    String? incomeCategoryId,
  }) async {
    final res = await _dio.post<Map<String, dynamic>>(
      '/receivables/$id/settle',
      data: {
        'create_income': createIncome,
        if (incomeCategoryId != null) 'income_category_id': incomeCategoryId,
      },
    );
    return ReceivableItem.fromJson(res.data!);
  }
}
