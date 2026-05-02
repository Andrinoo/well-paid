import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/authorized_dio_provider.dart';
import '../data/receivables_repository.dart';

final receivablesRepositoryProvider = Provider<ReceivablesRepository>(
  (ref) => ReceivablesRepository(ref.watch(dioProvider)),
);

final receivablesBundleProvider =
    FutureProvider.autoDispose<ReceivablesParseResult>((ref) async {
  return ref.watch(receivablesRepositoryProvider).listAll();
});
