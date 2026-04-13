package com.example.nonogram.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.nonogram.data.storage.PuzzleItem
import com.example.nonogram.data.storage.StorageManager
import com.example.nonogram.domain.entity.Game

@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun AppNavigation(
    puzzles: List<PuzzleItem>,
    storageManager: StorageManager,
    onDataChanged: () -> Unit
) {

    val navController = rememberNavController()

    NavHost(navController, startDestination = "start") {

        composable("start") {
            StartScreen(
                onCollectionClick = {
                    navController.navigate("collection")
                },
                onSettingsClick = {}
            )
        }

        composable("collection") {

            onDataChanged()

            CollectionScreen(
                puzzles = puzzles,
                storageManager = storageManager,

                onPlay = { puzzle ->
                    navController.navigate("game/${puzzle.id}")
                },

                onSend = { puzzle ->
                    navController.navigate("send/${puzzle.id}")
                },

                onUpload = {
                    navController.navigate("upload")
                }
            )
        }

        composable("game/{id}") { backStack ->

            val id = backStack.arguments?.getString("id")!!

            val puzzle = remember {
                storageManager.loadMeta().first { it.id == id }
            }

            var currentBlock by remember {
                mutableIntStateOf(puzzle.currentBlock)
            }

            var restartKey by remember { mutableIntStateOf(0) }

            val block = storageManager.loadBlock(id, currentBlock)!!

            val game = remember(currentBlock, restartKey) {
                Game(block)
            }

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
                    onDataChanged()
                    navController.popBackStack()
                }
            )
        }

        composable("upload") {
            UploadScreen(
                storageManager = storageManager,
                onPuzzleCreated = {
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
                    navController.popBackStack()
                }
            )
        }
    }
}