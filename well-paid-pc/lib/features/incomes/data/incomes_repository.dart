import 'package:dio/dio.dart';

import '../../../core/network/dio_client.dart';
import '../domain/income_category_option.dart';
import '../domain/income_item.dart';
import 'incomes_local_store.dart';

class IncomesRepository {
  IncomesRepository(this._dio, this._local);

  final Dio _dio;
  final IncomesLocalStore _local;

  static String _isoDate(DateTime d) =>
      '${d.year.toString().padLeft(4, '0')}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';

  Future<List<IncomeCategoryOption>> fetchIncomeCategories() async {
    try {
      final res = await _dio.get<List<dynamic>>('/income-categories');
      final list = res.data ?? [];
      final parsed = list
          .map((e) => IncomeCategoryOption.fromJson(e as Map<String, dynamic>))
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
        return cached.map(IncomeCategoryOption.fromJson).toList();
      }
      rethrow;
    }
  }

  Future<List<IncomeItem>> listIncomes({
    required int year,
    required int month,
  }) async {
    await _flushQueue();
    try {
      final res = await _dio.get<List<dynamic>>(
        '/incomes',
        queryParameters: {'year': year, 'month': month},
      );
      final list = res.data ?? [];
      final parsed = list
          .map((e) => IncomeItem.fromJson(e as Map<String, dynamic>))
          .toList();
      _local.upsertMany(parsed);
      return _filterAndSort(parsed, year: year, month: month);
    } on DioException catch (e, st) {
      logDioException(e, st);
      final localItems = _local.readAll();
      if (localItems.isNotEmpty) {
        return _filterAndSort(localItems, year: year, month: month);
      }
      rethrow;
    }
  }

  Future<IncomeItem> _fetchIncomeFromServerNoFlush(String id) async {
    final res = await _dio.get<Map<String, dynamic>>('/incomes/$id');
    final item = IncomeItem.fromJson(res.data!);
    _local.upsertOne(item);
    return item;
  }

  Future<IncomeItem> getIncome(String id) async {
    try {
      await _flushQueue();
      return await _fetchIncomeFromServerNoFlush(id);
    } on DioException catch (e, st) {
      logDioException(e, st);
      final cached = _local.getById(id);
      if (cached != null) return cached;
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
    final body = <String, dynamic>{
      'description': description.trim(),
      'amount_cents': amountCents,
      'income_date': _isoDate(incomeDate),
      'income_category_id': incomeCategoryId,
      if (notes != null && notes.trim().isNotEmpty) 'notes': notes.trim(),
    };
    try {
      final res = await _dio.post<Map<String, dynamic>>('/incomes', data: body);
      final item = IncomeItem.fromJson(res.data!);
      _local.upsertOne(item);
      await _flushQueue();
      return item;
    } on DioException catch (e, st) {
      logDioException(e, st);
      final now = DateTime.now();
      final localId = 'local_${now.microsecondsSinceEpoch}';
      final categories = _local.readCategories();
      final cat = categories.cast<Map<String, dynamic>?>().firstWhere(
            (c) => c?['id'] == incomeCategoryId,
            orElse: () => null,
          );
      final localItem = IncomeItem(
        id: localId,
        ownerUserId: null,
        description: description.trim(),
        amountCents: amountCents,
        incomeDate: incomeDate,
        incomeCategoryId: incomeCategoryId,
        categoryKey: (cat?['key'] as String?) ?? 'outros',
        categoryName: (cat?['name'] as String?) ?? 'Categoria',
        notes: (notes == null || notes.trim().isEmpty) ? null : notes.trim(),
        syncStatus: 1,
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

  Future<IncomeItem> updateIncome(
    String id, {
    required String description,
    required int amountCents,
    required DateTime incomeDate,
    required String incomeCategoryId,
    String? notes,
  }) async {
    final body = <String, dynamic>{
      'description': description.trim(),
      'amount_cents': amountCents,
      'income_date': _isoDate(incomeDate),
      'income_category_id': incomeCategoryId,
      'notes': notes == null || notes.trim().isEmpty ? null : notes.trim(),
    };
    try {
      final res = await _dio.put<Map<String, dynamic>>('/incomes/$id', data: body);
      final item = IncomeItem.fromJson(res.data!);
      _local.upsertOne(item);
      await _flushQueue();
      return item;
    } on DioException catch (e, st) {
      logDioException(e, st);
      final cached = _local.getById(id);
      if (cached != null) {
        final categories = _local.readCategories();
        final cat = categories.cast<Map<String, dynamic>?>().firstWhere(
              (c) => c?['id'] == incomeCategoryId,
              orElse: () => null,
            );
        final updated = cached.copyWith(
          description: description.trim(),
          amountCents: amountCents,
          incomeDate: incomeDate,
          incomeCategoryId: incomeCategoryId,
          categoryKey: (cat?['key'] as String?) ?? cached.categoryKey,
          categoryName: (cat?['name'] as String?) ?? cached.categoryName,
          notes: notes == null || notes.trim().isEmpty ? null : notes.trim(),
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

  Future<void> deleteIncome(String id) async {
    final cached = _local.getById(id);
    try {
      await _dio.delete<void>('/incomes/$id');
      _local.removeById(id);
    } on DioException catch (e, st) {
      logDioException(e, st);
      if (cached != null) {
        _local.removeById(id);
      }
      await _local.enqueue({
        'type': 'delete',
        'id': id,
      });
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
            '/incomes',
            data: op['body'],
          );
          final created = IncomeItem.fromJson(res.data!);
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
            '/incomes/$opId',
            data: op['body'],
          );
          _local.upsertOne(IncomeItem.fromJson(res.data!));
          continue;
        }
        if (type == 'delete') {
          await _dio.delete<void>('/incomes/$opId');
          _local.removeById(opId);
          continue;
        }
      } on DioException {
        remaining.add(op);
      }
    }
    await _local.replaceQueue(remaining);
  }

  List<IncomeItem> _filterAndSort(
    List<IncomeItem> items, {
    required int year,
    required int month,
  }) {
    return items
        .where(
          (e) => e.incomeDate.year == year && e.incomeDate.month == month,
        )
        .toList()
      ..sort((a, b) {
        final byDate = b.incomeDate.compareTo(a.incomeDate);
        if (byDate != 0) return byDate;
        return b.createdAt.compareTo(a.createdAt);
      });
  }
}
