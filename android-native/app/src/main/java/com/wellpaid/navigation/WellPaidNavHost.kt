package com.wellpaid.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.runtime.CompositionLocalProvider
import com.wellpaid.security.AppSecurityEntryPoint
import com.wellpaid.ui.security.AppLockScreen
import com.wellpaid.ui.security.SecurityScreen
import com.wellpaid.ui.theme.LocalPrivacyHideBalance
import dagger.hilt.android.EntryPointAccessors
import com.wellpaid.ui.auth.ForgotPasswordScreen
import com.wellpaid.ui.auth.ResetPasswordScreen
import com.wellpaid.ui.expenses.ExpenseFormScreen
import com.wellpaid.ui.expenses.InstallmentPlanScreen
import com.wellpaid.ui.goals.GoalDetailScreen
import com.wellpaid.ui.goals.GoalFormScreen
import com.wellpaid.ui.incomes.IncomeFormScreen
import com.wellpaid.ui.investments.InvestmentsAporteScreen
import com.wellpaid.ui.investments.InvestmentsScreen
import com.wellpaid.data.UiPreferencesRepository
import com.wellpaid.ui.SecureWindowPolicyEffect
import com.wellpaid.ui.emergency.EmergencyPlanDetailScreen
import com.wellpaid.ui.emergency.EmergencyPlanMonthBreakdownScreen
import com.wellpaid.ui.emergency.EmergencyReservePlanFormScreen
import com.wellpaid.ui.login.LoginScreen
import com.wellpaid.ui.main.MainShellScreen
import com.wellpaid.ui.register.RegisterScreen
import com.wellpaid.ui.register.VerifyEmailScreen
import com.wellpaid.ui.session.SessionViewModel
import com.wellpaid.ui.family.FamilyScreen
import com.wellpaid.ui.categories.ManageCategoriesScreen
import com.wellpaid.ui.settings.DisplayNameScreen
import com.wellpaid.ui.receivables.ReceivablesScreen
import com.wellpaid.ui.settings.SettingsScreen
import com.wellpaid.ui.shopping.ShoppingListDetailScreen
import com.wellpaid.ui.shopping.ShoppingListFormScreen
import com.wellpaid.ui.announcements.AnnouncementsScreen
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
            val refreshEmergencyAndPop: () -> Unit = {
                runCatching {
                    navController.getBackStackEntry(NavRoutes.Main).savedStateHandle["emergency_reserve_dirty"] =
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
                // Uma só operação: volta até ao Main (remove edição e, se existir, detalhe).
                // Dois popBackStack() seguidos falhavam quando a pilha era só Main → Editar (tela branca).
                val ok = navController.popBackStack(NavRoutes.Main, inclusive = false)
                if (!ok) {
                    navController.popBackStack()
                }
                runCatching {
                    navController.getBackStackEntry(NavRoutes.Main).savedStateHandle["goal_list_dirty"] =
                        System.currentTimeMillis()
                }
            }
            val context = LocalContext.current
            val securityManager = remember(context.applicationContext) {
                EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    AppSecurityEntryPoint::class.java,
                ).appSecurityManager()
            }
            val uiPreferencesRepository = remember(context.applicationContext) {
                UiPreferencesRepository(context.applicationContext)
            }
            var screenshotsAllowed by remember { mutableStateOf(false) }
            LaunchedEffect(uiPreferencesRepository) {
                uiPreferencesRepository.screenshotsAllowedFlow.collect { screenshotsAllowed = it }
            }
            SecureWindowPolicyEffect(screenshotsAllowed)
            val locked by securityManager.locked.collectAsStateWithLifecycle()
            val hidePrivacy by securityManager.privacyHideAmounts.collectAsStateWithLifecycle()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            LaunchedEffect(Unit) {
                securityManager.applyColdStartLockIfNeeded()
            }
            val showAppLock = locked && !isPublicAuthRoute(currentRoute)
            Box(modifier = modifier) {
                CompositionLocalProvider(LocalPrivacyHideBalance provides hidePrivacy) {
                    NavHost(
                        navController = navController,
                        startDestination = startRoute,
                        modifier = Modifier.fillMaxSize(),
                        enterTransition = {
                            fadeIn(animationSpec = tween(200))
                        },
                        exitTransition = {
                            fadeOut(animationSpec = tween(160))
                        },
                        popEnterTransition = {
                            fadeIn(animationSpec = tween(200))
                        },
                        popExitTransition = {
                            fadeOut(animationSpec = tween(160))
                        },
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
                        onOpenExpenseNew = {
                            navController.navigate(NavRoutes.ExpenseNew)
                        },
                        onOpenExpenseDetail = { id ->
                            navController.navigate(NavRoutes.expenseDetail(id))
                        },
                        onOpenInstallmentPlan = { groupId ->
                            navController.navigate(NavRoutes.installmentPlan(groupId))
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
                        onOpenGoalEdit = { id ->
                            navController.navigate(NavRoutes.goalEdit(id))
                        },
                        onOpenShoppingLists = {
                            navController.navigate(NavRoutes.ShoppingLists)
                        },
                        onOpenAnnouncements = {
                            navController.navigate(NavRoutes.Announcements)
                        },
                        onOpenReceivables = {
                            navController.navigate(NavRoutes.Receivables)
                        },
                        onOpenInvestments = {
                            navController.navigate(NavRoutes.Investments)
                        },
                        onOpenEmergencyReserveNew = {
                            navController.navigate(NavRoutes.EmergencyReserveNew)
                        },
                        onOpenEmergencyPlanDetail = { planId ->
                            navController.navigate(NavRoutes.emergencyPlanDetail(planId))
                        },
                    )
                }
                composable(NavRoutes.Announcements) {
                    AnnouncementsScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onEngagementChanged = {
                            runCatching {
                                navController.getBackStackEntry(NavRoutes.Main).savedStateHandle["announcements_dirty"] =
                                    System.currentTimeMillis()
                            }
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
                    route = NavRoutes.InstallmentPlanRoute,
                    arguments = listOf(
                        navArgument("groupId") { type = NavType.StringType },
                    ),
                ) {
                    InstallmentPlanScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onOpenExpenseDetail = { id ->
                            navController.navigate(NavRoutes.expenseDetail(id))
                        },
                        onPlanDeletedNavigateBack = {
                            runCatching {
                                navController.getBackStackEntry(NavRoutes.Main).savedStateHandle["expense_list_dirty"] =
                                    System.currentTimeMillis()
                            }
                            navController.popBackStack()
                        },
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
                        onGoalDeleted = {
                            runCatching {
                                navController.getBackStackEntry(NavRoutes.Main).savedStateHandle["goal_list_dirty"] =
                                    System.currentTimeMillis()
                            }
                            navController.popBackStack()
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
                        onOpenDisplayName = { navController.navigate(NavRoutes.DisplayName) },
                        onOpenFamily = { navController.navigate(NavRoutes.Family) },
                        onOpenSecurity = { navController.navigate(NavRoutes.Security) },
                        onOpenManageCategories = { navController.navigate(NavRoutes.ManageCategories) },
                    )
                }
                composable(NavRoutes.Receivables) {
                    ReceivablesScreen(onNavigateBack = { navController.popBackStack() })
                }
                composable(NavRoutes.Investments) {
                    InvestmentsScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onOpenAporte = { id ->
                            navController.navigate(NavRoutes.investmentAporte(id))
                        },
                    )
                }
                composable(
                    route = NavRoutes.InvestmentAporteRoute,
                    arguments = listOf(
                        navArgument("positionId") { type = NavType.StringType },
                    ),
                ) { aporteEntry ->
                    val posId = aporteEntry.arguments?.getString("positionId") ?: return@composable
                    val investEntry = remember {
                        runCatching { navController.getBackStackEntry(NavRoutes.Investments) }.getOrNull()
                    }
                    InvestmentsAporteScreen(
                        positionId = posId,
                        onNavigateBack = { navController.popBackStack() },
                        onAporteSuccess = { navController.popBackStack() },
                        viewModel = if (investEntry != null) {
                            hiltViewModel(investEntry)
                        } else {
                            hiltViewModel()
                        },
                    )
                }
                composable(NavRoutes.EmergencyReserveNew) {
                    val mainEntry = remember(navController) {
                        navController.getBackStackEntry(NavRoutes.Main)
                    }
                    EmergencyReservePlanFormScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onCreatedNeedRefresh = { informInvestments ->
                            refreshEmergencyAndPop()
                            if (informInvestments) {
                                navController.navigate(NavRoutes.Investments)
                            }
                        },
                        viewModel = hiltViewModel(mainEntry),
                    )
                }
                composable(
                    route = NavRoutes.EmergencyPlanDetailRoute,
                    arguments = listOf(
                        navArgument("planId") { type = NavType.StringType },
                    ),
                ) { backStackEntry ->
                    val planId = backStackEntry.arguments?.getString("planId") ?: return@composable
                    val mainEntry = remember(navController) {
                        navController.getBackStackEntry(NavRoutes.Main)
                    }
                    EmergencyPlanDetailScreen(
                        planId = planId,
                        onNavigateBack = { navController.popBackStack() },
                        onPlanDeletedNavigateBack = {
                            refreshEmergencyAndPop()
                        },
                        onOpenMonthlyProgress = {
                            navController.navigate(NavRoutes.emergencyPlanMonthBreakdown(planId))
                        },
                        onOpenInvestments = {
                            navController.navigate(NavRoutes.Investments)
                        },
                        viewModel = hiltViewModel(mainEntry),
                    )
                }
                composable(
                    route = NavRoutes.EmergencyPlanMonthBreakdownRoute,
                    arguments = listOf(
                        navArgument("planId") { type = NavType.StringType },
                    ),
                ) { monthEntry ->
                    val monthPlanId = monthEntry.arguments?.getString("planId") ?: return@composable
                    val mainEntry = remember(navController) {
                        navController.getBackStackEntry(NavRoutes.Main)
                    }
                    EmergencyPlanMonthBreakdownScreen(
                        planId = monthPlanId,
                        onNavigateBack = { navController.popBackStack() },
                        viewModel = hiltViewModel(mainEntry),
                    )
                }
                composable(NavRoutes.ManageCategories) {
                    ManageCategoriesScreen(onNavigateBack = { navController.popBackStack() })
                }
                composable(NavRoutes.Security) {
                    SecurityScreen(onNavigateBack = { navController.popBackStack() })
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
                        onSwipeNavigateToMainHome = {
                            runCatching {
                                mainEntry.savedStateHandle[MAIN_SHELL_SELECT_TAB] = 0
                            }
                            navController.popBackStack()
                        },
                        onOpenList = { id -> navController.navigate(NavRoutes.shoppingListDetail(id)) },
                        onListCreated = { id ->
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
                if (showAppLock) {
                    AppLockScreen(
                        manager = securityManager,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
}

private fun isPublicAuthRoute(route: String?): Boolean {
    if (route == null) return true
    if (route == NavRoutes.Login || route == NavRoutes.Register || route == NavRoutes.ForgotPassword) {
        return true
    }
    if (route.startsWith("verify_email")) return true
    if (route.startsWith("reset_password")) return true
    return false
}
