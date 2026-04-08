import 'package:dio/dio.dart';

import '../../../core/network/dio_client.dart';
import '../domain/category_option.dart';
import '../domain/expense_item.dart';
import 'expenses_local_store.dart';

class ExpensesRepository {
  ExpensesRepository(this._dio, this._local);

  final Dio _dio;
  final ExpensesLocalStore _local;

  static String _isoDate(DateTime d) =>
      '${d.year.toString().padLeft(4, '0')}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';

  Future<List<CategoryOption>> fetchCategories() async {
    try {
      final res = await _dio.get<List<dynamic>>('/categories');
      final list = res.data ?? [];
      final parsed = list
          .map((e) => CategoryOption.fromJson(e as Map<String, dynamic>))
          .toList();
      _local.saveCategories(
        parsed
            .map(
              (e) => {
                'id': e.id,
                'key': e.key,
                'name': e.name,
                'sort_order': e.sortOrder,
              },
            )
            .toList(),
      );
      return parsed;
    } on DioException catch (e, st) {
      logDioException(e, st);
      final cached = _local.readCategories();
      if (cached.isNotEmpty) {
        return cached.map(CategoryOption.fromJson).toList();
      }
      rethrow;
    }
  }

  Future<List<ExpenseItem>> listExpenses({
    int? year,
    int? month,
    String? status,
    String? categoryId,
  }) async {
    await _flushQueue();
    try {
      final q = <String, dynamic>{};
      if (year != null) q['year'] = year;
      if (month != null) q['month'] = month;
      if (status != null && status.isNotEmpty) q['status'] = status;
      if (categoryId != null) q['category_id'] = categoryId;

      final res = await _dio.get<List<dynamic>>(
        '/expenses',
        queryParameters: q.isEmpty ? null : q,
      );
      final list = res.data ?? [];
      final parsed = list
          .map((e) => ExpenseItem.fromJson(e as Map<String, dynamic>))
          .toList();
      _local.upsertMany(parsed);
      return parsed;
    } on DioException catch (e, st) {
      logDioException(e, st);
      final localItems = _local.readAll();
      if (localItems.isNotEmpty) {
        return _filterLocal(
          localItems,
          year: year,
          month: month,
          status: status,
          categoryId: categoryId,
        );
      }
      rethrow;
    }
  }

  Future<ExpenseItem> getExpense(String id) async {
    try {
      final res = await _dio.get<Map<String, dynamic>>('/expenses/$id');
      final item = ExpenseItem.fromJson(res.data!);
      _local.upsertOne(item);
      return item;
    } on DioException catch (e, st) {
      logDioException(e, st);
      final cached = _local.getById(id);
      if (cached != null) return cached;
      rethrow;
    }
  }

  Future<ExpenseItem> createExpense({
    required String description,
    required int amountCents,
    required DateTime expenseDate,
    DateTime? dueDate,
    required String categoryId,
    String status = 'pending',
    int installmentTotal = 1,
    String? recurringFrequency,
  }) async {
    final body = {
      'description': description.trim(),
      'amount_cents': amountCents,
      'expense_date': _isoDate(expenseDate),
      'due_date': dueDate == null ? null : _isoDate(dueDate),
      'category_id': categoryId,
      'status': status,
      'installment_total': installmentTotal,
      'recurring_frequency': recurringFrequency,
    };
    try {
      final res = await _dio.post<Map<String, dynamic>>(
        '/expenses',
        data: body,
      );
      final item = ExpenseItem.fromJson(res.data!);
      _local.upsertOne(item);
      await _flushQueue();
      return item;
    } on DioException catch (e, st) {
      logDioException(e, st);
      final now = DateTime.now();
      final localId = 'local_${now.microsecondsSinceEpoch}';
      final categories = _local.readCategories();
      final cat = categories.cast<Map<String, dynamic>?>().firstWhere(
            (c) => c?['id'] == categoryId,
            orElse: () => null,
          );
      final localItem = ExpenseItem(
        id: localId,
        description: description.trim(),
        amountCents: amountCents,
        expenseDate: expenseDate,
        dueDate: dueDate,
        status: status,
        categoryId: categoryId,
        categoryKey: (cat?['key'] as String?) ?? 'outros',
        categoryName: (cat?['name'] as String?) ?? 'Categoria',
        syncStatus: 1,
        installmentTotal: installmentTotal,
        installmentNumber: 1,
        installmentGroupId: null,
        recurringFrequency: recurringFrequency,
        createdAt: now,
        updatedAt: now,
      );
      _local.upsertOne(localItem);
      await _local.enqueue({
        'type': 'create',
        'local_id': localId,
        'body': body,
      });
      return localItem;
    }
  }

