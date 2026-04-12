package com.example.nonogram.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nonogram.data.storage.Block
import com.example.nonogram.data.storage.Hint
import com.example.nonogram.data.storage.ImageLoader.loadImageBitmapFromFile
import com.example.nonogram.data.storage.PuzzleItem
import com.example.nonogram.domain.entity.Game
import com.example.nonogram.domain.entity.InputMode
import com.example.nonogram.ui.theme.NonogramTheme

@Composable
fun CollectionScreen(
    puzzles: List<PuzzleItem>,
    onPlay: (PuzzleItem) -> Unit,
    onSend: (PuzzleItem) -> Unit,
    onUpload: () -> Unit
) {

    var selected by remember { mutableStateOf<PuzzleItem?>(null) }

    // 🔥 сортировка (завершённые вниз)
    val sorted = puzzles.sortedBy { it.currentBlock == it.totalBlocks }

    Row(modifier = Modifier.fillMaxSize()) {

        // 🔷 Список (75%)
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .weight(3f)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            items(sorted) { puzzle ->

                PuzzleItemView(
                    puzzle = puzzle,
                    isSelected = selected?.id == puzzle.id,
                    onClick = { selected = puzzle }
                )
            }
        }

        // 🔷 Панель кнопок (25%)
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Button(
                onClick = { selected?.let { onPlay(it) } },
                modifier = Modifier.fillMaxWidth(),
                enabled = selected != null
            ) {
                Text("Hrát")
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { selected?.let { onSend(it) } },
                modifier = Modifier.fillMaxWidth(),
                enabled = selected != null
            ) {
                Text("Odeslat")
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onUpload,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Nahrát")
            }
        }
    }
}

@Composable
fun PuzzleItemView(
    puzzle: PuzzleItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {

    val bitmap = remember(puzzle.previewPath) {
        loadImageBitmapFromFile(puzzle.previewPath)
    }

    val progress =
        puzzle.currentBlock.toFloat() / puzzle.totalBlocks.toFloat()

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

        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.LightGray)
            )
        }

        // 🔹 Прогресс бар
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .align(Alignment.BottomStart)
                .background(Color.DarkGray)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(Color.Green)
            )
        }
    }
}