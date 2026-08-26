package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.NotificationEntity
import com.example.data.model.OrderEntity
import com.example.data.model.OrderFees
import com.example.data.model.OrderStatus
import com.example.data.model.PayoutEntity
import com.example.data.model.ProviderEntity
import com.example.data.repository.NotificationRepository
import com.example.data.repository.PayoutRepository
import com.example.data.repository.SehatiRepository
import com.example.data.storage.StorageRepository
import com.example.data.supabase.RepositoryFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class UserRole {
    PATIENT, PROVIDER, ADMIN
}

data class SehatiUiState(
    val selectedCategory: String = "ALL",
    val selectedProvider: ProviderEntity? = null,
    val selectedOrder: OrderEntity? = null,
    val providers: List<ProviderEntity> = emptyList(),
    val orders: List<OrderEntity> = emptyList(),
    val isProviderAvailable: Boolean = true,
    /** True while the availability write is in flight, so the switch can show it is not settled yet. */
    val isSavingAvailability: Boolean = false,
    val selectedPaymentMethod: String = "بنكك (Bankak)",
    val transferSenderName: String = "",
    val transferRefNum: String = "",
    val receiptLocalUri: String? = null,
    val isUploadingReceipt: Boolean = false,
    val isCreatingBooking: Boolean = false,
    // Both feeds start "loading" so a first frame shows placeholders instead of an empty state
    // that would wrongly read as "you have no orders".
    val isLoadingProviders: Boolean = true,
    val isLoadingOrders: Boolean = true,
    /** The order currently being written to, so only its own card shows a spinner. */
    val busyOrderId: String? = null,
    /** Set when Firestore refuses the order listen, so the list can say so instead of passing off
     *  whatever the local cache still holds as live data. */
    val ordersErrorMessage: String? = null,
    val notificationMessage: String? = null,
    val navigationEvent: String? = null,
    val isRefreshing: Boolean = false,
    // Earnings ledger feeds (provider sees their own, admin sees all). Starts loading like the
    // order feeds so the first frame shows a placeholder instead of a false "nothing owed".
    val payouts: List<PayoutEntity> = emptyList(),
    val isLoadingPayouts: Boolean = true,
    /** The payout currently being marked paid, so only its own row shows a spinner. */
    val busyPayoutId: String? = null
) {
    val receiptImageSelected: Boolean get() = receiptLocalUri != null

    fun isBusy(orderId: String): Boolean = busyOrderId == orderId
}

