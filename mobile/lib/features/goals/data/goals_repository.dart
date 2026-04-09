import 'package:dio/dio.dart';

import '../../../core/network/dio_client.dart';
import '../domain/goal_contribution_item.dart';
import '../domain/goal_item.dart';
import 'goals_local_store.dart';

class GoalsRepository {
  GoalsRepository(this._dio, this._local);

  final Dio _dio;
  final GoalsLocalStore _local;

  Future<void> _flushQueue() async {
    final queue = _local.readQueue();
    if (queue.isEmpty) return;
    final remaining = <Map<String, dynamic>>[];
    for (final op in queue) {
      try {
        final type = op['type'] as String?;
        if (type == 'create') {
          final res = await _dio.post<Map<String, dynamic>>(
            '/goals',
            data: op['body'],
          );
          final created = GoalItem.fromJson(res.data!);
          final localId = op['local_id'] as String?;
          if (localId != null) {
            _local.removeById(localId);
          }
          _local.upsertOne(created);
          continue;
        }
        remaining.add(op);
      } on DioException {
        remaining.add(op);
      }
    }
    await _local.replaceQueue(remaining);
  }

  Future<List<GoalItem>> listGoals() async {
    await _flushQueue();
    try {
      final res = await _dio.get<List<dynamic>>('/goals');
      final list = res.data ?? [];
      final parsed = list
          .map((e) => GoalItem.fromJson(e as Map<String, dynamic>))
          .toList();
      _local.upsertMany(parsed);
      return _sortGoals(parsed);
    } on DioException catch (e, st) {
      logDioException(e, st);
      final cached = _local.readAll();
      if (cached.isNotEmpty) {
        return _sortGoals(cached);
      }
      rethrow;
    }
  }

  Future<GoalItem> createGoal({
    required String title,
    required int targetCents,
    int currentCents = 0,
    bool isActive = true,
  }) async {
    final body = <String, dynamic>{
      'title': title.trim(),
      'target_cents': targetCents,
      'current_cents': currentCents,
      'is_active': isActive,
    };
    try {
      final res = await _dio.post<Map<String, dynamic>>('/goals', data: body);
      final item = GoalItem.fromJson(res.data!);
      _local.upsertOne(item);
      await _flushQueue();
      return item;
    } on DioException catch (e, st) {
      logDioException(e, st);
      final now = DateTime.now();
      final localId = 'local_${now.microsecondsSinceEpoch}';
      final localItem = GoalItem(
        id: localId,
        ownerUserId: null,
        isMine: true,
        title: title.trim(),
        targetCents: targetCents,
        currentCents: currentCents,
        isActive: isActive,
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

  /// Alinha com GET /goals: `is_active` desc, depois `updated_at` desc.
  List<GoalItem> _sortGoals(List<GoalItem> goals) {
    return [...goals]..sort((a, b) {
          final ac = a.isActive ? 0 : 1;
          final bc = b.isActive ? 0 : 1;
          if (ac != bc) return ac - bc;
          return b.updatedAt.compareTo(a.updatedAt);
        });
  }

  Future<GoalItem> getGoal(String id) async {
    await _flushQueue();
    final res = await _dio.get<Map<String, dynamic>>('/goals/$id');
    final item = GoalItem.fromJson(res.data!);
    _local.upsertOne(item);
    return item;
  }

  Future<List<GoalContributionItem>> listContributions(String goalId) async {
    await _flushQueue();
    final res = await _dio.get<List<dynamic>>('/goals/$goalId/contributions');
    final list = res.data ?? [];
    return list
        .map((e) => GoalContributionItem.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<GoalItem> contributeToGoal({
    required String goalId,
    required int amountCents,
    String? note,
  }) async {
    final body = <String, dynamic>{
      'amount_cents': amountCents,
      if (note != null && note.trim().isNotEmpty) 'note': note.trim(),
    };
    final res = await _dio.post<Map<String, dynamic>>(
      '/goals/$goalId/contribute',
      data: body,
    );
    final item = GoalItem.fromJson(res.data!);
    _local.upsertOne(item);
    return item;
  }

  Future<GoalItem> updateGoalFields({
    required String id,
    bool? isActive,
  }) async {
    final body = <String, dynamic>{};
    if (isActive != null) body['is_active'] = isActive;
    final res = await _dio.put<Map<String, dynamic>>('/goals/$id', data: body);
    final item = GoalItem.fromJson(res.data!);
    _local.upsertOne(item);
    return item;
  }

  Future<void> deleteGoal(String id) async {
    await _dio.delete<void>('/goals/$id');
    _local.removeById(id);
  }
}
