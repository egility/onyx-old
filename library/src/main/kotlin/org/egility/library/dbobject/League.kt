/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.library.dbobject

import org.egility.library.database.*

open class leagueRaw<T: DbTable<T>>(_connection: DbConnection? = null, vararg columnNames: String) : DbTable<T>(_connection, "league", *columnNames) {

    open var code: Int by DbPropertyInt("leagueCode")
    open var dateStart: String by DbPropertyString("dateStart")
    open var heightCode: String by DbPropertyString("heightCode")
    open var idTeam: Int by DbPropertyInt("idTeam")
    open var points: Int by DbPropertyInt("points")

    val team: Team by DbLink<Team>({ Team() })

}

class league(vararg columnNames: String) : leagueRaw<league>(null, *columnNames) {

    companion object {

    }
}