/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.library.dbobject

import org.egility.library.database.DbConnection
import org.egility.library.database.DbLink
import org.egility.library.database.DbPropertyInt
import org.egility.library.database.DbTable
import org.egility.library.general.append
import org.egility.library.general.dbQuery

open class CompetitionOfficialRaw<T : DbTable<T>>(_connection: DbConnection? = null, vararg columnNames: String) : DbTable<T>(_connection, "competitionOfficial", *columnNames) {

    open var idCompetition: Int by DbPropertyInt("idCompetition")
    open var idCompetitor: Int by DbPropertyInt("idCompetitor")
    open var role: Int by DbPropertyInt("role")

    val competition: Competition by DbLink<Competition>({ Competition() })
    val competitor: Competitor by DbLink<Competitor>({ Competitor() })
}

class CompetitionOfficial(vararg columnNames: String) : CompetitionOfficialRaw<CompetitionOfficial>(null, *columnNames) {

    constructor(idCompetitionOfficial: Int) : this() {
        find(idCompetitionOfficial)
    }

    companion object {
        fun isOfficial(idCompetition: Int, idCompetitor: Int): Boolean {
            var result = false
            CompetitionOfficial().seek("idCompetition=$idCompetition AND idCompetitor=$idCompetitor") {
                result = true
            }
            return result
        }

        fun isOfficial(idCompetitor: Int): Boolean {
            var result = false
            CompetitionOfficial().seek("idCompetitor=$idCompetitor") {
                result = true
            }
            return result
        }
        fun competitionList(idCompetitor: Int): String {
            var result = ""
            dbQuery("SELECT GROUP_CONCAT(idCompetition) AS list FROM competitionOfficial WHERE idCompetitor=$idCompetitor") {
                result=getString("list")
            }
            return result
        }

    }
}