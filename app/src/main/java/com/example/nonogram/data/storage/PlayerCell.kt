package com.example.nonogram.data.storage

data class PlayerCell(
    var color: Int? = null,                // выбранный цвет (индекс)
    val notes: MutableSet<Int> = mutableSetOf() // эскизы (индексы цветов)
)
