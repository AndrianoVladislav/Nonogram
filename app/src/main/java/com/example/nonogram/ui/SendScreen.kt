package com.example.nonogram.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nonogram.data.storage.Block
import com.example.nonogram.data.storage.Hint
import com.example.nonogram.data.storage.ImageLoader.loadImageBitmapFromFile
import com.example.nonogram.data.storage.PuzzleItem
import com.example.nonogram.data.storage.StorageManager
import com.example.nonogram.domain.entity.Game
import com.example.nonogram.domain.entity.InputMode
import com.example.nonogram.ui.theme.NonogramTheme
import kotlinx.coroutines.delay

@Composable
fun SendScreen(
    puzzleId: String,
    storageManager: StorageManager,
    onDone: () -> Unit
) {

    val context = LocalContext.current

    var isExporting by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    val createFileLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/zip")
        ) { uri ->

            uri?.let {
                isExporting = true

                try {
                    storageManager.exportPuzzle(puzzleId, it, context)
                    message = "Export úspěšný"
                } catch (e: Exception) {
                    message = "Chyba při exportu"
                }

                isExporting = false
            }
        }

    Box(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Button(
                onClick = {
                    createFileLauncher.launch("puzzle.zip")
                },
                enabled = !isExporting,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Exportovat puzzle")
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Zpět")
            }
        }

        // 🔄 Лоадер
        if (isExporting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x88000000)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        // 💬 Сообщение
        message?.let {
            LaunchedEffect(it) {
                delay(1500)
                message = null
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .background(Color.Black)
                    .padding(12.dp)
            ) {
                Text(it, color = Color.White)
            }
        }
    }
}