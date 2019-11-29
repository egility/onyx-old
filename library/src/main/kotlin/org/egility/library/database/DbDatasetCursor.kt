/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.database

import org.egility.library.general.Variant
import org.egility.library.general.Wobbly
import org.egility.library.general.debug
import java.util.*

abstract class DbDatasetCursor<out T : DbDatasetCursor<T>>() : DbCursorInterface, DbValueInterface {

    var debugInfo = "n/a"
    var reference = ""

    val columns: ArrayList<DbColumn>?
        get() = _getDataset()?.columns

    protected abstract fun _getDataset(): DbDataset?

    protected open fun qualify(columnName: String): String {
        return columnName
    }

    override fun isColumn(columnName: String): Boolean {
        val dataset = _getDataset() ?: throw Wobbly("Attempt to test isColumn without a dataset")
        return dataset.isColumn(qualify(columnName))
    }

    override fun setVariant(columnName: String, value: Variant) {
        val dataset = _getDataset() ?: throw Wobbly("Attempt to setValue without a dataset")
        val qualifiedName = qualify(columnName)
        val oldValue = dataset.getValue(qualifiedName)
        if (!value.isEqual(oldValue)) {
            dataset.setValue(qualifiedName, value)
            whenAfterColumnChange(qualifiedName, oldValue, value)
        }
    }

    override fun getVariant(columnName: String): Variant {
        val dataset = _getDataset()
        if (dataset != null && isOnRow) {
            return dataset.getValue(qualify(columnName))
        } else {
            return Variant.nullValue()
        }
    }

    override val rowCount: Int
        get() = _getDataset()?.rowCount ?: 0

    override var cursor: Int
        get() = _getDataset()?.rowIndex ?: -1
        set(value) {
            val dataset = _getDataset()
            if (dataset != null) {
                val old=dataset.rowIndex
                dataset.rowIndex = value
                if (dataset.rowIndex!=old) {
                    whenAfterCursor(dataset.rowIndex)
                }
            }
        }

    open fun whenAfterCursor(cursor: Int) {
        /* do nothing */
    }

    protected open fun whenBeforePost() {
        /* do nothing */
    }

    protected open fun whenAfterColumnChange(qualifiedName: String, oldValue: Variant, newValue: Variant) {
        /* do nothing */
    }

    protected fun hasDataset(): Boolean {
        return _getDataset() != null
    }

    open fun post(ignore: Boolean = false) {
        val dataset = _getDataset() ?: throw Wobbly("Attempt to Post without a dataset")
        whenBeforePost()
        dataset.post(ignore)
    }

    protected fun append(tableName: String): T {
        val dataset = _getDataset() ?: throw Wobbly("Attempt to append without a dataset")
        cursor = dataset.append(tableName)
        return this as T
    }

    protected fun delete(tableName: String, reposition: Boolean) {
        val dataset = _getDataset() ?: throw Wobbly("Attempt to delete without a dataset")
        val proposedCursor = dataset.delete(tableName)
        if (reposition) cursor = proposedCursor else previous()
    }

    open fun undoEdits() {
        val dataset = _getDataset() ?: throw Wobbly("Attempt to cancel without a dataset")
        dataset.cancel()
    }

    val isAppending: Boolean
        get() {
            val dataset = _getDataset()
            if (dataset != null && isOnRow) {
                return dataset.isAppending
            } else {
                return false
            }
        }

    open val isModified: Boolean
        get() {
            val dataset = _getDataset()
            if (dataset != null && isOnRow) {
                return dataset.isModified()
            } else {
                return false
            }
        }

    fun isModified(columnName: String): Boolean {
        val dataset = _getDataset()
        if (dataset != null && isOnRow) {
            return dataset.isModified(qualify(columnName))
        } else {
            return false
        }
    }

    fun isTableModified(tableAlias: String): Boolean {
        val dataset = _getDataset()
        if (dataset != null && isOnRow) {
            return dataset.isTableModified(tableAlias)
        } else {
            return false
        }
    }

    fun columnsAccessed(): String {
        return _getDataset()?.columnsAccessed() ?: ""
    }

    fun logUsage() {
        debug("datasetCursor", "finalize ($reference): ${columnsAccessed()} ($debugInfo)")
    }

    fun forEach(body: (T) -> Unit): DbDatasetCursor<T> {
        this.beforeFirst()
        while (this.next()) {
            body(this as T)
        }
        return this
    }

    fun withFirst(body: (T) -> Unit): DbDatasetCursor<T> {
        if (first()) {
            body(this as T)
        }
        return this
    }

    fun withEach(body: T.() -> Unit): DbDatasetCursor<T> {
        val position = cursor
        beforeFirst()
        while (this.next()) {
            (this as T).body()
        }
        cursor = position
        return this
    }

    fun otherwise(body: T.() -> Unit): DbDatasetCursor<T> {
        if (!found()) {
            (this as T).body()
        }
        return this
    }

    fun eitherway(body: T.() -> Unit): DbDatasetCursor<T> {
        if (isAppending || isOnRow) {
            (this as T).body()
        }
        return this
    }

}