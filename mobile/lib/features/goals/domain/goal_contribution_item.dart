class GoalContributionItem {
  const GoalContributionItem({
    required this.id,
    required this.goalId,
    required this.amountCents,
    this.note,
    required this.recordedAt,
  });

  final String id;
  final String goalId;
  final int amountCents;
  final String? note;
  final DateTime recordedAt;

  factory GoalContributionItem.fromJson(Map<String, dynamic> json) {
    return GoalContributionItem(
      id: json['id'] as String,
      goalId: json['goal_id'] as String,
      amountCents: (json['amount_cents'] as num).toInt(),
      note: json['note'] as String?,
      recordedAt: DateTime.parse(json['recorded_at'] as String),
    );
  }
}
