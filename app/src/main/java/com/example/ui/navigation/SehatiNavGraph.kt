package com.example.ui.navigation

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.model.NotificationEntity
import com.example.data.model.OrderStatus
import com.example.data.repository.foldAdminFinance
import com.example.data.repository.foldEarnings
import com.example.data.repository.repeatBookingRate
import com.example.ui.components.BottomNavBar
import com.example.ui.components.SehatiDrawerContent
import com.example.ui.components.SehatiTopBar
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AdminFinanceScreen
import com.example.ui.screens.BookingConfirmScreen
import com.example.ui.screens.DailyCareScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MyOrdersScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.OrderSuccessScreen
import com.example.ui.screens.PaymentConfirmedScreen
import com.example.ui.screens.PaymentMethodScreen
import com.example.ui.screens.PaymentReviewScreen
import com.example.ui.screens.ProviderDashboardScreen
import com.example.ui.screens.ProviderDocsUploadScreen
import com.example.ui.screens.ProviderEarningsScreen
import com.example.ui.screens.ProviderPendingScreen
import com.example.ui.screens.ProviderProfileScreen
import com.example.ui.screens.ProviderRegisterScreen
import com.example.ui.screens.ProvidersListScreen
import com.example.ui.screens.RatingScreen
import com.example.ui.screens.UploadReceiptScreen
import com.example.ui.screens.UserProfileScreen
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.CareLoopViewModel
import com.example.ui.viewmodel.NotificationsViewModel
import com.example.ui.viewmodel.ProviderRegistrationViewModel
import com.example.ui.viewmodel.RatingViewModel
import com.example.ui.viewmodel.SehatiViewModel
import com.example.ui.viewmodel.UserRole
import kotlinx.coroutines.launch

