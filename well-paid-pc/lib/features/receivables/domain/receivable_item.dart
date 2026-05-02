class ReceivableItem {
  const ReceivableItem({
    required this.id,
    required this.amountCents,
    required this.settleBy,
    required this.status,
    this.debtorDisplayName,
    this.creditorDisplayName,
    this.settledAt,
  });

  final String id;
  final int amountCents;
  final DateTime settleBy;
  final String status;
  final String? debtorDisplayName;
  final String? creditorDisplayName;
  final DateTime? settledAt;

  factory ReceivableItem.fromJson(Map<String, dynamic> json) {
    DateTime? settled;
    final sa = json['settled_at'];
    if (sa is String) settled = DateTime.tryParse(sa);

    final sb = json['settle_by'];
    DateTime settleBy = DateTime.now();
    if (sb is String) settleBy = DateTime.tryParse(sb) ?? settleBy;

    return ReceivableItem(
      id: json['id'].toString(),
      amountCents: (json['amount_cents'] as num?)?.toInt() ?? 0,
      settleBy: settleBy,
      status: json['status'] as String? ?? '',
      debtorDisplayName: json['debtor_display_name'] as String?,
      creditorDisplayName: json['creditor_display_name'] as String?,
      settledAt: settled,
    );
  }
}
