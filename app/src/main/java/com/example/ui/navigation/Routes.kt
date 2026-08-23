package com.example.ui.navigation

object Routes {
    const val LOGIN = "login"
    const val PROVIDER_REGISTER = "provider_register"
    const val PROVIDER_DOCS = "provider_docs"
    const val PROVIDER_PENDING = "provider_pending"

    const val HOME = "home"
    const val DAILY_CARE = "daily_care"
    const val PROVIDERS_LIST = "providers_list/{category}"
    const val PROVIDER_PROFILE = "provider_profile/{providerId}"
    const val BOOKING_CONFIRM = "booking_confirm/{providerId}"
    const val ORDER_SUCCESS = "order_success"
    const val PAYMENT_METHOD = "payment_method"
    const val UPLOAD_RECEIPT = "upload_receipt"
    const val PAYMENT_REVIEW = "payment_review"
    const val PAYMENT_CONFIRMED = "payment_confirmed"
    const val MY_ORDERS = "my_orders"
    const val PROFILE = "profile"
    const val NOTIFICATIONS = "notifications"
    const val RATE_ORDER = "rate_order/{orderId}"

    const val PROVIDER_DASHBOARD = "provider_dashboard"
    const val ADMIN_DASHBOARD = "admin_dashboard"
    const val ADMIN_FINANCE = "admin_finance"
    const val PROVIDER_EARNINGS = "provider_earnings"

    fun providersListRoute(category: String) = "providers_list/$category"
    fun providerProfileRoute(providerId: String) = "provider_profile/$providerId"
    fun bookingConfirmRoute(providerId: String) = "booking_confirm/$providerId"
    fun rateOrderRoute(orderId: String) = "rate_order/$orderId"
}
