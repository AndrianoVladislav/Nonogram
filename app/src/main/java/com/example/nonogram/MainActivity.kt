package com.example.nonogram

import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.nonogram.data.converter.ImageConverter
import com.example.nonogram.data.storage.ImageLoader
import com.example.nonogram.domain.entity.Game
import com.example.nonogram.ui.GameScreen

class MainActivity : ComponentActivity() {

    private lateinit var game: Game

    @RequiresApi(Build.VERSION_CODES.P)
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { handleImageUri(it) }
        }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Запускаем выбор изображения при старте (можно заменить на кнопку)
        pickImageLauncher.launch("image/*")
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun handleImageUri(uri: android.net.Uri) {
        val bitmap: Bitmap? = ImageLoader.loadBitmapFromUri(this, uri)

        bitmap?.let {
            // Конвертируем изображение в блоки 25x25
            val matrix = ImageConverter.convertToMatrix(it)
            val blocks = ImageConverter.convertMatrixToBlocks(matrix)

            if (blocks.isNotEmpty()) {
                // Создаём Game для первого блока
                game = Game(blocks[0])

                // Отображаем экран игры через Compose
                setContent {
                    MaterialTheme {
                        Surface(color = MaterialTheme.colorScheme.background) {
                            GameScreen(game)
                        }
                    }
                }
            }
        }
    }
}
