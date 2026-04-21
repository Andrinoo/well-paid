package com.wellpaid.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import com.wellpaid.navigation.MAIN_SHELL_SELECT_TAB
import com.wellpaid.R
import kotlinx.coroutines.flow.distinctUntilChanged
import com.wellpaid.ui.emergency.EmergencyReserveContent
import com.wellpaid.ui.emergency.EmergencyReserveViewModel
import com.wellpaid.ui.expenses.ExpensesListContent
import com.wellpaid.ui.expenses.ExpensesViewModel
import com.wellpaid.ui.expenses.dueUrgencyColorOnLight
import com.wellpaid.ui.goals.GoalsListContent
import com.wellpaid.ui.goals.GoalsViewModel
import com.wellpaid.ui.home.HomeDashboardContent
import com.wellpaid.ui.home.HomeViewModel
import com.wellpaid.ui.incomes.IncomesListContent
import com.wellpaid.ui.incomes.IncomesViewModel
import com.wellpaid.ui.shopping.ShoppingListsViewModel
import com.wellpaid.ui.theme.WellPaidCreamMuted
import com.wellpaid.ui.theme.WellPaidGold
import com.wellpaid.ui.theme.WellPaidNavy
import com.wellpaid.ui.theme.wellPaidScreenHorizontalPadding

private data class MainTab(val labelRes: Int, val icon: ImageVector)

