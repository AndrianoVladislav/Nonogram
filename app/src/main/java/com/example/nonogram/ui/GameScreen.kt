package com.example.nonogram.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nonogram.data.sound.SoundManager
import com.example.nonogram.data.storage.Hint
import com.example.nonogram.domain.entity.PlayerCell
import com.example.nonogram.ui.viewmodel.GameViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max

enum class DragDirection { NONE, HORIZONTAL, VERTICAL }

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onExit: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val soundManager = remember { SoundManager(context) }
    DisposableEffect(Unit) {
        onDispose { soundManager.release() }
    }

    if (uiState.isLoading || uiState.grid.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    val grid = uiState.grid
    val gridSize = grid.size
    val palette = uiState.palette

    val cellScales = remember { mutableStateMapOf<Int, Animatable<Float, AnimationVector1D>>() }

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp

    val topSpacer = screenHeight * 0.02f
    val leftHintsWidth = screenWidth * 0.15f
    val fieldWidth = screenWidth * 0.80f

    val cellSize = fieldWidth / gridSize
    val maxHints = 20

    Column(
        modifier = Modifier
            .padding(10.dp, 10.dp, 0.dp, 10.dp)
            .fillMaxSize()
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LivesDisplay(lives = uiState.lives, soundManager = soundManager)
        }

        Spacer(modifier = Modifier.height(topSpacer))

        TopColumnHints(
            colHints = uiState.colHints,
            palette = palette,
            cellSize = cellSize,
            maxHints = maxHints,
            leftHintsWidth = leftHintsWidth,
            fieldWidth = fieldWidth
        )

        Row {
            LeftRowHints(
                rowHints = uiState.rowHints,
                palette = palette,
                cellSize = cellSize,
                leftHintsWidth = leftHintsWidth,
                fieldWidth = fieldWidth
            )

            BoardGridCanvas(
                grid = grid,
                gridSize = gridSize,
                palette = palette,
                cellSize = cellSize,
                fieldWidth = fieldWidth,
                selectedColor = uiState.selectedColor,
                fillMode = uiState.fillMode,
                isGameOver = uiState.isGameOver,
                cellScales = cellScales,
                soundManager = soundManager,
                onMove = { r, c ->
                    viewModel.makeMove(r, c)
                }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        BottomControls(
            palette = palette,
            selectedColor = uiState.selectedColor,
            fillMode = uiState.fillMode,
            onColorSelected = { index ->
                soundManager.playSwitch()
                viewModel.setSelectedColor(index)
            },
            onFillModeToggled = { fill ->
                soundManager.playSwitch()
                viewModel.setFillMode(fill)
            }
        )
    }

    if (uiState.isVictory) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Hotovo!") },
            text = { Text("Chceš pokračovat?") },
            confirmButton = {
                Button(onClick = {
                    soundManager.playSwitch()
                    if (uiState.currentBlockIndex + 1 < uiState.totalBlocks) {
                        viewModel.nextBlock()
                    } else {
                        onExit()
                    }
                }) {
                    Text("Pokračovat")
                }
            },
            dismissButton = {
                Button(onClick = {
                    soundManager.playSwitch()
                    onExit()
                }) {
                    Text("Zpět")
                }
            }
        )
    }

    if (uiState.isGameOver) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Prohráli jste") },
            text = { Text("Chceš to zkusit znovu?") },
            confirmButton = {
                Button(onClick = {
                    soundManager.playSwitch()
                    cellScales.clear()
                    viewModel.restartGame()
                }) {
                    Text("Znovu")
                }
            },
            dismissButton = {
                Button(onClick = {
                    soundManager.playSwitch()
                    onExit()
                }) {
                    Text("Zpět")
                }
            }
        )
    }
}

@Composable
private fun TopColumnHints(
    colHints: List<List<Hint>>,
    palette: List<Int>,
    cellSize: Dp,
    maxHints: Int,
    leftHintsWidth: Dp,
    fieldWidth: Dp
) {
    Canvas(
        modifier = Modifier
            .padding(start = leftHintsWidth)
            .width(fieldWidth)
            .height(cellSize * maxHints)
    ) {
        val px = cellSize.toPx()

        colHints.forEachIndexed { col, hints ->
            val startY = (maxHints - hints.size) * px

            hints.forEachIndexed { i, hint ->
                val x = col * px
                val y = startY + i * px

                drawCircle(
                    color = Color(palette[hint.color]),
                    radius = px / 2,
                    center = Offset(x + px / 2, y + px / 2)
                )

                drawContext.canvas.nativeCanvas.drawText(
                    hint.length.toString(),
                    x + px / 2,
                    y + 5 + px / 2,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize = px / 2
                        isFakeBoldText = true
                    }
                )
            }
        }
    }
}

