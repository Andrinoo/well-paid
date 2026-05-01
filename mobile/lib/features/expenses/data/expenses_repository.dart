import 'package:dio/dio.dart';

import '../../../core/network/dio_client.dart';
import '../domain/category_option.dart';
import '../domain/expense_delete_options.dart';
import '../domain/expense_item.dart';
import 'expenses_local_store.dart';

/// Resultado de [ExpensesRepository.listExpenses]: itens e se vieram só da cache local (rede falhou).
class ExpenseListResult {
  const ExpenseListResult({
    required this.items,
    this.servedFromLocalCache = false,
  });

  final List<ExpenseItem> items;
  final bool servedFromLocalCache;
}

class ExpenseAdvanceQuote {
  const ExpenseAdvanceQuote({
    required this.expenseId,
    required this.nominalAmountCents,
    required this.settlementAmountCents,
    required this.discountCents,
  });

  final String expenseId;
  final int nominalAmountCents;
  final int settlementAmountCents;
  final int discountCents;

  factory ExpenseAdvanceQuote.fromJson(Map<String, dynamic> json) {
    return ExpenseAdvanceQuote(
      expenseId: json['expense_id'] as String,
      nominalAmountCents: (json['nominal_amount_cents'] as num).toInt(),
      settlementAmountCents: (json['settlement_amount_cents'] as num).toInt(),
      discountCents: (json['discount_cents'] as num).toInt(),
    );
  }
}

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

  Future<ExpenseListResult> listExpenses({
    int? year,
    int? month,
    String? status,
    String? categoryId,
    String? installmentGroupId,
  }) async {
    await _flushQueue();
    try {
      final q = <String, dynamic>{};
      if (year != null) q['year'] = year;
      if (month != null) q['month'] = month;
      if (status != null && status.isNotEmpty) q['status'] = status;
      if (categoryId != null) q['category_id'] = categoryId;
      if (installmentGroupId != null) {
        q['installment_group_id'] = installmentGroupId;
      }

      final res = await _dio.get<List<dynamic>>(
        '/expenses',
        queryParameters: q.isEmpty ? null : q,
      );
      final list = res.data ?? [];
      final parsed = list
          .map((e) => ExpenseItem.fromJson(e as Map<String, dynamic>))
          .toList();
      _sortExpensesNewestFirst(parsed);
      _local.upsertMany(parsed);
      return ExpenseListResult(items: parsed);
    } on DioException catch (e, st) {
      logDioException(e, st);
      final localItems = _local.readAll();
      if (localItems.isNotEmpty) {
        final filtered = _filterLocal(
          localItems,
          year: year,
          month: month,
          status: status,
          categoryId: categoryId,
        );
        return ExpenseListResult(
          items: filtered,
          servedFromLocalCache: true,
        );
      }
      rethrow;
    }
  }

  Future<ExpenseItem> _fetchExpenseFromServerNoFlush(String id) async {
    final res = await _dio.get<Map<String, dynamic>>('/expenses/$id');
    final item = ExpenseItem.fromJson(res.data!);
    _local.upsertOne(item);
    return item;
  }

  Future<ExpenseItem> getExpense(String id) async {
    try {
      await _flushQueue();
      return await _fetchExpenseFromServerNoFlush(id);
    } on DioException catch (e, st) {
      logDioException(e, st);
      final cached = _local.getById(id);
      if (cached != null) return cached;
      rethrow;
    }
  }

  Future<List<ExpenseItem>> createExpense({
    required String description,
    required int amountCents,
    required DateTime expenseDate,
    DateTime? dueDate,
    required String categoryId,
    String status = 'pending',
    int installmentTotal = 1,
    String? recurringFrequency,
    bool isShared = false,
    String? sharedWithUserId,
    int? monthlyInterestBps,
  }) async {
    final body = <String, dynamic>{
      'description': description.trim(),
      'amount_cents': amountCents,
      'expense_date': _isoDate(expenseDate),
      'due_date': dueDate == null ? null : _isoDate(dueDate),
      'category_id': categoryId,
      'status': status,
      'installment_total': installmentTotal,
      'recurring_frequency': recurringFrequency,
      'is_shared': isShared,
      'shared_with_user_id': sharedWithUserId,
      if (monthlyInterestBps != null) 'monthly_interest_bps': monthlyInterestBps,
    };
    try {
      final res = await _dio.post<Map<String, dynamic>>(
        '/expenses',
        data: body,
      );
      final data = res.data!;
      final rawList = data['expenses'] as List<dynamic>? ?? [data];
      final list = rawList
          .map((e) => ExpenseItem.fromJson(e as Map<String, dynamic>))
          .toList();
      for (final item in list) {
        _local.upsertOne(item);
      }
      await _flushQueue();
      return list;
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
        isShared: isShared,
        sharedWithUserId: sharedWithUserId,
        description: description.trim(),
        amountCents: amountCents,
        monthlyInterestBps: monthlyInterestBps,
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
        recurringSeriesId: null,
        createdAt: now,
        updatedAt: now,
      );
      _local.upsertOne(localItem);
      await _local.enqueue({
        'type': 'create',
        'local_id': localId,
        'body': body,
      });
      return [localItem];
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
    required bool isShared,
    String? sharedWithUserId,
    int? monthlyInterestBps,
  }) async {
    final body = <String, dynamic>{
      'description': description.trim(),
      'amount_cents': amountCents,
      'expense_date': _isoDate(expenseDate),
      'due_date': dueDate == null ? null : _isoDate(dueDate),
      'category_id': categoryId,
      'status': status,
      'recurring_frequency': recurringFrequency,
      'is_shared': isShared,
      'shared_with_user_id': sharedWithUserId,
      if (monthlyInterestBps != null) 'monthly_interest_bps': monthlyInterestBps,
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
          isShared: isShared,
          sharedWithUserId: sharedWithUserId,
          monthlyInterestBps: monthlyInterestBps,
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

  void _removeExpenseClusterFromCache(String deletedId, ExpenseItem? anchor) {
    _local.removeById(deletedId);
    if (anchor == null) return;
    final gid = anchor.installmentGroupId;
    final sid = anchor.recurringSeriesId;
    if (gid == null && sid == null) return;
    for (final other in _local.readAll()) {
      if (other.id == deletedId) continue;
      if (gid != null && other.installmentGroupId == gid) {
        _local.removeById(other.id);
      } else if (sid != null && other.recurringSeriesId == sid) {
        _local.removeById(other.id);
      }
    }
  }

  Future<void> _applyDeleteCacheAfterSuccess(
    String id,
    ExpenseItem? cached,
    ExpenseDeleteTarget target,
    ExpenseDeleteScope scope,
  ) async {
    if (cached == null) {
      _local.removeById(id);
      return;
    }
    if (cached.installmentGroupId != null) {
      if (scope == ExpenseDeleteScope.all) {
        _removeExpenseClusterFromCache(id, cached);
      } else {
        _local.clearExpenseCache();
      }
      return;
    }
    if (cached.recurringSeriesId != null) {
      if (target == ExpenseDeleteTarget.occurrence) {
        if (cached.status == 'paid') {
          try {
            await _fetchExpenseFromServerNoFlush(id);
          } catch (_) {
            _local.clearExpenseCache();
          }
        } else {
          _local.removeById(id);
        }
        return;
      }
      _local.clearExpenseCache();
      return;
    }
    _local.removeById(id);
  }

  void _applyDeleteCacheAfterQueued(
    String id,
    ExpenseItem? cached,
    ExpenseDeleteTarget target,
    ExpenseDeleteScope scope,
  ) {
    if (cached == null) {
      _local.clearExpenseCache();
      return;
    }
    if (cached.installmentGroupId != null) {
      _local.clearExpenseCache();
      return;
    }
    if (cached.recurringSeriesId != null) {
      if (target == ExpenseDeleteTarget.occurrence && cached.status == 'pending') {
        _local.removeById(id);
        return;
      }
      _local.clearExpenseCache();
      return;
    }
    _local.removeById(id);
  }

  Future<void> deleteExpense(
    String id, {
    ExpenseDeleteTarget target = ExpenseDeleteTarget.series,
    ExpenseDeleteScope scope = ExpenseDeleteScope.all,
  }) async {
    final cached = _local.getById(id);
    final qp = <String, dynamic>{
      'delete_target': expenseDeleteTargetApi(target),
      'delete_scope': expenseDeleteScopeApi(scope),
    };
    try {
      await _dio.delete<void>('/expenses/$id', queryParameters: qp);
      await _applyDeleteCacheAfterSuccess(id, cached, target, scope);
    } on DioException catch (e, st) {
      logDioException(e, st);
      _applyDeleteCacheAfterQueued(id, cached, target, scope);
      await _local.enqueue({
        'type': 'delete',
        'id': id,
        'delete_target': qp['delete_target'],
        'delete_scope': qp['delete_scope'],
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
        final now = DateTime.now().toUtc();
        final paid = cached.copyWith(
          status: 'paid',
          syncStatus: 1,
          updatedAt: now,
          paidAt: now,
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

  Future<ExpenseAdvanceQuote> quoteAdvancePayment(String id) async {
    final res = await _dio.post<Map<String, dynamic>>('/expenses/$id/advance-quote');
    return ExpenseAdvanceQuote.fromJson(res.data!);
  }

  Future<ExpenseItem> payExpenseAdvanced(
    String id, {
    required int amountCents,
  }) async {
    final res = await _dio.post<Map<String, dynamic>>(
      '/expenses/$id/pay',
      data: {
        'allow_advance': true,
        'amount_cents': amountCents,
      },
    );
    final item = ExpenseItem.fromJson(res.data!);
    _local.upsertOne(item);
    await _flushQueue();
    return item;
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
          final data = res.data!;
          final rawList = data['expenses'] as List<dynamic>? ?? [data];
          final createdList = rawList
              .map((e) => ExpenseItem.fromJson(e as Map<String, dynamic>))
              .toList();
          final localId = op['local_id'] as String?;
          if (localId != null && createdList.isNotEmpty) {
            _local.removeById(localId);
            idMap[localId] = createdList.first.id;
          }
          for (final item in createdList) {
            _local.upsertOne(item);
          }
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
          final dt = op['delete_target'] as String?;
          final ds = op['delete_scope'] as String?;
          final q = <String, dynamic>{
            if (dt != null && dt.isNotEmpty) 'delete_target': dt,
            if (ds != null && ds.isNotEmpty) 'delete_scope': ds,
          };
          final cachedBefore = _local.getById(opId);
          await _dio.delete<void>(
            '/expenses/$opId',
            queryParameters: q.isEmpty ? null : q,
          );
          final t = dt == 'occurrence'
              ? ExpenseDeleteTarget.occurrence
              : ExpenseDeleteTarget.series;
          final s = ds == 'future_unpaid'
              ? ExpenseDeleteScope.futureUnpaid
              : ExpenseDeleteScope.all;
          await _applyDeleteCacheAfterSuccess(opId, cachedBefore, t, s);
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
      ..sort((a, b) {
        final byDate = b.expenseDate.compareTo(a.expenseDate);
        if (byDate != 0) return byDate;
        return b.createdAt.compareTo(a.createdAt);
      });
  }
}

void _sortExpensesNewestFirst(List<ExpenseItem> items) {
  items.sort((a, b) {
    final byDate = b.expenseDate.compareTo(a.expenseDate);
    if (byDate != 0) return byDate;
    return b.createdAt.compareTo(a.createdAt);
  });
}
