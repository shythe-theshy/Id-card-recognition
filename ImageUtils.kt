package com.example.tryagian

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

object ImageUtils {

    fun bitmapToBase64(bitmap: Bitmap): String {
        ByteArrayOutputStream().use { baos ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
            val byteArray = baos.toByteArray()
            return Base64.encodeToString(byteArray, Base64.DEFAULT)
        }
    }

    @Throws(IOException::class)
    fun getBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        context.contentResolver.openInputStream(uri)?.use { input ->
            return BitmapFactory.decodeStream(input)
        }
        return null
    }

    fun compressBitmap(bitmap: Bitmap, maxSize: Int): Bitmap? {
        ByteArrayOutputStream().use { baos ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)

            var quality = 100
            while (baos.toByteArray().size > maxSize * 1024 && quality > 10) {
                baos.reset()
                quality -= 10
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
            }

            val compressedBytes = baos.toByteArray()
            return BitmapFactory.decodeByteArray(compressedBytes, 0, compressedBytes.size)
        }
    }
}