/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.library.dbobject

import org.egility.library.database.DbConnection
import org.egility.library.database.DbPropertyInt
import org.egility.library.database.DbPropertyString
import org.egility.library.database.DbTable
import org.egility.library.general.eq

open class LedgerAccountRaw<T: DbTable<T>>(_connection: DbConnection? = null, vararg columnNames: String) : DbTable<T>(_connection, "ledgerAccount", *columnNames) {

    open var id: Int by DbPropertyInt("idLedgerAccount")
    open var textCode: String by DbPropertyString("textCode")
    open var description: String by DbPropertyString("description")
    open var type: Int by DbPropertyInt("type")

}

class LedgerAccount(vararg columnNames: String) : LedgerAccountRaw<LedgerAccount>(null, *columnNames) {

    constructor(idLedgerAccount: Int) : this() {
        find(idLedgerAccount)
    }

    companion object {

        fun select(where: String, orderBy: String = "", limit: Int = 0): LedgerAccount {
            val ledgerAccount = LedgerAccount()
            ledgerAccount.select(where, orderBy, limit)
            return ledgerAccount
        }

        private var _allAccounts: LedgerAccount? = null
        private val allAccounts: LedgerAccount
            get() {
                val result = _allAccounts
                if (result == null) {
                    val ledgerAccount= select("TRUE")
                    _allAccounts = ledgerAccount
                    return ledgerAccount
                } else {
                    return result
                }
            }

        fun get(code: String): Int {
            allAccounts.beforeFirst()
            while (allAccounts.next()) {
                if (allAccounts.textCode eq code) {
                    return allAccounts.id
                }
            }
            return -1
        }
        fun get(idLedgerAccount: Int): String {
            allAccounts.beforeFirst()
            while (allAccounts.next()) {
                if (allAccounts.id === idLedgerAccount) {
                    return allAccounts.textCode
                }
            }
            return "n/a"
        }


    }
}