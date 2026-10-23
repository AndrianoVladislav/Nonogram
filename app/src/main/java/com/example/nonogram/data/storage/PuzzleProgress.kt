package com.example.nonogram.data.storage

import com.example.nonogram.domain.entity.PlayerCell
import kotlinx.serialization.Serializable

@Serializable
data class SavedPlayerCell(
    val color: Int? = null,
    val notes: Set<Int> = emptySet()
)

fun PlayerCell.toSaved(): SavedPlayerCell = SavedPlayerCell(color = color, notes = notes)
fun SavedPlayerCell.toDomain(): PlayerCell = PlayerCell(color = color, notes = notes)

@Serializable
data class HintPair(val index1: Int, val index2: Int)

@Serializable
data class PuzzleProgress(
    val puzzleId: String,
    val currentBlockIndex: Int = 0,
    val lives: Int = 5,
    val currentBlockGrid: List<List<SavedPlayerCell>> = emptyList(),
    val crossedColHints: List<HintPair> = emptyList(),
    val crossedRowHints: List<HintPair> = emptyList(),
    val selectedColorIndex: Int = 1,
    val fillMode: Boolean = true,
    val isDarkTheme: Boolean = false
)
