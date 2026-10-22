package com.example.nonogram.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nonogram.domain.repository.PuzzleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SendUiState(
    val isExporting: Boolean = false,
    val message: String? = null
)

class SendViewModel(
    private val repository: PuzzleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SendUiState())
    val uiState: StateFlow<SendUiState> = _uiState.asStateFlow()

    fun exportPuzzle(puzzleId: String, uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, message = null) }
            val result = repository.exportPuzzle(puzzleId, uri)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isExporting = false, message = "Export úspěšný") }
                },
                onFailure = {
                    _uiState.update { it.copy(isExporting = false, message = "Chyba při exportu") }
                }
            )
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
