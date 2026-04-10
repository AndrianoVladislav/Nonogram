package com.example.nonogram.ui

import android.R.attr.maxWidth
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke /// !!!!!!!!!
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.nonogram.data.storage.Block
import com.example.nonogram.domain.entity.Game
import com.example.nonogram.domain.entity.InputMode
import com.example.nonogram.ui.theme.NonogramTheme

@Composable
fun GameScreen(game: Game) {

    val grid = game.board.getPlayerGrid()
    val gridSize = grid.size

    var selectedColor by remember { mutableIntStateOf(0) }
    var mode by remember { mutableStateOf(InputMode.FILL) }

    Column {

        // 🔹 Палитра цветов
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

        // 🔹 Переключение режима
        Row(modifier = Modifier.padding(8.dp)) {

            Button(onClick = { mode = InputMode.FILL }) {
                Text("Fill")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(onClick = { mode = InputMode.NOTE }) {
                Text("Note")
            }
        }

        // 🔷 Игровое поле (Canvas)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(8.dp)
                .pointerInput(Unit) {

                    // Внутри pointerInput доступно свойство size (размер в пикселях)
                    val cellSize = maxWidth.toFloat() / gridSize
//                    val cellSize = size.width.toFloat() / gridSize

                    detectDragGestures(
                        onDragStart = { offset ->
                            handleTouch(offset, cellSize, gridSize, game, selectedColor, mode)
                        },
                        onDrag = { change, _ ->
                            handleTouch(change.position, cellSize, gridSize, game, selectedColor, mode)
                        }
                    )
                }
        ) {

            val currentCellSize = size.width / gridSize

            // 🔹 Рисуем клетки
            for (row in 0 until gridSize) {
                for (col in 0 until gridSize) {

                    val cell = grid[row][col]

                    val x = col * currentCellSize
                    val y = row * currentCellSize

                    // 🔸 Основной цвет
                    cell.color?.let { colorIndex ->
                        drawRect(
                            color = game.block.usedColors[colorIndex],
                            topLeft = Offset(x, y),
                            size = Size(currentCellSize, currentCellSize)
                        )
                    }

                    // 🔸 Notes (маленькие кружки)
                    if (cell.color == null && cell.notes.isNotEmpty()) {

                        val radius = currentCellSize / 6

                        val notesList = cell.notes.toList()

                        notesList.forEachIndexed { i, noteColor ->

                            val nx = x + (i % 3) * (currentCellSize / 3) + radius
                            val ny = y + (i / 3) * (currentCellSize / 3) + radius

                            drawCircle(
                                color = game.block.usedColors[noteColor],
                                radius = radius,
                                center = Offset(nx, ny)
                            )
                        }
                    }

                    // 🔸 Сетка
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(x, y),
                        size = Size(currentCellSize, currentCellSize),
                        style = Stroke(
                            width = if (row % 5 == 0 || col % 5 == 0) 3f else 1f
                        )
                    )
                }
            }
        }
    }
}

private fun handleTouch(
    offset: Offset,
    cellSize: Float,
    size: Int,
    game: Game,
    selectedColor: Int,
    mode: InputMode
) {

    val col = (offset.x / cellSize).toInt()
    val row = (offset.y / cellSize).toInt()

    if (row in 0 until size && col in 0 until size) {
        game.makeMove(row, col, selectedColor, mode)
    }
}

@Preview(showBackground = true)
@Composable
fun GameScreenPreview() {
    val solution = arrayListOf(
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),

    )
    val block = Block(solution, listOf(Color(1)))
    val game = Game(block)
    NonogramTheme {
        GameScreen(game = game)
    }
}
