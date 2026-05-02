class AnnouncementRow {
  const AnnouncementRow({
    required this.id,
    required this.title,
    required this.body,
    required this.kind,
    this.ctaLabel,
    this.ctaUrl,
    this.userReadAt,
  });

  final String id;
  final String title;
  final String body;
  final String kind;
  final String? ctaLabel;
  final String? ctaUrl;
  final DateTime? userReadAt;

  factory AnnouncementRow.fromJson(Map<String, dynamic> json) {
    DateTime? readAt;
    final ur = json['user_read_at'];
    if (ur is String) {
      readAt = DateTime.tryParse(ur);
    }
    return AnnouncementRow(
      id: json['id'].toString(),
      title: json['title'] as String? ?? '',
      body: json['body'] as String? ?? '',
      kind: json['kind'] as String? ?? 'info',
      ctaLabel: json['cta_label'] as String?,
      ctaUrl: json['cta_url']?.toString(),
      userReadAt: readAt,
    );
  }
}
