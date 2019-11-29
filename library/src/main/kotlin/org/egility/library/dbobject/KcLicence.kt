/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.library.dbobject

import org.egility.library.database.*
import java.util.*

open class KcLicenceRaw<T: DbTable<T>>(_connection: DbConnection? = null, vararg columnNames: String) : DbTable<T>(_connection, "kcLicence", *columnNames) {
    open var id: Int by DbPropertyInt("idKcLicence")
    open var idKcClub: Int by DbPropertyInt("idKcClub")
    open var type: String by DbPropertyString("type")
    open var date: Date by DbPropertyDate("date")
    open var club: String by DbPropertyString("club")
    open var secretary: String by DbPropertyString("secretary")
    open var phone: String by DbPropertyString("phone")
    open var venue: String by DbPropertyString("venue")
    open var dateCreated: Date by DbPropertyDate("dateCreated")
    open var dateModified: Date by DbPropertyDate("dateModified")
    open var dateDeleted: Date by DbPropertyDate("dateDeleted")
}

class KcLicence(vararg columnNames: String) : KcLicenceRaw<KcLicence>(null, *columnNames) {

    constructor(idKcLicence: Int) : this() {
        find(idKcLicence)
    }

    companion object {

    }
}