@Composable
private fun LeftRowHints(
    rowHints: List<List<Hint>>,
    palette: List<Int>,
    cellSize: Dp,
    leftHintsWidth: Dp,
    fieldWidth: Dp
) {
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        scrollState.scrollTo(scrollState.maxValue)
    }

    Box(
        modifier = Modifier
            .width(leftHintsWidth)
            .height(fieldWidth)
            .horizontalScroll(scrollState)
    ) {
        Canvas(
            modifier = Modifier
                .width(cellSize * rowHints.size)
                .height(fieldWidth)
        ) {
            val px = cellSize.toPx()

            rowHints.forEachIndexed { row, hints ->
                val startX = (rowHints.size - hints.size) * px

                hints.forEachIndexed { i, hint ->
                    val x = startX + i * px
                    val y = row * px

                    drawCircle(
                        color = Color(palette[hint.color]),
                        radius = px / 2,
                        center = Offset(x + px / 2, y + px / 2)
                    )

                    drawContext.canvas.nativeCanvas.drawText(
                        hint.length.toString(),
                        x + px / 2,
                        y + 5 + px / 2,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.WHITE
                            textAlign = android.graphics.Paint.Align.CENTER
                            textSize = px / 2
                            isFakeBoldText = true
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BoardGridCanvas(
    grid: List<List<PlayerCell>>,
    gridSize: Int,
    palette: List<Int>,
    cellSize: Dp,
    fieldWidth: Dp,
    selectedColor: Int,
    fillMode: Boolean,
    isGameOver: Boolean,
    cellScales: MutableMap<Int, Animatable<Float, AnimationVector1D>>,
    soundManager: SoundManager,
    onMove: (row: Int, col: Int) -> Boolean
) {
    val coroutineScope = rememberCoroutineScope()

    Canvas(
        modifier = Modifier
            .size(fieldWidth)
            .pointerInput(gridSize, selectedColor, fillMode, isGameOver) {
                if (isGameOver) return@pointerInput

                val px = cellSize.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown()
                    var lastCell: Pair<Int, Int>? = null
                    var startCell: Pair<Int, Int>? = null
                    var direction = DragDirection.NONE
                    var isCurrentGestureValid = true

                    fun process(position: Offset) {
                        if (!isCurrentGestureValid || isGameOver) return

                        val rawCol = (position.x / px).toInt().coerceIn(0, gridSize - 1)
                        val rawRow = (position.y / px).toInt().coerceIn(0, gridSize - 1)

                        if (startCell == null) startCell = rawRow to rawCol

                        val (sRow, sCol) = startCell!!

                        if (direction == DragDirection.NONE) {
                            val dr = abs(rawRow - sRow)
                            val dc = abs(rawCol - sCol)
                            if (dr > 0 || dc > 0) {
                                direction = if (dr > dc) DragDirection.VERTICAL else DragDirection.HORIZONTAL
                            }
                        }

                        val targetCell = when (direction) {
                            DragDirection.HORIZONTAL -> sRow to rawCol
                            DragDirection.VERTICAL -> rawRow to sCol
                            DragDirection.NONE -> rawRow to rawCol
                        }

                        if (targetCell != lastCell) {
                            val (tRow, tCol) = targetCell
                            val (pRow, pCol) = lastCell ?: targetCell

                            val steps = max(abs(tRow - pRow), abs(tCol - pCol))
                            for (i in 0..steps) {
                                val r = if (steps == 0) pRow else pRow + (tRow - pRow) * i / steps
                                val c = if (steps == 0) pCol else pCol + (tCol - pCol) * i / steps

                                if (i == 0 && lastCell != null) continue

                                val wasEmpty = grid[r][c].color == null
                                val success = onMove(r, c)

                                if (!success) {
                                    isCurrentGestureValid = false
                                    soundManager.playHeartBreak()
                                    break
                                }

                                if (wasEmpty && grid[r][c].color != null) {
                                    soundManager.playCorrect()
                                    val cellId = r * gridSize + c
                                    coroutineScope.launch {
                                        val anim = Animatable(0.5f)
                                        cellScales[cellId] = anim
                                        anim.animateTo(1f, tween(300))
                                    }
                                }
                            }
                            lastCell = targetCell
                        }
                    }

                    process(down.position)

                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.all { !it.pressed }) break
                        event.changes.forEach { change ->
                            process(change.position)
                            change.consume()
                        }
                    }
                }
            }
    ) {
        val px = cellSize.toPx()

        for (row in 0 until gridSize) {
            for (col in 0 until gridSize) {
                val cell = grid[row][col]
                val x = col * px
                val y = row * px

                cell.color?.let { colorIdx ->
                    val cellId = row * gridSize + col
                    val scaleVal = cellScales[cellId]?.value ?: 1f
                    val mainColor = Color(palette[colorIdx])

                    val animatedSize = px * scaleVal
                    val offset = (px - animatedSize) / 2
                    val rectTopLeft = Offset(x + offset, y + offset)
                    val rectSize = Size(animatedSize, animatedSize)

                    val shadow = mainColor.darken(0.3f)
                    val highlight = mainColor.lighten(0.3f)

                    val pathShadow = Path().apply {
                        moveTo(rectTopLeft.x + rectSize.width, rectTopLeft.y)
                        lineTo(rectTopLeft.x + rectSize.width, rectTopLeft.y + rectSize.height)
                        lineTo(rectTopLeft.x, rectTopLeft.y + rectSize.height)
                        lineTo(rectTopLeft.x + 4, rectTopLeft.y + rectSize.height - 4)
                        lineTo(rectTopLeft.x + rectSize.width - 4, rectTopLeft.y + rectSize.height - 4)
                        lineTo(rectTopLeft.x + rectSize.width - 4, rectTopLeft.y + 4)
                        close()
                    }
                    drawPath(pathShadow, shadow)

                    val pathHighlight = Path().apply {
                        moveTo(rectTopLeft.x, rectTopLeft.y)
                        lineTo(rectTopLeft.x + rectSize.width, rectTopLeft.y)
                        lineTo(rectTopLeft.x + rectSize.width - 4, rectTopLeft.y + 4)
                        lineTo(rectTopLeft.x + 4, rectTopLeft.y + 4)
                        lineTo(rectTopLeft.x + 4, rectTopLeft.y + 4)
                        lineTo(rectTopLeft.x + 4, rectTopLeft.y + rectSize.height - 4)
                        lineTo(rectTopLeft.x, rectTopLeft.y + rectSize.height)
                        close()
                    }
                    drawPath(pathHighlight, highlight)

                    drawRect(
                        color = mainColor,
                        topLeft = Offset(rectTopLeft.x + 4, rectTopLeft.y + 4),
                        size = Size(rectSize.width - 8, rectSize.height - 8)
                    )
                }

                if (cell.color == null && cell.notes.isNotEmpty()) {
                    val radius = cellSize.toPx() / 6

                    cell.notes.forEachIndexed { i, note ->
                        val nx = x + (i % 3) * (cellSize.toPx() / 3) + radius
                        val ny = y + (i / 3) * (cellSize.toPx() / 3) + radius

                        drawCircle(
                            color = Color(palette[note]),
                            radius = radius,
                            center = Offset(nx, ny)
                        )
                    }
                }
            }
        }

        // Grid lines
        for (i in 0..gridSize) {
            val stroke = if (i % 5 == 0) 5f else 2f
            drawLine(
                color = Color.Black,
                start = Offset(i * px, 0f),
                end = Offset(i * px, gridSize * px),
                strokeWidth = stroke
            )
        }

        for (i in 0..gridSize) {
            val stroke = if (i % 5 == 0) 5f else 2f
            drawLine(
                color = Color.Black,
                start = Offset(0f, i * px),
                end = Offset(gridSize * px, i * px),
                strokeWidth = stroke
            )
        }
    }
}

@Composable
private fun BottomControls(
    palette: List<Int>,
    selectedColor: Int,
    fillMode: Boolean,
    onColorSelected: (Int) -> Unit,
    onFillModeToggled: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            palette.forEachIndexed { index, color ->
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .padding(4.dp)
                        .background(Color(color))
                        .border(
                            width = if (selectedColor == index) 3.dp else 1.dp,
                            color = if (selectedColor == index) Color.Black else Color.Gray
                        )
                        .clickable { onColorSelected(index) }
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("FILL", fontSize = 10.sp, color = if (fillMode) Color.Black else Color.Gray)
            Switch(
                checked = !fillMode,
                onCheckedChange = { onFillModeToggled(!it) },
                modifier = Modifier.rotate(90f).padding(vertical = 8.dp)
            )
            Text("NOTE", fontSize = 10.sp, color = if (!fillMode) Color.Black else Color.Gray)
        }
    }
}

@Composable
fun HeartIcon(filled: Boolean, soundManager: SoundManager?) {
    var previousFilled by remember { mutableStateOf(filled) }
    val scale = remember { Animatable(1f) }

    LaunchedEffect(filled) {
        if (previousFilled && !filled) {
            scale.animateTo(1.5f, animationSpec = tween(100))
            scale.animateTo(0f, animationSpec = tween(300))
        } else if (!previousFilled && filled) {
            scale.snapTo(1f)
        }
        previousFilled = filled
    }

    Icon(
        imageVector = Icons.Filled.Favorite,
        contentDescription = null,
        modifier = Modifier
            .size(32.dp)
            .scale(if (filled) 1f else scale.value),
        tint = if (filled) Color.Red else Color.Gray.copy(alpha = 0.5f)
    )
}

@Composable
fun LivesDisplay(lives: Int, soundManager: SoundManager?) {
    Row(
        modifier = Modifier.padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(3) { i ->
            HeartIcon(filled = i < lives, soundManager = soundManager)
        }
    }
}

fun Color.darken(factor: Float = 0.2f): Color = Color(
    red = red * (1 - factor),
    green = green * (1 - factor),
    blue = blue * (1 - factor),
    alpha = alpha
)

fun Color.lighten(factor: Float = 0.2f): Color = Color(
    red = red + (1 - red) * factor,
    green = green + (1 - green) * factor,
    blue = blue + (1 - blue) * factor,
    alpha = alpha
)
