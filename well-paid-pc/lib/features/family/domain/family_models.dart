class FamilyMemberItem {
  const FamilyMemberItem({
    required this.userId,
    required this.email,
    this.fullName,
    required this.role,
    this.isSelf = false,
  });

  final String userId;
  final String email;
  final String? fullName;
  final String role;
  final bool isSelf;

  factory FamilyMemberItem.fromJson(Map<String, dynamic> j) {
    return FamilyMemberItem(
      userId: j['user_id'] as String,
      email: j['email'] as String,
      fullName: j['full_name'] as String?,
      role: j['role'] as String,
      isSelf: j['is_self'] as bool? ?? false,
    );
  }

  bool get isOwner => role == 'owner';
}

class FamilyOverview {
  const FamilyOverview({
    required this.id,
    required this.name,
    required this.members,
  });

  final String id;
  final String name;
  final List<FamilyMemberItem> members;

  factory FamilyOverview.fromJson(Map<String, dynamic> j) {
    final raw = j['members'] as List<dynamic>? ?? [];
    return FamilyOverview(
      id: j['id'] as String,
      name: j['name'] as String,
      members: raw
          .map((e) => FamilyMemberItem.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }
}

class FamilyMeResponse {
  const FamilyMeResponse({this.family});

  final FamilyOverview? family;

  factory FamilyMeResponse.fromJson(Map<String, dynamic> j) {
    final f = j['family'];
    return FamilyMeResponse(
      family: f == null
          ? null
          : FamilyOverview.fromJson(f as Map<String, dynamic>),
    );
  }
}

class FamilyInviteCreated {
  const FamilyInviteCreated({
    required this.token,
    required this.expiresAt,
    required this.inviteUrl,
  });

  final String token;
  final DateTime expiresAt;
  final String inviteUrl;

  factory FamilyInviteCreated.fromJson(Map<String, dynamic> j) {
    return FamilyInviteCreated(
      token: j['token'] as String,
      expiresAt: DateTime.parse(j['expires_at'] as String),
      inviteUrl: j['invite_url'] as String,
    );
  }
}
