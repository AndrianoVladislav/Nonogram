package com.example.nonogram.domain.entity

class Game(solution: Array<BooleanArray>) {
    val board: Board = Board(solution)
    private var lives: Int = 3
    var isVictory: Boolean = false
        private set

    fun makeMove(row: Int, col: Int, value: CellState) {
        if (lives <= 0) return

        val correct = board.checkMove(row, col, value)

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
    }

    fun getStatus(): String {
        return "Жизни: $lives, Оставшиеся клетки: ${board.remainingFilledCells}"
    }
}

