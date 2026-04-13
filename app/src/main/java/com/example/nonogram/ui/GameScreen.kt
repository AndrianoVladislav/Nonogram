package com.example.nonogram.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.nonogram.data.storage.Block
import com.example.nonogram.data.storage.StorageManager
import com.example.nonogram.domain.entity.Game
import com.example.nonogram.domain.entity.InputMode
import com.example.nonogram.ui.theme.NonogramTheme

@Composable
fun GameScreen(
    game: Game,
    onExit: () -> Boolean,
    onNextBlock: () -> Unit,
    onRestart: () -> Unit,
    puzzleId: String,
    storageManager: StorageManager
) {

    val board = game.board
    val grid = board.getPlayerGrid()
    val gridSize = grid.size

    val palette = game.block.usedColors

    var selectedColor by remember { mutableIntStateOf(0) }
    var fillMode by remember { mutableStateOf(true) } // ON = FILL
    var tick by remember { mutableIntStateOf(0) }
    var showWinDialog by remember { mutableStateOf(false) }
    var showLoseDialog by remember { mutableStateOf(false) }

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp

    val topSpacer = screenHeight * 0.05f
    val leftHintsWidth = screenWidth * 0.15f
    val fieldWidth = screenWidth * 0.80f

    val cellSize = fieldWidth / gridSize
    val maxHints = 20

    Column(
        modifier = Modifier
            .padding(10.dp, 10.dp, 0.dp, 10.dp)
            .fillMaxSize()
    ) {

        Spacer(modifier = Modifier.height(topSpacer))

        // Nápověda v horní části
        Canvas(
            modifier = Modifier
                .padding(start = leftHintsWidth)
                .width(fieldWidth)
                .height(cellSize * maxHints)
        ) {

            val px = cellSize.toPx()

            board.colHints.forEachIndexed { col, hints ->

                val startY = (maxHints - hints.size) * px

                hints.forEachIndexed { i, hint ->

                    val x = col * px
                    val y = startY + i * px

                    drawCircle(
                        color = Color(palette[hint.color]),
                        radius = px / 2,
                        center = Offset(x + px / 2, y + px / 2)
                    )

                    drawContext.canvas.nativeCanvas.drawText(
                        hint.length.toString(),
                        x + px / 2,
                        y + 5 + px / 2,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.WHITE
                            textAlign = android.graphics.Paint.Align.CENTER
                            textSize = px / 2
                        }
                    )
                }
            }
        }

        // Hlavní část (vlevo nápověda + pole)
        Row {

            // Nápovědy (scroll)
            val rowHints = board.rowHints

            val scrollState = rememberScrollState()

            LaunchedEffect(Unit) {
                scrollState.scrollTo(scrollState.maxValue)
            }

            Box(
                modifier = Modifier
                    .width(leftHintsWidth)
                    .height(fieldWidth)
                    .horizontalScroll(scrollState)
            ) {

                Canvas(
                    modifier = Modifier
                        .width(cellSize * rowHints.size)
                        .height(fieldWidth)
                ) {

                    val px = cellSize.toPx()

                    rowHints.forEachIndexed { row, hints ->

                        val startX = (rowHints.size - hints.size) * px

                        hints.forEachIndexed { i, hint ->

                            val x = startX + i * px
                            val y = row * px

                            drawCircle(
                                color = Color(palette[hint.color]),
                                radius = px / 2,
                                center = Offset(x + px / 2, y + px / 2)
                            )

                            drawContext.canvas.nativeCanvas.drawText(
                                hint.length.toString(),
                                x + px / 2,
                                y + 5 + px / 2,
                                android.graphics.Paint().apply {
                                    color = android.graphics.Color.WHITE
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    textSize = px / 2
                                }
                            )
                        }
                    }
                }
            }

            // 🔶 Pole
            Canvas(
                modifier = Modifier
                    .size(fieldWidth)
                    .pointerInput(selectedColor, fillMode) {

                        detectDragGestures(
                            onDragStart = { offset ->
                                handleTouch(
                                    offset,
                                    cellSize.toPx(),
                                    gridSize,
                                    game,
                                    selectedColor,
                                    fillMode
                                )
                                tick++
                            },
                            onDrag = { change, _ ->
                                handleTouch(
                                    change.position,
                                    cellSize.toPx(),
                                    gridSize,
                                    game,
                                    selectedColor,
                                    fillMode
                                )
                                tick++
                            }
                        )
                    }
            ) {

                if (game.isVictory)
                    showWinDialog = true

                if (game.isGameOver) {
                    showLoseDialog = true
                }

                tick.let { _ ->
                    for (row in 0 until gridSize) {
                        for (col in 0 until gridSize) {

                            val cell = grid[row][col]

                            val x = col * cellSize.toPx()
                            val y = row * cellSize.toPx()

                            // Základní barva
                            cell.color?.let {
                                drawRect(
                                    color = Color(palette[it]),
                                    topLeft = Offset(x, y),
                                    size = Size(cellSize.toPx(), cellSize.toPx())
                                )
                            }

                            // Notes
                            if (cell.color == null && cell.notes.isNotEmpty()) {

                                val radius = cellSize.toPx() / 6

                                cell.notes.forEachIndexed { i, note ->
                                    val nx = x + (i % 3) * (cellSize.toPx() / 3) + radius
                                    val ny = y + (i / 3) * (cellSize.toPx() / 3) + radius

                                    drawCircle(
                                        color = Color(palette[note]),
                                        radius = radius,
                                        center = Offset(nx, ny)
                                    )
                                }
                            }
                        }
                    }
                }

                // Mřížka
                val px = cellSize.toPx()

                // Svislé čáry
                for (i in 0..gridSize) {

                    val stroke = if (i % 5 == 0) 5f else 2f

                    drawLine(
                        color = Color.Black,
                        start = Offset(i * px, 0f),
                        end = Offset(i * px, gridSize * px),
                        strokeWidth = stroke
                    )
                }

                // vodorovné čáry
                for (i in 0..gridSize) {

                    val stroke = if (i % 5 == 0) 5f else 2f

                    drawLine(
                        color = Color.Black,
                        start = Offset(0f, i * px),
                        end = Offset(gridSize * px, i * px),
                        strokeWidth = stroke
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Dolní panel
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Paleta
            Row {
                palette.forEachIndexed { index, color ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .padding(4.dp)
                            .background(Color(color))
                            .border(
                                width = if (selectedColor == index) 3.dp else 1.dp,
                                color = Color.Black
                            )
                            .clickable { selectedColor = index }
                    )
                }
            }

            // Switch
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("NOTE")
                Switch(
                    checked = fillMode,
                    onCheckedChange = { fillMode = it }
                )
                Text("FILL")
            }
        }
    }

    if (showWinDialog) {

        AlertDialog(
            onDismissRequest = {},
            title = { Text("Hotovo!") },
            text = { Text("Chceš pokračovat?") },

            confirmButton = {
                Button(onClick = {

                    val puzzle = storageManager
                        .loadMeta()
                        .first { it.id == puzzleId }

                    val nextBlock = puzzle.currentBlock + 1
                    storageManager.updateProgress(puzzleId, nextBlock)

                    showWinDialog = false

                    if (nextBlock < puzzle.totalBlocks) {
                        onNextBlock()
                    } else {
                        onExit()
                    }

                }) {
                    Text("Pokračovat")
                }
            },

            dismissButton = {
                Button(onClick = {

                    val puzzle = storageManager
                        .loadMeta()
                        .first { it.id == puzzleId }

                    val nextBlock = puzzle.currentBlock + 1
                    storageManager.updateProgress(puzzleId, nextBlock)

                    showWinDialog = false
                    onExit()

                }) {
                    Text("Zpět")
                }
            }
        )
    }

    if (showLoseDialog) {

        AlertDialog(
            onDismissRequest = {},

            title = { Text("Prohráli jste") },
            text = { Text("Chceš to zkusit znovu?") },

            confirmButton = {
                Button(onClick = {

                    showLoseDialog = false
                    onRestart()

                }) {
                    Text("Znovu")
                }
            },

            dismissButton = {
                Button(onClick = {
                    showLoseDialog = false
                    onExit()

                }) {
                    Text("Zpět")
                }
            }
        )
    }
}

