class ShoppingListItemRow {
  const ShoppingListItemRow({
    required this.id,
    required this.sortOrder,
    required this.label,
    required this.lineAmountCents,
  });

  final String id;
  final int sortOrder;
  final String label;
  final int? lineAmountCents;

  factory ShoppingListItemRow.fromJson(Map<String, dynamic> j) {
    return ShoppingListItemRow(
      id: j['id'] as String,
      sortOrder: (j['sort_order'] as num).toInt(),
      label: j['label'] as String,
      lineAmountCents: j['line_amount_cents'] == null
          ? null
          : (j['line_amount_cents'] as num).toInt(),
    );
  }
}

class ShoppingListSummary {
  const ShoppingListSummary({
    required this.id,
    required this.ownerUserId,
    required this.isMine,
    required this.title,
    required this.storeName,
    required this.status,
    required this.completedAt,
    required this.expenseId,
    required this.totalCents,
    required this.itemsCount,
    required this.createdAt,
    required this.updatedAt,
  });

  final String id;
  final String ownerUserId;
  final bool isMine;
  final String? title;
  final String? storeName;
  final String status;
  final DateTime? completedAt;
  final String? expenseId;
  final int? totalCents;
  final int itemsCount;
  final DateTime createdAt;
  final DateTime updatedAt;

  bool get isDraft => status == 'draft';
  bool get isCompleted => status == 'completed';

  factory ShoppingListSummary.fromJson(Map<String, dynamic> j) {
    return ShoppingListSummary(
      id: j['id'] as String,
      ownerUserId: j['owner_user_id'] as String,
      isMine: j['is_mine'] as bool,
      title: j['title'] as String?,
      storeName: j['store_name'] as String?,
      status: j['status'] as String,
      completedAt: j['completed_at'] == null
          ? null
          : DateTime.tryParse(j['completed_at'] as String),
      expenseId: j['expense_id'] as String?,
      totalCents: j['total_cents'] == null
          ? null
          : (j['total_cents'] as num).toInt(),
      itemsCount: (j['items_count'] as num).toInt(),
      createdAt: DateTime.parse(j['created_at'] as String),
      updatedAt: DateTime.parse(j['updated_at'] as String),
    );
  }
}

class ShoppingListDetail {
  const ShoppingListDetail({
    required this.id,
    required this.ownerUserId,
    required this.isMine,
    required this.title,
    required this.storeName,
    required this.status,
    required this.completedAt,
    required this.expenseId,
    required this.totalCents,
    required this.items,
    required this.createdAt,
    required this.updatedAt,
  });

  final String id;
  final String ownerUserId;
  final bool isMine;
  final String? title;
  final String? storeName;
  final String status;
  final DateTime? completedAt;
  final String? expenseId;
  final int? totalCents;
  final List<ShoppingListItemRow> items;
  final DateTime createdAt;
  final DateTime updatedAt;

  bool get isDraft => status == 'draft';
  bool get isCompleted => status == 'completed';

  int get sumLineCents => items
      .map((e) => e.lineAmountCents)
      .whereType<int>()
      .fold<int>(0, (a, b) => a + b);

  factory ShoppingListDetail.fromJson(Map<String, dynamic> j) {
    final rawItems = j['items'] as List<dynamic>? ?? const [];
    return ShoppingListDetail(
      id: j['id'] as String,
      ownerUserId: j['owner_user_id'] as String,
      isMine: j['is_mine'] as bool,
      title: j['title'] as String?,
      storeName: j['store_name'] as String?,
      status: j['status'] as String,
      completedAt: j['completed_at'] == null
          ? null
          : DateTime.tryParse(j['completed_at'] as String),
      expenseId: j['expense_id'] as String?,
      totalCents: j['total_cents'] == null
          ? null
          : (j['total_cents'] as num).toInt(),
      items: rawItems
          .map((e) => ShoppingListItemRow.fromJson(e as Map<String, dynamic>))
          .toList(),
      createdAt: DateTime.parse(j['created_at'] as String),
      updatedAt: DateTime.parse(j['updated_at'] as String),
    );
  }
}
