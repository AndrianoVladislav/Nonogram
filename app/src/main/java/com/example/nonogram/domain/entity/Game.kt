package com.example.nonogram.domain.entity

import com.example.nonogram.data.storage.Block

class Game(solution: Block) {

    val block = solution
    val board: Board = Board(block)
    private var lives: Int = 3
    var isVictory: Boolean = false
        private set

    fun makeMove(row: Int, col: Int, selectedColor: Int, mode: InputMode) {
        if (lives <= 0) return

        val correct = board.checkMove(row, col, selectedColor, mode)

        if (!correct) {
            lives--
            println("❌ Ошибка! Осталось жизней: $lives")
            if (lives <= 0) {
                println("💀 Конец игры!")
            }
        } else {
            println("✅ Верный ход!")
            if (board.isSolved()) {
                isVictory = true
                println("🎉 Победа!")
            }
        }
        print(" $col, $row ")
    }

    fun getStatus(): String {
        return "Жизни: $lives, Оставшиеся клетки: ${board.remainingCells}"
    }
}

