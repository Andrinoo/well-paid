import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:hive_flutter/hive_flutter.dart';

import 'app_router.dart';
import 'core/theme/well_paid_colors.dart';
import 'features/app_lock/presentation/app_lifecycle_lock.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await Hive.initFlutter();
  await Hive.openBox<dynamic>('expenses_cache');
  await Hive.openBox<dynamic>('expenses_sync_queue');
  await Hive.openBox<dynamic>('expenses_categories_cache');

  FlutterError.onError = (FlutterErrorDetails details) {
    FlutterError.presentError(details);
    debugPrint('[FlutterError] ${details.exceptionAsString()}');
    if (details.stack != null) {
      debugPrint(details.stack.toString());
    }
  };

  PlatformDispatcher.instance.onError = (error, stack) {
    debugPrint('[Uncaught] $error');
    debugPrint(stack.toString());
    return true;
  };

  runApp(const ProviderScope(child: WellPaidApp()));
}

class WellPaidApp extends ConsumerWidget {
  const WellPaidApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final router = ref.watch(goRouterProvider);

    final scheme = ColorScheme.fromSeed(
      seedColor: WellPaidColors.navy,
      brightness: Brightness.light,
    ).copyWith(
      primary: WellPaidColors.navy,
      onPrimary: Colors.white,
      secondary: WellPaidColors.gold,
      onSecondary: WellPaidColors.navy,
      surface: WellPaidColors.cream,
      onSurface: WellPaidColors.navy,
      onSurfaceVariant: WellPaidColors.navy.withValues(alpha: 0.62),
      outline: WellPaidColors.navy.withValues(alpha: 0.2),
    );

    return MaterialApp.router(
      title: 'Well Paid',
      builder: (context, child) => AppLifecycleLock(
        child: child ?? const SizedBox.shrink(),
      ),
      theme: ThemeData(
        useMaterial3: true,
        scaffoldBackgroundColor: WellPaidColors.cream,
        colorScheme: scheme,
        filledButtonTheme: FilledButtonThemeData(
          style: FilledButton.styleFrom(
            elevation: 0,
            backgroundColor: WellPaidColors.gold,
            foregroundColor: WellPaidColors.navy,
            disabledBackgroundColor: WellPaidColors.gold.withValues(alpha: 0.45),
            disabledForegroundColor: WellPaidColors.navy.withValues(alpha: 0.45),
            padding: const EdgeInsets.symmetric(vertical: 16, horizontal: 22),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(14),
            ),
            textStyle: const TextStyle(
              fontWeight: FontWeight.w700,
              letterSpacing: 0.25,
            ),
          ),
        ),
        textButtonTheme: TextButtonThemeData(
          style: TextButton.styleFrom(
            foregroundColor: WellPaidColors.navy,
          ),
        ),
        inputDecorationTheme: InputDecorationTheme(
          filled: true,
          fillColor: WellPaidColors.creamMuted.withValues(alpha: 0.85),
          border: OutlineInputBorder(
            borderRadius: BorderRadius.circular(14),
            borderSide: BorderSide(
              color: WellPaidColors.navy.withValues(alpha: 0.14),
            ),
          ),
          enabledBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(14),
            borderSide: BorderSide(
              color: WellPaidColors.navy.withValues(alpha: 0.14),
            ),
          ),
          focusedBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(14),
            borderSide: const BorderSide(color: WellPaidColors.gold, width: 2),
          ),
        ),
        appBarTheme: const AppBarTheme(
          backgroundColor: WellPaidColors.navy,
          foregroundColor: Colors.white,
          elevation: 0,
          centerTitle: true,
        ),
      ),
      routerConfig: router,
    );
  }
}
