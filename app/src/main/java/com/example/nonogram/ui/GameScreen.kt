package com.example.nonogram.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nonogram.domain.entity.CellState
import com.example.nonogram.domain.entity.Game

@Composable
fun GameScreen(game: Game) {
    val board = game.board
    var playerGrid by remember {
        mutableStateOf(board.getPlayerGrid().map { it.copyOf() }.toTypedArray())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Сетка 25x25
        for (row in playerGrid.indices) {
            Row {
                for (col in playerGrid[row].indices) {
                    CellView(
                        state = playerGrid[row][col],
                        row = row,
                        col = col,
                        onClick = {
                            // Меняем состояние по клику
                            val newState = when (playerGrid[row][col]) {
                                CellState.EMPTY -> CellState.MARKED
                                CellState.MARKED -> CellState.FILLED
                                CellState.FILLED -> CellState.EMPTY
                            }

                            game.makeMove(row, col, newState)
                            playerGrid = board.getPlayerGrid()
                                .map { it.copyOf() }
                                .toTypedArray()
                        },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = game.getStatus())
    }
}

@Composable
fun CellView(state: CellState, row: Int, col: Int, onClick: () -> Unit) {
    val color = when (state) {
        CellState.EMPTY -> Color.White
        CellState.FILLED -> Color.Black
        CellState.MARKED -> Color.White
    }

    // Базовый бордер
    var borderModifier = Modifier
        .size(20.dp)
        .border(1.dp, Color.Gray)

    // Каждая 5-я строка/колонка — толстый чёрный бордер
    if ((col + 1) % 5 == 0) {
        borderModifier = borderModifier.border(2, Color.Black, right = true)
    }
    if ((row + 1) % 5 == 0) {
        borderModifier = borderModifier.border(2, Color.Black, bottom = true)
    }

    Box(
        modifier = borderModifier
            .background(color)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (state == CellState.MARKED) {
            Text("✕", fontSize = 18.sp, color = Color.Red)
        }
    }
}

// Вспомогательный extension для частичных границ
fun Modifier.border(width: Int, color: Color, right: Boolean = false, bottom: Boolean = false): Modifier {
    return this.then(
        Modifier.drawWithContent {
            drawContent()
            val strokeWidth = width.dp.toPx()
            val w = size.width
            val h = size.height

            if (right) {
                drawLine(color, start = androidx.compose.ui.geometry.Offset(w - strokeWidth / 2, 0f),
                    end = androidx.compose.ui.geometry.Offset(w - strokeWidth / 2, h), strokeWidth)
            }
            if (bottom) {
                drawLine(color, start = androidx.compose.ui.geometry.Offset(0f, h - strokeWidth / 2),
                    end = androidx.compose.ui.geometry.Offset(w, h - strokeWidth / 2), strokeWidth)
            }
        }
    )
}



