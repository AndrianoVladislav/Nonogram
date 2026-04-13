package com.example.nonogram.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.nonogram.data.storage.StorageManager
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