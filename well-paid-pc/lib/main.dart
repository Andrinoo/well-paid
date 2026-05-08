import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:hive_flutter/hive_flutter.dart';

import 'app_router.dart';
import 'core/config/api_config.dart';
import 'core/locale/app_locale_provider.dart';
import 'core/notifications/goal_stall_reminder_service.dart';
import 'core/theme/well_paid_colors.dart';
import 'core/theme/well_paid_money_typography.dart';
import 'core/theme/well_paid_radii.dart';
import 'core/theme/well_paid_semantic_colors.dart';
import 'core/theme/well_paid_shadows.dart';
import 'core/theme/well_paid_spacing.dart';
import 'features/app_lock/presentation/app_lifecycle_lock.dart';
import 'l10n/app_localizations.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  try {
    await dotenv.load(fileName: 'assets/app.env');
  } catch (e, st) {
    debugPrint('[dotenv] assets/app.env: $e\n$st');
  }
  debugPrint('[Well Paid] API base URL (resolved): ${ApiConfig.baseUrl}');
  await Hive.initFlutter();
  await Hive.openBox<dynamic>('expenses_cache');
  await Hive.openBox<dynamic>('expenses_sync_queue');
  await Hive.openBox<dynamic>('expenses_categories_cache');
  await Hive.openBox<dynamic>('goals_cache');
  await Hive.openBox<dynamic>('goals_sync_queue');
  await Hive.openBox<dynamic>('incomes_cache');
  await Hive.openBox<dynamic>('incomes_sync_queue');
  await Hive.openBox<dynamic>('incomes_categories_cache');

  await GoalStallReminderService.init();

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
    final localeAsync = ref.watch(appLocaleProvider);
    final appLocale = localeAsync.valueOrNull ?? const Locale('pt');

    final scheme =
        ColorScheme.fromSeed(
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

    final textTheme = GoogleFonts.interTextTheme().apply(
      bodyColor: WellPaidColors.navy,
      displayColor: WellPaidColors.navy,
    );

    return MaterialApp.router(
      title: 'Well Paid',
      locale: appLocale,
      supportedLocales: AppLocalizations.supportedLocales,
      localizationsDelegates: AppLocalizations.localizationsDelegates,
      builder: (context, child) =>
          AppLifecycleLock(child: child ?? const SizedBox.shrink()),
      theme: ThemeData(
        useMaterial3: true,
        scaffoldBackgroundColor: WellPaidColors.cream,
        colorScheme: scheme,
        textTheme: textTheme,
        extensions: <ThemeExtension<dynamic>>[
          const WellPaidSpacing(),
          const WellPaidRadii(),
          const WellPaidShadows(),
          WellPaidSemanticColors.light(),
          WellPaidMoneyTypography.fromTextTheme(textTheme),
        ],
        filledButtonTheme: FilledButtonThemeData(
          style: FilledButton.styleFrom(
            elevation: 0,
            backgroundColor: WellPaidColors.gold,
            foregroundColor: WellPaidColors.navy,
            disabledBackgroundColor: WellPaidColors.gold.withValues(
              alpha: 0.45,
            ),
            disabledForegroundColor: WellPaidColors.navy.withValues(
              alpha: 0.45,
            ),
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
          style: TextButton.styleFrom(foregroundColor: WellPaidColors.navy),
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
        appBarTheme: AppBarTheme(
          backgroundColor: WellPaidColors.cream,
          foregroundColor: WellPaidColors.navy,
          elevation: 0,
          scrolledUnderElevation: 0,
          surfaceTintColor: Colors.transparent,
          centerTitle: false,
          titleTextStyle: GoogleFonts.inter(
            fontSize: 18,
            fontWeight: FontWeight.w700,
            color: WellPaidColors.navy,
            letterSpacing: -0.2,
          ),
          iconTheme: IconThemeData(
            color: WellPaidColors.navy.withValues(alpha: 0.88),
          ),
        ),
        navigationRailTheme: NavigationRailThemeData(
          backgroundColor: WellPaidColors.creamMuted.withValues(alpha: 0.98),
          selectedIconTheme: IconThemeData(
            color: WellPaidColors.navy,
            size: 24,
          ),
          unselectedIconTheme: IconThemeData(
            color: WellPaidColors.navy.withValues(alpha: 0.45),
            size: 22,
          ),
          selectedLabelTextStyle: GoogleFonts.inter(
            fontSize: 11,
            fontWeight: FontWeight.w700,
            color: WellPaidColors.navy,
          ),
          unselectedLabelTextStyle: GoogleFonts.inter(
            fontSize: 11,
            fontWeight: FontWeight.w600,
            color: WellPaidColors.navy.withValues(alpha: 0.45),
          ),
        ),
      ),
      routerConfig: router,
    );
  }
}
