package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.NotificationEntity
import com.example.data.model.OrderEntity
import com.example.data.model.RatingEntity
import com.example.data.repository.FirestoreNotificationRepository
import com.example.data.repository.FirestoreRatingRepository
import com.example.data.repository.NotificationRepository
import com.example.data.repository.RatingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class RatingUiState(
    val isSubmitting: Boolean = false,
    val isSubmitted: Boolean = false,
    val errorMessage: String? = null,
    val reviews: List<RatingEntity> = emptyList()
)

class RatingViewModel @JvmOverloads constructor(
    private val repository: RatingRepository = FirestoreRatingRepository(),
    private val notificationRepository: NotificationRepository = FirestoreNotificationRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(RatingUiState())
    val uiState: StateFlow<RatingUiState> = _uiState.asStateFlow()

    fun loadReviews(providerId: String) {
        if (providerId.isEmpty()) return
        viewModelScope.launch {
            repository.ratingsForProvider(providerId)
                .catch { _uiState.value = _uiState.value.copy(reviews = emptyList()) }
                .collectLatest { list ->
                    _uiState.value = _uiState.value.copy(reviews = list)
                }
        }
    }

    fun submitRating(order: OrderEntity, stars: Int, chips: List<String>, comment: String) {
        if (stars !in 1..5) {
            _uiState.value = _uiState.value.copy(errorMessage = "يرجى اختيار عدد النجوم")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, errorMessage = null)

            try {
                repository.submitRating(
                    RatingEntity(
                        orderId = order.id,
                        providerId = order.providerId,
                        patientName = order.patientName,
                        stars = stars,
                        chips = chips,
                        comment = comment.trim()
                    )
                )
                _uiState.value = _uiState.value.copy(isSubmitting = false, isSubmitted = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    errorMessage = "تعذر إرسال التقييم، حاول مرة أخرى"
                )
                return@launch
            }

            // The rating is already committed; a failed inbox write must not report failure.
            try {
                notificationRepository.push(
                    order.patientUid,
                    NotificationEntity(
                        type = "SYSTEM",
                        title = "شكراً لتقييمك",
                        body = "تم تسجيل تقييمك لخدمة ${order.serviceTitle}. رأيك يساعد مرضى آخرين.",
                        orderId = order.id
                    )
                )
            } catch (e: Exception) {
                // Best-effort.
            }
        }
    }

    fun consumeSubmitted() {
        _uiState.value = _uiState.value.copy(isSubmitted = false)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
