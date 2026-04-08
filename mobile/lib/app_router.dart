import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'features/app_lock/application/app_lock_notifier.dart';
import 'features/app_lock/presentation/security_settings_page.dart';
import 'features/app_lock/presentation/unlock_page.dart';
import 'features/auth/application/auth_notifier.dart';
import 'features/auth/application/router_refresh.dart';
import 'features/auth/presentation/forgot_password_page.dart';
import 'features/auth/presentation/login_page.dart';
import 'features/auth/presentation/register_page.dart';
import 'features/auth/presentation/reset_password_page.dart';
import 'features/auth/presentation/splash_page.dart';
import 'features/expenses/presentation/expense_detail_page.dart';
import 'features/expenses/presentation/expense_edit_page.dart';
import 'features/expenses/presentation/expense_list_page.dart';
import 'features/expenses/presentation/new_expense_page.dart';
import 'features/family/presentation/family_page.dart';
import 'features/goals/presentation/goals_placeholder_page.dart';
import 'features/home/presentation/home_page.dart';
import 'features/incomes/presentation/income_detail_page.dart';
import 'features/incomes/presentation/income_edit_page.dart';
import 'features/incomes/presentation/income_list_page.dart';
import 'features/incomes/presentation/new_income_page.dart';

final goRouterProvider = Provider<GoRouter>((ref) {
  final refresh = ref.watch(routerRefreshProvider);

  return GoRouter(
    initialLocation: '/splash',
    refreshListenable: refresh,
    redirect: (context, state) {
      final auth = ref.read(authNotifierProvider);
      final lock = ref.read(appLockNotifierProvider);
      final loc = state.matchedLocation;

      if (!auth.hydrated || !lock.hydrated) {
        return loc == '/splash' ? null : '/splash';
      }

      if (loc == '/splash') {
        if (!auth.isAuthenticated) return '/login';
        if (lock.pinEnabled && !lock.sessionUnlocked) return '/unlock';
        return '/home';
      }

      if (loc == '/unlock' && !auth.isAuthenticated) return '/login';

      const publicPaths = {
        '/login',
        '/register',
        '/forgot-password',
        '/reset-password',
      };
      final public = publicPaths.contains(loc);

      if (!auth.isAuthenticated) {
        if (public) return null;
        return '/login';
      }

      if (lock.pinEnabled && !lock.sessionUnlocked) {
        if (loc == '/unlock') return null;
        return '/unlock';
      }

      if (lock.sessionUnlocked && loc == '/unlock') {
        return '/home';
      }

      if (auth.isAuthenticated && public) return '/home';

      return null;
    },
    routes: [
      GoRoute(
        path: '/splash',
        builder: (context, state) => const SplashPage(),
      ),
      GoRoute(
        path: '/login',
        builder: (context, state) => const LoginPage(),
      ),
      GoRoute(
        path: '/register',
        builder: (context, state) => const RegisterPage(),
      ),
      GoRoute(
        path: '/forgot-password',
        builder: (context, state) => const ForgotPasswordPage(),
      ),
      GoRoute(
        path: '/reset-password',
        builder: (context, state) {
          final extra = state.extra;
          final fromExtra = extra is String ? extra : null;
          final fromQuery = state.uri.queryParameters['token'];
          return ResetPasswordPage(
            initialToken: fromQuery ?? fromExtra,
          );
        },
      ),
      GoRoute(
        path: '/home',
        builder: (context, state) => const HomePage(),
      ),
      GoRoute(
        path: '/expenses/new',
        builder: (context, state) => const NewExpensePage(),
      ),
      GoRoute(
        path: '/expenses/:expenseId/edit',
        builder: (context, state) => ExpenseEditPage(
          expenseId: state.pathParameters['expenseId']!,
        ),
      ),
      GoRoute(
        path: '/expenses/:expenseId',
        builder: (context, state) => ExpenseDetailPage(
          expenseId: state.pathParameters['expenseId']!,
        ),
      ),
      GoRoute(
        path: '/expenses',
        builder: (context, state) => ExpenseListPage(
          initialStatus: state.uri.queryParameters['status'],
        ),
      ),
      GoRoute(
        path: '/goals',
        builder: (context, state) => const GoalsPlaceholderPage(),
      ),
      GoRoute(
        path: '/family',
        builder: (context, state) => FamilyPage(
          initialInviteToken: state.uri.queryParameters['token'],
        ),
      ),
      GoRoute(
        path: '/unlock',
        builder: (context, state) => const UnlockPage(),
      ),
      GoRoute(
        path: '/security',
        builder: (context, state) => const SecuritySettingsPage(),
      ),
      GoRoute(
        path: '/incomes',
        builder: (context, state) => const IncomeListPage(),
      ),
      GoRoute(
        path: '/incomes/new',
        builder: (context, state) => const NewIncomePage(),
      ),
      GoRoute(
        path: '/incomes/:incomeId/edit',
        builder: (context, state) => IncomeEditPage(
          incomeId: state.pathParameters['incomeId']!,
        ),
      ),
      GoRoute(
        path: '/incomes/:incomeId',
        builder: (context, state) => IncomeDetailPage(
          incomeId: state.pathParameters['incomeId']!,
        ),
      ),
    ],
  );
});
