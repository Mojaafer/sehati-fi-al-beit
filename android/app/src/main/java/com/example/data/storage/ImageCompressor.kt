package com.example.data.storage

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream

/** Longest edge kept, then the byte budget the encoded JPEG has to fit. */
private const val MAX_DIMENSION = 1280
private const val MAX_BYTES = 400_000

/**
 * Downscales then re-encodes [imageUri] until the JPEG fits [MAX_BYTES].
 *
 * Both backends keep images inline rather than in object storage — base64 in a Firestore
 * document, base64 in a Postgres row — and a Firestore document is capped at 1 MiB while base64
 * inflates by a third, so the budget is deliberately modest. This lived twice, once per
 * repository, where the two copies could quietly drift to different limits.
 */
internal fun compressForInlineStorage(resolver: ContentResolver, imageUri: Uri): ByteArray {
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
