class GoalItem {
  const GoalItem({
    required this.id,
    this.ownerUserId,
    this.isMine = true,
    required this.title,
    required this.targetCents,
    required this.currentCents,
    required this.isActive,
    required this.createdAt,
    required this.updatedAt,
  });

  final String id;
  final String? ownerUserId;
  final bool isMine;
  final String title;
  final int targetCents;
  final int currentCents;
  final bool isActive;
  final DateTime createdAt;
  final DateTime updatedAt;

  factory GoalItem.fromJson(Map<String, dynamic> json) {
    final now = DateTime.now().toUtc();
    return GoalItem(
      id: json['id'] as String,
      ownerUserId: json['owner_user_id'] as String?,
      isMine: json['is_mine'] as bool? ?? true,
      title: json['title'] as String,
      targetCents: (json['target_cents'] as num).toInt(),
      currentCents: (json['current_cents'] as num).toInt(),
      isActive: json['is_active'] as bool? ?? true,
      createdAt: json['created_at'] == null
          ? now
          : DateTime.parse(json['created_at'] as String),
      updatedAt: json['updated_at'] == null
          ? now
          : DateTime.parse(json['updated_at'] as String),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      if (ownerUserId != null) 'owner_user_id': ownerUserId,
      'is_mine': isMine,
      'title': title,
      'target_cents': targetCents,
      'current_cents': currentCents,
      'is_active': isActive,
      'created_at': createdAt.toUtc().toIso8601String(),
      'updated_at': updatedAt.toUtc().toIso8601String(),
    };
  }
}
