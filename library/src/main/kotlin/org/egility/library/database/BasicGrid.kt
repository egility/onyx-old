/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.database

import org.egility.library.general.Variant
import org.egility.library.general.Wobbly
import org.egility.library.general.mandate
import java.util.*

open class BasicGrid {

    var columnCount = 0

    private val rows = ArrayList<Row>()

    private fun getCell(rowIndex: Int, columnIndex: Int, populate: Boolean): Variant {
        val row = getRow(rowIndex, populate)
        if (populate && columnIndex + 1 > columnCount) {
            columnCount = columnIndex + 1
        }
        if (!populate && columnIndex >= row.size) {
            return Variant.nullValue()
        }
        while (row.size < columnIndex + 1) {
            row.add(Variant())
        }
        return row[columnIndex]
    }

    private fun getRow(rowIndex: Int, populate: Boolean): Row {
        if (!populate && rowIndex >= rows.size) {
            throw Wobbly("Illegal attempt to extend rows in grid")
        }
        while (rows.size < rowIndex + 1) {
            val row = Row()
            rows.add(row)
        }
        return rows[rowIndex]
    }

    fun deleteRow(rowIndex: Int) {
        if (rowIndex < rows.size) {
            rows.removeAt(rowIndex)
        }
    }

    fun getValue(rowIndex: Int, columnIndex: Int): Variant {
        return getCell(rowIndex, columnIndex, true)
    }

    fun setValue(rowIndex: Int, columnIndex: Int, value: Variant) {
        getCell(rowIndex, columnIndex, true).set(value)
    }

    fun isModified(rowIndex: Int, columnIndex: Int): Boolean {
        val cell = getCell(rowIndex, columnIndex, false)
        return cell.isModified
    }

    fun checkPoint() {
        for (row in rows) {
            row.checkPoint()
        }
    }

    fun checkPoint(rowIndex: Int) {
        val row = getRow(rowIndex, false)
        row.checkPoint()
    }

    fun posted(rowIndex: Int) {
        val row = getRow(rowIndex, false)
        row.post()
    }

    fun cancel(rowIndex: Int): Int {
        val row = getRow(rowIndex, false)
        if (row.isAppending) {
            rows.remove(row)
            return -1
        } else {
            row.cancel()
            return rowIndex
        }
    }

    fun append(): Int {
        val row = Row()
        row.append()
        rows.add(row)
        return rows.size - 1
    }

    fun isAppending(rowIndex: Int): Boolean {
        val row = getRow(rowIndex, false)
        return row.isAppending
    }

    fun isPosted(rowIndex: Int): Boolean {
        val row = getRow(rowIndex, false)
        return row.isPosted
    }

    fun addColumn(): Int {
        return columnCount++
    }

    private inner class Row : ArrayList<Variant>() {

        var isAppending = false
        var isPosted = false

        val isModified: Boolean
            get() {
                for (cell in this) {
                    if (cell.isModified) {
                        return true
                    }
                }
                return false
            }

        fun checkPoint() {
            for (cell in this) {
                cell.checkPoint()
            }
        }

        fun post() {
            if (isModified) {
                isPosted = true
            }
            checkPoint()
            isAppending = false
        }

        fun cancel() {
            for (cell in this) {
                cell.rollBack()
            }
        }

        fun append() {
            isAppending = true
        }
    }
}
