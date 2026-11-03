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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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

    val isDark = uiState.isDarkTheme
    val backgroundColor = if (isDark) Color.Black else Color.White

    val grid = uiState.grid
    val gridHeight = grid.size
    val gridWidth = if (gridHeight > 0) grid[0].size else 0
    val palette = uiState.palette

    val cellScales = remember { mutableStateMapOf<Int, Animatable<Float, AnimationVector1D>>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(8.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LivesDisplay(lives = uiState.lives, soundManager = soundManager)
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 100% Weight Play Area
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val totalAreaWidth = this.maxWidth
            val totalAreaHeight = this.maxHeight

            val leftHintsWidth = totalAreaWidth * 0.25f
            val topHintsHeight = totalAreaHeight * 0.30f

            val availableBoardWidth = totalAreaWidth * 0.75f
            val availableBoardHeight = totalAreaHeight * 0.70f

            val density = LocalDensity.current
            val boardWidthPx = with(density) { availableBoardWidth.toPx() }
            val boardHeightPx = with(density) { availableBoardHeight.toPx() }

            val cellPxFromWidth = if (gridWidth > 0) boardWidthPx / gridWidth else boardWidthPx
            val cellPxFromHeight = if (gridHeight > 0) boardHeightPx / gridHeight else boardHeightPx
            val cellPx = minOf(cellPxFromWidth, cellPxFromHeight)

            val actualBoardWidthDp = with(density) { (cellPx * gridWidth).toDp() }
            val actualBoardHeightDp = with(density) { (cellPx * gridHeight).toDp() }

            Column(modifier = Modifier.fillMaxSize()) {
                // Top Row: Top Column Hints
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(topHintsHeight)
                ) {
                    Spacer(modifier = Modifier.width(leftHintsWidth))
                    TopColumnHints(
                        colHints = uiState.colHints,
                        palette = palette,
                        hintTextColors = uiState.hintTextColors,
                        cellPx = cellPx,
                        widthDp = actualBoardWidthDp,
                        heightDp = topHintsHeight,
                        crossedColHints = uiState.crossedColHints,
                        onColHintClick = { col, hintIndex ->
                            soundManager.playSwitch()
                            viewModel.toggleCrossColHint(col, hintIndex)
                        }
                    )
                }

                // Main Row: Left Row Hints + Board Canvas
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(actualBoardHeightDp)
                ) {
                    LeftRowHints(
                        rowHints = uiState.rowHints,
                        palette = palette,
                        hintTextColors = uiState.hintTextColors,
                        cellPx = cellPx,
                        widthDp = leftHintsWidth,
                        heightDp = actualBoardHeightDp,
                        crossedRowHints = uiState.crossedRowHints,
                        onRowHintClick = { row, hintIndex ->
                            soundManager.playSwitch()
                            viewModel.toggleCrossRowHint(row, hintIndex)
                        }
                    )

                    BoardGridCanvas(
                        grid = grid,
                        gridWidth = gridWidth,
                        gridHeight = gridHeight,
                        palette = palette,
                        cellPx = cellPx,
                        boardWidthDp = actualBoardWidthDp,
                        boardHeightDp = actualBoardHeightDp,
                        selectedColor = uiState.selectedColor,
                        fillMode = uiState.fillMode,
                        isGameOver = uiState.isGameOver,
                        isDarkTheme = isDark,
                        cellScales = cellScales,
                        soundManager = soundManager,
                        onMove = { r, c ->
                            viewModel.makeMove(r, c)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        BottomControls(
            palette = palette,
            selectedColor = uiState.selectedColor,
            fillMode = uiState.fillMode,
            isDarkTheme = isDark,
            onColorSelected = { index ->
                soundManager.playSwitch()
                viewModel.setSelectedColor(index)
            },
            onFillModeToggled = { fill ->
                soundManager.playSwitch()
                viewModel.setFillMode(fill)
            },
            onThemeToggled = { dark ->
                soundManager.playSwitch()
                viewModel.setDarkTheme(dark)
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
                    Text("Pokračovat")
                }
            }
        )
    }
}

@Composable
private fun TopColumnHints(
    colHints: List<List<Hint>>,
    palette: List<Int>,
    hintTextColors: List<Int>,
    cellPx: Float,
    widthDp: Dp,
    heightDp: Dp,
    crossedColHints: Set<Pair<Int, Int>>,
    onColHintClick: (col: Int, hintIndex: Int) -> Unit
) {
    val scrollState = rememberScrollState()
    val maxHintsCount = colHints.maxOfOrNull { it.size } ?: 1
    val totalHintsContentHeightPx = maxHintsCount * cellPx
    val density = LocalDensity.current
    val totalHintsContentHeightDp = with(density) { totalHintsContentHeightPx.toDp() }
    val containerHeightPx = with(density) { heightDp.toPx() }
    val canvasHeightPx = maxOf(containerHeightPx, totalHintsContentHeightPx)

    LaunchedEffect(Unit) {
        scrollState.scrollTo(scrollState.maxValue)
    }

    Box(
        modifier = Modifier
            .width(widthDp)
            .height(heightDp)
            .verticalScroll(scrollState)
    ) {
        Canvas(
            modifier = Modifier
                .width(widthDp)
                .height(maxOf(heightDp, totalHintsContentHeightDp))
                .pointerInput(colHints, crossedColHints, cellPx, canvasHeightPx) {
                    detectTapGestures { offset ->
                        val col = (offset.x / cellPx).toInt().coerceIn(0, colHints.size - 1)
                        val hints = colHints.getOrNull(col) ?: return@detectTapGestures
                        val startY = canvasHeightPx - (hints.size * cellPx)
                        if (offset.y >= startY) {
                            val hintIndex = ((offset.y - startY) / cellPx).toInt()
                            if (hintIndex in hints.indices) {
                                onColHintClick(col, hintIndex)
                            }
                        }
                    }
                }
        ) {
            val drawCanvasHeight = size.height

            colHints.forEachIndexed { col, hints ->
                val startY = drawCanvasHeight - (hints.size * cellPx)

                hints.forEachIndexed { i, hint ->
                    val x = col * cellPx
                    val y = startY + i * cellPx
                    val isCrossed = crossedColHints.contains(col to i)

                    val baseColorInt = palette[hint.color]
                    val baseColor = Color(baseColorInt)
                    val circleColor = baseColor

                    drawCircle(
                        color = circleColor,
                        radius = cellPx / 2,
                        center = Offset(x + cellPx / 2, y + cellPx / 2)
                    )

                    val textColor = if (isCrossed) {
                        android.graphics.Color.GRAY
                    } else {
                        hintTextColors.getOrElse(hint.color) { android.graphics.Color.WHITE }
                    }

                    drawContext.canvas.nativeCanvas.drawText(
                        hint.length.toString(),
                        x + cellPx / 2,
                        y + 5 + cellPx / 2,
                        android.graphics.Paint().apply {
                            color = textColor
                            textAlign = android.graphics.Paint.Align.CENTER
                            textSize = cellPx / 2
                            isFakeBoldText = true
                        }
                    )

                    if (isCrossed) {
                        drawLine(
                            color = Color.Red,
                            start = Offset(x + cellPx * 0.15f, y + cellPx * 0.85f),
                            end = Offset(x + cellPx * 0.85f, y + cellPx * 0.15f),
                            strokeWidth = 4f
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LeftRowHints(
    rowHints: List<List<Hint>>,
    palette: List<Int>,
    hintTextColors: List<Int>,
    cellPx: Float,
    widthDp: Dp,
    heightDp: Dp,
    crossedRowHints: Set<Pair<Int, Int>>,
    onRowHintClick: (row: Int, hintIndex: Int) -> Unit
) {
    val scrollState = rememberScrollState()
    val maxHintsCount = rowHints.maxOfOrNull { it.size } ?: 1
    val totalHintsContentWidthPx = maxHintsCount * cellPx
    val density = LocalDensity.current
    val totalHintsContentWidthDp = with(density) { totalHintsContentWidthPx.toDp() }
    val containerWidthPx = with(density) { widthDp.toPx() }
    val canvasWidthPx = maxOf(containerWidthPx, totalHintsContentWidthPx)

    LaunchedEffect(Unit) {
        scrollState.scrollTo(scrollState.maxValue)
    }

    Box(
        modifier = Modifier
            .width(widthDp)
            .height(heightDp)
            .horizontalScroll(scrollState)
    ) {
        Canvas(
            modifier = Modifier
                .width(maxOf(widthDp, totalHintsContentWidthDp))
                .height(heightDp)
                .pointerInput(rowHints, crossedRowHints, cellPx, canvasWidthPx) {
                    detectTapGestures { offset ->
                        val row = (offset.y / cellPx).toInt().coerceIn(0, rowHints.size - 1)
                        val hints = rowHints.getOrNull(row) ?: return@detectTapGestures
                        val startX = canvasWidthPx - (hints.size * cellPx)
                        if (offset.x >= startX) {
                            val hintIndex = ((offset.x - startX) / cellPx).toInt()
                            if (hintIndex in hints.indices) {
                                onRowHintClick(row, hintIndex)
                            }
                        }
                    }
                }
        ) {
            val drawCanvasWidth = size.width

            rowHints.forEachIndexed { row, hints ->
                val startX = drawCanvasWidth - (hints.size * cellPx)

                hints.forEachIndexed { i, hint ->
                    val x = startX + i * cellPx
                    val y = row * cellPx
                    val isCrossed = crossedRowHints.contains(row to i)

                    val baseColorInt = palette[hint.color]
                    val baseColor = Color(baseColorInt)
                    val circleColor = baseColor

                    drawCircle(
                        color = circleColor,
                        radius = cellPx / 2,
                        center = Offset(x + cellPx / 2, y + cellPx / 2)
                    )

                    val textColor = if (isCrossed) {
                        android.graphics.Color.GRAY
                    } else {
                        hintTextColors.getOrElse(hint.color) { android.graphics.Color.WHITE }
                    }

                    drawContext.canvas.nativeCanvas.drawText(
                        hint.length.toString(),
                        x + cellPx / 2,
                        y + 5 + cellPx / 2,
                        android.graphics.Paint().apply {
                            color = textColor
                            textAlign = android.graphics.Paint.Align.CENTER
                            textSize = cellPx / 2
                            isFakeBoldText = true
                        }
                    )

                    if (isCrossed) {
                        drawLine(
                            color = Color.Red,
                            start = Offset(x + cellPx * 0.15f, y + cellPx * 0.85f),
                            end = Offset(x + cellPx * 0.85f, y + cellPx * 0.15f),
                            strokeWidth = 4f
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BoardGridCanvas(
    grid: List<List<PlayerCell>>,
    gridWidth: Int,
    gridHeight: Int,
    palette: List<Int>,
    cellPx: Float,
    boardWidthDp: Dp,
    boardHeightDp: Dp,
    selectedColor: Int,
    fillMode: Boolean,
    isGameOver: Boolean,
    isDarkTheme: Boolean,
    cellScales: MutableMap<Int, Animatable<Float, AnimationVector1D>>,
    soundManager: SoundManager,
    onMove: (row: Int, col: Int) -> Boolean
) {
    val coroutineScope = rememberCoroutineScope()
    val gridLineColor = if (isDarkTheme) Color.LightGray else Color.Black

    Canvas(
        modifier = Modifier
            .size(width = boardWidthDp, height = boardHeightDp)
            .pointerInput(gridWidth, gridHeight, selectedColor, fillMode, isGameOver, cellPx) {
                if (isGameOver) return@pointerInput

                awaitEachGesture {
                    val down = awaitFirstDown()
                    var lastCell: Pair<Int, Int>? = null
                    var startCell: Pair<Int, Int>? = null
                    var direction = DragDirection.NONE
                    var isCurrentGestureValid = true

                    fun process(position: Offset) {
                        if (!isCurrentGestureValid || isGameOver) return

                        val rawCol = (position.x / cellPx).toInt().coerceIn(0, gridWidth - 1)
                        val rawRow = (position.y / cellPx).toInt().coerceIn(0, gridHeight - 1)

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
                                    val cellId = r * gridWidth + c
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
        for (row in 0 until gridHeight) {
            for (col in 0 until gridWidth) {
                val cell = grid[row][col]
                val x = col * cellPx
                val y = row * cellPx

                cell.color?.let { colorIdx ->
                    val cellId = row * gridWidth + col
                    val scaleVal = cellScales[cellId]?.value ?: 1f
                    val mainColor = Color(palette[colorIdx])

                    val animatedSize = cellPx * scaleVal
                    val offset = (cellPx - animatedSize) / 2
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
                    val radius = cellPx / 6

                    cell.notes.forEachIndexed { i, note ->
                        val nx = x + (i % 3) * (cellPx / 3) + radius
                        val ny = y + (i / 3) * (cellPx / 3) + radius

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
        for (col in 0..gridWidth) {
            val stroke = if (col % 5 == 0) 5f else 2f
            drawLine(
                color = gridLineColor,
                start = Offset(col * cellPx, 0f),
                end = Offset(col * cellPx, gridHeight * cellPx),
                strokeWidth = stroke
            )
        }

        for (row in 0..gridHeight) {
            val stroke = if (row % 5 == 0) 5f else 2f
            drawLine(
                color = gridLineColor,
                start = Offset(0f, row * cellPx),
                end = Offset(gridWidth * cellPx, row * cellPx),
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
    isDarkTheme: Boolean,
    onColorSelected: (Int) -> Unit,
    onFillModeToggled: (Boolean) -> Unit,
    onThemeToggled: (Boolean) -> Unit
) {
    val activeTextColor = if (isDarkTheme) Color.White else Color.Black
    val borderColor = if (isDarkTheme) Color.White else Color.Black

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
                            color = if (selectedColor == index) borderColor else Color.Gray
                        )
                        .clickable { onColorSelected(index) }
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Theme Switch (Black/White BG)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("DARK", fontSize = 10.sp, color = if (isDarkTheme) Color.White else Color.Gray)
                Switch(
                    checked = isDarkTheme,
                    onCheckedChange = { onThemeToggled(it) },
                    modifier = Modifier.scale(0.75f)
                )
            }

            // Fill / Note Switch
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("FILL", fontSize = 10.sp, color = if (fillMode) activeTextColor else Color.Gray)
                Switch(
                    checked = !fillMode,
                    onCheckedChange = { onFillModeToggled(!it) },
                    modifier = Modifier.rotate(90f).padding(vertical = 4.dp)
                )
                Text("NOTE", fontSize = 10.sp, color = if (!fillMode) activeTextColor else Color.Gray)
            }
        }
    }
}

@Composable
fun HeartIcon(filled: Boolean, soundManager: SoundManager?) {
    var previousFilled by remember { mutableStateOf(filled) }
    val scale = remember { Animatable(if (filled) 1f else 0f) }

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
        repeat(5) { i ->
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
