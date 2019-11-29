/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.library.dbobject

import org.egility.library.database.*
import org.egility.library.general.Json
import org.egility.library.general.append
import java.util.*

open class EntityOfficialRaw<T: DbTable<T>>(_connection: DbConnection? = null, vararg columnNames: String) : DbTable<T>(_connection, "entityOfficial", *columnNames) {
    open var id: Int by DbPropertyInt("idEntityOfficial")
    open var idEntity: Int by DbPropertyInt("idEntity")
    open var idCompetitor: Int by DbPropertyInt("idCompetitor")
    open var role: String by DbPropertyString("role")
    open var name: String by DbPropertyString("name")
    open var salutation: String by DbPropertyString("salutation")
    open var phone: String by DbPropertyString("phone")
    open var email: String by DbPropertyString("email")
    open var notes: String by DbPropertyString("notes")
    open var extra: Json by DbPropertyJson("extra")
    open var dateCreated: Date by DbPropertyDate("dateCreated")
    open var dateModified: Date by DbPropertyDate("dateModified")
    open var dateDeleted: Date by DbPropertyDate("dateDeleted")

    val competitor: Competitor by DbLink({ Competitor() })
    val entity: Entity by DbLink({ Entity() })

}

class EntityOfficial(vararg columnNames: String) : EntityOfficialRaw<EntityOfficial>(null, *columnNames) {

    constructor(idEntityOfficial: Int) : this() {
        find(idEntityOfficial)
    }

    fun updateFromCompetitor() {
        if (competitor.id>0) {
            name = competitor.fullName
            phone = competitor.phoneMobile.append(competitor.phoneOther, "/")
            email = competitor.email
            post()
        }

    }

    companion object {
        

    }
}