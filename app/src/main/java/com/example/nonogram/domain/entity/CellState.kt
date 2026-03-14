package com.example.nonogram.domain.entity

/**
 * Reprezentuje stav jedné buňky na hrací desce Nonogramu.
 * Každá buňka může být v jednom ze tří stavů:
 * - EMPTY: Buňka je aktuálně prázdná.
 * - FILLED: Buňka byla hráčem vyplněna.
 * - MARKED: Buňka byla hráčem označena křížkem (X), což znamená, že se předpokládá, že je prázdná.
 */
enum class CellState {
    EMPTY,
    FILLED,
    MARKED
}