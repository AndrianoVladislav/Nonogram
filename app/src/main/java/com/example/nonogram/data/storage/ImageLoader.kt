package com.example.nonogram.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File

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

    fun loadImageBitmapFromFile(path: String): ImageBitmap? {
        return try {
            val file = File(path)
            if (!file.exists()) return null

            BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }
}
