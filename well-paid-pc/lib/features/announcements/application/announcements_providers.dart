import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/authorized_dio_provider.dart';
import '../data/announcement_row.dart';
import '../data/announcements_repository.dart';

final announcementsRepositoryProvider = Provider<AnnouncementsRepository>(
  (ref) => AnnouncementsRepository(ref.watch(dioProvider)),
);

final announcementsListProvider =
    FutureProvider.autoDispose<List<AnnouncementRow>>((ref) async {
  return ref.watch(announcementsRepositoryProvider).listActive();
});
