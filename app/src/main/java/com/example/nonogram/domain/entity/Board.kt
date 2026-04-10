package com.example.nonogram.domain.entity

import androidx.compose.runtime.mutableStateOf
import com.example.nonogram.data.storage.Block
import com.example.nonogram.data.storage.PlayerCell

class Board(private val block: Block) {

    private val size = block.matrix.size

    private val playerGrid: Array<Array<PlayerCell>> =
        Array(size) { Array(size) { PlayerCell() } }

    var remainingCells: Int = 0
        private set

    init {
        remainingCells = size * size
    }

    fun checkMove(
        row: Int,
        col: Int,
        selectedColor: Int,
        mode: InputMode
    ): Boolean {

        val cell = playerGrid[row][col]

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

    fun getPlayerGrid(): Array<Array<PlayerCell>> = playerGrid

}
