class IncomeItem {
  const IncomeItem({
    required this.id,
    this.ownerUserId,
    this.isMine = true,
    required this.description,
    required this.amountCents,
    required this.incomeDate,
    required this.incomeCategoryId,
    required this.categoryKey,
    required this.categoryName,
    this.notes,
    required this.syncStatus,
    required this.createdAt,
    required this.updatedAt,
  });

  final String id;
  final String? ownerUserId;
  final bool isMine;
  final String description;
  final int amountCents;
  final DateTime incomeDate;
  final String incomeCategoryId;
  final String categoryKey;
  final String categoryName;
  final String? notes;
  final int syncStatus;
  final DateTime createdAt;
  final DateTime updatedAt;

  factory IncomeItem.fromJson(Map<String, dynamic> json) {
    return IncomeItem(
      id: json['id'] as String,
      ownerUserId: json['owner_user_id'] as String?,
      isMine: json['is_mine'] as bool? ?? true,
      description: json['description'] as String,
      amountCents: (json['amount_cents'] as num).toInt(),
      incomeDate: DateTime.parse(json['income_date'] as String),
      incomeCategoryId: json['income_category_id'] as String,
      categoryKey: json['category_key'] as String,
      categoryName: json['category_name'] as String,
      notes: json['notes'] as String?,
      syncStatus: (json['sync_status'] as num).toInt(),
      createdAt: DateTime.parse(json['created_at'] as String),
      updatedAt: DateTime.parse(json['updated_at'] as String),
    );
  }

  Map<String, dynamic> toJson() {
    String isoDate(DateTime d) =>
        '${d.year.toString().padLeft(4, '0')}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';
    return {
      'id': id,
      if (ownerUserId != null) 'owner_user_id': ownerUserId,
      'is_mine': isMine,
      'description': description,
      'amount_cents': amountCents,
      'income_date': isoDate(incomeDate),
      'income_category_id': incomeCategoryId,
      'category_key': categoryKey,
      'category_name': categoryName,
      'notes': notes,
      'sync_status': syncStatus,
      'created_at': createdAt.toUtc().toIso8601String(),
      'updated_at': updatedAt.toUtc().toIso8601String(),
    };
  }

  IncomeItem copyWith({
    String? id,
    String? ownerUserId,
    bool? isMine,
    String? description,
    int? amountCents,
    DateTime? incomeDate,
    String? incomeCategoryId,
    String? categoryKey,
    String? categoryName,
    String? notes,
    int? syncStatus,
    DateTime? createdAt,
    DateTime? updatedAt,
  }) {
    return IncomeItem(
      id: id ?? this.id,
      ownerUserId: ownerUserId ?? this.ownerUserId,
      isMine: isMine ?? this.isMine,
      description: description ?? this.description,
      amountCents: amountCents ?? this.amountCents,
      incomeDate: incomeDate ?? this.incomeDate,
      incomeCategoryId: incomeCategoryId ?? this.incomeCategoryId,
      categoryKey: categoryKey ?? this.categoryKey,
      categoryName: categoryName ?? this.categoryName,
      notes: notes ?? this.notes,
      syncStatus: syncStatus ?? this.syncStatus,
      createdAt: createdAt ?? this.createdAt,
      updatedAt: updatedAt ?? this.updatedAt,
    );
  }
}
