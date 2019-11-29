/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.database

import org.egility.library.general.*
import java.sql.Types

open class DbTable<T : DbTable<T>>(private val _connection: DbConnection? = null, val tableName: String, vararg columnNames: String) :
    DbDatasetCursor<T>() {

    private val selectedColumns = ArrayList<String>()

    init {
        selectColumns(*columnNames)
    }

    var label = ""
        get() = if (field == "") tableName else field

    private val linkKeys = ArrayList<String>()
    private var linkOn = ""
    private var _dataset: DbDataset? = null
    var _parent: DbTable<*>? = null

    private var type = Type.MAIN
    private var linkState = LinkState.CHECK
    val children = ArrayList<DbTable<*>>()
    private var tableAlias = ""

    private val connection: DbConnection
        get() {
            if (_connection != null) {
                return _connection
            } else {
                return Global.connection
            }
        }

    private var sql: String = ""
        set(value) {
            field = value.removeWhiteSpace
            debugInfo = "TABLE: $tableName, $field"
        }
    private var foundUsing = FoundUsing.UNDEFINED


    private enum class Type {
        MAIN, JOINED, LINKED, JOINED_PENDING
    }

    private enum class LinkState {
        GOOD, CHECK, CHECKING
    }

    private enum class FoundUsing {
        UNDEFINED, SELECT, SEEK, FIND
    }


    override fun qualify(columnName: String): String {
        if (columnName.contains(".")) {
            return columnName
        } else {
            return tableAlias + "." + columnName
        }
    }

    override var cursor: Int
        get() {
            val parent = _parent
            if (parent != null && isJoined) {
                return parent.cursor
            } else {
                return super.cursor
            }
        }
        set(value) {
            super.cursor = value
        }
    
    val hasDataset: Boolean 
        get()= _dataset!=null

    override fun _getDataset(): DbDataset? {
        val parent = _parent
        if (isDependent && parent != null && parent.isOnRow && linkState == LinkState.CHECK) {
            linkState = LinkState.CHECKING
            syncLinked()
        }
        if (parent != null && isJoined) {
            return parent._getDataset()
        } else if (isLinked && parent != null && parent.isOffRow) {
            return null
        } else {
            return _dataset
        }
    }

    fun rootTable(): DbTable<T> {
        var root = this
        while (root._parent != null) {
            root = root._parent as DbTable<T>
        }
        return root
    }

    private fun invalidateLinks() {
        for (child in children) {
            child.invalidateLinks()
        }
        linkState = LinkState.CHECK
    }

    private val syncLinkedLocked = false


    private fun getLinkKey(primaryKey: String): String {
        val index = schemaTable.primaryKeys.indexOf(primaryKey)
        var result = primaryKey
        if (index < linkKeys.size) {
            result = linkKeys[index]
        }
        return result
    }

    private fun syncLinked() {
        /* todo allow for non matching key names - eg dog has idOwner and idHandler pointing to the same table */
        if (isJoined || isJoinedPending) {
            type = Type.JOINED
            if (isInSync) {
                linkState = LinkState.GOOD
                return
            } else {
                type = Type.JOINED_PENDING
            }
        }

        if (!isInSync) {
            var whereClause = ""
            for (key in schemaTable.primaryKeys) {
                val parent = _parent ?: throw Wobbly("Attempt to syncLinked with no parent")
                val whereStatement =
                    (if (tableAlias.isNotEmpty()) tableAlias else tableName) + "." + key + "=" + parent.getVariant(
                        getLinkKey(key)
                    ).sql
                whereClause = whereClause.append(whereStatement, " AND ")
            }
            select(whereClause)
        }
        linkState = LinkState.GOOD
    }

    private val isInSync: Boolean // todo allow for non matching key names - eg dog has idOwner and idHandler pointing to the same table
        get() {
            var synced = true
            val parent = _parent

            if (linkOn.isEmpty() && parent != null) {
                for (key in schemaTable.primaryKeys) {
                    if (!parent.getVariant(getLinkKey(key)).isEqual(getVariant(key), loose = true)) {
                        synced = false
                    }
                }
            }
            return synced
        }

    private fun setDataset(dataset: DbDataset?) {
        mandate(!isJoined, "Attempt to assign dataset for a joined table ($tableName)")
        this._dataset = dataset
        if (dataset != null) {
            doWhenAfterAddColumns()
            if (isLinked) {
                first()
            } else {
                beforeFirst()
            }
            dataset.mainTableName = tableName
        }
    }

    protected fun doWhenAfterAddColumns() {
        whenAfterAddColumns()
        for (child in children) {
            if (child.isJoined) {
                child.doWhenAfterAddColumns()
            }
        }
    }

    protected open fun whenAfterAddColumns() {
        /* placeholder - do nothing */
    }

    override fun whenAfterCursor(cursor: Int) {
        for (child in children) {
            if (child.isJoined) {
                child.whenAfterCursor(cursor)
            }
        }
        invalidateLinks()
        super.whenAfterCursor(cursor)
    }

    override fun whenAfterColumnChange(qualifiedName: String, oldValue: Variant, newValue: Variant) {
        invalidateLinks()
    }

    val isMain: Boolean
        get() = _parent == null

    val isDependent: Boolean
        get() = _parent != null

    private val isJoined: Boolean
        get() = type == Type.JOINED

    private val isLinked: Boolean
        get() = type == Type.LINKED || type == Type.JOINED_PENDING

    private val isJoinedPending: Boolean
        get() = type == Type.JOINED_PENDING

    protected fun doSql(sql: String) {
        if (hasDataset() && isModified) {
            // we can only build this message if we have a dataset
            val message = "Attempt to doSql before posting updates"
            mandate(!hasDataset() || !isModified, message)
        }
        setDataset(connection.createDataset(sql, false, reference))
    }

    fun delete(reposition: Boolean = true) {
        super.delete(tableName, reposition)
    }

    open fun append(): T {
        mandate(isMain, "Attempt to Append linked or joined table")
        if (!hasDataset()) {
            select("1=0")
        }
        super.append(tableName)
        return this as T
    }

    fun select(where: String, vararg args: Any, body: ((T) -> Unit)? = null): DbTable<T> {
        select(where.format(*args), "", 0, body)
        return this
    }

    fun select(where: String, orderBy: String, vararg args: Any, body: ((T) -> Unit)? = null): DbTable<T> {
        select(where.format(*args), orderBy.format(*args), 0, body)
        return this
    }

    fun select(where: String, orderBy: String = "", limit: Int = 0, body: ((T) -> Unit)? = null): DbTable<T> {
        foundUsing = FoundUsing.UNDEFINED
        unpendJoins()

        // must generate from before select to create table aliases
        val from = generateFrom(ArrayList<String>())
        val select = generateSelect()
        var sql = "SELECT $select FROM $from"
        if (orderBy.isEmpty()) {
            sql += " WHERE $where"
        } else {
            sql += " WHERE $where ORDER BY $orderBy"
        }
        if (limit > 0) {
            sql += " LIMIT $limit"
        }
        doSql(sql)
        invalidateLinks()
        this.sql = sql
        foundUsing = FoundUsing.SELECT
        if (body != null) {
            forEach { body(it) }
            beforeFirst()
        }
        return this
    }

    fun where(where: String, vararg args: Any, body: (T.() -> Unit)? = null): DbTable<T> {
        select(where.format(*args), "", 0, body)
        return this
    }

    fun where(where: String, orderBy: String, vararg args: Any, body: (T.() -> Unit)? = null): DbTable<T> {
        select(where.format(*args), orderBy.format(*args), 0, body)
        return this
    }

    fun withPost(block: T.() -> Unit): T {
        block(this as T)
        post()
        return this as T
    }

    fun withAppendPost(block: T.() -> Unit): T {
        append()
        block(this as T)
        post()
        return this as T
    }

    fun where(where: String, orderBy: String = "", limit: Int = 0, body: (T.() -> Unit)? = null): DbTable<T> {
        foundUsing = FoundUsing.UNDEFINED
        unpendJoins()

        // must generate from before select to create table aliases
        val from = generateFrom(ArrayList<String>())
        val select = generateSelect()
        var sql = "SELECT $select FROM $from"
        if (orderBy.isEmpty()) {
            sql += " WHERE $where"
        } else {
            sql += " WHERE $where ORDER BY $orderBy"
        }
        if (limit > 0) {
            sql += " LIMIT $limit"
        }
        doSql(sql)
        invalidateLinks()
        this.sql = sql
        foundUsing = FoundUsing.SELECT
        if (body != null) {
            withEach(body)
            beforeFirst()
        }
        return this
    }


    fun join(body: T.() -> DbTable<*>): DbTable<T> {
        join((this as T).body())
        return this
    }


    private fun unpendJoins() {
        for (child in children) {
            if (child.isJoinedPending) {
                child.type = Type.JOINED
            }
            child.unpendJoins()
        }
    }

    private val schemaTable: DbSchemaTable
        get() {
            val result = connection._getTableSchema(tableName)
            if (result == null) {
                throw Wobbly("Can not get schema for table: $tableName")
            } else {
                return result
            }
        }

    fun generateFrom(asNames: ArrayList<String>): String {
        var from: String
        val parent = _parent
        if (parent != null && isJoined) {
            val parentTable = parent.tableName

            var alias = 0
            tableAlias = if (label.isNotEmpty()) label else tableName
            while (asNames.contains(tableAlias)) {
                tableAlias = "${tableName}_${++alias}"
            }
            asNames.add(tableAlias)
            var on = linkOn
            if (on.isEmpty()) {
                var onClause = DelimitedList(" AND ")
                for (key in schemaTable.primaryKeys) {
                    val condition = "%s.%s = %s.%s".format(tableAlias, key, parentTable, getLinkKey(key))
                    onClause.add(condition)
                }
                on = onClause.toString()
            }


            if (tableAlias == tableName) {
                from = "LEFT JOIN $tableName ON $on"
            } else {
                from = "LEFT JOIN $tableName AS $tableAlias ON $on"
            }
        } else {
            from = tableName
            tableAlias = tableName
        }
        for (child in children) {
            if (child.isJoined) {
                from = from + " " + child.generateFrom(asNames)
            }
        }
        return from
    }

    private fun generateSelect(): String {
        var select = CommaList()
        if (selectedColumns.size > 0) {
            for (columnName in selectedColumns) {
                select.add(tableAlias + '.' + columnName)
            }
        } else {
            select.add(tableAlias + ".*")
        }
        for (child in children) {
            if (child.isJoined) {
                select.add(child.generateSelect())
            }
        }
        return select.toString()
    }

    fun selectColumns(vararg columnNames: String) {
        selectedColumns.clear()
        for (column in columnNames) {
            selectedColumns.add(column)
        }
    }

    fun linkTo(parent: DbTable<*>, on: String, vararg keyNames: String): DbTable<T> {
        linkOn = on
        linkKeys.clear()
        linkKeys.addAll(keyNames)
        type = Type.LINKED
        this._parent = parent
        parent.children.add(this)
        setDataset(null)
        return this
    }

    fun joinToParent(vararg columnNames: String) {
        selectedColumns.clear()
        selectedColumns.addAll(columnNames)
        if (type != Type.JOINED) {
            type = Type.JOINED_PENDING
        }
    }

    fun join(vararg tables: DbTable<*>): DbTable<T> {
        for (table in tables) {
            table.joinToParent()
        }
        return this
    }

    fun find(vararg id: Int): Boolean {
        mandate(
            id.size == schemaTable.primaryKeys.size,
            "DbTable.seek: argument mismatch (%d/%d)",
            id.size,
            schemaTable.primaryKeys.size
        )
        val i = 0
        var where = DelimitedList(" AND ")
        for (key in schemaTable.primaryKeys) {
            where.add("%s.%s=%d".format(tableName, key, id[i]))
        }
        return find(where.toString())
    }

    fun find(vararg id: Variant): Boolean {
        mandate(
            id.size == schemaTable.primaryKeys.size,
            "DbTable.seek: argument mismatch (%d/%d)",
            id.size,
            schemaTable.primaryKeys.size
        )
        val i = 0
        var where = DelimitedList(" AND ")
        for (key in schemaTable.primaryKeys) {
            where.add("%s.%s=%s".format(tableName, key, id[i].sql))
        }
        return find(where.toString())
    }

    fun find(key: String, value: Int): Boolean {
        return find("%s.%s=%d".format(tableName, key, value))
    }

    fun find(where: String): Boolean {
        select(where)
        foundUsing = FoundUsing.SEEK
        if (rowCount == 1) {
            return first()
        }
        return false
    }

    fun seek(vararg id: Int, body: (T.() -> Unit)? = null): T {
        mandate(
            id.size == schemaTable.primaryKeys.size,
            "DbTable.find: argument mismatch (%d/%d)",
            id.size,
            schemaTable.primaryKeys.size
        )
        val i = 0
        val where = DelimitedList(" AND ")
        for (key in schemaTable.primaryKeys) {
            where.add("%s.%s=%d".format(tableName, key, id[i]))
        }
        return seek(where.toString(), body)
    }

    fun seek(vararg id: Variant, body: (T.() -> Unit)? = null): T {
        mandate(
            id.size == schemaTable.primaryKeys.size,
            "DbTable.find: argument mismatch (%d/%d)",
            id.size,
            schemaTable.primaryKeys.size
        )
        val i = 0
        val where = DelimitedList(" AND ")
        for (key in schemaTable.primaryKeys) {
            where.add("%s.%s=%s".format(tableName, key, id[i].sql))
        }
        return seek(where.toString(), body)
    }

    fun seek(key: String, value: Int, body: (T.() -> Unit)? = null): T {
        return seek("%s.%s=%d".format(tableName, key, value), body)
    }

    fun seek(where: String, body: (T.() -> Unit)? = null): T {
        select(where)
        foundUsing = FoundUsing.FIND
        if (first() && body != null) {
            (this as T).body()
        }
        return this as T
    }

    fun seekOrAppend(where: String, onAppend: (T.() -> Unit)? = null, body: (T.() -> Unit)? = null): T {
        select(where)
        foundUsing = FoundUsing.FIND
        if (!first()) {
            append()
            if (onAppend != null) {
                (this as T).onAppend()
            }
        }
        if (body != null) {
            (this as T).body()
        }
        return this as T
    }

    fun withAppend(body: (T.() -> Unit)): T {
        append()
        (this as T).body()
        return this as T
    }

    fun refresh() {
        if (sql != "" && !isJoined) {
            unpendJoins()
            doSql(sql)
            invalidateLinks()
            first()
        }
    }

    private fun hasJoinedChildren(): Boolean {
        for (child in children) {
            if (child.isJoined || child.isJoinedPending) {
                return true
            }
        }
        return false
    }


    protected val main: DbTable<*>
        get() {
            var main: DbTable<*> = this
            while (!main.isMain) {
                main = main._parent ?: throw Wobbly("DbTable is not main yet has no parent")
            }
            return main
        }

    protected fun doGetTableOfClass(tableClass: Class<Any>): DbDatasetCursor<*>? {
        var table: DbDatasetCursor<*>?
        if (tableClass.isInstance(this)) {
            return this
        } else {
            for (child in children) {
                table = child.doGetTableOfClass(tableClass)
                if (table != null) {
                    return table
                }
            }
        }
        return null
    }

    protected fun getTableOfClass(tableClass: Class<Any>): DbDatasetCursor<*> {
        return main.doGetTableOfClass(tableClass) ?: throw Wobbly("getTableOfClass failed")
    }

    fun release() {
        if (_dataset != null) {
            mandate(!isJoined, "Attempt to release a joined table")
            //mandate(!isModified, "Attempt to release before posting updates")
            setDataset(null)
        }
        for (child in children) {
            if (!child.isJoined) {
                child.release()
            }
        }
    }

    override fun post(ignore: Boolean) {
        if (isMain) {
            doPost(ignore)
        } else {
            val parent = _parent ?: throw Wobbly("DbTable is not main yet has no parent - when post()")
            parent.post(ignore)
        }
    }

    private fun doPost(ignore: Boolean) {
        if (!isJoined) {
            super.post(ignore)
        }
        for (child in children) {
            if (child.hasDataset && child.isOnRow) {
                child.doPost(ignore)
            }
        }
    }

    override fun undoEdits() {
        mandate(isMain, "Attempt to undo linked or joined table")
        doUndoEdits()
    }

    private fun doUndoEdits() {
        if (!isJoined && hasDataset()) {
            super.undoEdits()
        }
        for (child in children) {
            child.doUndoEdits()
        }
    }

    override val isModified: Boolean
        get() = isTableModified(tableAlias)

    fun cloneFrom(source: T, vararg exceptFields: String): T {
        val columns = source.columns
        if (columns != null) {
            for (column in columns) {
                if (!exceptFields.contains(column.columnName)) {
                    when (column.columnType) {
                        Types.DOUBLE -> setValue(column.columnName, source.getDouble(column.columnName))
                        Types.INTEGER, Types.DECIMAL -> setValue(column.columnName, source.getInt(column.columnName))
                        Types.VARCHAR, Types.LONGVARCHAR, Types.CHAR -> setValue(column.columnName, source.getString(column.columnName))
                        Types.DATE, Types.TIMESTAMP -> setValue(column.columnName, source.getDate(column.columnName))
                        Types.BOOLEAN, Types.BIT -> setValue(column.columnName, source.getBoolean(column.columnName))
                        else -> {
                            debug("cloneFrom", "${column.columnName} - unknown type ${column.columnType}")
                        }
                    }
                }
            }
        }
        return this as T
    }


}