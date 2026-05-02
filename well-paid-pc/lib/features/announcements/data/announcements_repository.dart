import 'package:dio/dio.dart';

import 'announcement_row.dart';

class AnnouncementsRepository {
  AnnouncementsRepository(this._dio);

  final Dio _dio;

  Future<List<AnnouncementRow>> listActive({
    String placement = 'home_banner',
    int limit = 24,
  }) async {
    final res = await _dio.get<Map<String, dynamic>>(
      '/announcements/active',
      queryParameters: {'placement': placement, 'limit': limit},
    );
    final data = res.data;
    if (data == null) return [];
    final raw = data['items'];
    if (raw is! List<dynamic>) return [];
    return raw
        .map((e) => AnnouncementRow.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<void> markRead(String id) async {
    await _dio.post<void>('/announcements/$id/read');
  }

  Future<void> hide(String id) async {
    await _dio.post<void>('/announcements/$id/hide');
  }
}
