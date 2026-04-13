package com.wellpaid.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wellpaid.ui.auth.ForgotPasswordScreen
import com.wellpaid.ui.auth.ResetPasswordScreen
import com.wellpaid.ui.expenses.ExpenseFormScreen
import com.wellpaid.ui.goals.GoalDetailScreen
import com.wellpaid.ui.goals.GoalFormScreen
import com.wellpaid.ui.incomes.IncomeFormScreen
import com.wellpaid.ui.login.LoginScreen
import com.wellpaid.ui.main.MainShellScreen
import com.wellpaid.ui.register.RegisterScreen
import com.wellpaid.ui.register.VerifyEmailScreen
import com.wellpaid.ui.session.SessionViewModel
import com.wellpaid.ui.family.FamilyScreen
import com.wellpaid.ui.settings.DisplayNameScreen
import com.wellpaid.ui.settings.SettingsScreen
import com.wellpaid.ui.shopping.ShoppingListDetailScreen
import com.wellpaid.ui.shopping.ShoppingListFormScreen
import com.wellpaid.ui.shopping.ShoppingListsScreen
import com.wellpaid.ui.shopping.ShoppingListsViewModel

@Composable
fun WellPaidNavHost(
    modifier: Modifier = Modifier,
    sessionViewModel: SessionViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val startRoute by sessionViewModel.startRoute.collectAsStateWithLifecycle()

    val goMain: () -> Unit = {
        navController.navigate(NavRoutes.Main) {
            popUpTo(NavRoutes.Login) { inclusive = true }
        }
    }

    when (val route = startRoute) {
        null -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        else -> {
            val refreshExpensesAndPop: () -> Unit = {
                runCatching {
                    navController.getBackStackEntry(NavRoutes.Main).savedStateHandle["expense_list_dirty"] =
                        System.currentTimeMillis()
                }
                navController.popBackStack()
            }
            val refreshIncomesAndPop: () -> Unit = {
                runCatching {
                    navController.getBackStackEntry(NavRoutes.Main).savedStateHandle["income_list_dirty"] =
                        System.currentTimeMillis()
                }
                navController.popBackStack()
            }
            val refreshGoalsAndPop: () -> Unit = {
                runCatching {
                    navController.getBackStackEntry(NavRoutes.Main).savedStateHandle["goal_list_dirty"] =
                        System.currentTimeMillis()
                }
                navController.popBackStack()
            }
            val refreshGoalDetailAndPop: () -> Unit = {
                runCatching {
                    navController.previousBackStackEntry?.savedStateHandle?.set(
                        "goal_detail_refresh",
                        System.currentTimeMillis(),
                    )
                }
                navController.popBackStack()
            }
            val popGoalEditAfterDelete: () -> Unit = {
                navController.popBackStack()
                navController.popBackStack()
                runCatching {
                    navController.getBackStackEntry(NavRoutes.Main).savedStateHandle["goal_list_dirty"] =
                        System.currentTimeMillis()
                }
            }
            NavHost(
                navController = navController,
                startDestination = route,
                modifier = modifier,
            ) {
                composable(NavRoutes.Login) {
                    LoginScreen(
                        onNavigateToMain = goMain,
                        onExploreWithoutAccount = goMain,
                        onNavigateToRegister = {
                            navController.navigate(NavRoutes.Register)
                        },
                        onNavigateToForgotPassword = {
                            navController.navigate(NavRoutes.ForgotPassword)
                        },
                    )
                }
                composable(NavRoutes.ForgotPassword) {
                    ForgotPasswordScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToResetToken = {
                            navController.navigate(NavRoutes.resetPassword())
                        },
                    )
                }
                composable(
                    route = NavRoutes.ResetPasswordRoute,
                    arguments = listOf(
                        navArgument("token") {
                            type = NavType.StringType
                            defaultValue = ""
                        },
                    ),
                ) {
                    ResetPasswordScreen(
                        onNavigateToLogin = {
                            if (!navController.popBackStack(NavRoutes.Login, inclusive = false)) {
                                navController.navigate(NavRoutes.Login) {
                                    launchSingleTop = true
                                }
                            }
                        },
                        onNavigateBack = { navController.popBackStack() },
                    )
                }
                composable(NavRoutes.Register) {
                    RegisterScreen(
                        onNavigateToLogin = { navController.popBackStack() },
                        onRegisteredNavigateToVerify = { email ->
                            navController.navigate(NavRoutes.verifyEmail(email)) {
                                popUpTo(NavRoutes.Register) { inclusive = true }
                            }
                        },
                    )
                }
                composable(
                    route = NavRoutes.VerifyEmailRoute,
                    arguments = listOf(
                        navArgument("email") {
                            type = NavType.StringType
                            defaultValue = ""
                        },
                    ),
                ) {
                    VerifyEmailScreen(
                        onNavigateToMain = goMain,
                        onNavigateBack = { navController.popBackStack() },
                    )
                }
                composable(NavRoutes.Main) { mainEntry ->
                    MainShellScreen(
                        mainRouteEntry = mainEntry,
                        onLoggedOut = {
                            navController.navigate(NavRoutes.Login) {
                                popUpTo(NavRoutes.Main) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onOpenSettings = {
                            navController.navigate(NavRoutes.Settings)
                        },
                        onOpenDisplayName = {
                            navController.navigate(NavRoutes.DisplayName)
                        },
                        onOpenExpenseNew = {
                            navController.navigate(NavRoutes.ExpenseNew)
                        },
                        onOpenExpenseDetail = { id ->
                            navController.navigate(NavRoutes.expenseDetail(id))
                        },
                        onOpenIncomeNew = {
                            navController.navigate(NavRoutes.IncomeNew)
                        },
                        onOpenIncomeDetail = { id ->
                            navController.navigate(NavRoutes.incomeDetail(id))
                        },
                        onOpenGoalNew = {
                            navController.navigate(NavRoutes.GoalNew)
                        },
                        onOpenGoalDetail = { id ->
                            navController.navigate(NavRoutes.goalDetail(id))
                        },
                        onOpenShoppingLists = {
                            navController.navigate(NavRoutes.ShoppingLists)
                        },
                    )
                }
                composable(NavRoutes.ExpenseNew) {
                    ExpenseFormScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onFinishedNeedRefresh = refreshExpensesAndPop,
                    )
                }
                composable(
                    route = NavRoutes.ExpenseDetailRoute,
                    arguments = listOf(
                        navArgument("expenseId") {
                            type = NavType.StringType
                        },
                    ),
                ) {
                    ExpenseFormScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onFinishedNeedRefresh = refreshExpensesAndPop,
                    )
                }
                composable(NavRoutes.IncomeNew) {
                    IncomeFormScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onFinishedNeedRefresh = refreshIncomesAndPop,
                    )
                }
                composable(
                    route = NavRoutes.IncomeDetailRoute,
                    arguments = listOf(
                        navArgument("incomeId") {
                            type = NavType.StringType
                        },
                    ),
                ) {
                    IncomeFormScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onFinishedNeedRefresh = refreshIncomesAndPop,
                    )
                }
                composable(NavRoutes.GoalNew) {
                    GoalFormScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onFinishedNeedRefresh = refreshGoalsAndPop,
                    )
                }
                composable(
                    route = NavRoutes.GoalDetailRoute,
                    arguments = listOf(
                        navArgument("goalId") {
                            type = NavType.StringType
                        },
                    ),
                ) {
                    GoalDetailScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onEditGoal = { id ->
                            navController.navigate(NavRoutes.goalEdit(id))
                        },
                    )
                }
                composable(
                    route = NavRoutes.GoalEditRoute,
                    arguments = listOf(
                        navArgument("goalId") {
                            type = NavType.StringType
                        },
                    ),
                ) {
                    GoalFormScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onFinishedNeedRefresh = refreshGoalDetailAndPop,
                        onDeleted = popGoalEditAfterDelete,
                    )
                }
                composable(NavRoutes.Settings) {
                    SettingsScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onLoggedOut = {
                            navController.navigate(NavRoutes.Login) {
                                popUpTo(NavRoutes.Main) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onOpenFamily = { navController.navigate(NavRoutes.Family) },
                        onOpenDisplayName = { navController.navigate(NavRoutes.DisplayName) },
                    )
                }
                composable(NavRoutes.DisplayName) {
                    DisplayNameScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onSavedNotifyMain = {
                            runCatching {
                                navController.getBackStackEntry(NavRoutes.Main).savedStateHandle["user_profile_dirty"] =
                                    System.currentTimeMillis()
                            }
                        },
                    )
                }
                composable(NavRoutes.Family) {
                    FamilyScreen(onNavigateBack = { navController.popBackStack() })
                }
                composable(NavRoutes.ShoppingLists) {
                    val mainEntry = remember(navController) {
                        navController.getBackStackEntry(NavRoutes.Main)
                    }
                    ShoppingListsScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onOpenList = { id -> navController.navigate(NavRoutes.shoppingListDetail(id)) },
                        onEmptyListCreated = { id ->
                            navController.navigate(NavRoutes.shoppingListDetail(id))
                        },
                        viewModel = hiltViewModel<ShoppingListsViewModel>(mainEntry),
                    )
                }
                composable(NavRoutes.ShoppingListNew) {
                    ShoppingListFormScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onListCreated = { id ->
                            navController.navigate(NavRoutes.shoppingListDetail(id)) {
                                popUpTo(NavRoutes.ShoppingListNew) { inclusive = true }
                            }
                        },
                    )
                }
                composable(
                    route = NavRoutes.ShoppingListDetailRoute,
                    arguments = listOf(
                        navArgument("listId") { type = NavType.StringType },
                    ),
                ) {
                    ShoppingListDetailScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onOpenExpense = { expenseId ->
                            navController.navigate(NavRoutes.expenseDetail(expenseId))
                        },
                    )
                }
            }
        }
    }
}
