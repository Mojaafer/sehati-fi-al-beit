package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.NotificationEntity
import com.example.data.model.ProviderEntity
import com.example.data.repository.NotificationRepository
import com.example.data.repository.ProviderRegistrationRepository
import com.example.data.storage.StorageRepository
import com.example.data.supabase.RepositoryFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProviderRegistrationUiState(
    val providerId: String = "",
    val providerName: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    /** Local content:// uris keyed by document type, before upload. */
    val docUris: Map<String, String> = emptyMap(),
    val basicInfoSaved: Boolean = false,
    val docsSubmitted: Boolean = false,
    val pendingApplications: List<ProviderEntity> = emptyList()
)

class ProviderRegistrationViewModel @JvmOverloads constructor(
    private val repository: ProviderRegistrationRepository = RepositoryFactory.createProviderRegistrationRepository(),
    storageRepository: StorageRepository? = null,
    private val notifications: NotificationRepository = RepositoryFactory.createNotificationRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProviderRegistrationUiState())
    val uiState: StateFlow<ProviderRegistrationUiState> = _uiState.asStateFlow()

    // Lazy so unit tests supplying a fake repository never touch Firebase singletons.
    private val storage: StorageRepository by lazy { storageRepository ?: RepositoryFactory.createStorageRepository() }

    fun submitBasicInfo(
        name: String,
        title: String,
        category: String,
        experienceYears: Int,
        priceSdg: Double,
        area: String,
        about: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, errorMessage = null)

            try {
                val id = repository.registerProvider(
                    ProviderEntity(
                        name = name,
                        title = title,
                        serviceCategory = category,
                        experienceYears = experienceYears,
                        priceSdg = priceSdg,
                        area = area,
                        about = about,
                        isAvailableNow = false
                    )
                )
                _uiState.value = _uiState.value.copy(
                    providerId = id,
                    providerName = name,
                    isSubmitting = false,
                    basicInfoSaved = true
                )

                // Nobody is watching the providers collection, so without this the application
                // would sit in PENDING_REVIEW until an admin happened to open their dashboard.
                notifyAdmins(
                    providerId = id,
                    title = "طلب انتساب جديد",
                    body = "$name — $title في $area. راجع المستندات ثم اعتمد الطلب أو ارفضه."
                )
            } catch (e: IllegalStateException) {
                // Reaching this screen while signed out is possible from the login link.
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    errorMessage = e.message ?: "تعذر حفظ البيانات"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    errorMessage = "تعذر حفظ البيانات، تأكد من اتصالك بالإنترنت"
                )
            }
        }
    }

    fun setDocUri(docType: String, uri: String) {
        _uiState.value = _uiState.value.copy(
            docUris = _uiState.value.docUris + (docType to uri)
        )
    }

    fun submitDocuments() {
        val providerId = _uiState.value.providerId
        val docUris = _uiState.value.docUris
        if (providerId.isEmpty() || docUris.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, errorMessage = null)

            try {
                val uploaded = docUris.mapValues { (docType, localUri) ->
                    storage.uploadProviderDoc(docType, Uri.parse(localUri))
                }
                repository.attachDocuments(providerId, uploaded)
                _uiState.value = _uiState.value.copy(isSubmitting = false, docsSubmitted = true)
            } catch (e: Exception) {
                android.util.Log.e("ProviderRegistration", "document upload failed", e)
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    errorMessage = (e as? IllegalStateException)?.message
                        ?: "تعذر رفع المستندات، حاول مرة أخرى"
                )
            }
        }
    }

    fun loadPendingApplications() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                pendingApplications = repository.pendingApplications()
            )
        }
    }

    fun approveProvider(providerId: String) {
        decide(providerId, "تعذر اعتماد مقدم الخدمة") {
            val approved = repository.approveProvider(providerId)
            notifyApplicant(
                provider = approved,
                title = "تم اعتماد طلبك ✓",
                body = "تمت الموافقة على انتسابك كمقدم خدمة. سجّل الخروج ثم الدخول مرة أخرى لتظهر لوحة العمل."
            )
        }
    }

    fun rejectProvider(providerId: String, reason: String) {
        decide(providerId, "تعذر رفض الطلب") {
            val rejected = repository.rejectProvider(providerId, reason)
            notifyApplicant(
                provider = rejected,
                title = "طلب الانتساب يحتاج مراجعة",
                body = if (reason.isBlank()) {
                    "لم تتم الموافقة على طلب الانتساب. تواصل مع الإدارة لمعرفة التفاصيل."
                } else {
                    "لم تتم الموافقة على طلب الانتساب. السبب: $reason"
                }
            )
        }
    }

    /**
     * Both decisions drop the application off the admin's list on success and word their own
     * failure, so the only thing that differs is the call and the message.
     */
    private fun decide(providerId: String, failureMessage: String, action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                action()
                _uiState.value = _uiState.value.copy(
                    pendingApplications = _uiState.value.pendingApplications.filterNot { it.id == providerId }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = failureMessage)
            }
        }
    }

    /** The decision is already committed, so a failed inbox write must not report failure. */
    private suspend fun notifyApplicant(provider: ProviderEntity?, title: String, body: String) {
        val ownerUid = provider?.ownerUid ?: return
        if (ownerUid.isEmpty()) return
        try {
            notifications.push(
                ownerUid,
                NotificationEntity(
                    type = "PROVIDER",
                    title = title,
                    body = body,
                    providerId = provider.id
                )
            )
        } catch (e: Exception) {
            // Best-effort delivery.
        }
    }

    private suspend fun notifyAdmins(providerId: String, title: String, body: String) {
        try {
            notifications.pushToAdmins(
                NotificationEntity(
                    type = "PROVIDER",
                    title = title,
                    body = body,
                    providerId = providerId
                )
            )
        } catch (e: Exception) {
            // The application is already filed; an undelivered alert must not fail the wizard.
        }
    }

    fun consumeBasicInfoSaved() {
        _uiState.value = _uiState.value.copy(basicInfoSaved = false)
    }

    fun consumeDocsSubmitted() {
        _uiState.value = _uiState.value.copy(docsSubmitted = false)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
