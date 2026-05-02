import 'package:hive/hive.dart';

import '../domain/goal_item.dart';

class GoalsLocalStore {
  GoalsLocalStore(this._cache, this._queue);

  final Box<dynamic> _cache;
  final Box<dynamic> _queue;

  List<GoalItem> readAll() {
    return _cache.values
        .whereType<Map>()
        .map((e) => GoalItem.fromJson(Map<String, dynamic>.from(e)))
        .toList();
  }

  void upsertMany(List<GoalItem> items) {
    for (final g in items) {
      upsertOne(g);
    }
  }

  void upsertOne(GoalItem item) {
    final raw = _cache.get(item.id);
    if (raw is Map) {
      final existing = GoalItem.fromJson(Map<String, dynamic>.from(raw));
      if (existing.updatedAt.isAfter(item.updatedAt)) {
        return;
      }
    }
    _cache.put(item.id, item.toJson());
  }

  void clear() {
    _cache.clear();
  }

  void removeById(String id) {
    _cache.delete(id);
  }

  List<Map<String, dynamic>> readQueue() {
    return _queue.values
        .whereType<Map>()
        .map((e) => Map<String, dynamic>.from(e))
        .toList();
  }

  Future<void> enqueue(Map<String, dynamic> op) async {
    final type = op['type'];
    final id = op['id'];
    if (id is String && type is String && type != 'create') {
      final compacted = <Map<String, dynamic>>[];
      for (final item in readQueue()) {
        if (item['id'] == id && item['type'] == type) continue;
        compacted.add(item);
      }
      compacted.add(op);
      await replaceQueue(compacted);
      return;
    }
    await _queue.add(op);
  }

  Future<void> replaceQueue(List<Map<String, dynamic>> ops) async {
    await _queue.clear();
    for (final o in ops) {
      await _queue.add(o);
    }
  }
}
