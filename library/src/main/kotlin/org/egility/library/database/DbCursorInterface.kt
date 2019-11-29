package org.egility.library.database

import org.egility.library.general.mandate

interface DbCursorInterface {

    val rowCount: Int
    var cursor: Int

    fun first(): Boolean {
        cursor = 0
        return isOnRow
    }

    fun last(): Boolean {
        cursor = rowCount - 1
        return isOnRow
    }

    fun next(): Boolean {
        cursor = cursor + 1
        return isOnRow
    }

    fun previous(): Boolean {
        cursor = cursor - 1
        return isOnRow
    }

    fun beforeFirst() {
        cursor = -1
    }

    fun afterLast() {
        cursor = rowCount
    }

    fun found(): Boolean {
        mandate(rowCount <= 1, "Too many rows (%d) for DbCursor.found()", rowCount)
        return rowCount == 1 && first()
    }

    val isFirst: Boolean
        get() = cursor == 0
    val isLast: Boolean
        get() = rowCount > 0 && cursor == rowCount - 1
    val isOnRow: Boolean
        get() = !isOffRow
    val isOffRow: Boolean
        get() = isBeforeFirst || isAfterLast
    val isBeforeFirst: Boolean
        get() = cursor <= -1 || rowCount == 0
    val isAfterLast: Boolean
        get() = cursor >= rowCount || rowCount == 0

    fun withPeekNext(block: () -> Unit) {
        if (!isLast) {
            next()
            block()
            previous()
        }
    }    
}