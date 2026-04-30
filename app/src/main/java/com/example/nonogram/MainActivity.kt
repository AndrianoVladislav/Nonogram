package com.example.nonogram

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.nonogram.data.storage.PuzzleItem
import com.example.nonogram.data.storage.StorageManager
import com.example.nonogram.ui.AppNavigation

class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {

        // Устанавливаем сплэш-скрин
        val splashScreen = installSplashScreen()

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
                onDataChanged = { reload() }
            )
        }
    }
}
