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

data class UploadUiState(
    val isProcessing: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

class UploadViewModel(
    private val repository: PuzzleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UploadUiState())
    val uiState: StateFlow<UploadUiState> = _uiState.asStateFlow()

    fun createPuzzleFromImage(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, errorMessage = null) }
            val result = repository.createPuzzleFromImage(uri)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isProcessing = false, isSuccess = true) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            errorMessage = error.localizedMessage ?: "Failed to process image"
                        )
                    }
                }
            )
        }
    }

    fun importPuzzleFromFile(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, errorMessage = null) }
            val result = repository.importPuzzle(uri)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isProcessing = false, isSuccess = true) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            errorMessage = error.localizedMessage ?: "Failed to import file"
                        )
                    }
                }
            )
        }
    }

    fun resetState() {
        _uiState.value = UploadUiState()
    }
}