// @JvmOverloads gives the default ViewModelProvider factory the no-arg constructor it reflects on.
class SehatiViewModel @JvmOverloads constructor(
    private val repository: SehatiRepository = RepositoryFactory.createSehatiRepository(),
    storageRepository: StorageRepository? = null,
    notificationRepository: NotificationRepository? = null,
    payoutRepository: PayoutRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(SehatiUiState())
    val uiState: StateFlow<SehatiUiState> = _uiState.asStateFlow()

    // Lazy so unit tests supplying a fake repository never touch Firebase singletons.
    private val storage: StorageRepository by lazy { storageRepository ?: RepositoryFactory.createStorageRepository() }
    private val notifications: NotificationRepository by lazy {
        notificationRepository ?: RepositoryFactory.createNotificationRepository()
    }
    private val payouts: PayoutRepository by lazy { payoutRepository ?: RepositoryFactory.createPayoutRepository() }

    private var ordersJob: Job? = null
    private var orderScope: Triple<UserRole, String, String>? = null
    private var providersJob: Job? = null
    private var payoutsJob: Job? = null
    private var payoutsScope: Pair<Boolean, String>? = null

    /** The signed-in provider's own document id, so [toggleProviderAvailability] knows what to write. */
    private var currentProviderId: String = ""

    /** Firestore rejects the catalogue read until a caller is signed in, so this waits for auth. */
    fun observeProviders() {
        if (providersJob?.isActive == true) return
        providersJob = viewModelScope.launch {
            repository.allProviders.collectLatest { providerList ->
                _uiState.value = _uiState.value.copy(
                    providers = providerList,
                    isLoadingProviders = false
                )
                syncOwnAvailability(providerList)
            }
        }
    }

    /**
     * The provider's own catalogue entry is the source of truth for the availability switch, so
     * the feed drives it — a provider who toggled on another device sees it here too.
     */
    private fun syncOwnAvailability(providerList: List<ProviderEntity>) {
        if (currentProviderId.isEmpty() || _uiState.value.isSavingAvailability) return
        val own = providerList.firstOrNull { it.id == currentProviderId } ?: return
        if (own.isAvailableNow != _uiState.value.isProviderAvailable) {
            _uiState.value = _uiState.value.copy(isProviderAvailable = own.isAvailableNow)
        }
    }

    /**
     * Starts (or restarts) the order feed for whoever is signed in. Security rules reject a
     * query outright unless every document it could match is readable, so a patient has to
     * ask for their own orders and a provider for theirs — the role must be known first.
     */
    fun observeOrders(role: UserRole, uid: String, providerId: String = "") {
        val scope = Triple(role, uid, providerId)
        if (scope == orderScope) return
        orderScope = scope
        currentProviderId = providerId
        _uiState.value = _uiState.value.copy(
            orders = emptyList(),
            selectedOrder = null,
            isLoadingOrders = true,
            ordersErrorMessage = null,
            transferSenderName = "",
            transferRefNum = "",
            receiptLocalUri = null
        )
        if (providerId.isNotEmpty()) {
            viewModelScope.launch {
                repository.getProviderById(providerId)?.let { own ->
                    _uiState.value = _uiState.value.copy(isProviderAvailable = own.isAvailableNow)
                }
            }
        }

        ordersJob?.cancel()
        ordersJob = viewModelScope.launch {
            val feed = when {
                role == UserRole.ADMIN -> repository.allOrders
                role == UserRole.PROVIDER && providerId.isNotEmpty() ->
                    repository.ordersForProvider(providerId)
                role == UserRole.PATIENT && uid.isNotEmpty() -> repository.ordersForPatient(uid)
                // Nothing to subscribe to, so stop showing placeholders for a feed that
                // will never arrive.
                else -> {
                    _uiState.value = _uiState.value.copy(isLoadingOrders = false)
                    return@launch
                }
            }
            feed.collectLatest { orderList ->
                _uiState.value = _uiState.value.copy(
                    orders = orderList,
                    isLoadingOrders = false,
                    ordersErrorMessage = null
                )
                val selectedId = _uiState.value.selectedOrder?.id
                if (selectedId != null) {
                    _uiState.value = _uiState.value.copy(
                        selectedOrder = orderList.firstOrNull { it.id == selectedId }
                    )
                }
            }

            // Reaching here means the feed ended by itself, which a snapshot listener only does
            // when Firestore refused the listen — cancellation throws instead of returning. The
            // listener hands over the local cache before that refusal arrives, so without this the
            // screen keeps rendering stale rows and quietly ignores every later change.
            orderScope = null
            _uiState.value = _uiState.value.copy(
                isLoadingOrders = false,
                ordersErrorMessage = "تعذر تحديث قائمة الطلبات. تحقق من صلاحيات حسابك ثم أعد المحاولة."
            )
        }
    }

    /** Re-subscribes after a refused listen; [observeOrders] skips a scope it is already on. */
    fun retryObserveOrders(role: UserRole, uid: String, providerId: String = "") {
        _uiState.value = _uiState.value.copy(
            ordersErrorMessage = null,
            isLoadingOrders = true
        )
        observeOrders(role, uid, providerId)
    }

    /** Triggered on swipe-to-refresh across screens to fetch fresh data from Firestore. */
    fun refreshData(role: UserRole, uid: String, providerId: String = "") {
        _uiState.value = _uiState.value.copy(isRefreshing = true)
        providersJob?.cancel()
        providersJob = null
        observeProviders()

        ordersJob?.cancel()
        ordersJob = null
        orderScope = null
        observeOrders(role, uid, providerId)

        viewModelScope.launch {
            kotlinx.coroutines.delay(650)
            _uiState.value = _uiState.value.copy(isRefreshing = false)
        }
    }

    /**
     * Starts the earnings-ledger feed: a provider gets their own payouts, an admin gets all of
     * them. Kept separate from the order feed so the finance screens can subscribe without
     * disturbing whatever orders scope is already running.
     */
    fun observePayouts(isAdmin: Boolean, providerId: String = "") {
        val scope = Pair(isAdmin, providerId)
        if (scope == payoutsScope) return
        payoutsScope = scope
        payoutsJob?.cancel()
        _uiState.value = _uiState.value.copy(payouts = emptyList(), isLoadingPayouts = true)

        payoutsJob = viewModelScope.launch {
            val feed = if (isAdmin) {
                payouts.allPayouts
            } else if (providerId.isNotEmpty()) {
                payouts.payoutsForProvider(providerId)
            } else {
                _uiState.value = _uiState.value.copy(isLoadingPayouts = false)
                return@launch
            }
            feed.collectLatest { payoutList ->
                _uiState.value = _uiState.value.copy(
                    payouts = payoutList,
                    isLoadingPayouts = false
                )
            }

            // Same convention as the order feed: a listen that ends by itself was refused by
            // the rules, so say so rather than passing off an empty list as "nothing owed".
            _uiState.value = _uiState.value.copy(
                isLoadingPayouts = false,
                notificationMessage = "تعذر تحديث سجل المستحقات. تحقق من صلاحيات حسابك ثم أعد المحاولة."
            )
        }
    }

    /** Admin records that a provider's accrued earnings were transferred via Bankak. */
    fun markPayoutPaidByAdmin(payoutId: String) {
        if (_uiState.value.busyPayoutId != null) return
        _uiState.value = _uiState.value.copy(busyPayoutId = payoutId)
        viewModelScope.launch {
            try {
                payouts.markPaid(payoutId)
                _uiState.value = _uiState.value.copy(
                    busyPayoutId = null,
                    notificationMessage = "تم تسجيل تحويل المستحق لمقدم الخدمة"
                )
            } catch (e: Exception) {
                android.util.Log.w("SehatiViewModel", "markPaid failed for $payoutId", e)
                _uiState.value = _uiState.value.copy(
                    busyPayoutId = null,
                    notificationMessage = "تعذر تسجيل الدفع، تحقق من الاتصال وحاول مرة أخرى"
                )
            }
        }
    }

    fun selectCategory(category: String) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun selectProvider(provider: ProviderEntity) {
        _uiState.value = _uiState.value.copy(selectedProvider = provider)
    }

    fun selectProviderById(providerId: String) {
        if (_uiState.value.selectedProvider?.id == providerId) return
        _uiState.value = _uiState.value.copy(selectedProvider = null)
        viewModelScope.launch {
            val provider = _uiState.value.providers.firstOrNull { it.id == providerId }
                ?: repository.getProviderById(providerId)
            _uiState.value = _uiState.value.copy(selectedProvider = provider)
        }
    }

    fun selectOrder(order: OrderEntity) {
        _uiState.value = _uiState.value.copy(selectedOrder = order)
    }

    /**
     * Flips the switch locally first so it never feels laggy, then persists. On failure the
     * switch snaps back rather than lying about a state patients would never see.
     */
    fun toggleProviderAvailability() {
        if (_uiState.value.isSavingAvailability) return
        val previous = _uiState.value.isProviderAvailable
        val next = !previous

        _uiState.value = _uiState.value.copy(
            isProviderAvailable = next,
            isSavingAvailability = true
        )

        viewModelScope.launch {
            try {
                repository.setProviderAvailability(currentProviderId, next)
                _uiState.value = _uiState.value.copy(
                    isSavingAvailability = false,
                    notificationMessage = if (next) {
                        "أنت الآن متاح — سيظهر ملفك للمرضى ويمكنهم إرسال طلبات لك"
                    } else {
                        "أنت الآن غير متاح — لن تصلك طلبات جديدة حتى تعود"
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isProviderAvailable = previous,
                    isSavingAvailability = false,
                    notificationMessage = "تعذر تحديث حالتك، تحقق من الاتصال وحاول مرة أخرى"
                )
            }
        }
    }

    fun updatePaymentDetails(senderName: String, refNum: String) {
        _uiState.value = _uiState.value.copy(
            transferSenderName = senderName,
            transferRefNum = refNum
        )
    }

    fun selectPaymentMethod(method: String) {
        _uiState.value = _uiState.value.copy(selectedPaymentMethod = method)
    }

    fun setSelectedReceiptImage(uri: String?) {
        _uiState.value = _uiState.value.copy(receiptLocalUri = uri)
    }

    fun createNewBooking(
        date: String,
        time: String,
        address: String,
        notes: String,
        patientName: String,
        patientPhone: String = ""
    ) {
        if (_uiState.value.isCreatingBooking) return
        val provider = _uiState.value.selectedProvider ?: return

        val newOrderNum = OrderFees.newOrderNumber()
        // The few extra pounds make this order's payable amount unique among open orders, so
        // one bank-statement line identifies its order even before the reference is checked.
        val payableSuffix = OrderFees.randomUniqueSuffix()
        val newOrder = OrderEntity(
            orderNumber = newOrderNum,
            serviceTitle = when (provider.serviceCategory) {
                "LAB_DRAW" -> "سحب عينات منزلية"
                "NURSING" -> "تمريض منزلي ومتابعة"
                "PHYSIO" -> "جلسة علاج طبيعي"
                else -> "كشف وطبيب منزلي"
            },
            serviceDetails = "مريض واحد • زيارة منزلية",
            patientName = patientName.ifBlank { "مريض" },
            patientPhone = patientPhone,
            providerName = provider.name,
            providerTitle = provider.title,
            providerPhone = provider.phoneNumber,
            areaLocation = address,
            visitDate = date,
            visitTime = time,
            notes = notes,
            priceSdg = provider.priceSdg,
            payableAmountSdg = OrderFees.payableAmountSdg(provider.priceSdg, payableSuffix),
            providerPayoutSdg = OrderFees.providerPayoutSdg(provider.priceSdg),
            commissionSdg = OrderFees.commissionSdg(provider.priceSdg),
            paymentMethod = _uiState.value.selectedPaymentMethod,
            status = "ORDER_SENT",
            providerId = provider.id
        )

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreatingBooking = true)
            try {
                val id = repository.createOrder(newOrder)
                val savedOrder = repository.getOrderById(id) ?: newOrder.copy(id = id)
                _uiState.value = _uiState.value.copy(
                    selectedOrder = savedOrder,
                    transferSenderName = "",
                    transferRefNum = "",
                    receiptLocalUri = null,
                    selectedPaymentMethod = "بنكك (Bankak)",
                    notificationMessage = "تم إرسال الطلب بنجاح برقم $newOrderNum",
                    navigationEvent = com.example.ui.navigation.Routes.ORDER_SUCCESS
                )
                notifyProvider(
                    order = savedOrder,
                    type = "ORDER",
                    title = "طلب جديد",
                    body = "${newOrder.patientName} طلب ${newOrder.serviceTitle} في ${newOrder.areaLocation}" +
                        " يوم $date الساعة $time."
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    notificationMessage = "تعذر إرسال الطلب، تحقق من الاتصال وحاول مرة أخرى"
                )
            } finally {
                _uiState.value = _uiState.value.copy(isCreatingBooking = false)
            }
        }
    }

    fun submitPaymentReceiptForReview() {
        val currentOrder = _uiState.value.selectedOrder ?: return
        val localUri = _uiState.value.receiptLocalUri ?: return
        if (_uiState.value.isUploadingReceipt) return
        if (currentOrder.status !in setOf(
                OrderStatus.ACCEPTED_BY_PROVIDER,
                OrderStatus.PAYMENT_PENDING,
                OrderStatus.REJECTED
            )
        ) {
            _uiState.value = _uiState.value.copy(
                notificationMessage = "انتظر قبول مقدم الخدمة قبل إكمال الدفع"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUploadingReceipt = true)
            try {
                val receiptUrl = storage.uploadReceipt(currentOrder.id, Uri.parse(localUri))
                val updatedOrder = currentOrder.copy(
                    status = OrderStatus.PAYMENT_UNDER_REVIEW,
                    paymentMethod = _uiState.value.selectedPaymentMethod,
                    transferSenderName = _uiState.value.transferSenderName,
                    transferRefNum = _uiState.value.transferRefNum,
                    receiptImageUri = receiptUrl
                )

                repository.updateOrder(updatedOrder)
                _uiState.value = _uiState.value.copy(
                    selectedOrder = updatedOrder,
                    notificationMessage = "تم رفع إشعار التحويل وهو قيد المراجعة من قبل الإدارة",
                    navigationEvent = com.example.ui.navigation.Routes.PAYMENT_REVIEW
                )
                notifyAdmins(
                    orderId = updatedOrder.id,
                    title = "إشعار تحويل بانتظار المراجعة",
                    body = "${updatedOrder.patientName} رفع إشعار تحويل للطلب #${updatedOrder.orderNumber}" +
                        " بمبلغ ${OrderFees.effectivePayableAmountSdg(updatedOrder).toLong()} ج.س. راجع الصورة ثم اعتمد الدفع أو ارفضه."
                )
            } catch (e: Exception) {
                android.util.Log.e("SehatiViewModel", "receipt submission failed", e)
                _uiState.value = _uiState.value.copy(
                    notificationMessage = (e as? IllegalStateException)?.message
                        ?: "تعذر إرسال إشعار التحويل، تحقق من الاتصال وحاول مرة أخرى"
                )
            } finally {
                _uiState.value = _uiState.value.copy(isUploadingReceipt = false)
            }
        }
    }

    fun approvePaymentByAdmin(orderId: String) {
        runOrderAction(orderId) {
            val current = repository.getOrderById(orderId)
            if (current?.status != OrderStatus.PAYMENT_UNDER_REVIEW || current.receiptImageUri.isNullOrBlank()) {
                _uiState.value = _uiState.value.copy(
                    notificationMessage = "لا يمكن اعتماد طلب لا يحتوي على إشعار تحويل قيد المراجعة"
                )
                return@runOrderAction
            }
            repository.updateOrderStatus(orderId, OrderStatus.PAYMENT_CONFIRMED)
            val updated = repository.getOrderById(orderId)
            if (_uiState.value.selectedOrder?.id == orderId) {
                _uiState.value = _uiState.value.copy(selectedOrder = updated)
            }
            _uiState.value = _uiState.value.copy(
                notificationMessage = "تم اعتماد الدفع بنجاح للطلب #${updated?.orderNumber}"
            )
            notifyPatient(
                order = updated,
                type = "PAYMENT",
                title = "تم اعتماد الدفع",
                body = "تم تأكيد دفعتك للطلب #${updated?.orderNumber}، سيتواصل معك مقدم الخدمة قبل الزيارة."
            )
        }
    }

    fun rejectPaymentByAdmin(orderId: String) {
        runOrderAction(orderId) {
            val current = repository.getOrderById(orderId)
            if (current?.status != OrderStatus.PAYMENT_UNDER_REVIEW) {
                _uiState.value = _uiState.value.copy(
                    notificationMessage = "هذا الطلب ليس بانتظار مراجعة الدفع"
                )
                return@runOrderAction
            }
            repository.updateOrderStatus(orderId, OrderStatus.REJECTED)
            val updated = repository.getOrderById(orderId)
            _uiState.value = _uiState.value.copy(
                notificationMessage = "تم رفض/طلب توضيح لإشعار الطلب"
            )
            notifyPatient(
                order = updated,
                type = "PAYMENT",
                title = "إشعار التحويل يحتاج توضيح",
                body = "لم نتمكن من مطابقة إشعار التحويل للطلب #${updated?.orderNumber}، يرجى إعادة رفع الصورة."
            )
        }
    }

    fun acceptOrderByProvider(orderId: String) {
        runOrderAction(orderId) {
            val current = repository.getOrderById(orderId)
            if (current?.status != OrderStatus.ORDER_SENT) {
                _uiState.value = _uiState.value.copy(notificationMessage = "تمت معالجة هذا الطلب بالفعل")
                return@runOrderAction
            }
            repository.updateOrderStatus(orderId, OrderStatus.ACCEPTED_BY_PROVIDER)
            val updated = repository.getOrderById(orderId)
            _uiState.value = _uiState.value.copy(
                notificationMessage = "تم قبول الطلب بواسطة مقدم الخدمة!"
            )
            notifyPatient(
                order = updated,
                type = "PROVIDER",
                title = "تم قبول طلبك",
                body = "${updated?.providerName} قبل طلبك #${updated?.orderNumber} وسيصل في الموعد المحدد."
            )
        }
    }

    fun completeOrderByProvider(orderId: String) {
        runOrderAction(orderId) {
            val current = repository.getOrderById(orderId)
            if (current == null || !OrderStatus.canProviderComplete(current.status)) {
                _uiState.value = _uiState.value.copy(
                    notificationMessage = "لا يمكن إكمال الزيارة قبل تأكيد الدفع"
                )
                return@runOrderAction
            }
            repository.updateOrderStatus(orderId, OrderStatus.COMPLETED)
            val updated = repository.getOrderById(orderId)
            // The visit is done, so what it owes the provider is recorded immediately. A failed
            // accrual must not roll the visit back — it is logged and the finance screen still
            // shows the collected side, so the gap stays visible instead of silent.
            if (updated != null) {
                try {
                    payouts.accrueForCompletedOrder(updated)
                } catch (e: Exception) {
                    android.util.Log.w("SehatiViewModel", "payout accrual failed for $orderId", e)
                }
            }
            if (_uiState.value.selectedOrder?.id == orderId) {
                _uiState.value = _uiState.value.copy(selectedOrder = updated)
            }
            _uiState.value = _uiState.value.copy(
                notificationMessage = "تم إكمال الزيارة بنجاح"
            )
            notifyPatient(
                order = updated,
                type = "ORDER",
                title = "تمت الزيارة",
                body = "تم إكمال زيارة الطلب #${updated?.orderNumber}، شاركنا تقييمك للخدمة."
            )
        }
    }

    /**
     * Patients back out from [com.example.ui.screens.MyOrdersScreen]. The status is re-read rather
     * than trusted from the tapped card, because the provider may have moved the order on while
     * the confirmation dialog was open.
     */
    fun cancelOrderByPatient(orderId: String, reason: String) {        runOrderAction(orderId) {
            val order = repository.getOrderById(orderId) ?: return@runOrderAction
            if (!OrderStatus.canPatientCancel(order.status)) {
                _uiState.value = _uiState.value.copy(
                    notificationMessage = "لا يمكن إلغاء هذا الطلب في وضعه الحالي، تواصل مع الإدارة"
                )
                return@runOrderAction
            }

            repository.cancelOrder(orderId, OrderStatus.BY_PATIENT, reason)
            val updated = repository.getOrderById(orderId)
            if (_uiState.value.selectedOrder?.id == orderId) {
                _uiState.value = _uiState.value.copy(selectedOrder = updated)
            }
            _uiState.value = _uiState.value.copy(
                notificationMessage = "تم إلغاء الطلب #${order.orderNumber}"
            )
            notifyProvider(
                order = updated,
                type = "ORDER",
                title = "ألغى المريض الطلب",
                body = "تم إلغاء الطلب #${order.orderNumber}" +
                    if (reason.isNotBlank()) " — السبب: $reason" else ""
            )
        }
    }

    /** A provider turning down a request they have not accepted yet. */
    fun declineOrderByProvider(orderId: String, reason: String) {
        runOrderAction(orderId) {
            val order = repository.getOrderById(orderId) ?: return@runOrderAction
            if (!OrderStatus.canProviderDecline(order.status)) {
                _uiState.value = _uiState.value.copy(
                    notificationMessage = "لا يمكن الاعتذار عن طلب تم قبوله بالفعل"
                )
                return@runOrderAction
            }

            repository.cancelOrder(orderId, OrderStatus.BY_PROVIDER, reason)
            _uiState.value = _uiState.value.copy(
                notificationMessage = "تم الاعتذار عن الطلب #${order.orderNumber}"
            )
            val why = if (reason.isNotBlank()) " — السبب: $reason" else ""
            notifyPatient(
                order = repository.getOrderById(orderId),
                type = "ORDER",
                title = "اعتذر مقدم الخدمة",
                body = "لم يتمكن ${order.providerName} من تنفيذ الطلب #${order.orderNumber}$why" +
                    "، يمكنك اختيار مقدم خدمة آخر."
            )
        }
    }

    /**
     * Patient asks for their money back while it is held and before the visit. Only flags the
     * request — the actual Bankak return transfer is an admin action outside the app, so the
     * order parks in REFUND_REQUESTED until an admin grants (cancels) or declines (resumes).
     */
    fun requestRefundByPatient(orderId: String, reason: String) {
        runOrderAction(orderId) {
            val order = repository.getOrderById(orderId) ?: return@runOrderAction
            if (!OrderStatus.canPatientRequestRefund(order.status)) {
                _uiState.value = _uiState.value.copy(
                    notificationMessage = "لا يمكن طلب الاسترجاع في وضع هذا الطلب الحالي"
                )
                return@runOrderAction
            }

            // The refund motive rides in cancelReason: the field is already shown to admins
            // reviewing a walk-away, which is exactly the context they need here.
            firestoreUpdateRefundRequest(
                orderId = orderId,
                status = OrderStatus.REFUND_REQUESTED,
                reason = reason
            )
            val updated = repository.getOrderById(orderId)
            if (_uiState.value.selectedOrder?.id == orderId) {
                _uiState.value = _uiState.value.copy(selectedOrder = updated)
            }
            _uiState.value = _uiState.value.copy(
                notificationMessage = "تم إرسال طلب الاسترجاع للإدارة وسيصلك رد قريباً"
            )
            notifyAdmins(
                orderId = orderId,
                title = "طلب استرجاع جديد",
                body = "${order.patientName} يطلب استرجاع مبلغ الطلب #${order.orderNumber}" +
                    if (reason.isNotBlank()) " — السبب: $reason" else "."
            )
        }
    }

    /** Admin resolves a refund: [grant] sends the money back path (cancel), declining resumes. */
    fun resolveRefundByAdmin(orderId: String, grant: Boolean) {
        runOrderAction(orderId) {
            val current = repository.getOrderById(orderId)
            if (current == null || current.status != OrderStatus.REFUND_REQUESTED) {
                _uiState.value = _uiState.value.copy(
                    notificationMessage = "هذا الطلب ليس بانتظار قرار استرجاع"
                )
                return@runOrderAction
            }

            if (grant) {
                repository.cancelOrder(orderId, OrderStatus.BY_ADMIN,
                    current.cancelReason.ifBlank { "قبول طلب استرجاع المريض" })
                notifyProvider(
                    order = current,
                    type = "ORDER",
                    title = "إلغاء طلب مجدول",
                    body = "ألغيت الإدارة الطلب #${current.orderNumber} بعد قبول استرجاع المبلغ، " +
                        "ولن تُصرف مستحقاته."
                )
                notifyPatient(
                    order = current,
                    type = "PAYMENT",
                    title = "تم قبول طلب الاسترجاع",
                    body = "سيعاد مبلغ الطلب #${current.orderNumber} إلى حسابك عبر بنكك خلال 3 أيام عمل."
                )
            } else {
                repository.updateOrderStatus(orderId, OrderStatus.PAYMENT_CONFIRMED)
                notifyPatient(
                    order = current,
                    type = "PAYMENT",
                    title = "لم يُقبل طلب الاسترجاع",
                    body = "بعد المراجعة سيستمر الطلب #${current.orderNumber} كما هو، تواصل مع الدعم لتفاصيل أكثر."
                )
            }

            val updated = repository.getOrderById(orderId)
            if (_uiState.value.selectedOrder?.id == orderId) {
                _uiState.value = _uiState.value.copy(selectedOrder = updated)
            }
            _uiState.value = _uiState.value.copy(
                notificationMessage = if (grant) "تم قبول الاسترجاع وإلغاء الطلب" else "تم رفض طلب الاسترجاع واستمرار الحجز"
            )
        }
    }

    private suspend fun firestoreUpdateRefundRequest(orderId: String, status: String, reason: String) {
        if (orderId.isEmpty()) return
        repository.updateOrder(
            (repository.getOrderById(orderId) ?: return).copy(
                status = status,
                cancelReason = reason
            )
        )
    }

    /**
     * Marks the order busy for the duration so its card can disable its buttons — without it a
     * double tap fires the same status write twice and sends the patient two notifications.
     */
    private fun runOrderAction(orderId: String, block: suspend () -> Unit) {
        if (_uiState.value.busyOrderId != null) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busyOrderId = orderId)
            try {
                block()
            } catch (e: Exception) {
                android.util.Log.w("SehatiViewModel", "order action failed for $orderId", e)
                _uiState.value = _uiState.value.copy(
                    notificationMessage = "تعذر تنفيذ العملية، تحقق من الاتصال وحاول مرة أخرى"
                )
            } finally {
                _uiState.value = _uiState.value.copy(busyOrderId = null)
            }
        }
    }

    /**
     * Notifications are written by whoever changes the status (no Cloud Functions), so a
     * failed write must not roll back or surface over the status change that already succeeded.
     */
    private suspend fun notifyPatient(
        order: OrderEntity?,
        type: String,
        title: String,
        body: String
    ) {
        val patientUid = order?.patientUid ?: return
        if (patientUid.isEmpty()) return

        try {
            notifications.push(
                patientUid,
                NotificationEntity(type = type, title = title, body = body, orderId = order.id)
            )
        } catch (e: Exception) {
            android.util.Log.w("SehatiViewModel", "patient notification failed for order=${order.id}", e)
        }
    }

    /** Mirrors [notifyPatient] for the other side: the provider's inbox is keyed by their owner uid. */
    private suspend fun notifyProvider(
        order: OrderEntity?,
        type: String,
        title: String,
        body: String
    ) {
        val providerId = order?.providerId ?: return
        if (providerId.isEmpty()) return

        try {
            val ownerUid = repository.getProviderById(providerId)?.ownerUid ?: return
            if (ownerUid.isEmpty()) return
            notifications.push(
                ownerUid,
                NotificationEntity(type = type, title = title, body = body, orderId = order.id)
            )
        } catch (e: Exception) {
            android.util.Log.w("SehatiViewModel", "provider notification failed for order=${order.id}", e)
        }
    }

    /** No uid to address, so this lands in the shared inbox any admin can read. */
    private suspend fun notifyAdmins(orderId: String, title: String, body: String) {
        try {
            notifications.pushToAdmins(
                NotificationEntity(type = "PAYMENT", title = title, body = body, orderId = orderId)
            )
        } catch (e: Exception) {
            android.util.Log.w("SehatiViewModel", "admin notification failed for order=$orderId", e)
        }
    }

    fun clearNotification() {
        _uiState.value = _uiState.value.copy(notificationMessage = null)
    }

    fun consumeNavigationEvent() {
        _uiState.value = _uiState.value.copy(navigationEvent = null)
    }

    fun clearSession() {
        ordersJob?.cancel()
        providersJob?.cancel()
        ordersJob = null
        providersJob = null
        orderScope = null
        currentProviderId = ""
        _uiState.value = SehatiUiState()
    }
}
