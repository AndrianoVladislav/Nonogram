package com.example.nonogram.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nonogram.data.storage.PuzzleItem
import com.example.nonogram.domain.repository.PuzzleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class CollectionUiState(
    val puzzles: List<PuzzleItem> = emptyList(),
    val selectedPuzzle: PuzzleItem? = null,
    val isLoading: Boolean = false
)

class CollectionViewModel(
    private val repository: PuzzleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CollectionUiState())
    val uiState: StateFlow<CollectionUiState> = _uiState.asStateFlow()

    fun loadPuzzles() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val items = repository.getPuzzles()
            _uiState.update {
                it.copy(
                    puzzles = items,
                    isLoading = false
                )
            }
        }
    }

    fun selectPuzzle(puzzle: PuzzleItem?) {
        _uiState.update { it.copy(selectedPuzzle = puzzle) }
    }

    fun renamePuzzle(puzzleId: String, newTitle: String) {
        viewModelScope.launch {
            repository.renamePuzzle(puzzleId, newTitle)
            loadPuzzles()
        }
    }

    suspend fun getResultImageFile(puzzleId: String): File {
        return repository.getResultImageFile(puzzleId)
    }
}
