import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

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
import 'features/goals/presentation/goals_placeholder_page.dart';
import 'features/home/presentation/home_page.dart';

final goRouterProvider = Provider<GoRouter>((ref) {
  final refresh = ref.watch(routerRefreshProvider);

  return GoRouter(
    initialLocation: '/splash',
    refreshListenable: refresh,
    redirect: (context, state) {
      final auth = ref.read(authNotifierProvider);
      final loc = state.matchedLocation;

      if (!auth.hydrated) {
        return loc == '/splash' ? null : '/splash';
      }

      if (loc == '/splash') {
        return auth.isAuthenticated ? '/home' : '/login';
      }

      const publicPaths = {
        '/login',
        '/register',
        '/forgot-password',
        '/reset-password',
      };
      final public = publicPaths.contains(loc);
      if (!auth.isAuthenticated && !public) return '/login';
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
    ],
  );
});
