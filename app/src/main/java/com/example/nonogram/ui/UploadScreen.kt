package com.example.nonogram.ui

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.nonogram.data.converter.ImageConverterBlock
import com.example.nonogram.data.converter.ImageConverterQuantization
import com.example.nonogram.data.converter.ImageConverterResize
import com.example.nonogram.data.storage.ImageLoader
import com.example.nonogram.data.storage.PuzzleData
import com.example.nonogram.data.storage.PuzzleItem
import com.example.nonogram.data.storage.StorageManager

@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun UploadScreen(
    storageManager: StorageManager,
    onPuzzleCreated: () -> Unit
) {

    val context = LocalContext.current

    var isProcessing by remember { mutableStateOf(false) }

    // Galerie
    val imageLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                isProcessing = true

                handleImageUri(context, it, storageManager)

                isProcessing = false
                onPuzzleCreated()
            }
        }

    // Soubor
    val fileLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                storageManager.importPuzzle(it, context)
                onPuzzleCreated()
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
                onClick = { imageLauncher.launch("image/*") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isProcessing
            ) {
                Text("Galerie")
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { fileLauncher.launch(arrayOf("*/*")) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isProcessing
            ) {
                Text("Soubor")
            }
        }

        if (isProcessing) {
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
}

@RequiresApi(Build.VERSION_CODES.P)
fun handleImageUri(
    context: Context,
    uri: Uri,
    storageManager: StorageManager
) {

    val bitmap = ImageLoader.loadBitmapFromUri(context, uri) ?: return

    val resized = ImageConverterResize.resizeToBlocks(bitmap)
    val quantized = ImageConverterQuantization.quantizeKMeans(resized)
    val blocks = ImageConverterBlock.splitToBlocks(quantized)

    val puzzleData = PuzzleData(
        palette = blocks[0].usedColors,
        blocks = blocks
    )

    val id = "puzzle_${System.currentTimeMillis()}"
    val resultBitmap = ImageConverterQuantization.quantizedToBitmap(quantized)

    storageManager.savePuzzleData(id, puzzleData)
    storageManager.saveResultImage(id, resultBitmap)

    val meta = storageManager.loadMeta().toMutableList()

    meta.add(
        PuzzleItem(
            id = id,
            currentBlock = 0,
            totalBlocks = blocks.size
        )
    )

    storageManager.saveMeta(meta)
}