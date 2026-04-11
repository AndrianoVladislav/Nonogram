package com.example.nonogram.data.converter

import com.example.nonogram.data.storage.Block
import com.example.nonogram.data.storage.QuantizationResult

object ImageConverterBlock {

    fun splitToBlocks(
        quantizationResult: QuantizationResult,
        blockSize: Int = 25
    ): List<Block> {

        val height = quantizationResult.solution.size
        val width = quantizationResult.solution[0].size

        val blocks = mutableListOf<Block>()

        val blocksY = height / blockSize
        val blocksX = width / blockSize

        for (by in 0 until blocksY) {
            for (bx in 0 until blocksX) {

//                val block = MutableList(blockSize) { IntArray(blockSize) }
                val matrix = ArrayList<IntArray>(blockSize)
                for (i in 0 until blockSize) matrix.add(IntArray(blockSize))

                for (y in 0 until blockSize) {
                    for (x in 0 until blockSize) {

                        val globalY = by * blockSize + y
                        val globalX = bx * blockSize + x

                        val value = quantizationResult.solution[globalY][globalX]

                        matrix[y][x] = value
                    }
                }

                blocks.add(Block(matrix, quantizationResult.palette))
            }
        }

        return blocks
    }
}