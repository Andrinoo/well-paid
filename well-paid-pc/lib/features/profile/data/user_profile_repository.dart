import 'package:dio/dio.dart';

class UserMe {
  const UserMe({
    required this.email,
    this.fullName,
    this.displayName,
    this.familyModeEnabled = false,
  });

  final String email;
  final String? fullName;
  final String? displayName;
  final bool familyModeEnabled;

  factory UserMe.fromJson(Map<String, dynamic> json) {
    return UserMe(
      email: json['email'] as String? ?? '',
      fullName: json['full_name'] as String?,
      displayName: json['display_name'] as String?,
      familyModeEnabled: json['family_mode_enabled'] as bool? ?? false,
    );
  }
}

class UserProfileRepository {
  UserProfileRepository(this._dio);

  final Dio _dio;

  Future<UserMe> getMe() async {
    final res = await _dio.get<Map<String, dynamic>>('/auth/me');
    return UserMe.fromJson(res.data!);
  }

  Future<UserMe> patchProfile({String? displayName}) async {
    final res = await _dio.patch<Map<String, dynamic>>(
      '/auth/me',
      data: {
        if (displayName != null) 'display_name': displayName,
      },
    );
    return UserMe.fromJson(res.data!);
  }
}
