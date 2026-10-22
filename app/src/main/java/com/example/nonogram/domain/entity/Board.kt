package com.example.nonogram.domain.entity

import com.example.nonogram.data.storage.Block
import com.example.nonogram.data.storage.Hint

class Board(private val block: Block) {

    val size: Int = block.matrix.size

    private var _playerGrid: List<List<PlayerCell>> = List(size) { List(size) { PlayerCell() } }
    val playerGrid: List<List<PlayerCell>>
        get() = _playerGrid

    var remainingCells: Int = size * size
        private set

    val rowHints: List<List<Hint>> = block.matrix.map { getRowHints(it) }
    val colHints: List<List<Hint>> = List(size) { col -> getColumnHints(col) }

    fun checkMove(
        row: Int,
        col: Int,
        selectedColor: Int,
        mode: InputMode
    ): Boolean {
        val cell = _playerGrid[row][col]
        return cell.color != null || when (mode) {
            InputMode.FILL -> applyFill(cell, row, col, selectedColor)
            InputMode.NOTE -> {
                toggleNote(cell, row, col, selectedColor)
                true
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
            updateCell(row, col, cell.copy(color = selectedColor, notes = emptySet()))
            return true
        }

        return false
    }

    private fun toggleNote(cell: PlayerCell, row: Int, col: Int, color: Int) {
        val newNotes = if (cell.notes.contains(color)) {
            cell.notes - color
        } else {
            cell.notes + color
        }
        updateCell(row, col, cell.copy(notes = newNotes))
    }

    private fun updateCell(row: Int, col: Int, newCell: PlayerCell) {
        _playerGrid = _playerGrid.mapIndexed { r, rowList ->
            if (r == row) {
                rowList.mapIndexed { c, oldCell ->
                    if (c == col) newCell else oldCell
                }
            } else {
                rowList
            }
        }
    }

    fun isSolved(): Boolean = remainingCells == 0

    fun getRowHints(row: List<Int>): List<Hint> {
        if (row.isEmpty()) return emptyList()

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
