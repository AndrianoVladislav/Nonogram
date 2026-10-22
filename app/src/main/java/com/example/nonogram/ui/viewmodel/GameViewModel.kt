package com.example.nonogram.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nonogram.data.storage.Block
import com.example.nonogram.data.storage.Hint
import com.example.nonogram.domain.entity.Game
import com.example.nonogram.domain.entity.InputMode
import com.example.nonogram.domain.entity.PlayerCell
import com.example.nonogram.domain.repository.PuzzleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GameUiState(
    val puzzleId: String = "",
    val currentBlockIndex: Int = 0,
    val totalBlocks: Int = 0,
    val block: Block? = null,
    val grid: List<List<PlayerCell>> = emptyList(),
    val palette: List<Int> = emptyList(),
    val lives: Int = 3,
    val selectedColor: Int = 1,
    val fillMode: Boolean = true,
    val isVictory: Boolean = false,
    val isGameOver: Boolean = false,
    val rowHints: List<List<Hint>> = emptyList(),
    val colHints: List<List<Hint>> = emptyList(),
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
            val puzzle = repository.getPuzzle(puzzleId) ?: return@launch
            val block = repository.getBlock(puzzleId, puzzle.currentBlock) ?: return@launch

            val game = Game(block)
            currentGame = game

            _uiState.update {
                GameUiState(
                    puzzleId = puzzleId,
                    currentBlockIndex = puzzle.currentBlock,
                    totalBlocks = puzzle.totalBlocks,
                    block = block,
                    grid = game.board.playerGrid,
                    palette = block.usedColors,
                    lives = game.lives,
                    selectedColor = 1.coerceAtMost(block.usedColors.size - 1),
                    fillMode = true,
                    isVictory = game.isVictory,
                    isGameOver = game.isGameOver,
                    rowHints = game.board.rowHints,
                    colHints = game.board.colHints,
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

        if (game.isVictory) {
            viewModelScope.launch {
                val nextBlock = currentState.currentBlockIndex + 1
                repository.updateProgress(currentState.puzzleId, nextBlock)
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

    fun restartGame() {
        val currentState = _uiState.value
        val block = currentState.block ?: return
        val game = Game(block)
        currentGame = game

        _uiState.update {
            it.copy(
                grid = game.board.playerGrid,
                lives = game.lives,
                isVictory = false,
                isGameOver = false
            )
        }
    }

    fun nextBlock() {
        val currentState = _uiState.value
        val nextBlockIndex = currentState.currentBlockIndex + 1
        if (nextBlockIndex >= currentState.totalBlocks) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val block = repository.getBlock(currentState.puzzleId, nextBlockIndex) ?: return@launch
            val game = Game(block)
            currentGame = game

            _uiState.update {
                it.copy(
                    currentBlockIndex = nextBlockIndex,
                    block = block,
                    grid = game.board.playerGrid,
                    palette = block.usedColors,
                    lives = game.lives,
                    selectedColor = 1.coerceAtMost(block.usedColors.size - 1),
                    fillMode = true,
                    isVictory = false,
                    isGameOver = false,
                    rowHints = game.board.rowHints,
                    colHints = game.board.colHints,
                    isLoading = false
                )
            }
        }
    }
}
