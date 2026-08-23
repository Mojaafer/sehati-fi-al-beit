package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import com.example.data.storage.FIRESTORE_IMAGE_SCHEME
import com.example.data.storage.FirestoreImageRepository
import com.example.data.storage.StorageRepository

/**
 * Resolves an image reference into something Coil can render. A `fsimg://` reference is
 * fetched from Firestore and handed over as bytes; a local `content://` uri or an http url
 * is passed through untouched.
 */
@Composable
fun rememberImageModel(
    reference: String?,
    repository: StorageRepository = remember { FirestoreImageRepository() }
): State<Any?> {
    if (reference == null) return remember { mutableStateOf(null) }
    if (!reference.startsWith(FIRESTORE_IMAGE_SCHEME)) {
        return remember(reference) { mutableStateOf(reference) }
    }
    return produceState<Any?>(initialValue = null, reference) {
        value = runCatching { repository.loadImage(reference) }.getOrNull()
    }
}