  Future<ExpenseItem> updateExpense(
    String id, {
    required String description,
    required int amountCents,
    required DateTime expenseDate,
    DateTime? dueDate,
    required String categoryId,
    required String status,
    String? recurringFrequency,
  }) async {
    final body = {
      'description': description.trim(),
      'amount_cents': amountCents,
      'expense_date': _isoDate(expenseDate),
      'due_date': dueDate == null ? null : _isoDate(dueDate),
      'category_id': categoryId,
      'status': status,
      'recurring_frequency': recurringFrequency,
    };
    try {
      final res = await _dio.put<Map<String, dynamic>>(
        '/expenses/$id',
        data: body,
      );
      final item = ExpenseItem.fromJson(res.data!);
      _local.upsertOne(item);
      await _flushQueue();
      return item;
    } on DioException catch (e, st) {
      logDioException(e, st);
      final cached = _local.getById(id);
      if (cached != null) {
        final updated = cached.copyWith(
          description: description.trim(),
          amountCents: amountCents,
          expenseDate: expenseDate,
          dueDate: dueDate,
          categoryId: categoryId,
          status: status,
          recurringFrequency: recurringFrequency,
          syncStatus: 1,
          updatedAt: DateTime.now(),
        );
        _local.upsertOne(updated);
      }
      await _local.enqueue({
        'type': 'update',
        'id': id,
        'body': body,
      });
      final updated = _local.getById(id);
      if (updated != null) return updated;
      rethrow;
    }
  }

  Future<void> deleteExpense(String id) async {
    try {
      await _dio.delete<void>('/expenses/$id');
      _local.removeById(id);
    } on DioException catch (e, st) {
      logDioException(e, st);
      _local.removeById(id);
      await _local.enqueue({
        'type': 'delete',
        'id': id,
      });
    }
  }

  Future<ExpenseItem> payExpense(String id) async {
    try {
      final res = await _dio.post<Map<String, dynamic>>('/expenses/$id/pay');
      final item = ExpenseItem.fromJson(res.data!);
      _local.upsertOne(item);
      await _flushQueue();
      return item;
    } on DioException catch (e, st) {
      logDioException(e, st);
      final cached = _local.getById(id);
      if (cached != null) {
        final paid = cached.copyWith(
          status: 'paid',
          syncStatus: 1,
          updatedAt: DateTime.now(),
        );
        _local.upsertOne(paid);
      }
      await _local.enqueue({
        'type': 'pay',
        'id': id,
      });
      final paid = _local.getById(id);
      if (paid != null) return paid;
      rethrow;
    }
  }

  Future<void> _flushQueue() async {
    final queue = _local.readQueue();
    if (queue.isEmpty) return;
    final remaining = <Map<String, dynamic>>[];
    final idMap = <String, String>{};
    for (final op in queue) {
      try {
        final type = op['type'] as String?;
        final opIdRaw = op['id'] as String?;
        final opId = idMap[opIdRaw] ?? opIdRaw;
        if (type == 'create') {
          final res = await _dio.post<Map<String, dynamic>>(
            '/expenses',
            data: op['body'],
          );
          final created = ExpenseItem.fromJson(res.data!);
          final localId = op['local_id'] as String?;
          if (localId != null) {
            _local.removeById(localId);
            idMap[localId] = created.id;
          }
          _local.upsertOne(created);
          continue;
        }
        if (opId == null || opId.startsWith('local_')) {
          remaining.add(op);
          continue;
        }
        if (type == 'update') {
          final res = await _dio.put<Map<String, dynamic>>(
            '/expenses/$opId',
            data: op['body'],
          );
          _local.upsertOne(ExpenseItem.fromJson(res.data!));
          continue;
        }
        if (type == 'pay') {
          final res = await _dio.post<Map<String, dynamic>>('/expenses/$opId/pay');
          _local.upsertOne(ExpenseItem.fromJson(res.data!));
          continue;
        }
        if (type == 'delete') {
          await _dio.delete<void>('/expenses/$opId');
          _local.removeById(opId);
          continue;
        }
      } on DioException {
        remaining.add(op);
      }
    }
    await _local.replaceQueue(remaining);
  }

  List<ExpenseItem> _filterLocal(
    List<ExpenseItem> items, {
    int? year,
    int? month,
    String? status,
    String? categoryId,
  }) {
    return items.where((e) {
      if (year != null && e.expenseDate.year != year) return false;
      if (month != null && e.expenseDate.month != month) return false;
      if (status != null && status.isNotEmpty && e.status != status) return false;
      if (categoryId != null && e.categoryId != categoryId) return false;
      return true;
    }).toList()
      ..sort((a, b) => b.expenseDate.compareTo(a.expenseDate));
  }
}
