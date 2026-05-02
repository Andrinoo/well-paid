import 'package:dio/dio.dart';

import '../../../core/network/dio_client.dart';
import '../domain/family_models.dart';

class FamilyRepository {
  FamilyRepository(this._dio);

  final Dio _dio;

  Future<FamilyMeResponse> getMe() async {
    try {
      final res = await _dio.get<Map<String, dynamic>>('/families/me');
      return FamilyMeResponse.fromJson(res.data ?? {});
    } on DioException catch (e, st) {
      logDioException(e, st);
      rethrow;
    }
  }

  Future<FamilyOverview> createFamily({String? name}) async {
    try {
      final res = await _dio.post<Map<String, dynamic>>(
        '/families/me',
        data: name == null || name.isEmpty ? <String, dynamic>{} : {'name': name},
      );
      return FamilyOverview.fromJson(res.data ?? {});
    } on DioException catch (e, st) {
      logDioException(e, st);
      rethrow;
    }
  }

  Future<FamilyOverview> updateName(String name) async {
    try {
      final res = await _dio.patch<Map<String, dynamic>>(
        '/families/me',
        data: {'name': name},
      );
      return FamilyOverview.fromJson(res.data ?? {});
    } on DioException catch (e, st) {
      logDioException(e, st);
      rethrow;
    }
  }

  Future<FamilyInviteCreated> createInvite() async {
    try {
      final res = await _dio.post<Map<String, dynamic>>('/families/me/invites');
      return FamilyInviteCreated.fromJson(res.data ?? {});
    } on DioException catch (e, st) {
      logDioException(e, st);
      rethrow;
    }
  }

  Future<FamilyOverview> join(String token) async {
    try {
      final res = await _dio.post<Map<String, dynamic>>(
        '/families/join',
        data: {'token': token.trim()},
      );
      return FamilyOverview.fromJson(res.data ?? {});
    } on DioException catch (e, st) {
      logDioException(e, st);
      rethrow;
    }
  }

  Future<void> removeMember(String userId) async {
    try {
      await _dio.delete<void>('/families/me/members/$userId');
    } on DioException catch (e, st) {
      logDioException(e, st);
      rethrow;
    }
  }

  Future<void> leave() async {
    try {
      await _dio.delete<void>('/families/me');
    } on DioException catch (e, st) {
      logDioException(e, st);
      rethrow;
    }
  }
}
