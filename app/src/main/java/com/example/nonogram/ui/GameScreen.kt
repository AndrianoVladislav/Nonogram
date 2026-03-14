package com.example.nonogram.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nonogram.domain.entity.CellState
import com.example.nonogram.domain.entity.Game
import com.example.nonogram.ui.theme.NonogramTheme

@Composable
fun GameScreen(game: Game) {

    val board = game.board

    var playerGrid by remember {
        mutableStateOf(board.getPlayerGrid().map { it.copyOf() }.toTypedArray())
    }

    val cellSize = 15.dp
    val gridSize = playerGrid.size

    val density = LocalDensity.current
    val cellPx = with(density) { cellSize.toPx() }

    // режим текущего свайпа
    var dragState by remember { mutableStateOf<CellState?>(null) }

    fun getCell(offset: Offset): Pair<Int, Int>? {

        val col = (offset.x / cellPx).toInt()
        val row = (offset.y / cellPx).toInt()

        if (row !in playerGrid.indices || col !in playerGrid.indices) return null

        return row to col
    }

    fun applyMove(row: Int, col: Int) {

        val state = dragState ?: return

        game.makeMove(row, col, state)

        playerGrid = board.getPlayerGrid()
            .map { it.copyOf() }
            .toTypedArray()
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(20.dp))

        Canvas(
            modifier = Modifier
                .size(cellSize * gridSize)
                .pointerInput(Unit) {

                    detectDragGestures(

                        onDragStart = { offset ->
                            val cell = getCell(offset)

                            cell?.let { (row, col) ->

                                val current = playerGrid[row][col]

                                dragState = when (current) {
                                    CellState.EMPTY -> CellState.MARKED
                                    CellState.MARKED -> CellState.FILLED
                                    CellState.FILLED -> CellState.EMPTY
                                }

                                applyMove(row, col)
                            }
                        },

                        onDrag = { change, _ ->
                            val cell = getCell(change.position)

                            cell?.let { (row, col) ->
                                applyMove(row, col)
                            }
                        },

                        onDragEnd = {
                            dragState = null
                        }
                    )
                }
        ) {

            val grid = playerGrid

            // клетки
            for (row in grid.indices) {
                for (col in grid[row].indices) {

                    val left = col * cellPx
                    val top = row * cellPx

                    val color = when (grid[row][col]) {
                        CellState.EMPTY -> Color.White
                        CellState.FILLED -> Color.Black
                        CellState.MARKED -> Color.White
                    }

                    drawRect(
                        color = color,
                        topLeft = Offset(left + 1f, top + 1f),
                        size = Size(cellPx - 2f, cellPx - 2f)
                    )

                    if (grid[row][col] == CellState.MARKED) {

                        val pad = cellPx * 0.2f

                        drawLine(
                            color = Color.Red,
                            start = Offset(left + pad, top + pad),
                            end = Offset(left + cellPx - pad, top + cellPx - pad),
                            strokeWidth = 3f
                        )

                        drawLine(
                            color = Color.Red,
                            start = Offset(left + cellPx - pad, top + pad),
                            end = Offset(left + pad, top + cellPx - pad),
                            strokeWidth = 3f
                        )
                    }
                }
            }

            // сетка
            for (i in 0..gridSize) {

                val stroke = if (i % 5 == 0) 5f else 2f

                drawLine(
                    color = Color.Black,
                    start = Offset(i * cellPx, 0f),
                    end = Offset(i * cellPx, gridSize * cellPx),
                    strokeWidth = stroke
                )

                drawLine(
                    color = Color.Black,
                    start = Offset(0f, i * cellPx),
                    end = Offset(gridSize * cellPx, i * cellPx),
                    strokeWidth = stroke
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(game.getStatus())
    }
}

@Preview(showBackground = true)
@Composable
fun GameScreenPreview() {
    val solution = arrayOf(
        booleanArrayOf(true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true),
        booleanArrayOf(true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true),
        booleanArrayOf(true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true),
        booleanArrayOf(true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true),
        booleanArrayOf(true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true),
        booleanArrayOf(true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true),
        booleanArrayOf(true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true),
        booleanArrayOf(true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true),
        booleanArrayOf(true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true),
        booleanArrayOf(true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true),
        booleanArrayOf(true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true),
        booleanArrayOf(true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true),
        booleanArrayOf(true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true),
        booleanArrayOf(true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true),
        booleanArrayOf(true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true),
        booleanArrayOf(true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true),
        booleanArrayOf(true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true),
        booleanArrayOf(true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true),
        booleanArrayOf(true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true),
        booleanArrayOf(true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true),
        booleanArrayOf(true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true),
        booleanArrayOf(true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true),
        booleanArrayOf(true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true),
        booleanArrayOf(true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true),
        booleanArrayOf(true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true),

    )
    val game = Game(solution)
    NonogramTheme {
        GameScreen(game = game)
    }
}
