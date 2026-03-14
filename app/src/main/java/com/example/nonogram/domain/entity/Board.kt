package com.example.nonogram.domain.entity

class Board( private val solution: Array<BooleanArray>) {
    private val playerGrid: Array<Array<CellState>> =
        Array(solution.size) { Array(solution.size) { CellState.EMPTY } }

    var remainingFilledCells: Int = 0
        private set

    init {
        for (row in solution) {
            for (cell in row) {
                if (cell) remainingFilledCells++
            }
        }
    }

    /**
     * Hráč provede tah.
     * Vrátí true, pokud je tah MARKED.
     * Vrátí true, pokud je tah správný.
     * Vrátí false, pokud je tah chybný.
     */
    fun checkMove(x: Int, y: Int, state: CellState): Boolean {
        if (x !in 0 until solution.size || y !in 0 until solution.size) return false

        if (state == CellState.MARKED) {
            playerGrid[x][y] = state
            return true
        }
        if (solution[x][y]) {
            playerGrid[x][y] = CellState.FILLED
            remainingFilledCells--
        } else {
            playerGrid[x][y] = CellState.EMPTY
        }

        return solution[x][y] == (state == CellState.FILLED)
    }

    fun isSolved(): Boolean = remainingFilledCells == 0

    fun getRowHints(row: Int): List<Int> {
        return extractHints(solution[row])
    }

    fun getColumnHints(col: Int): List<Int> {
        val column = BooleanArray(solution.size) { solution[it][col] }
        return extractHints(column)
    }

    private fun extractHints(line: BooleanArray): List<Int> {
        val hints = mutableListOf<Int>()
        var count = 0
        for (cell in line) {
            if (cell) {
                count++
            } else if (count > 0) {
                hints.add(count)
                count = 0
            }
        }
        if (count > 0) hints.add(count)
        return if (hints.isEmpty()) listOf(0) else hints
    }

    fun getPlayerGrid(): Array<Array<CellState>> = playerGrid
}
