package com.example.nonogram.data.converter

import androidx.compose.ui.graphics.toArgb
import com.example.nonogram.data.storage.Block
import com.example.nonogram.data.storage.QuantizationResult

object ImageConverterBlock {

    fun splitToBlocks(
        quantizationResult: QuantizationResult,
        blockSizeX: Int = 25,
        blockSizeY: Int = 25
    ): List<Block> {

        val height = quantizationResult.solution.size
        if (height == 0) return emptyList()
        val width = quantizationResult.solution[0].size
        if (width == 0) return emptyList()

        val blocks = mutableListOf<Block>()

        val blocksY = height / blockSizeY
        val blocksX = width / blockSizeX

        val palette = quantizationResult.palette.map { it.toArgb() }

        for (by in 0 until blocksY) {
            for (bx in 0 until blocksX) {

                val matrix: List<List<Int>> = List(blockSizeY) { y ->
                    val globalY = by * blockSizeY + y
                    List(blockSizeX) { x ->
                        val globalX = bx * blockSizeX + x
                        quantizationResult.solution[globalY][globalX]
                    }
                }

                blocks.add(Block(matrix, palette))
            }
        }

        return blocks
    }
}
