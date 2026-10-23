package com.example.nonogram.domain.entity

import com.example.nonogram.data.storage.Block

class Game(val block: Block, initialLives: Int = 5) {

    val board: Board = Board(block)

    var lives: Int = initialLives
        private set

    var isVictory: Boolean = false
        private set

    var isGameOver: Boolean = false
        private set

    fun makeMove(row: Int, col: Int, selectedColor: Int, mode: InputMode): Boolean {
        if (lives <= 0 || isVictory || isGameOver) return false

        val correct = board.checkMove(row, col, selectedColor, mode)

        if (!correct) {
            lives--
            if (lives <= 0) {
                isGameOver = true
            }
            return false
        } else {
            if (board.isSolved()) {
                isVictory = true
            }
        }
        return true
    }

    fun getStatus(): String {
        return "Životy: $lives, Zbývající buňky: ${board.remainingCells}"
    }
}
