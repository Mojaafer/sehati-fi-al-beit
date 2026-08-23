package com.example.service

import com.example.ui.viewmodel.OrderNotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Handles incoming Firebase Cloud Messaging (FCM) background push notifications.
 * Wakes the device and shows a high-priority heads-up banner even if the app is completely closed.
 */
class SehatiMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        saveTokenToFirestore(uid, token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "تنبيه جديد - صحتي في البيت"

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: "لديك تحديث جديد بخصوص الزيارة الطبية."

        val orderId = remoteMessage.data["orderId"] ?: ""

        OrderNotificationHelper.showNotification(
            context = applicationContext,
            title = title,
            body = body,
            orderId = orderId
        )
    }

    companion object {
        fun registerCurrentToken() {
            val auth = FirebaseAuth.getInstance()
            val uid = auth.currentUser?.uid ?: return
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token ->
                    saveTokenToFirestore(uid, token)
                }
        }

        private fun saveTokenToFirestore(uid: String, token: String) {
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(uid)
                .set(mapOf("fcmToken" to token), SetOptions.merge())
        }
    }
}
