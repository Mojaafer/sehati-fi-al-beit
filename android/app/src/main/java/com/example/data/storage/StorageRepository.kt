package com.example.data.storage

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

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
        val jpeg = withContext(Dispatchers.IO) { compress(imageUri) }

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

    /**
     * Downscales then re-encodes until the result fits [MAX_BYTES]. A Firestore document is
     * capped at 1 MiB and base64 inflates by a third, so the budget is deliberately modest.
     */
    private fun compress(imageUri: Uri): ByteArray {
        val resolver = FirebaseApp.getInstance().applicationContext.contentResolver

        // decodeStream returns null whenever inJustDecodeBounds is set, so the stream itself is
        // what gets null-checked here; the measured size lands in `bounds`.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsStream = resolver.openInputStream(imageUri)
            ?: throw IllegalStateException("تعذر قراءة الصورة المختارة")
        boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IllegalStateException("تعذر قراءة الصورة المختارة")
        }

        var sample = 1
        while (bounds.outWidth / sample > MAX_DIMENSION || bounds.outHeight / sample > MAX_DIMENSION) {
            sample *= 2
        }

        val bitmap = resolver.openInputStream(imageUri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: throw IllegalStateException("تعذر قراءة الصورة المختارة")

        try {
            for (quality in intArrayOf(80, 70, 60, 50, 40, 30)) {
                val out = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                if (out.size() <= MAX_BYTES) return out.toByteArray()
            }
            throw IllegalStateException("الصورة كبيرة جداً، جرّب صورة أوضح وأصغر")
        } finally {
            bitmap.recycle()
        }
    }

    private companion object {
        const val MAX_DIMENSION = 1280
        const val MAX_BYTES = 400_000
    }
}
