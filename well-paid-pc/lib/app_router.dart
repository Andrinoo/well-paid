import 'package:flutter/material.dart';
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
import 'features/auth/presentation/verify_email_page.dart';
import 'features/emergency_reserve/presentation/emergency_plan_detail_page.dart';
import 'features/emergency_reserve/presentation/emergency_plans_list_page.dart';
import 'features/emergency_reserve/presentation/emergency_reserve_page.dart';
import 'features/emergency_reserve/presentation/emergency_reserve_plan_create_page.dart';
import 'features/announcements/presentation/announcements_page.dart';
import 'features/expenses/presentation/expense_detail_page.dart';
import 'features/expenses/presentation/installment_plan_page.dart';
import 'features/expenses/presentation/expense_edit_page.dart';
import 'features/expenses/presentation/expense_list_page.dart';
import 'features/expenses/presentation/new_expense_page.dart';
import 'features/expenses/presentation/to_pay_page.dart';
import 'features/family/presentation/family_page.dart';
import 'features/goals/presentation/goal_detail_page.dart';
import 'features/goals/presentation/goals_placeholder_page.dart';
import 'features/goals/presentation/new_goal_page.dart';
import 'features/home/presentation/home_page.dart';
import 'features/incomes/presentation/income_detail_page.dart';
import 'features/incomes/presentation/income_edit_page.dart';
import 'features/incomes/presentation/income_list_page.dart';
import 'features/incomes/presentation/new_income_page.dart';
import 'features/investments/presentation/investment_aporte_page.dart';
import 'features/investments/presentation/investments_page.dart';
import 'features/receivables/presentation/receivables_page.dart';
import 'features/settings/presentation/display_name_page.dart';
import 'features/settings/presentation/manage_categories_page.dart';
import 'features/settings/presentation/settings_page.dart';
import 'features/shell/presentation/main_shell.dart';
import 'features/shopping_lists/presentation/shopping_list_detail_page.dart';
import 'features/shopping_lists/presentation/shopping_list_new_route_page.dart';
import 'features/shopping_lists/presentation/shopping_lists_page.dart';

/// Root navigator — full-screen routes (forms, settings) use [parentNavigatorKey].
final GlobalKey<NavigatorState> rootNavigatorKey =
    GlobalKey<NavigatorState>(debugLabel: 'root');

