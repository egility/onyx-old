/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.library.dbobject

import org.egility.library.database.*

open class BlankRaw<T: DbTable<T>>(_connection: DbConnection? = null, vararg columnNames: String) : DbTable<T>(_connection, "blank", *columnNames) {
    open var id: Int by DbPropertyInt("idBlank")

}

class Blank(vararg columnNames: String) : BlankRaw<Blank>(null, *columnNames) {

    constructor(idBlank: Int) : this() {
        find(idBlank)
    }

    companion object {

    }
}