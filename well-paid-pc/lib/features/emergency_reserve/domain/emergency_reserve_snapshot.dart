class EmergencyReserveSnapshot {
  const EmergencyReserveSnapshot({
    required this.monthlyTargetCents,
    required this.balanceCents,
    required this.trackingStartIso,
    required this.configured,
  });

  final int monthlyTargetCents;
  final int balanceCents;
  final String trackingStartIso;
  final bool configured;

  factory EmergencyReserveSnapshot.fromJson(Map<String, dynamic> json) {
    return EmergencyReserveSnapshot(
      monthlyTargetCents: (json['monthly_target_cents'] as num).toInt(),
      balanceCents: (json['balance_cents'] as num).toInt(),
      trackingStartIso: json['tracking_start'] as String,
      configured: json['configured'] as bool? ?? false,
    );
  }
}
