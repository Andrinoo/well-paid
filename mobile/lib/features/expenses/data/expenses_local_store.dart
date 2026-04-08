import 'package:hive/hive.dart';

import '../domain/expense_item.dart';

class ExpensesLocalStore {
  ExpensesLocalStore(this._cache, this._queue, this._categories);

  final Box<dynamic> _cache;
  final Box<dynamic> _queue;
  final Box<dynamic> _categories;

  List<ExpenseItem> readAll() {
    return _cache.values
        .whereType<Map>()
        .map((e) => ExpenseItem.fromJson(Map<String, dynamic>.from(e)))
        .toList();
  }

  void upsertMany(List<ExpenseItem> items) {
    for (final e in items) {
      upsertOne(e);
    }
  }

  void upsertOne(ExpenseItem item) {
    final raw = _cache.get(item.id);
    if (raw is Map) {
      final existing = ExpenseItem.fromJson(Map<String, dynamic>.from(raw));
      if (existing.updatedAt.isAfter(item.updatedAt)) {
        return;
      }
    }
    _cache.put(item.id, item.toJson());
  }

  ExpenseItem? getById(String id) {
    final raw = _cache.get(id);
    if (raw is! Map) return null;
    return ExpenseItem.fromJson(Map<String, dynamic>.from(raw));
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
    for (final op in ops) {
      await _queue.add(op);
    }
  }

  void saveCategories(List<Map<String, dynamic>> categories) {
    _categories.put('all', categories);
  }

  List<Map<String, dynamic>> readCategories() {
    final raw = _categories.get('all');
    if (raw is! List) return const [];
    return raw.whereType<Map>().map((e) => Map<String, dynamic>.from(e)).toList();
  }
}
