package com.example.nonogram.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nonogram.data.storage.Block
import com.example.nonogram.data.storage.Hint
import com.example.nonogram.data.storage.HintPair
import com.example.nonogram.data.storage.PuzzleProgress
import com.example.nonogram.data.storage.toDomain
import com.example.nonogram.data.storage.toSaved
import com.example.nonogram.domain.entity.Game
import com.example.nonogram.domain.entity.InputMode
import com.example.nonogram.domain.entity.PlayerCell
import com.example.nonogram.domain.repository.PuzzleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs

data class GameUiState(
    val puzzleId: String = "",
    val currentBlockIndex: Int = 0,
    val totalBlocks: Int = 0,
    val block: Block? = null,
    val grid: List<List<PlayerCell>> = emptyList(),
    val palette: List<Int> = emptyList(),
    val hintTextColors: List<Int> = emptyList(),
    val lives: Int = 5,
    val selectedColor: Int = 1,
    val fillMode: Boolean = true,
    val isDarkTheme: Boolean = false,
    val isVictory: Boolean = false,
    val isGameOver: Boolean = false,
    val rowHints: List<List<Hint>> = emptyList(),
    val colHints: List<List<Hint>> = emptyList(),
    val crossedColHints: Set<Pair<Int, Int>> = emptySet(),
    val crossedRowHints: Set<Pair<Int, Int>> = emptySet(),
    val isLoading: Boolean = true
)