/** Curva tipo Material / motion moderna (ease padrão). */
private val TabContentEnterEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
private val TabContentExitEasing = CubicBezierEasing(0.4f, 0f, 1f, 1f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainShellScreen(
    mainRouteEntry: NavBackStackEntry,
    onLoggedOut: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenExpenseNew: () -> Unit,
    onOpenExpenseDetail: (String) -> Unit,
    onOpenInstallmentPlan: (String) -> Unit,
    onOpenIncomeNew: () -> Unit,
    onOpenIncomeDetail: (String) -> Unit,
    onOpenGoalNew: () -> Unit,
    onOpenGoalDetail: (String) -> Unit,
    onOpenGoalEdit: (String) -> Unit,
    onOpenShoppingLists: () -> Unit,
    onOpenAnnouncements: () -> Unit,
    onOpenReceivables: () -> Unit,
    onOpenInvestments: () -> Unit,
    onOpenEmergencyReserveNew: () -> Unit,
    onOpenEmergencyPlanDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainShellViewModel = hiltViewModel(),
) {
    LaunchedEffect(viewModel) {
        viewModel.loggedOutEvents.collect {
            onLoggedOut()
        }
    }

    // Prefetch: scope ViewModels to Main (or Activity for shopping lists) so init { refresh() }
    // runs as soon as the shell is shown — lists are ready when the user opens each tab/screen.
    // Home first: dashboard + utilizador competem com menos ecrãs em paralelo.
    val homeViewModel = hiltViewModel<HomeViewModel>(mainRouteEntry)
    val expensesViewModel = hiltViewModel<ExpensesViewModel>(mainRouteEntry)
    val incomesViewModel = hiltViewModel<IncomesViewModel>(mainRouteEntry)
    val goalsViewModel = hiltViewModel<GoalsViewModel>(mainRouteEntry)
    val emergencyViewModel = hiltViewModel<EmergencyReserveViewModel>(mainRouteEntry)
    @Suppress("UNUSED_VARIABLE")
    val shoppingListsPrefetch = hiltViewModel<ShoppingListsViewModel>(mainRouteEntry)
    val receivablesBadgeVm = hiltViewModel<MainReceivablesBadgeViewModel>(mainRouteEntry)
    val receivablesBadge by receivablesBadgeVm.state.collectAsStateWithLifecycle()

    DisposableEffect(mainRouteEntry) {
        val owner = mainRouteEntry.lifecycle
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                receivablesBadgeVm.refresh()
            }
        }
        owner.addObserver(obs)
        onDispose { owner.removeObserver(obs) }
    }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var shortcutsExpanded by rememberSaveable { mutableStateOf(false) }

    fun navigateToExpensesPending() {
        mainRouteEntry.savedStateHandle["pending_expense_status"] = "pending"
        selectedTab = 1
    }

    val tabSwipeModifier = rememberMainShellTabSwipeModifier(
        enabled = selectedTab in 1..4,
        currentTabIndex = selectedTab,
        onNavigateHome = { selectedTab = 0 },
        onNavigateNext = {
            selectedTab = if (selectedTab == 4) 0 else selectedTab + 1
        },
    )

    LaunchedEffect(mainRouteEntry) {
        snapshotFlow {
            mainRouteEntry.savedStateHandle.get<Int>(MAIN_SHELL_SELECT_TAB) ?: -1
        }.distinctUntilChanged().collect { tab ->
            if (tab in 0..4) {
                selectedTab = tab
                mainRouteEntry.savedStateHandle.remove<Int>(MAIN_SHELL_SELECT_TAB)
            }
        }
    }

    val tabs = listOf(
        MainTab(R.string.tab_home, Icons.Filled.Home),
        MainTab(R.string.tab_expenses, Icons.AutoMirrored.Filled.List),
        MainTab(R.string.tab_incomes, Icons.Filled.Payments),
        MainTab(R.string.tab_goals, Icons.Filled.Flag),
        MainTab(R.string.tab_emergency, Icons.Filled.Shield),
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (selectedTab != 0) {
                val title = when (selectedTab) {
                    1 -> stringResource(R.string.main_title_expenses)
                    2 -> stringResource(R.string.main_title_incomes)
                    3 -> stringResource(R.string.main_title_goals)
                    4 -> stringResource(R.string.main_title_emergency)
                    else -> stringResource(R.string.app_name)
                }
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White,
                    ),
                    title = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { selectedTab = 0 }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.main_back_to_home),
                            )
                        }
                    },
                )
            }
        },
        bottomBar = {
            val bottomBarTopCorner = 8.dp
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = WellPaidNavy.copy(alpha = 0.07f),
                        shape = RoundedCornerShape(topStart = bottomBarTopCorner, topEnd = bottomBarTopCorner),
                    ),
                color = WellPaidCreamMuted,
                shape = RoundedCornerShape(topStart = bottomBarTopCorner, topEnd = bottomBarTopCorner),
                tonalElevation = 1.dp,
                shadowElevation = 6.dp,
            ) {
                Column(Modifier.navigationBarsPadding()) {
                    if (shortcutsExpanded) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(onClick = {
                                navigateToExpensesPending()
                                shortcutsExpanded = false
                            }) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Filled.CreditCard,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        stringResource(R.string.home_shortcut_pending_pay),
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                            }
                            TextButton(onClick = {
                                shortcutsExpanded = false
                                onOpenShoppingLists()
                            }) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Filled.ShoppingCart,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        stringResource(R.string.home_shortcut_shopping_lists),
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                            }
                            TextButton(onClick = {
                                shortcutsExpanded = false
                                onOpenInvestments()
                            }) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Filled.Savings,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        stringResource(R.string.home_shortcut_investments),
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                            }
                        }
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            IconButton(onClick = { shortcutsExpanded = false }) {
                                Icon(
                                    Icons.Filled.KeyboardArrowDown,
                                    contentDescription = stringResource(R.string.home_shortcuts_collapse),
                                )
                            }
                        }
                    } else {
                        val expandInteraction = remember { MutableInteractionSource() }
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(28.dp)
                                .clickable(
                                    interactionSource = expandInteraction,
                                    indication = null,
                                    onClick = { shortcutsExpanded = true },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.KeyboardArrowUp,
                                contentDescription = stringResource(R.string.home_shortcuts_expand),
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    if (receivablesBadge.showInBottomBar) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 0.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = onOpenReceivables) {
                                BadgedBox(
                                    badge = {
                                        val u = receivablesBadge.worstUrgency
                                        val bg = if (u != null) {
                                            dueUrgencyColorOnLight(u)
                                        } else {
                                            MaterialTheme.colorScheme.error
                                        }
                                        Badge(
                                            containerColor = bg,
                                            contentColor = Color.White,
                                        ) {
                                            Text(
                                                text = if (receivablesBadge.openCount > 99) {
                                                    "99+"
                                                } else {
                                                    receivablesBadge.openCount.toString()
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 10.sp,
                                            )
                                        }
                                    },
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.AccountBalanceWallet,
                                        contentDescription = stringResource(R.string.main_nav_receivables_cd),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(26.dp),
                                    )
                                }
                            }
                        }
                    }
                    NavigationBar(
                        containerColor = WellPaidCreamMuted,
                    ) {
                        tabs.forEachIndexed { index, tab ->
                            val label = stringResource(tab.labelRes)
                            NavigationBarItem(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = label,
                                    )
                                },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                alwaysShowLabel = true,
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = WellPaidGold.copy(alpha = 0.38f),
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            )
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        val layoutDirection = LocalLayoutDirection.current
        // Home: fundo navy até ao topo (edge-to-edge); o conteúdo usa statusBarsPadding no ecrã.
        val contentPadding = PaddingValues(
            start = innerPadding.calculateLeftPadding(layoutDirection),
            top = if (selectedTab == 0) 0.dp else innerPadding.calculateTopPadding(),
            end = innerPadding.calculateRightPadding(layoutDirection),
            bottom = innerPadding.calculateBottomPadding(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .then(
                    if (selectedTab == 0) Modifier else Modifier
                        .wellPaidScreenHorizontalPadding()
                        .padding(vertical = 8.dp),
                ),
            contentAlignment = Alignment.TopStart,
        ) {
            AnimatedContent(
                targetState = selectedTab,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    val forward =
                        targetState > initialState || (initialState == 4 && targetState == 0)
                    val enterMs = 340
                    val exitMs = 260
                    (slideInHorizontally(
                        animationSpec = tween(enterMs, easing = TabContentEnterEasing),
                        initialOffsetX = { full -> if (forward) full else -full },
                    ) + fadeIn(
                        animationSpec = tween(enterMs, easing = TabContentEnterEasing),
                    )) togetherWith (
                        slideOutHorizontally(
                            animationSpec = tween(exitMs, easing = TabContentExitEasing),
                            targetOffsetX = { full -> if (forward) -full else full },
                        ) + fadeOut(
                            animationSpec = tween(exitMs, easing = TabContentExitEasing),
                        )
                    )
                },
                label = "main_shell_tabs",
            ) { tab ->
                when (tab) {
                    0 -> HomeDashboardContent(
                        modifier = Modifier.fillMaxSize(),
                        mainRouteEntry = mainRouteEntry,
                        onOpenSettings = onOpenSettings,
                        onOpenAnnouncements = onOpenAnnouncements,
                        viewModel = homeViewModel,
                    )
                    1 -> ExpensesListContent(
                        mainRouteEntry = mainRouteEntry,
                        onExpenseClick = onOpenExpenseDetail,
                        onOpenInstallmentPlan = onOpenInstallmentPlan,
                        onNewExpense = onOpenExpenseNew,
                        modifier = Modifier.fillMaxSize(),
                        tabSwipe = tabSwipeModifier,
                        viewModel = expensesViewModel,
                    )
                    2 -> IncomesListContent(
                        mainRouteEntry = mainRouteEntry,
                        onIncomeClick = onOpenIncomeDetail,
                        onNewIncome = onOpenIncomeNew,
                        modifier = Modifier.fillMaxSize(),
                        tabSwipe = tabSwipeModifier,
                        viewModel = incomesViewModel,
                    )
                    3 -> GoalsListContent(
                        mainRouteEntry = mainRouteEntry,
                        onGoalClick = onOpenGoalDetail,
                        onEditGoal = onOpenGoalEdit,
                        onNewGoal = onOpenGoalNew,
                        modifier = Modifier.fillMaxSize(),
                        tabSwipe = tabSwipeModifier,
                        viewModel = goalsViewModel,
                    )
                    4 -> EmergencyReserveContent(
                        mainRouteEntry = mainRouteEntry,
                        modifier = Modifier.fillMaxSize(),
                        tabSwipe = tabSwipeModifier,
                        onOpenReserveNew = onOpenEmergencyReserveNew,
                        onOpenPlanDetail = onOpenEmergencyPlanDetail,
                        viewModel = emergencyViewModel,
                    )
                    else -> Text(
                        text = stringResource(
                            R.string.main_tab_placeholder,
                            stringResource(tabs[tab].labelRes),
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
