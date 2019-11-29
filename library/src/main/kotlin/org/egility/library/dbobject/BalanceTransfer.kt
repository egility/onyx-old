/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.dbobject

import org.egility.library.database.*
import org.egility.library.general.*
import java.util.*

/**
 * Created by mbrickman on 19/12/16.
 */
open class BalanceTransferRaw<T: DbTable<T>>(_connection: DbConnection? = null, vararg columnNames: String) : DbTable<T>(_connection, "balanceTransfer", *columnNames) {

    open var id: Int by DbPropertyInt("idbalanceTransfer")
    open var source: String by DbPropertyString("source")
    open var idUka: Int by DbPropertyInt("idUka")
    open var idCompetitor: Int by DbPropertyInt("idCompetitor")
    open var memberName: String by DbPropertyString("memberName")
    open var amount: Int by DbPropertyInt("amount")
    open var idAccount: Int by DbPropertyInt("idAccount")
    open var idLedger: Int by DbPropertyInt("idLedger")
    open var dateCreated: Date by DbPropertyDate("dateCreated")
    open var deviceCreated: Int by DbPropertyInt("deviceCreated")
    open var dateModified: Date by DbPropertyDate("dateModified")
    open var deviceModified: Int by DbPropertyInt("deviceModified")
    open var dateDeleted: Date by DbPropertyDate("dateDeleted")

    val competitor: Competitor by DbLink<Competitor>({ Competitor() })
    val account: Account by DbLink<Account>({ Account() })

}

class BalanceTransfer(vararg columnNames: String) : BalanceTransferRaw<BalanceTransfer>(null, *columnNames) {

    companion object {

        fun process(source: String, date: Date=today) {
            val b = BalanceTransfer()
            val account=Account()
            b.select("source=${source.quoted} AND idAccount>0 AND idLedger=0")
            while (b.next()) {
                dbTransaction{
                    account.find(b.idAccount)
                    if (account.found()) {
                        when (source) {
                            "SWAP" -> b.idLedger = Ledger.addSWAPTransfer(b.idAccount, b.dateCreated, b.amount, "${b.memberName}")
                            "UKA" -> b.idLedger = Ledger.addUKATransfer(b.idAccount, b.dateCreated, b.amount, "${b.idUka}/${b.memberName}")
                        }
                        
                        b.post()
                    }
                }
            }
        }


    }

}