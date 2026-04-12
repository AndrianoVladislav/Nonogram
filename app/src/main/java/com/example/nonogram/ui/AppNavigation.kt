package com.example.nonogram.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun AppNavigation() {

    val navController = rememberNavController()

    NavHost(navController, startDestination = "start") {

        composable("start") {
            StartScreen(
                onCollectionClick = {
                    navController.navigate("collection")
                },
                onSettingsClick = { }
            )
        }

        composable("collection") {
            CollectionScreen(
                puzzles = sampleData(),
                onPlay = {
                    navController.navigate("game")
                },
                onSend = {
                    navController.navigate("send")
                },
                onUpload = {
                    navController.navigate("upload")
                }
            )
        }

        composable("game") {
            GameScreen(game = sampleGame())
        }

        composable("send") {
            Text("Send Screen")
        }

        composable("upload") {
            Text("Upload Screen")
        }
    }
}