package com.example.ui.navigation

import com.example.ui.viewmodel.UserRole

/**
 * A tab in the navigation bar. [selectedFor] lists the deeper routes that should keep the tab
 * highlighted, so walking the payment flow still reads as being inside "طلباتي".
 */
enum class TopLevelDestination(
    val route: String,
    val label: String,
    val selectedFor: Set<String> = emptySet()
) {
    PATIENT_HOME(Routes.HOME, "الرئيسية", setOf(Routes.PROVIDERS_LIST, Routes.PROVIDER_PROFILE, Routes.DAILY_CARE)),
    PATIENT_ORDERS(
        Routes.MY_ORDERS,
        "طلباتي",
        setOf(
            Routes.BOOKING_CONFIRM,
            Routes.ORDER_SUCCESS,
            Routes.PAYMENT_METHOD,
            Routes.UPLOAD_RECEIPT,
            Routes.PAYMENT_REVIEW,
            Routes.PAYMENT_CONFIRMED,
            Routes.RATE_ORDER
        )
    ),
    PROVIDER_WORK(Routes.PROVIDER_DASHBOARD, "لوحة العمل"),
    ADMIN_REVIEW(Routes.ADMIN_DASHBOARD, "لوحة الإدارة"),
    INBOX(Routes.NOTIFICATIONS, "الإشعارات"),
    ACCOUNT(
        Routes.PROFILE,
        "حسابي",
        setOf(Routes.PROVIDER_REGISTER, Routes.PROVIDER_DOCS, Routes.PROVIDER_PENDING)
    );

    fun isCurrent(route: String?): Boolean = route == this.route || route in selectedFor
}

/**
 * The single place that decides which destinations a role may reach. The navigation bar builds
 * itself from [destinationsFor] and the graph bounces anything [canAccess] rejects, so a screen
 * cannot be reached by a role that has no tab for it — including via a stale back stack.
 */
object RoleAccess {

    /**
     * Reachable before sign-in: nothing but the sign-in pair. The provider application used to be
     * open to a visitor, which only meant filling the whole form before the write was refused for
     * having no account to own it.
     */
    val UNAUTHENTICATED = setOf(
        Routes.LOGIN,
    )

    /**
     * A provider application outlives the session that files it: an admin reviews it later, and the
     * approval arrives as a role change on the account that owns it. A guest signs in anonymously
     * and cannot get that session back, so the application — and the dashboard it unlocks — would be
     * stranded on a user nobody can reach. Any account that can be signed back into will do —
     * Google or passwordless email.
     */
    val REQUIRES_PERMANENT_ACCOUNT = setOf(
        Routes.PROVIDER_REGISTER,
        Routes.PROVIDER_DOCS,
        Routes.PROVIDER_PENDING
    )

    /** Booking is the patient's alone; nobody else has a use for the ordering or payment screens. */
    private val PATIENT_ROUTES = setOf(
        Routes.HOME,
        Routes.DAILY_CARE,
        Routes.PROVIDERS_LIST,
        Routes.PROVIDER_PROFILE,
        Routes.BOOKING_CONFIRM,
        Routes.ORDER_SUCCESS,
        Routes.PAYMENT_METHOD,
        Routes.UPLOAD_RECEIPT,
        Routes.PAYMENT_REVIEW,
        Routes.PAYMENT_CONFIRMED,
        Routes.MY_ORDERS,
        Routes.RATE_ORDER,
        Routes.PROVIDER_REGISTER,
        Routes.PROVIDER_DOCS
    )

    /**
     * Every role needs the inbox and the account tab, because signing out lives on the account
     * screen — gating it would strand a provider or an admin in their dashboard.
     */
    private val SHARED_ROUTES = setOf(
        Routes.LOGIN,
        Routes.NOTIFICATIONS,
        Routes.PROFILE
    )

    fun destinationsFor(role: UserRole): List<TopLevelDestination> = when (role) {
        UserRole.PATIENT -> listOf(
            TopLevelDestination.PATIENT_HOME,
            TopLevelDestination.PATIENT_ORDERS,
            TopLevelDestination.INBOX,
            TopLevelDestination.ACCOUNT
        )
        UserRole.PROVIDER -> listOf(
            TopLevelDestination.PROVIDER_WORK,
            TopLevelDestination.INBOX,
            TopLevelDestination.ACCOUNT
        )
        UserRole.ADMIN -> listOf(
            TopLevelDestination.ADMIN_REVIEW,
            TopLevelDestination.INBOX,
            TopLevelDestination.ACCOUNT
        )
    }

    fun homeRouteFor(role: UserRole): String = destinationsFor(role).first().route

    fun canAccess(role: UserRole, route: String?): Boolean {
        if (route == null) return true
        if (route in SHARED_ROUTES) return true
        return when (role) {
            // A provider awaiting approval still carries the PROVIDER role, so the "under review"
            // screen has to stay open to them as well as to the patient who just applied.
            UserRole.PATIENT -> route in PATIENT_ROUTES || route == Routes.PROVIDER_PENDING
            UserRole.PROVIDER -> route == Routes.PROVIDER_DASHBOARD ||
                route == Routes.PROVIDER_PENDING
            UserRole.ADMIN -> route == Routes.ADMIN_DASHBOARD
        }
    }

    /**
     * The role decides what exists; being a guest decides whether it can be acted on. Kept separate
     * from [canAccess] so the navigation bar still builds from the role alone — a guest browses the
     * same patient tabs, they just cannot file a provider application from an account that will be
     * gone at sign-out.
     */
    fun canAccess(role: UserRole, route: String?, isGuest: Boolean): Boolean {
        if (isGuest && route in REQUIRES_PERMANENT_ACCOUNT) return false
        return canAccess(role, route)
    }

    fun roleLabel(role: UserRole): String = when (role) {
        UserRole.PATIENT -> "طالب خدمة"
        UserRole.PROVIDER -> "مقدم خدمة"
        UserRole.ADMIN -> "مدير النظام"
    }
}
