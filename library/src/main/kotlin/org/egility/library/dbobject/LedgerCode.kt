/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.library.dbobject

import org.egility.library.database.DbConnection
import org.egility.library.database.DbPropertyInt
import org.egility.library.database.DbPropertyString
import org.egility.library.database.DbTable
import org.egility.library.general.eq

open class LedgerCodeRaw<T: DbTable<T>>(_connection: DbConnection? = null, vararg columnNames: String) : DbTable<T>(_connection, "ledgerCode", *columnNames) {

    open var id: Int by DbPropertyInt("idLedgerCode")
    open var textCode: String by DbPropertyString("textCode")
    open var description: String by DbPropertyString("description")
    open var type: Int by DbPropertyInt("type")

}

class LedgerCode(vararg columnNames: String) : LedgerCodeRaw<LedgerCode>(null, *columnNames) {

    constructor(idLedgerCode: Int) : this() {
        find(idLedgerCode)
    }

    companion object {

        fun select(where: String, orderBy: String = "", limit: Int = 0): LedgerCode {
            val ledgerCode = LedgerCode()
            ledgerCode.select(where, orderBy, limit)
            return ledgerCode
        }

        private var _allCodes: LedgerCode? = null
        private val allCodes: LedgerCode
            get() {
                val result = _allCodes
                if (result == null) {
                    val ledgerCode= select("TRUE")
                    _allCodes = ledgerCode
                    return ledgerCode
                } else {
                    return result
                }
            }

        fun get(code: String): Int {
            allCodes.beforeFirst()
            while (allCodes.next()) {
                if (allCodes.textCode eq code) {
                    return allCodes.id
                }
            }
            return -1
        }

        fun describe(idLedgerCode: Int): String {
            allCodes.beforeFirst()
            while (allCodes.next()) {
                if (allCodes.id==idLedgerCode) {
                    return allCodes.description
                }
            }
            return "($idLedgerCode)"
        }

    }
}