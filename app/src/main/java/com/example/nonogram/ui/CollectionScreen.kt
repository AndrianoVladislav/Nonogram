package com.example.nonogram.ui

import android.graphics.BitmapFactory
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

@Composable
fun CollectionScreen(
    puzzles: List<PuzzleItem>,
    storageManager: StorageManager,
    onPlay: (PuzzleItem) -> Unit,
    onSend: (PuzzleItem) -> Unit,
    onUpload: () -> Unit
) {

    var selected by remember { mutableStateOf<PuzzleItem?>(null) }

    // 🔥 сортировка (завершённые вниз)
    val sorted = puzzles.sortedBy { it.currentBlock == it.totalBlocks }

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val topSpacer = screenHeight * 0.75f

    Column (modifier = Modifier.fillMaxSize()) {

        // 🔷 Список (75%)
        LazyColumn(
            modifier = Modifier
                .height(topSpacer)
                .weight(3f)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            items(sorted) { puzzle ->

                PuzzleItemView(
                    puzzle = puzzle,
                    storageManager = storageManager,
                    isSelected = selected?.id == puzzle.id,
                    onClick = { selected = puzzle }
                )
            }
        }

        // 🔷 Панель кнопок (25%)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(screenHeight - topSpacer)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {

            Button(
                onClick = {
                    if (selected?.currentBlock != selected?.totalBlocks) {
                        selected?.let { onPlay(it) }
                    }
                     },
                enabled = selected != null
            ) {
                Text("Hrát")
            }

            Button(
                onClick = { selected?.let { onSend(it) } },
                enabled = selected != null
            ) {
                Text("Odeslat")
            }


            Button(
                onClick = onUpload,
            ) {
                Text("Nahrát")
            }
        }
    }
}

@Composable
fun PuzzleItemView(
    puzzle: PuzzleItem,
    storageManager: StorageManager,
    isSelected: Boolean,
    onClick: () -> Unit
) {

    val context = LocalContext.current

    val resultFile = remember {
        storageManager.getResultImage(puzzle.id)
    }

    val isCompleted = puzzle.currentBlock >= puzzle.totalBlocks

    val bitmap = remember(resultFile) {
        if (resultFile.exists()) {
            BitmapFactory.decodeFile(resultFile.absolutePath)?.asImageBitmap()
        } else null
    }

    val progress =
        puzzle.currentBlock.toFloat() / puzzle.totalBlocks

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) Color.Green else Color.Black
            )
            .clickable { onClick() }
    ) {

        if (isCompleted && bitmap != null) {

            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )

        } else {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${(progress * 100).toInt()}%",
                    fontSize = 20.sp,
                    color = Color.Black
                )
            }
        }
    }
}