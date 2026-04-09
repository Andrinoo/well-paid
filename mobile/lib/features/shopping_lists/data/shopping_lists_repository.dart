import 'package:dio/dio.dart';

import '../../../core/network/dio_client.dart';
import '../domain/shopping_list_models.dart';

class ShoppingListsRepository {
  ShoppingListsRepository(this._dio);

  final Dio _dio;

  Future<List<ShoppingListSummary>> fetchLists() async {
    try {
      final res = await _dio.get<List<dynamic>>('/shopping-lists');
      final raw = res.data ?? const [];
      return raw
          .map((e) => ShoppingListSummary.fromJson(e as Map<String, dynamic>))
          .toList();
    } on DioException catch (e, st) {
      logDioException(e, st);
      rethrow;
    }
  }

  Future<ShoppingListDetail> fetchDetail(String listId) async {
    try {
      final res = await _dio.get<Map<String, dynamic>>('/shopping-lists/$listId');
      return ShoppingListDetail.fromJson(res.data!);
    } on DioException catch (e, st) {
      logDioException(e, st);
      rethrow;
    }
  }

  Future<ShoppingListDetail> createList({String? title, String? storeName}) async {
    try {
      final res = await _dio.post<Map<String, dynamic>>(
        '/shopping-lists',
        data: {
          if (title != null && title.isNotEmpty) 'title': title,
          if (storeName != null && storeName.isNotEmpty) 'store_name': storeName,
        },
      );
      return ShoppingListDetail.fromJson(res.data!);
    } on DioException catch (e, st) {
      logDioException(e, st);
      rethrow;
    }
  }

  Future<ShoppingListDetail> patchList(
    String listId, {
    required bool setTitle,
    String? title,
    required bool setStore,
    String? storeName,
  }) async {
    final body = <String, dynamic>{};
    if (setTitle) {
      body['title'] = (title == null || title.isEmpty) ? null : title;
    }
    if (setStore) {
      body['store_name'] =
          (storeName == null || storeName.isEmpty) ? null : storeName;
    }
    if (body.isEmpty) {
      return fetchDetail(listId);
    }
    try {
      final res = await _dio.patch<Map<String, dynamic>>(
        '/shopping-lists/$listId',
        data: body,
      );
      return ShoppingListDetail.fromJson(res.data!);
    } on DioException catch (e, st) {
      logDioException(e, st);
      rethrow;
    }
  }

  Future<void> deleteList(String listId) async {
    try {
      await _dio.delete<void>('/shopping-lists/$listId');
    } on DioException catch (e, st) {
      logDioException(e, st);
      rethrow;
    }
  }

  Future<ShoppingListDetail> addItem(
    String listId, {
    required String label,
    int? lineAmountCents,
  }) async {
    try {
      final res = await _dio.post<Map<String, dynamic>>(
        '/shopping-lists/$listId/items',
        data: {
          'label': label,
          if (lineAmountCents != null) 'line_amount_cents': lineAmountCents,
        },
      );
      return ShoppingListDetail.fromJson(res.data!);
    } on DioException catch (e, st) {
      logDioException(e, st);
      rethrow;
    }
  }

  Future<ShoppingListDetail> patchItem(
    String listId,
    String itemId, {
    String? label,
    int? lineAmountCents,
    bool clearLineAmount = false,
  }) async {
    final body = <String, dynamic>{};
    if (label != null) body['label'] = label;
    if (clearLineAmount) {
      body['line_amount_cents'] = null;
    } else if (lineAmountCents != null) {
      body['line_amount_cents'] = lineAmountCents;
    }
    try {
      final res = await _dio.patch<Map<String, dynamic>>(
        '/shopping-lists/$listId/items/$itemId',
        data: body,
      );
      return ShoppingListDetail.fromJson(res.data!);
    } on DioException catch (e, st) {
      logDioException(e, st);
      rethrow;
    }
  }

  Future<ShoppingListDetail> deleteItem(String listId, String itemId) async {
    try {
      final res = await _dio.delete<Map<String, dynamic>>(
        '/shopping-lists/$listId/items/$itemId',
      );
      return ShoppingListDetail.fromJson(res.data!);
    } on DioException catch (e, st) {
      logDioException(e, st);
      rethrow;
    }
  }

  Future<ShoppingListDetail> completeList(
    String listId, {
    required String categoryId,
    required DateTime expenseDate,
    required bool markPaid,
    String? description,
    int? totalCents,
    bool isShared = false,
    String? sharedWithUserId,
  }) async {
    try {
      final res = await _dio.post<Map<String, dynamic>>(
        '/shopping-lists/$listId/complete',
        data: {
          'category_id': categoryId,
          'expense_date':
              '${expenseDate.year.toString().padLeft(4, '0')}-${expenseDate.month.toString().padLeft(2, '0')}-${expenseDate.day.toString().padLeft(2, '0')}',
          'status': markPaid ? 'paid' : 'pending',
          if (description != null && description.isNotEmpty) 'description': description,
          if (totalCents != null) 'total_cents': totalCents,
          'is_shared': isShared,
          if (sharedWithUserId != null) 'shared_with_user_id': sharedWithUserId,
        },
      );
      return ShoppingListDetail.fromJson(res.data!);
    } on DioException catch (e, st) {
      logDioException(e, st);
      rethrow;
    }
  }
}
