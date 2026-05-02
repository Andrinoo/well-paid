import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

class RouterRefresh extends ChangeNotifier {
  void ping() => notifyListeners();
}

final routerRefreshProvider = Provider<RouterRefresh>((ref) {
  final n = RouterRefresh();
  ref.onDispose(n.dispose);
  return n;
});