final goRouterProvider = Provider<GoRouter>((ref) {
  final refresh = ref.watch(routerRefreshProvider);

  return GoRouter(
    navigatorKey: rootNavigatorKey,
    initialLocation: '/login',
    refreshListenable: refresh,
    redirect: (context, state) {
      final auth = ref.read(authNotifierProvider);
      final lock = ref.read(appLockNotifierProvider);
      final loc = state.matchedLocation;

      if (!auth.hydrated || !lock.hydrated) {
        const publicWhileHydrating = {
          '/login',
          '/register',
          '/verify-email',
          '/forgot-password',
          '/reset-password',
        };
        if (publicWhileHydrating.contains(loc)) return null;
        return '/login';
      }

      if (loc == '/unlock' && !auth.isAuthenticated) return '/login';

      const publicPaths = {
        '/login',
        '/register',
        '/verify-email',
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
        redirect: (context, state) => '/login',
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
        path: '/verify-email',
        builder: (context, state) {
          final q = state.uri.queryParameters;
          return VerifyEmailPage(
            initialEmail: q['email'],
            initialToken: q['token'],
          );
        },
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
      StatefulShellRoute.indexedStack(
        builder: (context, state, navigationShell) {
          return MainShell(navigationShell: navigationShell);
        },
        branches: [
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: '/home',
                builder: (context, state) => const HomePage(),
              ),
            ],
          ),
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: '/expenses',
                builder: (context, state) => ExpenseListPage(
                  initialStatus: state.uri.queryParameters['status'],
                ),
                routes: [
                  GoRoute(
                    path: 'new',
                    parentNavigatorKey: rootNavigatorKey,
                    builder: (context, state) => const NewExpensePage(),
                  ),
                  GoRoute(
                    path: ':expenseId',
                    parentNavigatorKey: rootNavigatorKey,
                    builder: (context, state) => ExpenseDetailPage(
                      expenseId: state.pathParameters['expenseId']!,
                    ),
                    routes: [
                      GoRoute(
                        path: 'edit',
                        parentNavigatorKey: rootNavigatorKey,
                        builder: (context, state) => ExpenseEditPage(
                          expenseId: state.pathParameters['expenseId']!,
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ],
          ),
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: '/incomes',
                builder: (context, state) => const IncomeListPage(),
                routes: [
                  GoRoute(
                    path: 'new',
                    parentNavigatorKey: rootNavigatorKey,
                    builder: (context, state) => const NewIncomePage(),
                  ),
                  GoRoute(
                    path: ':incomeId',
                    parentNavigatorKey: rootNavigatorKey,
                    builder: (context, state) => IncomeDetailPage(
                      incomeId: state.pathParameters['incomeId']!,
                    ),
                    routes: [
                      GoRoute(
                        path: 'edit',
                        parentNavigatorKey: rootNavigatorKey,
                        builder: (context, state) => IncomeEditPage(
                          incomeId: state.pathParameters['incomeId']!,
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ],
          ),
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: '/goals',
                builder: (context, state) => const GoalsPlaceholderPage(),
                routes: [
                  GoRoute(
                    path: 'new',
                    parentNavigatorKey: rootNavigatorKey,
                    builder: (context, state) => const NewGoalPage(),
                  ),
                  GoRoute(
                    path: ':goalId',
                    parentNavigatorKey: rootNavigatorKey,
                    builder: (context, state) => GoalDetailPage(
                      goalId: state.pathParameters['goalId']!,
                    ),
                  ),
                ],
              ),
            ],
          ),
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: '/emergency-reserve',
                builder: (context, state) => const EmergencyReservePage(),
              ),
            ],
          ),
        ],
      ),
      GoRoute(
        path: '/unlock',
        parentNavigatorKey: rootNavigatorKey,
        builder: (context, state) => const UnlockPage(),
      ),
      GoRoute(
        path: '/security',
        parentNavigatorKey: rootNavigatorKey,
        builder: (context, state) => const SecuritySettingsPage(),
      ),
      GoRoute(
        path: '/settings',
        parentNavigatorKey: rootNavigatorKey,
        builder: (context, state) => const SettingsPage(),
      ),
      GoRoute(
        path: '/family',
        parentNavigatorKey: rootNavigatorKey,
        builder: (context, state) => FamilyPage(
          initialInviteToken: state.uri.queryParameters['token'],
        ),
      ),
      GoRoute(
        path: '/to-pay',
        parentNavigatorKey: rootNavigatorKey,
        builder: (context, state) => const ToPayPage(),
      ),
      GoRoute(
        path: '/shopping-lists',
        parentNavigatorKey: rootNavigatorKey,
        builder: (context, state) => const ShoppingListsPage(),
        routes: [
          GoRoute(
            path: 'new',
            parentNavigatorKey: rootNavigatorKey,
            builder: (context, state) => const ShoppingListNewRoutePage(),
          ),
          GoRoute(
            path: ':listId',
            parentNavigatorKey: rootNavigatorKey,
            builder: (context, state) => ShoppingListDetailPage(
              listId: state.pathParameters['listId']!,
            ),
          ),
        ],
      ),
      GoRoute(
        path: '/announcements',
        parentNavigatorKey: rootNavigatorKey,
        builder: (context, state) => const AnnouncementsPage(),
      ),
      GoRoute(
        path: '/receivables',
        parentNavigatorKey: rootNavigatorKey,
        builder: (context, state) => const ReceivablesPage(),
      ),
      GoRoute(
        path: '/investments',
        parentNavigatorKey: rootNavigatorKey,
        builder: (context, state) => const InvestmentsPage(),
      ),
      GoRoute(
        path: '/investments/:positionId/aporte',
        parentNavigatorKey: rootNavigatorKey,
        builder: (context, state) => InvestmentAportePage(
          positionId: state.pathParameters['positionId']!,
        ),
      ),
      GoRoute(
        path: '/installment-plan/:groupId',
        parentNavigatorKey: rootNavigatorKey,
        builder: (context, state) => InstallmentPlanPage(
          groupId: state.pathParameters['groupId']!,
        ),
      ),
      GoRoute(
        path: '/emergency-plans',
        parentNavigatorKey: rootNavigatorKey,
        builder: (context, state) => const EmergencyPlansListPage(),
      ),
      GoRoute(
        path: '/emergency-plan/:planId',
        parentNavigatorKey: rootNavigatorKey,
        builder: (context, state) => EmergencyPlanDetailPage(
          planId: state.pathParameters['planId']!,
        ),
      ),
      GoRoute(
        path: '/emergency-plan/:planId/months',
        parentNavigatorKey: rootNavigatorKey,
        redirect: (context, state) =>
            '/emergency-plan/${state.pathParameters['planId']}',
      ),
      GoRoute(
        path: '/emergency-plan/:planId/status',
        parentNavigatorKey: rootNavigatorKey,
        redirect: (context, state) =>
            '/emergency-plan/${state.pathParameters['planId']}',
      ),
      GoRoute(
        path: '/emergency-reserve/plan-new',
        parentNavigatorKey: rootNavigatorKey,
        builder: (context, state) =>
            const EmergencyReservePlanCreatePage(),
      ),
      GoRoute(
        path: '/display-name',
        parentNavigatorKey: rootNavigatorKey,
        builder: (context, state) => const DisplayNamePage(),
      ),
      GoRoute(
        path: '/manage-categories',
        parentNavigatorKey: rootNavigatorKey,
        builder: (context, state) => const ManageCategoriesPage(),
      ),
    ],
  );
});
