class ExpenseItem {
  const ExpenseItem({
    required this.id,
    this.ownerUserId,
    this.isMine = true,
    this.isShared = false,
    this.sharedWithUserId,
    this.sharedWithLabel,
    required this.description,
    required this.amountCents,
    required this.expenseDate,
    this.dueDate,
    required this.status,
    required this.categoryId,
    required this.categoryKey,
    required this.categoryName,
    required this.syncStatus,
    required this.installmentTotal,
    required this.installmentNumber,
    this.installmentGroupId,
    this.recurringFrequency,
    required this.createdAt,
    required this.updatedAt,
  });

  final String id;
  final String? ownerUserId;
  final bool isMine;
  final bool isShared;
  final String? sharedWithUserId;
  final String? sharedWithLabel;
  final String description;
  final int amountCents;
  final DateTime expenseDate;
  final DateTime? dueDate;
  final String status;
  final String categoryId;
  final String categoryKey;
  final String categoryName;
  final int syncStatus;
  final int installmentTotal;
  final int installmentNumber;
  final String? installmentGroupId;
  final String? recurringFrequency;
  final DateTime createdAt;
  final DateTime updatedAt;

  bool get isPending => status == 'pending';

  bool get isInstallmentPlan => installmentTotal > 1;

  String? get recurringLabelPt {
    switch (recurringFrequency) {
      case 'monthly':
        return 'Recorrente · mensal';
      case 'weekly':
        return 'Recorrente · semanal';
      case 'yearly':
        return 'Recorrente · anual';
      default:
        return null;
    }
  }

  factory ExpenseItem.fromJson(Map<String, dynamic> json) {
    return ExpenseItem(
      id: json['id'] as String,
      ownerUserId: json['owner_user_id'] as String?,
      isMine: json['is_mine'] as bool? ?? true,
      isShared: json['is_shared'] as bool? ?? false,
      sharedWithUserId: json['shared_with_user_id'] as String?,
      sharedWithLabel: json['shared_with_label'] as String?,
      description: json['description'] as String,
      amountCents: (json['amount_cents'] as num).toInt(),
      expenseDate: DateTime.parse(json['expense_date'] as String),
      dueDate: json['due_date'] == null
          ? null
          : DateTime.parse(json['due_date'] as String),
      status: json['status'] as String,
      categoryId: json['category_id'] as String,
      categoryKey: json['category_key'] as String,
      categoryName: json['category_name'] as String,
      syncStatus: (json['sync_status'] as num).toInt(),
      installmentTotal: (json['installment_total'] as num?)?.toInt() ?? 1,
      installmentNumber: (json['installment_number'] as num?)?.toInt() ?? 1,
      installmentGroupId: json['installment_group_id'] as String?,
      recurringFrequency: json['recurring_frequency'] as String?,
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
      'is_shared': isShared,
      if (sharedWithUserId != null) 'shared_with_user_id': sharedWithUserId,
      if (sharedWithLabel != null) 'shared_with_label': sharedWithLabel,
      'description': description,
      'amount_cents': amountCents,
      'expense_date': isoDate(expenseDate),
      'due_date': dueDate == null ? null : isoDate(dueDate!),
      'status': status,
      'category_id': categoryId,
      'category_key': categoryKey,
      'category_name': categoryName,
      'sync_status': syncStatus,
      'installment_total': installmentTotal,
      'installment_number': installmentNumber,
      'installment_group_id': installmentGroupId,
      'recurring_frequency': recurringFrequency,
      'created_at': createdAt.toUtc().toIso8601String(),
      'updated_at': updatedAt.toUtc().toIso8601String(),
    };
  }

  ExpenseItem copyWith({
    String? id,
    String? ownerUserId,
    bool? isMine,
    bool? isShared,
    String? sharedWithUserId,
    String? sharedWithLabel,
    String? description,
    int? amountCents,
    DateTime? expenseDate,
    DateTime? dueDate,
    String? status,
    String? categoryId,
    String? categoryKey,
    String? categoryName,
    int? syncStatus,
    int? installmentTotal,
    int? installmentNumber,
    String? installmentGroupId,
    String? recurringFrequency,
    DateTime? createdAt,
    DateTime? updatedAt,
  }) {
    return ExpenseItem(
      id: id ?? this.id,
      ownerUserId: ownerUserId ?? this.ownerUserId,
      isMine: isMine ?? this.isMine,
      isShared: isShared ?? this.isShared,
      sharedWithUserId: sharedWithUserId ?? this.sharedWithUserId,
      sharedWithLabel: sharedWithLabel ?? this.sharedWithLabel,
      description: description ?? this.description,
      amountCents: amountCents ?? this.amountCents,
      expenseDate: expenseDate ?? this.expenseDate,
      dueDate: dueDate ?? this.dueDate,
      status: status ?? this.status,
      categoryId: categoryId ?? this.categoryId,
      categoryKey: categoryKey ?? this.categoryKey,
      categoryName: categoryName ?? this.categoryName,
      syncStatus: syncStatus ?? this.syncStatus,
      installmentTotal: installmentTotal ?? this.installmentTotal,
      installmentNumber: installmentNumber ?? this.installmentNumber,
      installmentGroupId: installmentGroupId ?? this.installmentGroupId,
      recurringFrequency: recurringFrequency ?? this.recurringFrequency,
      createdAt: createdAt ?? this.createdAt,
      updatedAt: updatedAt ?? this.updatedAt,
    );
  }
}
