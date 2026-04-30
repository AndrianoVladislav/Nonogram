package com.example.nonogram.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.nonogram.R
import com.example.nonogram.data.storage.PuzzleItem
import com.example.nonogram.data.storage.StorageManager
import com.example.nonogram.domain.entity.Game
import com.example.nonogram.domain.entity.SoundManager

@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun AppNavigation(
    puzzles: List<PuzzleItem>,
    storageManager: StorageManager,
    onDataChanged: () -> Unit
) {
    val context = LocalContext.current

    val navController = rememberNavController()
    val soundManager = remember { SoundManager(context) }

    DisposableEffect(Unit) {
        onDispose { soundManager.release() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Фоновое изображение (замените R.drawable.bg_main на ваше имя файла)
        Image(
            painter = painterResource(id = R.drawable.bg_main),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop // Заполняет весь экран, обрезая лишнее
        )

        NavHost(
            navController = navController,
            startDestination = "start",
            modifier = Modifier.fillMaxSize(),
            enterTransition = { fadeIn(animationSpec = tween(700)) },
            exitTransition = { fadeOut(animationSpec = tween(700)) },
            popEnterTransition = { fadeIn(animationSpec = tween(700)) },
            popExitTransition = { fadeOut(animationSpec = tween(700)) }
        ) {

            composable("start") {
                StartScreen(
                    onCollectionClick = {
                        soundManager.playSwitch()
                        navController.navigate("collection")
                    },
                    onSettingsClick = {
                        soundManager.playSwitch()
                    }
                )
            }

            composable("collection") {
                LaunchedEffect(Unit) {
                    onDataChanged()
                }
                CollectionScreen(
                    puzzles = puzzles,
                    storageManager = storageManager,
                    onPlay = { puzzle ->
                        soundManager.playSwitch()
                        navController.navigate("game/${puzzle.id}")
                    },
                    onSend = { puzzle ->
                        soundManager.playSwitch()
                        navController.navigate("send/${puzzle.id}")
                    },
                    onUpload = {
                        soundManager.playSwitch()
                        navController.navigate("upload")
                    }
                )
            }

            composable("game/{id}") { backStack ->
                val id = backStack.arguments?.getString("id")!!
                val puzzle = remember(id) {
                    storageManager.loadMeta().first { it.id == id }
                }

                var currentBlock by remember {
                    mutableIntStateOf(puzzle.currentBlock)
                }
                var restartKey by remember { mutableIntStateOf(0) }
                val block = remember(id, currentBlock) {
                    storageManager.loadBlock(id, currentBlock)!!
                }

                val game = remember(block, restartKey) {
                    Game(block)
                }

                Surface(
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxSize()
                ) {
                    GameScreen(
                        game = game,
                        puzzleId = id,
                        storageManager = storageManager,
                        onNextBlock = {
                            currentBlock++
                        },
                        onRestart = {
                            restartKey++
                        },
                        onExit = {
                            soundManager.playSwitch()
                            onDataChanged()
                            navController.popBackStack()
                        }
                    )
                }
            }

            composable("upload") {
                UploadScreen(
                    storageManager = storageManager,
                    onPuzzleCreated = {
                        soundManager.playSwitch()
                        onDataChanged()
                        navController.popBackStack()
                    }
                )
            }

            composable("send/{id}") { backStack ->
                val id = backStack.arguments?.getString("id")!!
                SendScreen(
                    puzzleId = id,
                    storageManager = storageManager,
                    onDone = {
                        soundManager.playSwitch()
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
