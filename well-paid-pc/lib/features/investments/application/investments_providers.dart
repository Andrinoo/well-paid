import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/authorized_dio_provider.dart';
import '../data/investment_models.dart';
import '../data/investments_repository.dart';

final investmentsRepositoryProvider = Provider<InvestmentsRepository>(
  (ref) => InvestmentsRepository(ref.watch(dioProvider)),
);

final investmentOverviewProvider =
    FutureProvider.autoDispose<InvestmentOverview>((ref) async {
  return ref.watch(investmentsRepositoryProvider).fetchOverview();
});

final investmentPositionsProvider =
    FutureProvider.autoDispose<List<InvestmentPosition>>((ref) async {
  return ref.watch(investmentsRepositoryProvider).listPositions();
});
