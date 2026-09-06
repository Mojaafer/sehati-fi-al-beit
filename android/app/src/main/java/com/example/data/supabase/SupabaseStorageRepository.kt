package com.example.data.supabase

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.example.data.storage.FIRESTORE_IMAGE_SCHEME
import com.example.data.storage.StorageRepository
import com.example.data.storage.compressForInlineStorage
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

const val SUPABASE_IMAGE_SCHEME = "spimg://"

class SupabaseStorageRepository(
    private val context: Context,
    private val api: SupabaseApiService = SupabaseClient.api,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : StorageRepository {

    override suspend fun uploadReceipt(orderId: String, imageUri: Uri): String =
        store(imageUri, "receipt", orderId, "لا يمكن رفع الإشعار قبل تسجيل الدخول")

    override suspend fun uploadProviderDoc(docType: String, imageUri: Uri): String =
        store(imageUri, "provider_doc", docType, "لا يمكن رفع المستند قبل تسجيل الدخول")

    override suspend fun loadImage(reference: String): ByteArray? {
        val id = when {
            reference.startsWith(SUPABASE_IMAGE_SCHEME) -> reference.removePrefix(SUPABASE_IMAGE_SCHEME)
            reference.startsWith(FIRESTORE_IMAGE_SCHEME) -> reference.removePrefix(FIRESTORE_IMAGE_SCHEME)
            else -> return null
        }

        val images = try {
            api.getImage("eq.$id")
        } catch (_: Exception) {
            return null
        }

        val encoded = images.firstOrNull()?.data ?: return null
        return runCatching { Base64.decode(encoded, Base64.NO_WRAP) }.getOrNull()
    }

    private suspend fun store(
        imageUri: Uri,
        kind: String,
        label: String,
        signedOutMessage: String
    ): String {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException(signedOutMessage)
        val jpeg = withContext(Dispatchers.IO) { compressForInlineStorage(context.contentResolver, imageUri) }

        val imageId = UUID.randomUUID().toString()
        val dto = SupabaseImageDto(
            id = imageId,
            ownerUid = uid,
            data = Base64.encodeToString(jpeg, Base64.NO_WRAP),
            createdAtTimestamp = System.currentTimeMillis()
        )

        val inserted = api.insertImage(dto)
        val finalId = inserted.firstOrNull()?.id ?: imageId

        return "$SUPABASE_IMAGE_SCHEME$finalId"
    }
}
