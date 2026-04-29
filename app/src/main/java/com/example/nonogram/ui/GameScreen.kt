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
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nonogram.data.storage.Block
import com.example.nonogram.data.storage.StorageManager
import com.example.nonogram.domain.entity.Game
import com.example.nonogram.domain.entity.InputMode
import com.example.nonogram.domain.entity.SoundManager
import com.example.nonogram.ui.theme.NonogramTheme
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max

enum class DragDirection { NONE, HORIZONTAL, VERTICAL }

@Composable
fun GameScreen(
    game: Game,
    onExit: () -> Boolean,
    onNextBlock: () -> Unit,
    onRestart: () -> Unit,
    puzzleId: String,
    storageManager: StorageManager
) {

    val board = game.board
    val grid = board.getPlayerGrid()
    val gridSize = grid.size
    val palette = game.block.usedColors
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val soundManager = remember { SoundManager(context) }
    DisposableEffect(Unit) {
        onDispose { soundManager.release() }
    }

    var selectedColor by remember { mutableIntStateOf(1.coerceAtMost(palette.size - 1)) }
    var fillMode by remember { mutableStateOf(true) } // ON = FILL
    var tick by remember { mutableIntStateOf(0) }
    var showWinDialog by remember { mutableStateOf(false) }
    var showLoseDialog by remember { mutableStateOf(false) }

    // Map to store scale animations for each cell
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
            key(tick) {
                LivesDisplay(lives = game.lives, soundManager)
            }
        }

        Spacer(modifier = Modifier.height(topSpacer))

        // Nápověda v horní části
        Canvas(
            modifier = Modifier
                .padding(start = leftHintsWidth)
                .width(fieldWidth)
                .height(cellSize * maxHints)
        ) {

            val px = cellSize.toPx()

            board.colHints.forEachIndexed { col, hints ->

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

        // Hlavní část (vlevo nápověda + pole)
        Row {

            // Nápovědy (scroll)
            val rowHints = board.rowHints

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

            // 🔶 Pole
            Canvas(
                modifier = Modifier
                    .size(fieldWidth)
                    .pointerInput(gridSize, selectedColor, fillMode, game.isGameOver) {
                        if (game.isGameOver) return@pointerInput

                        val px = cellSize.toPx()
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            var lastCell: Pair<Int, Int>? = null
                            var startCell: Pair<Int, Int>? = null
                            var direction = DragDirection.NONE
                            var isCurrentGestureValid = true

                            fun process(position: Offset) {
                                if (!isCurrentGestureValid || game.isGameOver) return

                                val rawCol = (position.x / px).toInt().coerceIn(0, gridSize - 1)
                                val rawRow = (position.y / px).toInt().coerceIn(0, gridSize - 1)

                                if (startCell == null) startCell = rawRow to rawCol

                                val (sRow, sCol) = startCell!!

                                // Определение направления (фиксация оси)
                                if (direction == DragDirection.NONE) {
                                    val dr = abs(rawRow - sRow)
                                    val dc = abs(rawCol - sCol)
                                    if (dr > 0 || dc > 0) {
                                        direction = if (dr > dc) DragDirection.VERTICAL else DragDirection.HORIZONTAL
                                    }
                                }

                                // Привязка к оси
                                val targetCell = when (direction) {
                                    DragDirection.HORIZONTAL -> sRow to rawCol
                                    DragDirection.VERTICAL -> rawRow to sCol
                                    DragDirection.NONE -> rawRow to rawCol
                                }

                                if (targetCell != lastCell) {
                                    val (tRow, tCol) = targetCell
                                    val (pRow, pCol) = lastCell ?: targetCell

                                    // Интерполяция (заполнение всех клеток в линии)
                                    val steps = max(abs(tRow - pRow), abs(tCol - pCol))
                                    for (i in 0..steps) {
                                        val r = if (steps == 0) pRow else pRow + (tRow - pRow) * i / steps
                                        val c = if (steps == 0) pCol else pCol + (tCol - pCol) * i / steps

                                        // Не обрабатываем одну и ту же клетку дважды за одно движение
                                        if (i == 0 && lastCell != null) continue

                                        val mode = if (fillMode) InputMode.FILL else InputMode.NOTE
                                        val wasEmpty = grid[r][c].color == null

                                        if (!game.makeMove(r, c, selectedColor, mode)) {
                                            isCurrentGestureValid = false
                                            soundManager.playHeartBreak()
                                            tick++
                                            break
                                        }

                                        // Start animation if a new cell was filled
                                        if (wasEmpty && grid[r][c].color != null) {
                                            soundManager.playCorrect()
                                            val cellId = r * gridSize + c
                                            scope.launch {
                                                val anim = Animatable(0.5f)
                                                cellScales[cellId] = anim
                                                anim.animateTo(1f, tween(300))
                                            }
                                        }

                                        tick++
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

                if (game.isVictory) showWinDialog = true
                if (game.isGameOver) showLoseDialog = true

                // Mřížka
                val px = cellSize.toPx()
                tick.let { _ ->
                    for (row in 0 until gridSize) {
                        for (col in 0 until gridSize) {

                            val cell = grid[row][col]

                            val x = col * px
                            val y = row * px

                            cell.color?.let { colorIdx ->
                                val cellId = row * gridSize + col
                                val scaleVal = cellScales[cellId]?.value ?: 1f
                                val mainColor = Color(palette[colorIdx])

                                // Draw volumetric cell
                                val animatedSize = px * scaleVal
                                val offset = (px - animatedSize) / 2
                                val rectTopLeft = Offset(x + offset, y + offset)
                                val rectSize = Size(animatedSize, animatedSize)

                                // 3D Effect using paths for edges
                                val shadow = mainColor.darken(0.3f)
                                val highlight = mainColor.lighten(0.3f)

                                // Bottom/Right shadow
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

                                // Top/Left highlight
                                val pathHighlight = Path().apply {
                                    moveTo(rectTopLeft.x, rectTopLeft.y)
                                    lineTo(rectTopLeft.x + rectSize.width, rectTopLeft.y)
                                    lineTo(rectTopLeft.x + rectSize.width - 4, rectTopLeft.y + 4)
                                    lineTo(rectTopLeft.x + 4, rectTopLeft.y + 4)
                                    lineTo(rectTopLeft.x + 4, rectTopLeft.y + rectSize.height - 4)
                                    lineTo(rectTopLeft.x, rectTopLeft.y + rectSize.height)
                                    close()
                                }
                                drawPath(pathHighlight, highlight)

                                // Main face
                                drawRect(
                                    color = mainColor,
                                    topLeft = Offset(rectTopLeft.x + 4, rectTopLeft.y + 4),
                                    size = Size(rectSize.width - 8, rectSize.height - 8)
                                )
                            }

                            // Notes
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
                }

                // Svislé čáry
                for (i in 0..gridSize) {

                    val stroke = if (i % 5 == 0) 5f else 2f

                    drawLine(
                        color = Color.Black,
                        start = Offset(i * px, 0f),
                        end = Offset(i * px, gridSize * px),
                        strokeWidth = stroke
                    )
                }

                // vodorovné čáry
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

        Spacer(modifier = Modifier.weight(1f))

        // Dolní panel
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Paleta
            Row (horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
                            .clickable {
                                soundManager.playSwitch()
                                selectedColor = index
                            }
                    )
                }
            }

            // Switch
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("FILL", fontSize = 10.sp, color = if (fillMode) Color.Black else Color.Gray)
                Switch(
                    checked = !fillMode, // Swapped logic to match "Vertical" feel if needed, but let's keep it simple
                    onCheckedChange = {
                        soundManager.playSwitch()
                        fillMode = !it
                    },
                    modifier = Modifier.rotate(90f).padding(vertical = 8.dp)
                )
                Text("NOTE", fontSize = 10.sp, color = if (!fillMode) Color.Black else Color.Gray)
            }
        }
    }

    if (showWinDialog) {

        AlertDialog(
            onDismissRequest = {},
            title = { Text("Hotovo!") },
            text = { Text("Chceš pokračovat?") },

            confirmButton = {
                Button(onClick = {
                    soundManager.playSwitch()
                    val puzzle = storageManager
                        .loadMeta()
                        .first { it.id == puzzleId }

                    val nextBlock = puzzle.currentBlock + 1
                    storageManager.updateProgress(puzzleId, nextBlock)

                    showWinDialog = false

                    if (nextBlock < puzzle.totalBlocks) {
                        onNextBlock()
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
                    val puzzle = storageManager
                        .loadMeta()
                        .first { it.id == puzzleId }

                    val nextBlock = puzzle.currentBlock + 1
                    storageManager.updateProgress(puzzleId, nextBlock)

                    showWinDialog = false
                    onExit()

                }) {
                    Text("Zpět")
                }
            }
        )
    }

    if (showLoseDialog) {

        AlertDialog(
            onDismissRequest = {},

            title = { Text("Prohráli jste") },
            text = { Text("Chceš to zkusit znovu?") },

            confirmButton = {
                Button(onClick = {
                    soundManager.playSwitch()
                    showLoseDialog = false
                    cellScales.clear()
                    onRestart()

                }) {
                    Text("Znovu")
                }
            },

            dismissButton = {
                Button(onClick = {
                    soundManager.playSwitch()
                    showLoseDialog = false
                    onExit()

                }) {
                    Text("Zpět")
                }
            }
        )
    }
}

@Composable
fun HeartIcon(filled: Boolean, soundManager: SoundManager?) {
    var previousFilled by remember { mutableStateOf(filled) }
    val scale = remember { Animatable(1f) }

    LaunchedEffect(filled) {
        if (previousFilled && !filled) {
            // Heart breaking animation
//            soundManager?.playHeartBreak()
            scale.animateTo(1.5f, animationSpec = tween(100))
            scale.animateTo(0f, animationSpec = tween(300))
        } else if (!previousFilled && filled) {
            // Reset scale if restarted
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
            HeartIcon(filled = i < lives, soundManager)
        }
    }
}

// Helper to darken/lighten colors for 3D effect
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

@Preview(showBackground = true)
@Composable
fun GameScreenPreview() {
    val solution = List(25) { y ->
        if (y == 24) {
            listOf(1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1, 2, 3, 4, 5)
        } else {
            List(25) { 0 }
        }
    }
    val block = Block(
        solution, listOf(
            Color.Red.toArgb(), Color.Blue.toArgb(),
            Color.Green.toArgb(), Color.Yellow.toArgb(), Color.Cyan.toArgb(), Color.Magenta.toArgb()
        )
    )

    val game = Game(block)
    NonogramTheme {
        GameScreen(
            game = game,
            onExit = { true },
            onNextBlock = {},
            onRestart = {},
            puzzleId = "123",
            storageManager = StorageManager(LocalContext.current)
        )
    }
}