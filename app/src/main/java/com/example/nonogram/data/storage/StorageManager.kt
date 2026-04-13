package com.example.nonogram.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class StorageManager(context: Context) {

    private val json = Json { prettyPrint = true }

    private val rootDir = File(context.filesDir, "puzzles")

    init {
        if (!rootDir.exists()) rootDir.mkdirs()
    }

    private val metaFile = File(rootDir, "meta.json")

    fun loadMeta(): List<PuzzleItem> {

        if (!metaFile.exists()) return emptyList()

        return try {
            val text = metaFile.readText()
            json.decodeFromString(text)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveMeta(puzzles: List<PuzzleItem>) {

        val text = json.encodeToString(puzzles)
        metaFile.writeText(text)
    }

    private fun getPuzzleDir(id: String): File {
        return File(rootDir, id).apply { mkdirs() }
    }

    fun savePuzzleData(id: String, data: PuzzleData) {

        val dir = getPuzzleDir(id)
        val file = File(dir, "blocks.json")

        val text = json.encodeToString(data)
        file.writeText(text)
    }

    fun loadPuzzleData(id: String): PuzzleData? {

        val file = File(getPuzzleDir(id), "blocks.json")
        if (!file.exists()) return null

        return try {
            json.decodeFromString(file.readText())
        } catch (e: Exception) {
            null
        }
    }

    fun loadBlock(id: String, index: Int): Block? {

        val data = loadPuzzleData(id) ?: return null

        return data.blocks.getOrNull(index)
    }

    fun updateProgress(id: String, newBlock: Int) {

        val puzzles = loadMeta().toMutableList()

        val index = puzzles.indexOfFirst { it.id == id }
        if (index == -1) return

        val puzzle = puzzles[index]
        puzzles[index] = puzzle.copy(currentBlock = newBlock)

        saveMeta(puzzles)
    }

    fun isCompleted(puzzle: PuzzleItem): Boolean {
        return puzzle.currentBlock >= puzzle.totalBlocks
    }

    fun getResultImage(id: String): File {
        return File(getPuzzleDir(id), "result.png")
    }

    fun addPuzzle(puzzle: PuzzleItem) {

        val list = loadMeta().toMutableList()

        list.add(puzzle)

        saveMeta(list)
    }

    fun saveResultImage(id: String, bitmap: Bitmap) {

        val file = File(getPuzzleDir(id), "result.png")

        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    fun exportPuzzle(id: String, outputUri: Uri, context: Context) {

        val puzzleDir = getPuzzleDir(id)

        val original = loadPuzzleData(id)!!

        val rleBlocks = original.blocks.map { block ->
            block.matrix.map { row ->
                encodeRLE(row).map { RLE(it.color, it.count) }
            }
        }

        val compressedBlocks = RLEBlocks(rleBlocks, original.palette)

        val json = Json.encodeToString(compressedBlocks)

        val resolver = context.contentResolver

        resolver.openOutputStream(outputUri)?.use { outputStream ->
            ZipOutputStream(BufferedOutputStream(outputStream)).use { zip ->

                // 🔹 blocks.json
                zip.putNextEntry(ZipEntry("blocks.json"))
                zip.write(json.toByteArray())
                zip.closeEntry()

                // 🔹 result.png
                val resultFile = File(puzzleDir, "result.png")
                if (resultFile.exists()) {
                    zip.putNextEntry(ZipEntry("result.png"))
                    zip.write(resultFile.readBytes())
                    zip.closeEntry()
                }
            }
        }
    }

    fun importPuzzle(uri: Uri, context: Context) {

        val resolver = context.contentResolver

        val newId = "puzzle_${System.currentTimeMillis()}"
        val puzzleDir = getPuzzleDir(newId)

        resolver.openInputStream(uri)?.use { inputStream ->
            ZipInputStream(BufferedInputStream(inputStream)).use { zip ->

                var entry: ZipEntry?

                while (zip.nextEntry.also { entry = it } != null) {

                    val file = File(puzzleDir, entry!!.name)

                    FileOutputStream(file).use { out ->
                        zip.copyTo(out)
                    }

                    zip.closeEntry()

                    if (!entry.name.endsWith(".json") && !entry.name.endsWith(".png")) return
                }
            }
        }

        // blocks.json
        val compressedData: RLEBlocks =
            Json.decodeFromString(File(puzzleDir, "blocks.json").readText())

        // RLE
        val blocks: List<Block> = compressedData.blocks.map { block ->
            Block(
                matrix = block.map {
                    decodeRLE(it)
                },
                usedColors = compressedData.palette
            )
        }

        val decompressedBlocks = PuzzleData(
            palette = compressedData.palette,
            blocks = blocks
        )

        savePuzzleData(newId, decompressedBlocks)
        val meta = loadMeta().toMutableList()

        meta.add(
            PuzzleItem(
                id = newId,
                currentBlock = 0,
                totalBlocks = decompressedBlocks.blocks.size
            )
        )

        saveMeta(meta)
    }

    fun encodeRLE(row: List<Int>): List<RLE> {

        if (row.isEmpty()) return emptyList()

        val result = mutableListOf<RLE>()

        var current = row[0]
        var count = 1

        for (i in 1 until row.size) {
            if (row[i] == current) {
                count++
            } else {
                result.add(RLE(current, count))
                current = row[i]
                count = 1
            }
        }

        result.add(RLE(current, count))

        return result
    }

    fun decodeRLE(data: List<RLE>): List<Int> {

        val result = mutableListOf<Int>()

        for ((value, count) in data) {
            repeat(count) {
                result.add(value)
            }
        }

        return result
    }
}