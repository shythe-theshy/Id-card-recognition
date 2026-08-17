package com.example.tryagian

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.IOException

object ImageUtils {

    // 从文件路径采样解码，避免 OOM
    fun decodeSampledBitmapFromFile(path: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
            BitmapFactory.decodeFile(path, this)
            inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)
            inJustDecodeBounds = false
        }
        return BitmapFactory.decodeFile(path, options)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height, width) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    fun bitmapToBase64(bitmap: Bitmap): String {
        ByteArrayOutputStream().use { baos ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
            return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
        }
    }

    @Throws(IOException::class)
    fun getBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        context.contentResolver.openInputStream(uri)?.use { input ->
            return BitmapFactory.decodeStream(input)
        }
        return null
    }

    // 压缩到最大长边尺寸（像素）
    fun compressBitmap(bitmap: Bitmap, maxSize: Int): Bitmap? {
        val (width, height) = bitmap.width to bitmap.height
        val maxDimension = maxOf(width, height)
        if (maxDimension <= maxSize) return bitmap
        val scale = maxSize.toFloat() / maxDimension
        return Bitmap.createScaledBitmap(bitmap, (width * scale).toInt(), (height * scale).toInt(), true)
    }
}
