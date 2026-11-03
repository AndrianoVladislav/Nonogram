package com.example.nonogram.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nonogram.data.sound.SoundManager
import com.example.nonogram.data.storage.PuzzleItem
import com.example.nonogram.ui.viewmodel.CollectionViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CollectionScreen(
    viewModel: CollectionViewModel,
    onPlay: (PuzzleItem) -> Unit,
    onSend: (PuzzleItem) -> Unit,
    onUpload: () -> Unit
) {
    val context = LocalContext.current
    val soundManager = remember { SoundManager(context) }
    DisposableEffect(Unit) {
        onDispose { soundManager.release() }
    }

    LaunchedEffect(Unit) {
        viewModel.loadPuzzles()
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selected = uiState.selectedPuzzle
    val sortedPuzzles = remember(uiState.puzzles) {
        uiState.puzzles.sortedBy { it.currentBlock == it.totalBlocks }
    }

    var renamingPuzzle by remember { mutableStateOf<PuzzleItem?>(null) }
    var renameInputText by remember { mutableStateOf("") }

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val topSpacer = screenHeight * 0.75f

    if (uiState.isLoading && uiState.puzzles.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(screenHeight * 0.05f))

        LazyColumn(
            modifier = Modifier
                .height(topSpacer)
                .weight(3f)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(sortedPuzzles) { puzzle ->
                PuzzleItemView(
                    puzzle = puzzle,
                    viewModel = viewModel,
                    isSelected = selected?.id == puzzle.id,
                    onClick = {
                        soundManager.playSwitch()
                        viewModel.selectPuzzle(puzzle)
                    },
                    onLongClick = {
                        soundManager.playSwitch()
                        renameInputText = puzzle.title ?: puzzle.id
                        renamingPuzzle = puzzle
                    }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(screenHeight - topSpacer)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = {
                    if (selected?.currentBlock != selected?.totalBlocks) {
                        selected?.let { onPlay(it) }
                    }
                },
                enabled = selected != null
            ) {
                Text("Hrát")
            }

            Button(
                onClick = { selected?.let { onSend(it) } },
                enabled = selected != null
            ) {
                Text("Odeslat")
            }

            Button(
                onClick = onUpload
            ) {
                Text("Nahrát")
            }
        }
    }

    renamingPuzzle?.let { puzzle ->
        AlertDialog(
            onDismissRequest = { renamingPuzzle = null },
            title = { Text("Přejmenovat puzzle") },
            text = {
                OutlinedTextField(
                    value = renameInputText,
                    onValueChange = { renameInputText = it },
                    label = { Text("Název") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newTitle = renameInputText.ifBlank { puzzle.id }
                        viewModel.renamePuzzle(puzzle.id, newTitle)
                        renamingPuzzle = null
                    }
                ) {
                    Text("Uložit")
                }
            },
            dismissButton = {
                TextButton(onClick = { renamingPuzzle = null }) {
                    Text("Zrušit")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PuzzleItemView(
    puzzle: PuzzleItem,
    viewModel: CollectionViewModel,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val bitmapState = produceState<ImageBitmap?>(
        initialValue = null,
        puzzle.id,
        puzzle.currentBlock
    ) {
        val file = viewModel.getResultImageFile(puzzle.id)
        value = if (file.exists()) {
            BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
        } else null
    }

    val progress = puzzle.currentBlock.toFloat() / puzzle.totalBlocks

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) Color.Green else Color.Black
            )
            .combinedClickable(
                onClick = { onClick() },
                onLongClick = { onLongClick() }
            )
    ) {
        if (bitmapState.value != null) {
            Image(
                bitmap = bitmapState.value!!,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            )
        }

        // Full-width title at top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(vertical = 6.dp, horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = puzzle.title ?: puzzle.id,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Bottom-Left Badge (% completion)
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
                .background(
                    color = Color.DarkGray.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = "${(progress * 100).toInt()}%",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Bottom-Right Badge (Matrix dimension e.g. 25x25)
        if (puzzle.matrixSizeText.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .background(
                        color = Color.DarkGray.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = puzzle.matrixSizeText,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
