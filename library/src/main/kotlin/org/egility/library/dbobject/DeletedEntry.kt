/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.library.dbobject

import org.egility.library.database.*
import java.util.*

open class DeletedEntryRaw<T: DbTable<T>>(_connection: DbConnection? = null, vararg columnNames: String) : DbTable<T>(_connection, "deletedEntry", *columnNames) {
    open var id: Int by DbPropertyInt("idDeletedEntry")
    open var idAccount: Int by DbPropertyInt("idAccount")
    open var idCompetition: Int by DbPropertyInt("idCompetition")
    open var idAgilityClass: Int by DbPropertyInt("idAgilityClass")
    open var idTeam: Int by DbPropertyInt("idTeam")
    open var gradeCode: String by DbPropertyString("gradeCode")
    open var heightCode: String by DbPropertyString("heightCode")
    open var jumpHeightCode: String by DbPropertyString("jumpHeightCode")
    open var entryType: Int by DbPropertyInt("entryType")
    open var timeEntered: Date by DbPropertyDate("timeEntered")
    open var entryFee: Int by DbPropertyInt("entryFee")
    open var reasonDeleted: Int by DbPropertyInt("reasonDeleted")
    open var dateCreated: Date by DbPropertyDate("dateCreated")
    open var deviceCreated: Int by DbPropertyInt("deviceCreated")
    open var dateModified: Date by DbPropertyDate("dateModified")
    open var deviceModified: Int by DbPropertyInt("deviceModified")

    val team: Team by DbLink<Team>({ Team() })
    val agilityClass: AgilityClass by DbLink<AgilityClass>({ AgilityClass() })
    val account: Account by DbLink<Account>({ Account() })

}

class DeletedEntry(vararg columnNames: String) : DeletedEntryRaw<DeletedEntry>(null, *columnNames) {

    constructor(idDeletedEntry: Int) : this() {
        find(idDeletedEntry)
    }

    val jumpHeightText: String
        get() {
            var result = Height.getHeightName(jumpHeightCode)
            return result
        }


    companion object {

        fun select(where: String, orderBy: String = "", limit: Int = 0): DeletedEntry {
            val deletedEntry = DeletedEntry()
            deletedEntry.select(where, orderBy, limit)
            return deletedEntry
        }

    }
}