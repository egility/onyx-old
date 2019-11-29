/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.dbobject

import org.egility.library.database.DbConnection
import org.egility.library.database.DbPropertyInt
import org.egility.library.database.DbPropertyString
import org.egility.library.database.DbTable
import org.egility.library.general.eq

open class OrganizationRaw<T: DbTable<T>>(_connection: DbConnection? = null, vararg columnNames: String) : DbTable<T>(_connection, "organization", *columnNames) {
    var id: Int by DbPropertyInt("idOrganization")
    var code: String by DbPropertyString("code")
    var name: String by DbPropertyString("name")
    var prefix: String by DbPropertyString("prefix")
}

data class OrganizationData(
    val id: Int,
    val code: String,
    val name: String,
    val prefix: String)

class Organization(vararg columnNames: String) : OrganizationRaw<Organization>(null, *columnNames) {
    
    companion object {
        val all =ArrayList<OrganizationData>()
        
        init {
            Organization().where("true") {
                all.add( OrganizationData(id, code, name, prefix))
            }
        }
        
        fun get(id: Int): OrganizationData {
            all.forEach { 
                if (it.id==id) return it
            }
            return OrganizationData(id, "", "", "")
        }

        fun get(code: String): OrganizationData {
            all.forEach {
                if (it.code.eq(code)) return it
            }
            return OrganizationData(-1, code, "", "")
        }
    }
}