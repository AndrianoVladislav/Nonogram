package com.example.nonogram

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.example.nonogram.data.storage.PuzzleItem
import com.example.nonogram.data.storage.StorageManager
import com.example.nonogram.ui.AppNavigation

class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val storageManager = StorageManager(this)

        setContent {

            val puzzlesState = remember { mutableStateOf(emptyList<PuzzleItem>()) }

            fun reload() {
                puzzlesState.value = storageManager.loadMeta()
            }

            LaunchedEffect(Unit) {
                reload()
            }

            AppNavigation(
                puzzles = puzzlesState.value,
                storageManager = storageManager,
                onDataChanged = { reload() } // 🔥 ключевой момент
            )
        }
    }
}

//class MainActivity : ComponentActivity() {
//
//    private lateinit var game: Game
//
//    @RequiresApi(Build.VERSION_CODES.P)
//    private val pickImageLauncher =
//        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
//            uri?.let { handleImageUri(it) }
//        }
//
//    @RequiresApi(Build.VERSION_CODES.P)
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        // Запускаем выбор изображения при старте (можно заменить на кнопку)
//        pickImageLauncher.launch("image/*")
//    }
//
//    @RequiresApi(Build.VERSION_CODES.P)
//    private fun handleImageUri(uri: android.net.Uri) {
//        val bitmap: Bitmap? = ImageLoader.loadBitmapFromUri(this, uri)
//
//        bitmap?.let {
//            // Конвертируем изображение в блоки 25x25
//            val resizedImage = ImageConverterResize.resizeToBlocks(it)
//            val quantizationImage = ImageConverterQuantization.quantizeKMeans(resizedImage)
//            val blocks = ImageConverterBlock.splitToBlocks(quantizationImage)
//
//            if (blocks.isNotEmpty()) {
//                // Создаём Game для первого блока
//                game = Game(blocks[0])
//
//                // Отображаем экран игры через Compose
//                setContent {
//
//                    StartScreen(
//                        onCollectionClick = { /* TODO */ },
//                        onSettingsClick = { /* TODO */ }
//                    )
////                    MaterialTheme {
////                        Surface(color = MaterialTheme.colorScheme.background) {
////                            GameScreen(game)
////                        }
////                    }
//                }
//            }
//        }
//    }
//}
