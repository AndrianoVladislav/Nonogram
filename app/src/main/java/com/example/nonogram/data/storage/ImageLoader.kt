package com.example.nonogram.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi

object ImageLoader {

    /**
     * Загружает Bitmap по Uri (выбранному из галереи или файловой системы)
     */
    @RequiresApi(Build.VERSION_CODES.P)
    fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