@Composable
fun SehatiApp(
    authViewModel: AuthViewModel,
    viewModel: SehatiViewModel,
    activity: Activity,
    navController: NavHostController = rememberNavController(),
    openDailyCare: Boolean = false,
    onDailyCareOpened: () -> Unit = {},
    openOrderId: String? = null,
    onOrderIdOpened: () -> Unit = {}
) {
    val notificationsViewModel: NotificationsViewModel = viewModel()
    val ratingViewModel: RatingViewModel = viewModel()
    val registrationViewModel: ProviderRegistrationViewModel = viewModel()
    val careLoopViewModel: CareLoopViewModel = viewModel()

    val authState by authViewModel.uiState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val notificationsState by notificationsViewModel.uiState.collectAsState()
    val ratingState by ratingViewModel.uiState.collectAsState()
    val registrationState by registrationViewModel.uiState.collectAsState()
    val careState by careLoopViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    LaunchedEffect(uiState.notificationMessage) {
        uiState.notificationMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearNotification()
        }
    }

    LaunchedEffect(uiState.navigationEvent) {
        uiState.navigationEvent?.let { route ->
            navController.navigate(route)
            viewModel.consumeNavigationEvent()
        }
    }

    // Handle deep linking from system notification clicks
    LaunchedEffect(openOrderId, authState.isAuthenticated, uiState.orders) {
        val targetId = openOrderId
        if (!targetId.isNullOrEmpty() && authState.isAuthenticated) {
            val order = uiState.orders.firstOrNull { it.id == targetId }
            if (order != null) {
                viewModel.selectOrder(order)
                val route = when (order.status) {
                    OrderStatus.PAYMENT_CONFIRMED -> Routes.PAYMENT_CONFIRMED
                    OrderStatus.PAYMENT_UNDER_REVIEW -> Routes.PAYMENT_REVIEW
                    else -> Routes.ORDER_SUCCESS
                }
                navController.navigate(route) { launchSingleTop = true }
                onOrderIdOpened()
            }
        }
    }

    // The inbox listener needs a uid, and this graph is composed before sign-in completes. The
    // shared admin inbox is only readable by an admin, so the role has to be known too.
    LaunchedEffect(authState.isAuthenticated, authState.role) {
        if (authState.isAuthenticated) {
            com.example.service.SehatiMessagingService.registerCurrentToken()
            notificationsViewModel.refresh(isAdmin = authState.role == UserRole.ADMIN)
            viewModel.observeProviders()
        } else {
            notificationsViewModel.clearSession()
            viewModel.clearSession()
        }
    }

    LaunchedEffect(authState.uid) {
        careLoopViewModel.bindUser(authState.uid)
    }

    LaunchedEffect(openDailyCare, authState.isAuthenticated, authState.role) {
        if (openDailyCare && authState.isAuthenticated && authState.role == UserRole.PATIENT) {
            navController.navigate(Routes.DAILY_CARE) { launchSingleTop = true }
            onDailyCareOpened()
        }
    }

    // Orders are queried per role, so the feed can only start once the role is known.
    LaunchedEffect(authState.isAuthenticated, authState.role, authState.uid, authState.providerId) {
        if (authState.isAuthenticated) {
            viewModel.observeOrders(authState.role, authState.uid, authState.providerId)
        }
    }

    var lastSeenNotifId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(notificationsState.notifications) {
        val latest = notificationsState.notifications.firstOrNull()
        if (latest != null && !latest.read && latest.id != lastSeenNotifId) {
            if (lastSeenNotifId != null) {
                com.example.ui.viewmodel.OrderNotificationHelper.showNotification(
                    context = context,
                    title = latest.title,
                    body = latest.body,
                    orderId = latest.orderId
                )
            }
            lastSeenNotifId = latest.id
        }
    }

    // Role decides which graph is reachable. Signing in lands on that role's home, and any
    // destination the role has no tab for bounces straight back to it — a deep link or a back
    // stack left over from a role switch cannot leave someone on a screen meant for another role.
    LaunchedEffect(authState.isAuthenticated, authState.role, currentRoute) {
        if (!authState.isAuthenticated) {
            if (currentRoute != null && currentRoute !in RoleAccess.UNAUTHENTICATED) {
                navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
            }
            return@LaunchedEffect
        }

        val home = RoleAccess.homeRouteFor(authState.role)
        val stillOnAuthScreen = currentRoute == null ||
            currentRoute == Routes.LOGIN
        val forbidden = !RoleAccess.canAccess(authState.role, currentRoute, authState.isGuest)

        if ((stillOnAuthScreen || forbidden) && currentRoute != home) {
            navController.navigate(home) { popUpTo(0) { inclusive = true } }
        }
    }

    val isAuthRoute = currentRoute == Routes.LOGIN
    val showChrome = authState.isAuthenticated && !isAuthRoute
    val roleHome = RoleAccess.homeRouteFor(authState.role)

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // A drawer left open across a sign-out would hang over the login screen, and its items would
    // still be the previous role's.
    LaunchedEffect(showChrome) {
        if (!showChrome) drawerState.close()
    }

    val navigateTopLevel: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(roleHome) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    var showProviderAccountPrompt by remember { mutableStateOf(false) }

    // Both the drawer and the account screen offer to become a provider, so the guest check lives
    // here rather than being repeated — and being repeated inconsistently — in each of them.
    val onBecomeProvider: () -> Unit = {
        if (authState.isGuest) {
            showProviderAccountPrompt = true
        } else {
            navController.navigate(Routes.PROVIDER_REGISTER)
        }
    }

    if (showProviderAccountPrompt) {
        AlertDialog(
            onDismissRequest = { showProviderAccountPrompt = false },
            title = { Text("التسجيل يحتاج حساباً دائماً") },
            text = {
                Text(
                    "جلسة الضيف مؤقتة ولا يمكن الرجوع إليها، وطلب الانضمام كمقدم خدمة يحتاج مراجعة " +
                        "من الإدارة ثم يعود إليك. سجّل الدخول ببريدك الإلكتروني أولاً لتتمكن من متابعة طلبك."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showProviderAccountPrompt = false
                        authViewModel.signOut()
                    },
                    modifier = Modifier.testTag("btn_signin_to_apply")
                ) {
                    Text("تسجيل الدخول بالبريد الإلكتروني")
                }
            },
            dismissButton = {
                TextButton(onClick = { showProviderAccountPrompt = false }) {
                    Text("لاحقاً")
                }
            },
            modifier = Modifier.testTag("dialog_provider_account_required")
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        // Gesture-opening it on the auth screens would expose a menu for a role nobody holds yet.
        gesturesEnabled = showChrome,
        drawerContent = {
            if (showChrome) {
                SehatiDrawerContent(
                    role = authState.role,
                    displayName = authState.displayName,
                    email = authState.email,
                    isGuest = authState.isGuest,
                    unreadCount = notificationsState.unreadCount,
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        scope.launch { drawerState.close() }
                        navigateTopLevel(route)
                    },
                    onBecomeProvider = {
                        scope.launch { drawerState.close() }
                        onBecomeProvider()
                    },
                    onLogout = {
                        scope.launch { drawerState.close() }
                        authViewModel.signOut()
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                Column(modifier = Modifier.statusBarsPadding()) {
                    if (showChrome) {
                        SehatiTopBar(
                            role = authState.role,
                            onMenuClick = { scope.launch { drawerState.open() } }
                        )
                    }
                }
            },
            bottomBar = {
                if (showChrome) {
                    BottomNavBar(
                        destinations = RoleAccess.destinationsFor(authState.role),
                        currentRoute = currentRoute,
                        unreadCount = notificationsState.unreadCount,
                        onNavigate = navigateTopLevel
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                NavHost(
                    navController = navController,
                    startDestination = Routes.LOGIN,
                    // Start/End rather than Left/Right so the motion follows the RTL layout: pushing a
                    // screen slides it in from the side the back arrow points away from.
                    enterTransition = {
                        slideIntoContainer(
                            AnimatedContentTransitionScope.SlideDirection.Start,
                            animationSpec = tween(280)
                        ) + fadeIn(animationSpec = tween(220))
                    },
                    exitTransition = {
                        slideOutOfContainer(
                            AnimatedContentTransitionScope.SlideDirection.Start,
                            animationSpec = tween(280)
                        ) + fadeOut(animationSpec = tween(180))
                    },
                    popEnterTransition = {
                        slideIntoContainer(
                            AnimatedContentTransitionScope.SlideDirection.End,
                            animationSpec = tween(280)
                        ) + fadeIn(animationSpec = tween(220))
                    },
                    popExitTransition = {
                        slideOutOfContainer(
                            AnimatedContentTransitionScope.SlideDirection.End,
                            animationSpec = tween(280)
                        ) + fadeOut(animationSpec = tween(180))
                    }
                ) {
                    composable(Routes.LOGIN) {
                        LoginScreen(
                            isLoading = authState.isLoading,
                            errorMessage = authState.errorMessage,
                            emailLinkSentTo = authState.emailLinkSentTo,
                            needsEmailForLink = authState.pendingEmailLink != null,
                            onSendEmailLink = { email -> authViewModel.sendEmailLink(email, activity) },
                            onCompleteEmailLink = { email -> authViewModel.completePendingEmailLink(email, activity) },
                            onChangeEmail = authViewModel::changeEmail,
                            onGoogleSignIn = { authViewModel.signInWithGoogle(activity) }
                        )
                    }

                    composable(Routes.HOME) {
                        HomeScreen(
                            providers = uiState.providers,
                            onSelectCategory = { category ->
                                viewModel.selectCategory(category)
                                navController.navigate(Routes.providersListRoute(category))
                            },
                            onSelectProvider = { provider ->
                                viewModel.selectProvider(provider)
                                navController.navigate(Routes.providerProfileRoute(provider.id))
                            },
                            userName = authState.displayName,
                            userArea = authState.savedAddress,
                            isLoading = uiState.isLoadingProviders,
                            isRefreshing = uiState.isRefreshing,
                            onRefresh = {
                                viewModel.refreshData(
                                    authState.role,
                                    authState.uid,
                                    authState.providerId
                                )
                            },
                            hasCheckedInToday = careState.checkedInToday,
                            onOpenDailyCare = { navController.navigate(Routes.DAILY_CARE) }
                        )
                    }

                    composable(Routes.DAILY_CARE) {
                        val recentCompleted = uiState.orders.firstOrNull { it.status == OrderStatus.COMPLETED }
                        DailyCareScreen(
                            state = careState,
                            recentCompletedOrder = recentCompleted,
                            onCheckIn = careLoopViewModel::recordCheckIn,
                            onCompleteWeeklyReview = careLoopViewModel::completeWeeklyReview,
                            onToggleReminders = { enabled ->
                                if (enabled && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                    activity.requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 4107)
                                }
                                careLoopViewModel.setRemindersEnabled(enabled)
                            },
                            onBookAgain = { order ->
                                val provider = uiState.providers.firstOrNull { it.id == order.providerId }
                                if (provider != null) {
                                    viewModel.selectProvider(provider)
                                    navController.navigate(Routes.bookingConfirmRoute(provider.id))
                                } else {
                                    Toast.makeText(context, "مقدم الخدمة غير متاح للحجز حالياً", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onRequestSupport = {
                                Toast.makeText(context, "تم تسجيل طلبك، سيتواصل معك فريق الدعم", Toast.LENGTH_LONG).show()
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(Routes.PROVIDERS_LIST) { backStackEntry ->
                        val category = backStackEntry.arguments?.getString("category") ?: "ALL"
                        ProvidersListScreen(
                            category = category,
                            providers = uiState.providers,
                            onSelectProvider = { provider ->
                                viewModel.selectProvider(provider)
                                navController.navigate(Routes.providerProfileRoute(provider.id))
                            },
                            onBack = { navController.popBackStack() },
                            isLoading = uiState.isLoadingProviders,
                            isRefreshing = uiState.isRefreshing,
                            onRefresh = {
                                viewModel.refreshData(
                                    authState.role,
                                    authState.uid,
                                    authState.providerId
                                )
                            }
                        )
                    }

                    composable(Routes.PROVIDER_PROFILE) { backStackEntry ->
                        val providerId = backStackEntry.arguments?.getString("providerId")
                        LaunchedEffect(providerId) {
                            providerId?.let {
                                viewModel.selectProviderById(it)
                                ratingViewModel.loadReviews(it)
                            }
                        }

                        val provider = uiState.selectedProvider?.takeIf { it.id == providerId }
                        if (provider != null) {
                            ProviderProfileScreen(
                                provider = provider,
                                reviews = ratingState.reviews,
                                onBook = { navController.navigate(Routes.bookingConfirmRoute(provider.id)) },
                                onBack = { navController.popBackStack() }
                            )
                        } else {
                            RouteLoadingState("جاري تحميل بيانات مقدم الخدمة…")
                        }
                    }

                    composable(Routes.BOOKING_CONFIRM) { backStackEntry ->
                        val providerId = backStackEntry.arguments?.getString("providerId")
                        LaunchedEffect(providerId) {
                            providerId?.let { viewModel.selectProviderById(it) }
                        }

                        val provider = uiState.selectedProvider?.takeIf { it.id == providerId }
                        if (provider != null) {
                            BookingConfirmScreen(
                                provider = provider,
                                onConfirmBooking = { date, time, address, notes ->
                                    viewModel.createNewBooking(
                                        date = date,
                                        time = time,
                                        address = address,
                                        notes = notes,
                                        patientName = authState.displayName,
                                        patientPhone = ""
                                    )
                                },
                                onBack = { navController.popBackStack() },
                                savedAddress = authState.savedAddress,
                                isSubmitting = uiState.isCreatingBooking
                            )
                        } else {
                            RouteLoadingState("جاري تجهيز تفاصيل الحجز…")
                        }
                    }

                    composable(Routes.ORDER_SUCCESS) {
                        val order = uiState.selectedOrder
                        if (order != null) {
                            OrderSuccessScreen(
                                order = order,
                                onProceedToPayment = { navController.navigate(Routes.PAYMENT_METHOD) },
                                onGoToHome = { navController.navigateToRoleHome(authState.role) }
                            )
                        } else {
                            RouteMissingState("تعذر العثور على الطلب", { navController.navigate(Routes.MY_ORDERS) })
                        }
                    }

                    composable(Routes.PAYMENT_METHOD) {
                        val order = uiState.selectedOrder
                        if (order != null && OrderStatus.canPatientPay(order.status)) {
                            PaymentMethodScreen(
                                order = order,
                                selectedMethod = uiState.selectedPaymentMethod,
                                onSelectMethod = { method -> viewModel.selectPaymentMethod(method) },
                                onContinueToUpload = { navController.navigate(Routes.UPLOAD_RECEIPT) },
                                onBack = { navController.popBackStack() }
                            )
                        } else {
                            RouteMissingState("الدفع متاح بعد قبول مقدم الخدمة", { navController.navigate(Routes.MY_ORDERS) })
                        }
                    }

                    composable(Routes.UPLOAD_RECEIPT) {
                        val order = uiState.selectedOrder
                        if (order != null && OrderStatus.canPatientPay(order.status)) {
                            UploadReceiptScreen(
                                order = order,
                                paymentMethod = uiState.selectedPaymentMethod,
                                senderName = uiState.transferSenderName,
                                refNum = uiState.transferRefNum,
                                receiptUri = uiState.receiptLocalUri,
                                isUploading = uiState.isUploadingReceipt,
                                onUpdateDetails = { name, ref -> viewModel.updatePaymentDetails(name, ref) },
                                onReceiptSelected = { uri -> viewModel.setSelectedReceiptImage(uri) },
                                onSubmitReceipt = { viewModel.submitPaymentReceiptForReview() },
                                onBack = { navController.popBackStack() }
                            )
                        } else {
                            RouteMissingState("لا يمكن رفع إشعار لهذا الطلب الآن", { navController.navigate(Routes.MY_ORDERS) })
                        }
                    }

                    composable(Routes.PAYMENT_REVIEW) {
                        val order = uiState.selectedOrder
                        if (order != null) {
                            if (order.status == "PAYMENT_CONFIRMED") {
                                PaymentConfirmedScreen(
                                    order = order,
                                    onCallProvider = {
                                        val phone = order.providerPhone.ifBlank {
                                            uiState.providers.firstOrNull { it.id == order.providerId }?.phoneNumber.orEmpty()
                                        }
                                        context.dialProvider(phone)
                                    },
                                    onGoToHome = { navController.navigateToRoleHome(authState.role) }
                                )
                            } else {
                                PaymentReviewScreen(
                                    order = order,
                                    onGoToHome = { navController.navigateToRoleHome(authState.role) }
                                )
                            }
                        } else {
                            RouteMissingState("تعذر العثور على الطلب", { navController.navigate(Routes.MY_ORDERS) })
                        }
                    }

                    composable(Routes.PAYMENT_CONFIRMED) {
                        val order = uiState.selectedOrder
                        if (order != null) {
                            PaymentConfirmedScreen(
                                order = order,
                                onCallProvider = {
                                    val phone = order.providerPhone.ifBlank {
                                        uiState.providers.firstOrNull { it.id == order.providerId }?.phoneNumber.orEmpty()
                                    }
                                    context.dialProvider(phone)
                                },
                                onGoToHome = { navController.navigateToRoleHome(authState.role) }
                            )
                        } else {
                            RouteMissingState("تعذر العثور على الطلب", { navController.navigate(Routes.MY_ORDERS) })
                        }
                    }

                    composable(Routes.MY_ORDERS) {
                        MyOrdersScreen(
                            orders = uiState.orders,
                            isLoading = uiState.isLoadingOrders,
                            isRefreshing = uiState.isRefreshing,
                            onRefresh = {
                                viewModel.refreshData(
                                    authState.role,
                                    authState.uid,
                                    authState.providerId
                                )
                            },
                            busyOrderId = uiState.busyOrderId,
                            errorMessage = uiState.ordersErrorMessage,
                            onRetry = {
                                viewModel.retryObserveOrders(
                                    authState.role,
                                    authState.uid,
                                    authState.providerId
                                )
                            },
                            onSelectOrder = { order ->
                                viewModel.selectOrder(order)
                                val route = when (order.status) {
                                    OrderStatus.PAYMENT_CONFIRMED -> Routes.PAYMENT_CONFIRMED
                                    OrderStatus.PAYMENT_UNDER_REVIEW -> Routes.PAYMENT_REVIEW
                                    else -> Routes.ORDER_SUCCESS
                                }
                                navController.navigate(route)
                            },
                            onRateOrder = { order ->
                                viewModel.selectOrder(order)
                                navController.navigate(Routes.rateOrderRoute(order.id))
                            },
                            onCancelOrder = { order, reason ->
                                viewModel.cancelOrderByPatient(order.id, reason)
                            },
                            onRequestRefund = { order, reason ->
                                viewModel.requestRefundByPatient(order.id, reason)
                            }
                        )
                    }

                    composable(Routes.PROFILE) {
                        UserProfileScreen(
                            email = authState.email,
                            displayName = authState.displayName,
                            savedAddress = authState.savedAddress,
                            isGuest = authState.isGuest,
                            onSaveProfile = { name, address -> authViewModel.saveProfile(name, address) },
                            onBecomeProvider = onBecomeProvider,
                            onLogout = { authViewModel.signOut() }
                        )
                    }

                    composable(Routes.NOTIFICATIONS) {
                        NotificationsScreen(
                            notifications = notificationsState.notifications,
                            onNotificationClick = { notification ->
                                notificationsViewModel.markRead(notification)
                                // A shared-inbox alert always asks for an admin decision, and
                                // those are all taken on the dashboard — never on المشتريات.
                                if (notification.scope == NotificationEntity.SCOPE_ADMIN) {
                                    navController.navigate(Routes.ADMIN_DASHBOARD)
                                } else if (notification.orderId.isNotEmpty()) {
                                    uiState.orders.firstOrNull { it.id == notification.orderId }?.let { order ->
                                        viewModel.selectOrder(order)
                                        val route = when (order.status) {
                                            OrderStatus.PAYMENT_CONFIRMED -> Routes.PAYMENT_CONFIRMED
                                            OrderStatus.PAYMENT_UNDER_REVIEW -> Routes.PAYMENT_REVIEW
                                            else -> Routes.ORDER_SUCCESS
                                        }
                                        navController.navigate(route)
                                    }
                                }
                            },
                            onMarkAllRead = { notificationsViewModel.markAllRead() },
                            onRefresh = { notificationsViewModel.refresh(authState.role == UserRole.ADMIN) }
                        )
                    }

                    composable(Routes.RATE_ORDER) { backStackEntry ->
                        val orderId = backStackEntry.arguments?.getString("orderId")
                        val order = uiState.orders.firstOrNull { it.id == orderId }
                            ?: uiState.selectedOrder

                        LaunchedEffect(ratingState.isSubmitted) {
                            if (ratingState.isSubmitted) {
                                ratingViewModel.consumeSubmitted()
                                navController.popBackStack()
                            }
                        }

                        if (order != null) {
                            RatingScreen(
                                order = order,
                                isSubmitting = ratingState.isSubmitting,
                                onSubmitRating = { stars, chips, comment ->
                                    ratingViewModel.submitRating(order, stars, chips, comment)
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }

                    composable(Routes.PROVIDER_REGISTER) {
                        LaunchedEffect(registrationState.basicInfoSaved) {
                            if (registrationState.basicInfoSaved) {
                                registrationViewModel.consumeBasicInfoSaved()
                                navController.navigate(Routes.PROVIDER_DOCS)
                            }
                        }

                        ProviderRegisterScreen(
                            isSubmitting = registrationState.isSubmitting,
                            errorMessage = registrationState.errorMessage,
                            onSubmit = { name, title, category, years, price, area, about ->
                                registrationViewModel.submitBasicInfo(
                                    name = name,
                                    title = title,
                                    category = category,
                                    experienceYears = years,
                                    priceSdg = price,
                                    area = area,
                                    about = about
                                )
                            },
                            onBack = {
                                registrationViewModel.clearError()
                                navController.popBackStack()
                            }
                        )
                    }

                    composable(Routes.PROVIDER_DOCS) {
                        LaunchedEffect(registrationState.docsSubmitted) {
                            if (registrationState.docsSubmitted) {
                                registrationViewModel.consumeDocsSubmitted()
                                navController.navigate(Routes.PROVIDER_PENDING)
                            }
                        }

                        ProviderDocsUploadScreen(
                            docUris = registrationState.docUris,
                            isUploading = registrationState.isSubmitting,
                            errorMessage = registrationState.errorMessage,
                            onDocSelected = { docType, uri -> registrationViewModel.setDocUri(docType, uri) },
                            onSubmitDocs = { registrationViewModel.submitDocuments() },
                            onBack = {
                                registrationViewModel.clearError()
                                navController.popBackStack()
                            }
                        )
                    }

                    composable(Routes.PROVIDER_PENDING) {
                        ProviderPendingScreen(
                            providerName = registrationState.providerName,
                            onGoToHome = { navController.navigateToRoleHome(authState.role) }
                        )
                    }

                    composable(Routes.PROVIDER_DASHBOARD) {
                        LaunchedEffect(Unit) {
                            viewModel.observePayouts(isAdmin = false, providerId = authState.providerId)
                        }

                        ProviderDashboardScreen(
                            isAvailable = uiState.isProviderAvailable,
                            isSavingAvailability = uiState.isSavingAvailability,
                            onToggleAvailability = { viewModel.toggleProviderAvailability() },
                            orders = uiState.orders,
                            onAcceptOrder = { id -> viewModel.acceptOrderByProvider(id) },
                            onCompleteOrder = { id -> viewModel.completeOrderByProvider(id) },
                            onDeclineOrder = { id, reason ->
                                viewModel.declineOrderByProvider(id, reason)
                            },
                            // ACTION_DIAL only opens the dialer with the number filled in, so the
                            // provider still confirms the call and the app needs no CALL_PHONE grant.
                            onCallPatient = { phone ->
                                if (phone.isBlank()) {
                                    Toast.makeText(
                                        context,
                                        "لا يوجد رقم هاتف مسجل لهذا المريض",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    context.startActivity(
                                        Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                    )
                                }
                            },
                            providerName = authState.displayName,
                            isLoading = uiState.isLoadingOrders,
                            isRefreshing = uiState.isRefreshing,
                            onRefresh = {
                                viewModel.refreshData(
                                    authState.role,
                                    authState.uid,
                                    authState.providerId
                                )
                            },
                            busyOrderId = uiState.busyOrderId,
                            errorMessage = uiState.ordersErrorMessage,
                            onRetry = {
                                viewModel.retryObserveOrders(
                                    authState.role,
                                    authState.uid,
                                    authState.providerId
                                )
                            },
                            onOpenEarnings = { navController.navigate(Routes.PROVIDER_EARNINGS) }
                        )
                    }

                    composable(Routes.PROVIDER_EARNINGS) {
                        LaunchedEffect(Unit) {
                            viewModel.observePayouts(isAdmin = false, providerId = authState.providerId)
                        }

                        ProviderEarningsScreen(
                            earnings = foldEarnings(uiState.payouts),
                            payouts = uiState.payouts,
                            onBack = { navController.popBackStack() },
                            isLoading = uiState.isLoadingPayouts,
                            errorMessage = uiState.ordersErrorMessage
                        )
                    }

                    composable(Routes.ADMIN_DASHBOARD) {
                        LaunchedEffect(Unit) {
                            registrationViewModel.loadPendingApplications()
                            viewModel.observePayouts(isAdmin = true)
                        }

                        AdminDashboardScreen(
                            orders = uiState.orders,
                            onApprovePayment = { id -> viewModel.approvePaymentByAdmin(id) },
                            onRejectPayment = { id -> viewModel.rejectPaymentByAdmin(id) },
                            pendingProviders = registrationState.pendingApplications,
                            onApproveProvider = { id -> registrationViewModel.approveProvider(id) },
                            onRejectProvider = { id, reason ->
                                registrationViewModel.rejectProvider(id, reason)
                            },
                            isLoading = uiState.isLoadingOrders,
                            busyOrderId = uiState.busyOrderId,
                            errorMessage = uiState.ordersErrorMessage,
                            onRetry = {
                                viewModel.retryObserveOrders(
                                    authState.role,
                                    authState.uid,
                                    authState.providerId
                                )
                            },
                            onOpenFinance = { navController.navigate(Routes.ADMIN_FINANCE) },
                            onGrantRefund = { id -> viewModel.resolveRefundByAdmin(id, grant = true) },
                            onDeclineRefund = { id -> viewModel.resolveRefundByAdmin(id, grant = false) }
                        )
                    }

                    composable(Routes.ADMIN_FINANCE) {
                        LaunchedEffect(Unit) {
                            viewModel.observePayouts(isAdmin = true)
                        }

                        AdminFinanceScreen(
                            summary = foldAdminFinance(uiState.orders, uiState.payouts),
                            payouts = uiState.payouts,
                            onMarkPaid = { id -> viewModel.markPayoutPaidByAdmin(id) },
                            onBack = { navController.popBackStack() },
                            repeatRate = repeatBookingRate(uiState.orders),
                            isLoading = uiState.isLoadingPayouts,
                            busyPayoutId = uiState.busyPayoutId,
                            errorMessage = uiState.ordersErrorMessage
                        )
                    }
                }
            }
        }
    }
}

private fun NavHostController.navigateToRoleHome(role: UserRole) {
    val home = RoleAccess.homeRouteFor(role)
    navigate(home) {
        popUpTo(home) { inclusive = true }
    }
}

@Composable
private fun RouteLoadingState(message: String) {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(text = message, modifier = Modifier.padding(24.dp))
    }
}

@Composable
private fun RouteMissingState(message: String, onOpenOrders: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(message)
        TextButton(onClick = onOpenOrders) { Text("العودة إلى طلباتي") }
    }
}

private fun android.content.Context.dialProvider(phoneNumber: String) {
    if (phoneNumber.isBlank()) {
        Toast.makeText(this, "لا يوجد رقم هاتف متاح لمقدم الخدمة", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber")))
    } catch (e: Exception) {
        Toast.makeText(this, "تعذر فتح تطبيق الاتصال", Toast.LENGTH_SHORT).show()
    }
}
