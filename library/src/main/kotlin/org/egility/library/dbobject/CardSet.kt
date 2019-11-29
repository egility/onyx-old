/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.library.dbobject

import org.egility.library.database.*
import java.util.*

open class CardSetRaw<T: DbTable<T>>(_connection: DbConnection? = null, vararg columnNames: String) : DbTable<T>(_connection, "cardSet", *columnNames) {
    open var id: Int by DbPropertyInt("idCardSet")
    open var idCompetition: Int by DbPropertyInt("idCompetition")
    open var name: String by DbPropertyString("name")
    open var address: String by DbPropertyString("address")
    open var numberRequired: Int by DbPropertyInt("numberRequired")
    open var holder: Int by DbPropertyInt("holder")
    open var requiredBy: Date by DbPropertyDate("requiredBy")
    open var dateSent: Date by DbPropertyDate("dateSent")
    open var dateReceived: Date by DbPropertyDate("dateReceived")
    open var dateReturned: Date by DbPropertyDate("dateReturned")
    open var dateCreated: Date by DbPropertyDate("dateCreated")
    open var deviceCreated: Int by DbPropertyInt("deviceCreated")
    open var dateModified: Date by DbPropertyDate("dateModified")
    open var deviceModified: Int by DbPropertyInt("deviceModified")
}

class CardSet(vararg columnNames: String) : CardSetRaw<CardSet>(null, *columnNames) {

    constructor(idCardSet: Int) : this() {
        find(idCardSet)
    }

    companion object {

        fun select(where: String, orderBy: String = "", limit: Int = 0): CardSet {
            val cardSet = CardSet()
            cardSet.select(where, orderBy, limit)
            return cardSet
        }

    }
}