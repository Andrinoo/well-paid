import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/authorized_dio_provider.dart';
import '../data/goals_repository.dart';
import '../domain/goal_item.dart';

final goalsRepositoryProvider = Provider<GoalsRepository>(
  (ref) => GoalsRepository(ref.watch(dioProvider)),
);

final goalsListProvider = FutureProvider.autoDispose<List<GoalItem>>(
  (ref) => ref.watch(goalsRepositoryProvider).listGoals(),
);
