import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/authorized_dio_provider.dart';
import '../data/user_profile_repository.dart';

final userProfileRepositoryProvider = Provider<UserProfileRepository>(
  (ref) => UserProfileRepository(ref.watch(dioProvider)),
);

final userMeProvider = FutureProvider.autoDispose<UserMe>((ref) async {
  return ref.watch(userProfileRepositoryProvider).getMe();
});
