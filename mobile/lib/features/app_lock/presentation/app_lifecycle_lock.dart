import 'package:flutter/widgets.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../application/app_lock_notifier.dart';

/// Bloqueia a sessão local quando a app vai para segundo plano (se PIN activo).
class AppLifecycleLock extends ConsumerStatefulWidget {
  const AppLifecycleLock({super.key, required this.child});

  final Widget child;

  @override
  ConsumerState<AppLifecycleLock> createState() => _AppLifecycleLockState();
}

class _AppLifecycleLockState extends ConsumerState<AppLifecycleLock>
    with WidgetsBindingObserver {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    // `paused`: segundo plano típico. `hidden`: app já não visível (ex.: multitarefa).
    if (state == AppLifecycleState.paused ||
        state == AppLifecycleState.hidden) {
      ref.read(appLockNotifierProvider.notifier).lockSessionIfEnabled();
    }
  }

  @override
  Widget build(BuildContext context) => widget.child;
}
