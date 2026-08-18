package com.qrtoolkit.app.data.generate

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object QrExportUtils {

    /** Inserts [bitmap] into the public Pictures/QR Toolkit gallery collection. Returns its content Uri, or null on failure. */
    fun saveToGallery(context: Context, bitmap: Bitmap, displayName: String): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/QR Toolkit")
            }
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        return try {
            resolver.openOutputStream(uri)?.use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
            uri
        } catch (e: Exception) {
            null
        }
    }

    /** Writes [bitmap] to the app's own cache dir and returns a FileProvider content Uri, for sharing without a permanent gallery save. */
    fun shareableUri(context: Context, bitmap: Bitmap, displayName: String): Uri {
        val cacheDir = File(context.cacheDir, "qr_images").apply { mkdirs() }
        val file = File(cacheDir, "$displayName.png")
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}
