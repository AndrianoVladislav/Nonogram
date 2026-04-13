package com.example.nonogram.domain.entity

import androidx.compose.runtime.mutableStateOf
import com.example.nonogram.data.storage.Block
import com.example.nonogram.data.storage.Hint
import com.example.nonogram.data.storage.PlayerCell

class Board(private val block: Block) {

    private val size = block.matrix.size

    val playerGrid = mutableStateOf(Array(size) { Array(size) { PlayerCell() } })

    var remainingCells: Int = 0
        private set

    val rowHints: List<List<Hint>> =
        block.matrix.map { getRowHints(it) }

    val colHints: List<List<Hint>> =
        List(block.matrix.size) { col -> getColumnHints(col) }

    init {
        remainingCells = size * size
    }

    fun checkMove(
        row: Int,
        col: Int,
        selectedColor: Int,
        mode: InputMode
    ): Boolean {

        val cell = playerGrid.value[row][col]

        when (mode) {

            InputMode.FILL -> {
                return applyFill(cell, row, col, selectedColor)
            }

            InputMode.NOTE -> {
                toggleNote(cell, selectedColor)
                return true
            }
        }
    }

    private fun applyFill(
        cell: PlayerCell,
        row: Int,
        col: Int,
        selectedColor: Int
    ): Boolean {

        val correctColor = block.matrix[row][col]

        if (selectedColor == correctColor) {

            if (cell.color == null) {
                remainingCells--
            }

            cell.color = selectedColor
            cell.notes.clear()

            return true
        }

        return false
    }

    private fun toggleNote(cell: PlayerCell, color: Int) {

        if (cell.notes.contains(color)) {
            cell.notes.remove(color)
        } else {
            cell.notes.add(color)
        }
    }

    fun isSolved(): Boolean {
        return remainingCells == 0
    }

    fun getPlayerGrid(): Array<Array<PlayerCell>> = playerGrid.value

    fun getRowHints(row: List<Int>): List<Hint> {

        val hints = mutableListOf<Hint>()

        var count = 1
        var currentColor = row[0]

        for (i in 1 until row.size) {

            if (row[i] == currentColor) {
                count++
            } else {
                hints.add(Hint(count, currentColor))
                currentColor = row[i]
                count = 1
            }
        }

        hints.add(Hint(count, currentColor))

        return hints.sortedWith(compareBy(Hint::length, Hint::color))
    }

    fun getColumnHints(col: Int): List<Hint> {

        val column: List<Int> = List(block.matrix.size) { y ->
            block.matrix[y][col]
        }

        return getRowHints(column)
    }



}
