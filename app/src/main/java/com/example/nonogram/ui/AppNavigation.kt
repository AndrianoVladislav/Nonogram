package com.example.nonogram.ui

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.nonogram.R
import com.example.nonogram.data.repository.PuzzleRepositoryImpl
import com.example.nonogram.data.sound.SoundManager
import com.example.nonogram.domain.repository.PuzzleRepository
import com.example.nonogram.ui.viewmodel.AppViewModelFactory
import com.example.nonogram.ui.viewmodel.CollectionViewModel
import com.example.nonogram.ui.viewmodel.GameViewModel
import com.example.nonogram.ui.viewmodel.SendViewModel
import com.example.nonogram.ui.viewmodel.UploadViewModel

@Composable
fun AppNavigation() {
    val context = LocalContext.current

    val repository: PuzzleRepository = remember(context) { PuzzleRepositoryImpl(context) }
    val viewModelFactory = remember(repository) { AppViewModelFactory(repository) }

    val navController = rememberNavController()
    val soundManager = remember { SoundManager(context) }

    DisposableEffect(Unit) {
        onDispose { soundManager.release() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.bg_main),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
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
                val viewModel: CollectionViewModel = viewModel(factory = viewModelFactory)
                CollectionScreen(
                    viewModel = viewModel,
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
                val viewModel: GameViewModel = viewModel(factory = viewModelFactory)

                LaunchedEffect(id) {
                    viewModel.loadGame(id)
                }

                Surface(
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxSize()
                ) {
                    GameScreen(
                        viewModel = viewModel,
                        onExit = {
                            soundManager.playSwitch()
                            navController.popBackStack()
                        }
                    )
                }
            }

            composable("upload") {
                val viewModel: UploadViewModel = viewModel(factory = viewModelFactory)
                UploadScreen(
                    viewModel = viewModel,
                    onPuzzleCreated = {
                        soundManager.playSwitch()
                        navController.popBackStack()
                    }
                )
            }

            composable("send/{id}") { backStack ->
                val id = backStack.arguments?.getString("id")!!
                val viewModel: SendViewModel = viewModel(factory = viewModelFactory)

                SendScreen(
                    puzzleId = id,
                    viewModel = viewModel,
                    onDone = {
                        soundManager.playSwitch()
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
