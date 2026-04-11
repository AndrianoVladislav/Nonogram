package com.example.nonogram.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nonogram.data.storage.Block
import com.example.nonogram.data.storage.Hint
import com.example.nonogram.domain.entity.Game
import com.example.nonogram.domain.entity.InputMode
import com.example.nonogram.ui.theme.NonogramTheme

@Composable
fun GameScreen(game: Game) {

    val grid = game.board.getPlayerGrid()
    val gridSize = grid.size

    var selectedColor by remember { mutableIntStateOf(0) }
    var mode by remember { mutableStateOf(InputMode.FILL) }

    // Добавляем state для перерисовки при изменениях в Board
    var tick by remember { mutableIntStateOf(0) }

    Column {
        // Палитра цветов
        Row(modifier = Modifier.padding(8.dp)) {
            game.block.usedColors.forEachIndexed { index, color ->
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .padding(4.dp)
                        .background(color)
                        .border(
                            width = if (selectedColor == index) 3.dp else 1.dp,
                            color = Color.Black
                        )
                        .clickable {
                            selectedColor = index
                        }
                )
            }
        }

        // Переключение режима
        Row(modifier = Modifier.padding(8.dp)) {
            Button(onClick = { mode = InputMode.FILL }) {
                Text("Fill")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { mode = InputMode.NOTE }) {
                Text("Note")
            }
        }

        // Игровое поле
        BoxWithConstraints(modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(8.dp)
        ) {
            val boardSizePx = this.constraints.maxWidth.toFloat()
            val cellSize = boardSizePx / gridSize

            Row {
                Spacer(modifier = Modifier.size(boardSizePx.dp))

                game.board.rowHints[0].indices.forEach { col ->
                    HintColumn(
                        hints = game.board.colHints[col],
                        palette = game.block.usedColors,
                        size = cellSize
                    )
                }
            }

            Row {

                Column {
                    game.board.rowHints.forEach { hints ->
                        HintRow(
                            hints = hints,
                            palette = game.block.usedColors,
                            size = cellSize
                        )
                    }
                }


                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(gridSize, selectedColor, mode) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    if (handleTouch(offset, cellSize, gridSize, game, selectedColor, mode)) {
                                        tick++
                                    }
                                },
                                onDrag = { change, _ ->
                                    if (handleTouch(change.position, cellSize, gridSize, game, selectedColor, mode)) {
                                        tick++
                                    }
                                }
                            )
                        }
                ) {
                    // Используем tick чтобы Canvas знал что нужно перерисоваться
                    tick.let { _ ->
                        for (row in 0 until gridSize) {
                            for (col in 0 until gridSize) {
                                val cell = grid[row][col]
                                val x = col * cellSize
                                val y = row * cellSize

                                cell.color?.let { colorIndex ->
                                    drawRect(
                                        color = game.block.usedColors[colorIndex],
                                        topLeft = Offset(x, y),
                                        size = Size(cellSize, cellSize)
                                    )
                                }

                                if (cell.color == null && cell.notes.isNotEmpty()) {
                                    val radius = cellSize / 6
                                    cell.notes.forEachIndexed { i, noteColor ->
                                        val nx = x + (i % 3) * (cellSize / 3) + radius
                                        val ny = y + (i / 3) * (cellSize / 3) + radius
                                        drawCircle(
                                            color = game.block.usedColors[noteColor],
                                            radius = radius,
                                            center = Offset(nx, ny)
                                        )
                                    }
                                }

                                drawRect(
                                    color = Color.Black,
                                    topLeft = Offset(x, y),
                                    size = Size(cellSize, cellSize),
                                    style = Stroke(width = if (row % 5 == 0 || col % 5 == 0) 3f else 1f)
                                )
                            }
                        }
                    }
                }
            }


        }
    }
}

@Composable
fun HintRow(hints: List<Hint>, palette: List<Color>, size: Float) {

    Row(
//        modifier = Modifier.height(size.dp / 2)
        modifier = Modifier.height((15.09).dp),
    ) {
        hints.forEach { hint ->

            Box(
                modifier = Modifier
//                    .size(size.dp / 2)
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {

                Canvas(modifier = Modifier.size(size.dp)) {

                    drawCircle(
                        color = palette[hint.color],
                        radius = size / 2
                    )
                }

                Text(
                    text = hint.length.toString(),
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun HintColumn(hints: List<Hint>, palette: List<Color>, size: Float) {

    Column(
        modifier = Modifier.width(size.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        hints.forEach { hint ->

            Box(
                modifier = Modifier
                    .size(size.dp)
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {

                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = palette[hint.color],
                        radius = size / 2
                    )
                }

                Text(
                    text = hint.length.toString(),
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
        }
    }
}
private fun handleTouch(
    offset: Offset,
    cellSize: Float,
    gridSize: Int,
    game: Game,
    selectedColor: Int,
    mode: InputMode
): Boolean {
    val col = (offset.x / cellSize).toInt()
    val row = (offset.y / cellSize).toInt()

    if (row in 0 until gridSize && col in 0 until gridSize) {
        return game.makeMove(row, col, selectedColor, mode)
    }
    return false
}

@Preview(showBackground = true)
@Composable
fun GameScreenPreview() {
    val solution = ArrayList<IntArray>()
    repeat(25) { solution.add(IntArray(25)) }
    val block = Block(solution, listOf(Color.Red, Color.Blue))
    val game = Game(block)
    NonogramTheme {
        GameScreen(game = game)
    }
}