private fun handleTouch(
    offset: Offset,
    cellSize: Float,
    size: Int,
    game: Game,
    selectedColor: Int,
    fillMode: Boolean
) {

    val col = (offset.x / cellSize).toInt()
    val row = (offset.y / cellSize).toInt()

    if (row in 0 until size && col in 0 until size) {

        val mode = if (fillMode) InputMode.FILL else InputMode.NOTE

        val success = game.makeMove(row, col, selectedColor, mode)

        if (!success) return
    }
}

@Preview(showBackground = true)
@Composable
fun GameScreenPreview() {
    val solution = List(25) { y ->
        if (y == 24) {
            listOf(1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1, 2, 3, 4, 5)
        } else {
            List(25) { 0 }
        }
    }
    val block = Block(
        solution, listOf(
            Color.Red.toArgb(), Color.Blue.toArgb(),
            Color.Green.toArgb(), Color.Yellow.toArgb(), Color.Cyan.toArgb(), Color.Magenta.toArgb()
        )
    )

    fun reload(): Boolean {
        return true
    }

    val game = Game(block)
    NonogramTheme {
        GameScreen(
            game = game,
            onExit = { reload() },
            onNextBlock = {},
            onRestart = {},
            puzzleId = "123",
            storageManager = StorageManager(LocalContext.current)
        )
    }
}