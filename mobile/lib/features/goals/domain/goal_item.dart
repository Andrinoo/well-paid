class GoalItem {
  const GoalItem({
    required this.id,
    required this.title,
    required this.targetCents,
    required this.currentCents,
    required this.isActive,
  });

  final String id;
  final String title;
  final int targetCents;
  final int currentCents;
  final bool isActive;

  factory GoalItem.fromJson(Map<String, dynamic> json) {
    return GoalItem(
      id: json['id'] as String,
      title: json['title'] as String,
      targetCents: (json['target_cents'] as num).toInt(),
      currentCents: (json['current_cents'] as num).toInt(),
      isActive: json['is_active'] as bool? ?? true,
    );
  }
}
