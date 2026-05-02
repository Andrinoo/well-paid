class EmergencyReserveAccrualItem {
  const EmergencyReserveAccrualItem({
    required this.year,
    required this.month,
    required this.amountCents,
    this.createdAtIso,
  });

  final int year;
  final int month;
  final int amountCents;
  final String? createdAtIso;

  factory EmergencyReserveAccrualItem.fromJson(Map<String, dynamic> json) {
    return EmergencyReserveAccrualItem(
      year: (json['year'] as num).toInt(),
      month: (json['month'] as num).toInt(),
      amountCents: (json['amount_cents'] as num).toInt(),
      createdAtIso: json['created_at'] as String?,
    );
  }
}
