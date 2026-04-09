import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:hive/hive.dart';

import '../../../core/network/authorized_dio_provider.dart';
import '../data/goals_local_store.dart';
import '../data/goals_repository.dart';
import '../domain/goal_contribution_item.dart';
import '../domain/goal_item.dart';

final goalsLocalStoreProvider = Provider<GoalsLocalStore>((ref) {
  return GoalsLocalStore(
    Hive.box<dynamic>('goals_cache'),
    Hive.box<dynamic>('goals_sync_queue'),
  );
});

final goalsRepositoryProvider = Provider<GoalsRepository>(
  (ref) => GoalsRepository(
    ref.watch(dioProvider),
    ref.watch(goalsLocalStoreProvider),
  ),
);

final goalsListProvider = FutureProvider.autoDispose<List<GoalItem>>(
  (ref) async {
    final link = ref.keepAlive();
    final timer = Timer(const Duration(minutes: 5), link.close);
    ref.onDispose(timer.cancel);
    return ref.watch(goalsRepositoryProvider).listGoals();
  },
);

final goalProvider = FutureProvider.autoDispose.family<GoalItem, String>(
  (ref, id) => ref.watch(goalsRepositoryProvider).getGoal(id),
);

final goalContributionsProvider =
    FutureProvider.autoDispose.family<List<GoalContributionItem>, String>(
  (ref, id) => ref.watch(goalsRepositoryProvider).listContributions(id),
);
