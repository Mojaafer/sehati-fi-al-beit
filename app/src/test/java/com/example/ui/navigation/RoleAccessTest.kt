package com.example.ui.navigation

import com.example.ui.viewmodel.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoleAccessTest {

    private val allRoutes = listOf(
        Routes.HOME,
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
        Routes.PROFILE,
        Routes.NOTIFICATIONS,
        Routes.PROVIDER_REGISTER,
        Routes.PROVIDER_DOCS,
        Routes.PROVIDER_PENDING,
        Routes.PROVIDER_DASHBOARD,
        Routes.ADMIN_DASHBOARD
    )

    @Test
    fun patientCannotReachProviderOrAdminDashboards() {
        assertFalse(RoleAccess.canAccess(UserRole.PATIENT, Routes.PROVIDER_DASHBOARD))
        assertFalse(RoleAccess.canAccess(UserRole.PATIENT, Routes.ADMIN_DASHBOARD))
    }

    @Test
    fun providerCannotReachBookingOrPaymentScreens() {
        listOf(
            Routes.HOME,
            Routes.PROVIDERS_LIST,
            Routes.BOOKING_CONFIRM,
            Routes.PAYMENT_METHOD,
            Routes.UPLOAD_RECEIPT,
            Routes.PAYMENT_REVIEW,
            Routes.MY_ORDERS,
            Routes.RATE_ORDER,
            Routes.ADMIN_DASHBOARD
        ).forEach { route ->
            assertFalse(route, RoleAccess.canAccess(UserRole.PROVIDER, route))
        }
    }

    @Test
    fun adminIsLimitedToItsOwnDashboardPlusSharedScreens() {
        assertTrue(RoleAccess.canAccess(UserRole.ADMIN, Routes.ADMIN_DASHBOARD))
        assertFalse(RoleAccess.canAccess(UserRole.ADMIN, Routes.PROVIDER_DASHBOARD))
        assertFalse(RoleAccess.canAccess(UserRole.ADMIN, Routes.HOME))
        assertFalse(RoleAccess.canAccess(UserRole.ADMIN, Routes.UPLOAD_RECEIPT))
    }

    /** Sign-out lives on the account screen, so gating it would trap a provider or an admin. */
    @Test
    fun everyRoleKeepsTheAccountAndInboxScreens() {
        UserRole.values().forEach { role ->
            assertTrue(role.name, RoleAccess.canAccess(role, Routes.PROFILE))
            assertTrue(role.name, RoleAccess.canAccess(role, Routes.NOTIFICATIONS))
        }
    }

    @Test
    fun patientOwnsTheWholeBookingFlow() {
        listOf(
            Routes.HOME,
            Routes.PROVIDERS_LIST,
            Routes.PROVIDER_PROFILE,
            Routes.BOOKING_CONFIRM,
            Routes.ORDER_SUCCESS,
            Routes.PAYMENT_METHOD,
            Routes.UPLOAD_RECEIPT,
            Routes.PAYMENT_REVIEW,
            Routes.PAYMENT_CONFIRMED,
            Routes.MY_ORDERS,
            Routes.RATE_ORDER
        ).forEach { route ->
            assertTrue(route, RoleAccess.canAccess(UserRole.PATIENT, route))
        }
    }

    @Test
    fun pendingProviderApplicationStaysOpenToApplicantAndProvider() {
        assertTrue(RoleAccess.canAccess(UserRole.PATIENT, Routes.PROVIDER_PENDING))
        assertTrue(RoleAccess.canAccess(UserRole.PROVIDER, Routes.PROVIDER_PENDING))
    }

    @Test
    fun everyRoleLandsOnItsOwnHome() {
        assertEquals(Routes.HOME, RoleAccess.homeRouteFor(UserRole.PATIENT))
        assertEquals(Routes.PROVIDER_DASHBOARD, RoleAccess.homeRouteFor(UserRole.PROVIDER))
        assertEquals(Routes.ADMIN_DASHBOARD, RoleAccess.homeRouteFor(UserRole.ADMIN))
    }

    /** A tab the role may not open would bounce the user straight back out of it. */
    @Test
    fun everyTabIsAccessibleToTheRoleThatShowsIt() {
        UserRole.values().forEach { role ->
            RoleAccess.destinationsFor(role).forEach { destination ->
                assertTrue(
                    "$role → ${destination.route}",
                    RoleAccess.canAccess(role, destination.route)
                )
            }
        }
    }

    @Test
    fun everyRoleGetsBetweenThreeAndFiveTabs() {
        UserRole.values().forEach { role ->
            val count = RoleAccess.destinationsFor(role).size
            assertTrue("$role has $count tabs", count in 3..5)
        }
    }

    @Test
    fun everyRouteIsReachableBySomeRole() {
        allRoutes.forEach { route ->
            assertTrue(
                route,
                UserRole.values().any { RoleAccess.canAccess(it, route) }
            )
        }
    }

    /** Nothing but the sign-in pair may render before a user exists. */
    @Test
    fun unauthenticatedSetIsLimitedToSignIn() {
        assertEquals(setOf(Routes.LOGIN), RoleAccess.UNAUTHENTICATED)
        assertFalse(Routes.HOME in RoleAccess.UNAUTHENTICATED)
        assertFalse(Routes.ADMIN_DASHBOARD in RoleAccess.UNAUTHENTICATED)
    }

    /**
     * An application filed from a guest session would be reviewed by an admin and approved onto an
     * account the applicant can never sign back into.
     */
    @Test
    fun guestCannotReachTheProviderApplication() {
        RoleAccess.REQUIRES_PERMANENT_ACCOUNT.forEach { route ->
            assertFalse(route, RoleAccess.canAccess(UserRole.PATIENT, route, isGuest = true))
        }
    }

    @Test
    fun guestKeepsTheRestOfThePatientApp() {
        listOf(Routes.HOME, Routes.PROVIDERS_LIST, Routes.BOOKING_CONFIRM, Routes.MY_ORDERS,
            Routes.PROFILE, Routes.NOTIFICATIONS).forEach { route ->
            assertTrue(route, RoleAccess.canAccess(UserRole.PATIENT, route, isGuest = true))
        }
    }

    @Test
    fun signedInPatientStillReachesTheProviderApplication() {
        RoleAccess.REQUIRES_PERMANENT_ACCOUNT.forEach { route ->
            assertTrue(route, RoleAccess.canAccess(UserRole.PATIENT, route, isGuest = false))
        }
    }

    @Test
    fun deepPaymentRoutesKeepTheOrdersTabHighlighted() {
        assertTrue(TopLevelDestination.PATIENT_ORDERS.isCurrent(Routes.UPLOAD_RECEIPT))
        assertTrue(TopLevelDestination.PATIENT_ORDERS.isCurrent(Routes.PAYMENT_CONFIRMED))
        assertTrue(TopLevelDestination.PATIENT_HOME.isCurrent(Routes.PROVIDER_PROFILE))
        assertFalse(TopLevelDestination.PATIENT_HOME.isCurrent(Routes.UPLOAD_RECEIPT))
    }

    /** The drawer offers "انضم كمقدم خدمة" to patients only; other roles would bounce off it. */
    @Test
    fun onlyPatientsAreOfferedTheProviderApplication() {
        assertTrue(RoleAccess.canAccess(UserRole.PATIENT, Routes.PROVIDER_REGISTER))
        assertFalse(RoleAccess.canAccess(UserRole.PROVIDER, Routes.PROVIDER_REGISTER))
        assertFalse(RoleAccess.canAccess(UserRole.ADMIN, Routes.PROVIDER_REGISTER))
    }

    @Test
    fun rolesHaveDistinctArabicLabels() {
        val labels = UserRole.values().map { RoleAccess.roleLabel(it) }
        assertEquals(labels.size, labels.toSet().size)
        assertTrue(labels.none { it.isBlank() })
    }
}
