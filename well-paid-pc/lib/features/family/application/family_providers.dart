import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/authorized_dio_provider.dart';
import '../data/family_repository.dart';
import '../domain/family_models.dart';

final familyRepositoryProvider = Provider<FamilyRepository>(
  (ref) => FamilyRepository(ref.watch(dioProvider)),
);

final familyMeProvider = FutureProvider.autoDispose<FamilyMeResponse>(
  (ref) => ref.watch(familyRepositoryProvider).getMe(),
);
