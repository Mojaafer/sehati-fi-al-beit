package com.example.data.storage

import android.net.Uri
import android.util.Base64
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/** Marks a reference that resolves to a document in the `images` collection. */
const val FIRESTORE_IMAGE_SCHEME = "fsimg://"

interface StorageRepository {
    suspend fun uploadReceipt(orderId: String, imageUri: Uri): String
    suspend fun uploadProviderDoc(docType: String, imageUri: Uri): String
    suspend fun loadImage(reference: String): ByteArray?
}

/**
 * Stores images as base64 inside Firestore instead of Cloud Storage, which needs a paid
 * plan. Each image is its own document so that listing orders or providers does not drag
 * the payload along; callers keep only a short `fsimg://` reference.
 */
class FirestoreImageRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : StorageRepository {

    override suspend fun uploadReceipt(orderId: String, imageUri: Uri): String =
        store(imageUri, "receipt", orderId, "لا يمكن رفع الإشعار قبل تسجيل الدخول")

    override suspend fun uploadProviderDoc(docType: String, imageUri: Uri): String =
        store(imageUri, "provider_doc", docType, "لا يمكن رفع المستند قبل تسجيل الدخول")

    override suspend fun loadImage(reference: String): ByteArray? {
        if (!reference.startsWith(FIRESTORE_IMAGE_SCHEME)) return null
        val id = reference.removePrefix(FIRESTORE_IMAGE_SCHEME)
        val encoded = firestore.collection("images").document(id).get().await()
            .getString("data") ?: return null
        return runCatching { Base64.decode(encoded, Base64.NO_WRAP) }.getOrNull()
    }

    private suspend fun store(
        imageUri: Uri,
        kind: String,
        label: String,
        signedOutMessage: String
    ): String {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException(signedOutMessage)
        val resolver = FirebaseApp.getInstance().applicationContext.contentResolver
        val jpeg = withContext(Dispatchers.IO) { compressForInlineStorage(resolver, imageUri) }

        val doc = firestore.collection("images").document()
        doc.set(
            mapOf(
                "ownerUid" to uid,
                "kind" to kind,
                "label" to label,
                "data" to Base64.encodeToString(jpeg, Base64.NO_WRAP),
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()

        return "$FIRESTORE_IMAGE_SCHEME${doc.id}"
    }
}
