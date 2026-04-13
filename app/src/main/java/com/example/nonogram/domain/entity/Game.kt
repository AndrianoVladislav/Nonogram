package com.example.nonogram.domain.entity

import com.example.nonogram.data.storage.Block

class Game(solution: Block) {

    val block = solution
    val board: Board = Board(block)
    private var lives: Int = 3
    var isVictory: Boolean = false
        private set

    var isGameOver: Boolean = false
        private set

    fun makeMove(row: Int, col: Int, selectedColor: Int, mode: InputMode): Boolean {
        if (lives <= 0) return false

        val correct = board.checkMove(row, col, selectedColor, mode)

        if (!correct) {
            lives--
            println("X Chyba! Zbývající životy: $lives")
            if (lives <= 0) {
                println("X Konec hry!")
                isGameOver = true
            }
        } else {
            println("Správný tah!")
            if (board.isSolved()) {
                isVictory = true
                println("Win!")
            }
        }
        print(" $col, $row ")
        return true
    }

    fun getStatus(): String {
        return "Životy: $lives, Zbývající buňky: ${board.remainingCells}"
    }
}

