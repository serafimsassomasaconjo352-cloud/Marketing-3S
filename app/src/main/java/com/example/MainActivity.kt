package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.CfoAiRepository
import com.example.data.FinancialRepository
import com.example.firebase.configureAppCheck
import com.example.firebase.performSignOut
import com.example.model.UiState
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CashflowScreen
import com.example.ui.screens.CfoAdvisorScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.InvestmentsScreen
import com.example.ui.theme.RumoCfoTheme
import com.example.viewmodel.CfoViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

const val ROUTE_DASHBOARD = "dashboard"
const val ROUTE_CASHFLOW = "cashflow"
const val ROUTE_INVESTMENTS = "investments"
const val ROUTE_ADVISOR = "advisor"

private data class NavTabItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // App Check MUST be configured with launch intent before any Compose UI or ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            RumoCfoTheme {
                RumoCfoRootApp()
            }
        }
    }
}

@Composable
fun RumoCfoRootApp() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        val uid = user.uid
        key(uid) {
            RumoCfoAuthenticatedShell(
                currentUserId = uid,
                onSignedOut = { currentUser = null }
            )
        }
    }
}

@Composable
private fun RumoCfoAuthenticatedShell(
    currentUserId: String,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: CfoViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                CfoViewModel(
                    repository = FinancialRepository(db),
                    aiRepository = CfoAiRepository(),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val planState by viewModel.planState.collectAsStateWithLifecycle()
    val transactionsState by viewModel.transactionsState.collectAsStateWithLifecycle()
    val investmentsState by viewModel.investmentsState.collectAsStateWithLifecycle()
    val reportsState by viewModel.reportsState.collectAsStateWithLifecycle()
    val metrics by viewModel.metricsState.collectAsStateWithLifecycle()
    val feedbackMessage by viewModel.feedbackMessage.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val chatBusy by viewModel.chatBusy.collectAsStateWithLifecycle()
    val reportGenState by viewModel.reportGenState.collectAsStateWithLifecycle()
    val investmentReviewState by viewModel.investmentReviewState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(feedbackMessage) {
        val msg = feedbackMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_DASHBOARD

    val tabs = remember {
        listOf(
            NavTabItem(
                route = ROUTE_DASHBOARD,
                labelRes = R.string.nav_dashboard,
                selectedIcon = Icons.Filled.Dashboard,
                unselectedIcon = Icons.Outlined.Dashboard,
                testTag = "nav_dashboard"
            ),
            NavTabItem(
                route = ROUTE_CASHFLOW,
                labelRes = R.string.nav_cashflow,
                selectedIcon = Icons.Filled.AccountBalanceWallet,
                unselectedIcon = Icons.Outlined.AccountBalanceWallet,
                testTag = "nav_cashflow"
            ),
            NavTabItem(
                route = ROUTE_INVESTMENTS,
                labelRes = R.string.nav_investments,
                selectedIcon = Icons.Filled.TrendingUp,
                unselectedIcon = Icons.Outlined.TrendingUp,
                testTag = "nav_investments"
            ),
            NavTabItem(
                route = ROUTE_ADVISOR,
                labelRes = R.string.nav_advisor,
                selectedIcon = Icons.Filled.AutoAwesome,
                unselectedIcon = Icons.Outlined.AutoAwesome,
                testTag = "nav_advisor"
            )
        )
    }

    fun navigateTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 700.dp

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(windowInsets = WindowInsets.navigationBars) {
                        tabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            val label = stringResource(tab.labelRes)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail {
                        tabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            val label = stringResource(tab.labelRes)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_DASHBOARD,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(ROUTE_DASHBOARD) {
                        DashboardScreen(
                            metrics = metrics,
                            plan = (planState as? UiState.Success)?.data,
                            isSavingPlan = isSaving,
                            onSavePlan = { plan, onDone -> viewModel.savePlan(plan, onDone) },
                            onNavigateToCashflow = { navigateTab(ROUTE_CASHFLOW) },
                            onNavigateToInvestments = { navigateTab(ROUTE_INVESTMENTS) },
                            onNavigateToAdvisor = { navigateTab(ROUTE_ADVISOR) },
                            onSignOut = {
                                performSignOut(
                                    credentialManager = credentialManager,
                                    scope = scope,
                                    onSignOutComplete = onSignedOut
                                )
                            }
                        )
                    }
                    composable(ROUTE_CASHFLOW) {
                        CashflowScreen(
                            transactionsState = transactionsState,
                            isSaving = isSaving,
                            onAddTransaction = { tx, onDone -> viewModel.addTransaction(tx, onDone) },
                            onToggleSettled = { tx -> viewModel.toggleTransactionSettled(tx) },
                            onDeleteTransaction = { id -> viewModel.deleteTransaction(id) }
                        )
                    }
                    composable(ROUTE_INVESTMENTS) {
                        InvestmentsScreen(
                            metrics = metrics,
                            investmentsState = investmentsState,
                            reviewState = investmentReviewState,
                            isSaving = isSaving,
                            onAddInvestment = { item, onDone -> viewModel.addInvestment(item, onDone) },
                            onUpdateInvestmentStatus = { item, status, actual ->
                                viewModel.updateInvestmentStatus(item, status, actual)
                            },
                            onDeleteInvestment = { id -> viewModel.deleteInvestment(id) },
                            onEvaluateWithAi = { item -> viewModel.evaluateInvestmentWithAi(item) },
                            onClearReview = { viewModel.clearInvestmentReview() }
                        )
                    }
                    composable(ROUTE_ADVISOR) {
                        CfoAdvisorScreen(
                            chatMessages = chatMessages,
                            chatBusy = chatBusy,
                            reportsState = reportsState,
                            reportGenState = reportGenState,
                            onSendChatMessage = { q -> viewModel.sendCfoChatMessage(q) },
                            onGenerateReport = { period -> viewModel.generateAndSavePeriodReport(period) },
                            onDeleteReport = { id -> viewModel.deleteReport(id) },
                            onClearReportGenState = { viewModel.clearReportGenState() }
                        )
                    }
                }
            }
        }
    }
}
