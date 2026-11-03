package com.example.nonogram.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nonogram.data.sound.SoundManager
import com.example.nonogram.ui.viewmodel.UploadViewModel

@Composable
fun UploadScreen(
    viewModel: UploadViewModel,
    onPuzzleCreated: () -> Unit
) {
    val context = LocalContext.current
    val soundManager = remember { SoundManager(context) }
    DisposableEffect(Unit) {
        onDispose { soundManager.release() }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedSize by remember { mutableStateOf(20 to 25) }

    val validSizes = listOf( Pair(10, 15), Pair(10, 20),
            Pair(15, 15), Pair(15, 20), Pair(15, 25),
            Pair(20, 20), Pair(20, 25) )

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            viewModel.resetState()
            onPuzzleCreated()
        }
    }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            soundManager.playSwitch()
            selectedImageUri = it
        }
    }

    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            soundManager.playSwitch()
            viewModel.importPuzzleFromFile(it)
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
                    soundManager.playSwitch()
                    imageLauncher.launch("image/*")
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isProcessing
            ) {
                Text("Galerie")
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    soundManager.playSwitch()
                    fileLauncher.launch(arrayOf("*/*"))
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isProcessing
            ) {
                Text("Soubor")
            }
        }

        if (uiState.isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x88000000)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }
    }

    selectedImageUri?.let {
        AlertDialog(
            onDismissRequest = { selectedImageUri = null },
            title = { Text("Vyberte velikost matice") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Šířka × Výška (блоки):",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    validSizes.chunked(3).forEach { rowSizes ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowSizes.forEach { (w, h) ->
                                val isSelected = selectedSize == (w to h)
                                Button(
                                    onClick = {
                                        soundManager.playSwitch()
                                        selectedSize = w to h
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("${w}×${h}", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        soundManager.playSwitch()
                        val uriToProcess = selectedImageUri
                        selectedImageUri = null
                        uriToProcess?.let {
                            viewModel.createPuzzleFromImage(it, selectedSize.first, selectedSize.second)
                        }
                    }
                ) {
                    Text("Vytvořit")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    soundManager.playSwitch()
                    selectedImageUri = null
                }) {
                    Text("Zrušit")
                }
            }
        )
    }
}
