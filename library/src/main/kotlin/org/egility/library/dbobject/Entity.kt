/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.library.dbobject

import org.egility.library.database.*
import org.egility.library.general.Json
import org.egility.library.general.quoted
import java.util.*

open class EntityRaw<T: DbTable<T>>(_connection: DbConnection? = null, vararg columnNames: String) : DbTable<T>(_connection, "entity", *columnNames) {

    open var id: Int by DbPropertyInt("idEntity")
    open var idKcClub: Int by DbPropertyInt("idKcClub")
    open var idProcessor: Int by DbPropertyInt("idProcessor")
    open var key: String by DbPropertyString("entityKey")
    open var name: String by DbPropertyString("name")
    open var marketingPriority: Int by DbPropertyInt("marketingPriority")
    open var secretary: String by DbPropertyString("secretary")
    open var email: String by DbPropertyString("email")
    open var phone: String by DbPropertyString("phone")
    open var venue: String by DbPropertyString("venue")
    open var streetAddress: String by DbPropertyString("streetAddress")
    open var town: String by DbPropertyString("town")
    open var regionCode: String by DbPropertyString("regionCode")
    open var countryCode: String by DbPropertyString("countryCode")
    open var postcode: String by DbPropertyString("postcode")
    open var notes: String by DbPropertyString("notes")
    open var flags: Int by DbPropertyInt("flags")
    open var kcContactDetails: String by DbPropertyString("kcContactDetails")
    open var extra: Json by DbPropertyJson("extra")
    open var dateCreated: Date by DbPropertyDate("dateCreated")
    open var dateModified: Date by DbPropertyDate("dateModified")
    open var dateDeleted: Date by DbPropertyDate("dateDeleted")


    var hasShows: Boolean by DbPropertyBit("flags", 0)
    var trainingClub: Boolean by DbPropertyBit("flags", 8)

}

class Entity(vararg columnNames: String) : EntityRaw<Entity>(null, *columnNames) {

    constructor(idEntity: Int) : this() {
        find(idEntity)
    }

    companion object {
        
        fun keyToId(key: String, default: Int=0): Int {
            var result = default
            Entity().seek("entityKey=${key.trim().quoted}") {
                result = id
            }
            return result
        }
    }
}