/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.dbobject

import org.egility.library.database.*
import org.egility.library.general.*
import java.util.*

/**
 * Created by mbrickman on 12/12/17.
 */

open class BankTransactionHSBCRaw<T: DbTable<T>>(_connection: DbConnection? = null, vararg columnNames: String) : DbTable<T>(_connection, "bankTransactionHSBC", *columnNames) {

    open var id: Int by DbPropertyInt("idBankTransaction")
    open var idLedger: Int by DbPropertyInt("idLedger")
    open var transactionDate: Date by DbPropertyDate("transactionDate")
    open var transactionType: String by DbPropertyString("transactionType")
    open var transactionReference: String by DbPropertyString("transactionReference")
    open var paidOut: Int by DbPropertyInt("paidOut")
    open var paidIn: Int by DbPropertyInt("paidIn")
    open var balance: Int by DbPropertyInt("balance")
    open var revisedReference: String by DbPropertyString("revisedReference")

}

class BankTransactionHSBC(vararg columnNames: String) : BankTransactionHSBCRaw<BankTransactionHSBC>(null, *columnNames) {

    constructor(idBankHSBC: Int) : this() {
        find(idBankHSBC)
    }

    companion object {

        fun select(where: String, orderBy: String = "", limit: Int = 0): BankTransactionHSBC {
            val bankHSBC = BankTransactionHSBC()
            bankHSBC.select(where, orderBy, limit)
            return bankHSBC
        }
        
    }
}