class GameViewModel(
    private val repository: PuzzleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var currentGame: Game? = null

    fun loadGame(puzzleId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val puzzleData = repository.getPuzzleData(puzzleId) ?: return@launch
            val savedProgress = repository.loadProgress(puzzleId)

            val currentBlockIndex = savedProgress?.currentBlockIndex ?: 0
            val block = puzzleData.blocks.getOrNull(currentBlockIndex) ?: return@launch

            val initialLives = savedProgress?.lives ?: 5
            val game = Game(block, initialLives = initialLives)
            currentGame = game

            // Restore saved cell grid state if available
            if (savedProgress != null && savedProgress.currentBlockGrid.isNotEmpty()) {
                savedProgress.currentBlockGrid.forEachIndexed { r, row ->
                    row.forEachIndexed { c, savedCell ->
                        val domainCell = savedCell.toDomain()
                        if (domainCell.color != null) {
                            game.board.checkMove(r, c, domainCell.color, InputMode.FILL)
                        }
                        domainCell.notes.forEach { note ->
                            game.board.checkMove(r, c, note, InputMode.NOTE)
                        }
                    }
                }
            }

            val textColors = block.usedColors.map { calculateComplementaryColor(it) }

            val restoredColHints = savedProgress?.crossedColHints?.map { it.index1 to it.index2 }?.toSet() ?: emptySet()
            val restoredRowHints = savedProgress?.crossedRowHints?.map { it.index1 to it.index2 }?.toSet() ?: emptySet()

            _uiState.update {
                GameUiState(
                    puzzleId = puzzleId,
                    currentBlockIndex = currentBlockIndex,
                    totalBlocks = puzzleData.blocks.size,
                    block = block,
                    grid = game.board.playerGrid,
                    palette = block.usedColors,
                    hintTextColors = textColors,
                    lives = savedProgress?.lives ?: game.lives,
                    selectedColor = savedProgress?.selectedColorIndex ?: 1.coerceAtMost(block.usedColors.size - 1),
                    fillMode = savedProgress?.fillMode ?: true,
                    isDarkTheme = savedProgress?.isDarkTheme ?: false,
                    isVictory = game.isVictory,
                    isGameOver = game.isGameOver,
                    rowHints = game.board.rowHints,
                    colHints = game.board.colHints,
                    crossedColHints = restoredColHints,
                    crossedRowHints = restoredRowHints,
                    isLoading = false
                )
            }
        }
    }

    fun makeMove(row: Int, col: Int): Boolean {
        val game = currentGame ?: return false
        val currentState = _uiState.value
        if (game.isGameOver || game.isVictory) return false

        val mode = if (currentState.fillMode) InputMode.FILL else InputMode.NOTE

        val success = game.makeMove(row, col, currentState.selectedColor, mode)

        _uiState.update {
            it.copy(
                grid = game.board.playerGrid,
                lives = game.lives,
                isVictory = game.isVictory,
                isGameOver = game.isGameOver
            )
        }

        if (game.isGameOver) {
            resetCurrentBlockProgress()
        } else {
            saveCurrentProgress()
        }

        if (game.isVictory) {
            viewModelScope.launch {
                repository.updateBlockProgressAndGenerateImage(
                    currentState.puzzleId,
                    currentState.currentBlockIndex + 1
                )
            }
        }

        return success
    }

    fun setSelectedColor(colorIndex: Int) {
        _uiState.update { it.copy(selectedColor = colorIndex) }
    }

    fun setFillMode(fill: Boolean) {
        _uiState.update { it.copy(fillMode = fill) }
    }

    fun setDarkTheme(isDark: Boolean) {
        _uiState.update { it.copy(isDarkTheme = isDark) }
        saveCurrentProgress()
    }

    fun toggleCrossColHint(col: Int, hintIndex: Int) {
        _uiState.update { state ->
            val pair = col to hintIndex
            val updated = if (state.crossedColHints.contains(pair)) {
                state.crossedColHints - pair
            } else {
                state.crossedColHints + pair
            }
            state.copy(crossedColHints = updated)
        }
        saveCurrentProgress()
    }

    fun toggleCrossRowHint(row: Int, hintIndex: Int) {
        _uiState.update { state ->
            val pair = row to hintIndex
            val updated = if (state.crossedRowHints.contains(pair)) {
                state.crossedRowHints - pair
            } else {
                state.crossedRowHints + pair
            }
            state.copy(crossedRowHints = updated)
        }
        saveCurrentProgress()
    }

    fun restartGame() {
        val currentState = _uiState.value
        val block = currentState.block ?: return
        val game = Game(block, initialLives = 5)
        currentGame = game

        _uiState.update {
            it.copy(
                grid = game.board.playerGrid,
                lives = 5,
                isVictory = false,
                isGameOver = false,
                crossedColHints = emptySet(),
                crossedRowHints = emptySet()
            )
        }

        resetCurrentBlockProgress()
    }

    fun nextBlock() {
        val currentState = _uiState.value
        val nextBlockIndex = currentState.currentBlockIndex + 1
        if (nextBlockIndex >= currentState.totalBlocks) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val puzzleData = repository.getPuzzleData(currentState.puzzleId) ?: return@launch
            val block = puzzleData.blocks.getOrNull(nextBlockIndex) ?: return@launch
            val game = Game(block, initialLives = 5)
            currentGame = game

            val textColors = block.usedColors.map { calculateComplementaryColor(it) }

            _uiState.update {
                it.copy(
                    currentBlockIndex = nextBlockIndex,
                    block = block,
                    grid = game.board.playerGrid,
                    palette = block.usedColors,
                    hintTextColors = textColors,
                    lives = 5,
                    selectedColor = 1.coerceAtMost(block.usedColors.size - 1),
                    fillMode = true,
                    isDarkTheme = it.isDarkTheme,
                    isVictory = false,
                    isGameOver = false,
                    rowHints = game.board.rowHints,
                    colHints = game.board.colHints,
                    crossedColHints = emptySet(),
                    crossedRowHints = emptySet(),
                    isLoading = false
                )
            }

            saveCurrentProgress()
        }
    }

    private fun saveCurrentProgress() {
        val state = _uiState.value
        if (state.puzzleId.isEmpty()) return

        val savedGrid = state.grid.map { row ->
            row.map { it.toSaved() }
        }
        val colHintsPairs = state.crossedColHints.map { HintPair(it.first, it.second) }
        val rowHintsPairs = state.crossedRowHints.map { HintPair(it.first, it.second) }

        val progress = PuzzleProgress(
            puzzleId = state.puzzleId,
            currentBlockIndex = state.currentBlockIndex,
            lives = state.lives,
            currentBlockGrid = savedGrid,
            crossedColHints = colHintsPairs,
            crossedRowHints = rowHintsPairs,
            selectedColorIndex = state.selectedColor,
            fillMode = state.fillMode,
            isDarkTheme = state.isDarkTheme
        )

        viewModelScope.launch {
            repository.saveProgress(state.puzzleId, progress)
        }
    }

    private fun resetCurrentBlockProgress() {
        val state = _uiState.value
        if (state.puzzleId.isEmpty()) return

        val progress = PuzzleProgress(
            puzzleId = state.puzzleId,
            currentBlockIndex = state.currentBlockIndex,
            lives = 5,
            currentBlockGrid = emptyList(),
            crossedColHints = emptyList(),
            crossedRowHints = emptyList(),
            selectedColorIndex = state.selectedColor,
            fillMode = state.fillMode,
            isDarkTheme = state.isDarkTheme
        )

        viewModelScope.launch {
            repository.saveProgress(state.puzzleId, progress)
        }
    }

    private fun calculateComplementaryColor(colorInt: Int): Int {
        val r = android.graphics.Color.red(colorInt)
        val g = android.graphics.Color.green(colorInt)
        val b = android.graphics.Color.blue(colorInt)

        val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0

        val compR = 255 - r
        val compG = 255 - g
        val compB = 255 - b

        val compLuminance = (0.299 * compR + 0.587 * compG + 0.114 * compB) / 255.0
        val contrast = abs(luminance - compLuminance)

        return if (contrast > 0.3) {
            android.graphics.Color.rgb(compR, compG, compB)
        } else {
            if (luminance > 0.5) android.graphics.Color.BLACK else android.graphics.Color.WHITE
        }
    }
}
