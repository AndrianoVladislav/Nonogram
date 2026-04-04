package com.example.nonogram.data.converter

import com.example.nonogram.data.storage.Block

object ImageConverterBlock {

    fun splitToBlocks(
        solution: Array<IntArray>,
        blockSize: Int = 25
    ): List<Block> {

        val height = solution.size
        val width = solution[0].size

        val blocks = mutableListOf<Block>()

        val blocksY = height / blockSize
        val blocksX = width / blockSize

        for (by in 0 until blocksY) {
            for (bx in 0 until blocksX) {

                val block = Array(blockSize) { IntArray(blockSize) }
                val colors = mutableSetOf<Int>()

                for (y in 0 until blockSize) {
                    for (x in 0 until blockSize) {

                        val globalY = by * blockSize + y
                        val globalX = bx * blockSize + x

                        val value = solution[globalY][globalX]

                        block[y][x] = value
                        colors.add(value)
                    }
                }

                blocks.add(Block(block, colors))
            }
        }

        return blocks
    }
}