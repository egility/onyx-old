/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.library.dbobject

import org.egility.library.database.*
import org.egility.library.general.Json
import java.util.*

open class CompetitionExtraRaw<T: DbTable<T>>(_connection: DbConnection? = null, vararg columnNames: String) : DbTable<T>(_connection, "competitionExtra", *columnNames) {
    open var id: Int by DbPropertyInt("idCompetitionExtra")
    open var idCompetition: Int by DbPropertyInt("idCompetition")
    open var ledgerItemType: Int by DbPropertyInt("ledgerItemType")
    open var description: String by DbPropertyString("description")
    open var sizes: String by DbPropertyString("sizes")
    open var unitPrice: Int by DbPropertyInt("unitPrice")
    open var needsQuantity: Boolean by DbPropertyBoolean("needsQuantity")
    open var perDog: Boolean by DbPropertyBoolean("perDog")
    open var extra: Json by DbPropertyJson("extra")
    open var dateCreated: Date by DbPropertyDate("dateCreated")
    open var deviceCreated: Int by DbPropertyInt("deviceCreated")
    open var dateModified: Date by DbPropertyDate("dateModified")
    open var deviceModified: Int by DbPropertyInt("deviceModified")
    open var dateDeleted: Date by DbPropertyDate("dateDeleted")
}

class CompetitionExtra(vararg columnNames: String) : CompetitionExtraRaw<CompetitionExtra>(null, *columnNames) {
    
    constructor(idCompetitionExtra: Int) : this() {
        find(idCompetitionExtra)
    }

    companion object {

    }
}