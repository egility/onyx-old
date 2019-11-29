/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.dbobject

import org.egility.library.database.*
import org.egility.library.general.*
import java.util.*

data class RingSelector(var idCompetition: Int, var date: Date, var ringNumber: Int)

open class RingRaw<T: DbTable<T>>(_connection: DbConnection? = null, vararg columnNames: String) : DbTable<T>(_connection, "ring", *columnNames) {
    open var idCompetition: Int by DbPropertyInt("idCompetition")
    open var date: Date by DbPropertyDate("date")
    open var number: Int by DbPropertyInt("ringNumber")
    open var idAgilityClass: Int by DbPropertyInt("idAgilityClass")
    open var heightCode: String by DbPropertyString("heightCode")
    open var group: String by DbPropertyString("group")
    open var judge: String by DbPropertyString("judge")
    open var manager: String by DbPropertyString("ringManager")
    open var helpers: String by DbPropertyString("helpers")
    open var note: String by DbPropertyString("note")
    open var idEntry: Int by DbPropertyInt("idEntry")
    open var runningOrder: Int by DbPropertyInt("runningOrder")
    open var runner: String by DbPropertyString("runner")
    open var runnerHeightCode: String by DbPropertyString("runnerHeightCode")
    open var lunchStart: Date by DbPropertyDate("lunchStart")
    open var lunchEnd: Date by DbPropertyDate("lunchEnd")
    open var notBreaking: Boolean by DbPropertyBoolean("notBreaking")
    open var flag: Boolean by DbPropertyBoolean("flag")
    open var extra: String by DbPropertyString("extra")
    
    open var dateCreated: Date by DbPropertyDate("dateCreated")
    open var deviceCreated: Int by DbPropertyInt("deviceCreated")
    open var dateModified: Date by DbPropertyDate("dateModified")
    open var deviceModified: Int by DbPropertyInt("deviceModified")
    open var paperBased: Boolean by DbPropertyBoolean("paperBased")

    var heightBase: String by DbPropertyJsonString("extra", "heightBase")

    val competition: Competition by DbLink<Competition>({ Competition() })
    val agilityClass: AgilityClass by DbLink<AgilityClass>({ AgilityClass() })

    val height: Height by DbLink<Height>({ Height() })
}

class Ring(vararg columnNames: String) : RingRaw<Ring>(null, *columnNames) {

    val selector: RingSelector
        get() = RingSelector(idCompetition, date, number)

    val agilityClasses: AgilityClass
        get() {
            val agilityClass = AgilityClass()
            agilityClass.grade.joinToParent()
            if (isOnRow) {
                val where = "AgilityClass.idCompetition=%d AND AgilityClass.classDate=%s AND AgilityClass.ringNumber=%d"
                agilityClass.select(where, "ringOrder", idCompetition, date.sqlDate, number)
            }
            return agilityClass
        }

    fun seek(idCompetition: Int, date: Date, ringNumber: Int): Boolean {
        return find("ring.idCompetition=$idCompetition AND ring.date=${date.sqlDate} AND ring.ringNumber=$ringNumber")
    }

    fun select(idCompetition: Int, date: Date, ringNumber: Int) {
        val sql: String
        val query: DbQuery

        val where = "ring.idCompetition=$idCompetition AND ring.date=${date.sqlDate} AND ring.ringNumber=$ringNumber"
        if (!find(where)) {
            // check ring number is applicable and add ring entry
            sql = "SELECT ringNumber FROM agilityClass WHERE idCompetition=%d AND classDate=%s AND ringNumber>0 ORDER BY if(ringNumber=%d, 0, ringNumber) LIMIT 1"
            query = DbQuery(sql, idCompetition, date.sqlDate, ringNumber)
            if (query.found()) {
                if (query.getInt("ringNumber") != ringNumber) {
                    select(idCompetition, date, query.getInt("ringNumber"))
                    return
                }
                append()
                this.idCompetition = idCompetition
                this.date = date
                this.number = query.getInt("ringNumber")
                this.note = ""
                post()
            } else {
                throw Wobbly("There are no classes today")
            }
        }
        if (idAgilityClass == 0) {
            selectFirstOpenAgilityClass()
        }
    }

    fun getActiveClassId(): Int {
        var sql = """
            SELECT
                idAgilityClass
            FROM
                agilityClass
            WHERE
                AgilityClass.idCompetition=$idCompetition AND
                AgilityClass.classDate=${date.sqlDate} AND
                AgilityClass.ringNumber=$number AND
                classProgress<$CLASS_CLOSED
            ORDER BY
                ringOrder
            LIMIT 1
          """
        var query = DbQuery(sql)

        if (!query.found()) {
            sql = """
                SELECT
                    idAgilityClass
                FROM
                    agilityClass
                WHERE
                    AgilityClass.idCompetition=$idCompetition AND
                    AgilityClass.classDate=${date.sqlDate} AND
                    AgilityClass.ringNumber=$number
                ORDER BY
                    ringOrder DESC
                LIMIT 1
            """
            query = DbQuery(sql)
        }

        if (query.found()) {
            return query.getInt("idAgilityClass")
        }
        return -1
    }

    fun selectFirstOpenAgilityClass() {
        val idAgilityClass = getActiveClassId()
        if (idAgilityClass > 0) {
            chooseAgilityClass(idAgilityClass)
        }
    }

    fun checkActiveClass() {
        val idAgilityClass = getActiveClassId()
        if (idAgilityClass > 0 && idAgilityClass != this.idAgilityClass) {
            this.idAgilityClass = idAgilityClass
            val heights = agilityClass.heightRunningOrder.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (heights.size > 0) {
                heightCode = heights[0]
            }
            post()
        }
    }

    fun chooseAgilityClass(proposedIdAgilityClass: Int, reOpen: Boolean=false) {
        if (idAgilityClass != proposedIdAgilityClass) {
            if (idAgilityClass > 0) {
                agilityClass.refresh()
                if (agilityClass.progress < CLASS_CLOSED) {
                    agilityClass.progress = CLASS_PENDING
                    post()
                }
            }
            idAgilityClass = proposedIdAgilityClass
            agilityClass.refresh()
            val heights = agilityClass.heightRunningOrder.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (heights.size > 0) {
                heightCode = heights[0]
            }
        }

        if (agilityClass.progress == CLASS_PENDING && !agilityClass.isLocked) {
            agilityClass.progress = CLASS_PREPARING
        } else if (agilityClass.progress == CLASS_CLOSED && reOpen) {
            agilityClass.progress = CLASS_PREPARING
        }
        post()
    }

    fun noDogsRunYet(): Boolean {
        val sql = "SELECT true AS running from entry where idAgilityClass=$idAgilityClass AND progress in ($PROGRESS_RUNNING, $PROGRESS_RUN) limit 1"
        val query = DbQuery(sql)
        return !query.found()
    }

    val title: String
        get() {
            if (isOnRow) {
                return "Ring %d (%s)".format(number, date.dayName())
            } else {
                return "TBA"
            }
        }

    val classText: String
        get() {
            return if (isOnRow) {
                if (group.isNotEmpty())
                    "${agilityClass.name} ($group) - ${agilityClass.progressTextExtended}"
                else
                    "${agilityClass.name} - ${agilityClass.progressTextExtended}"

            } else {
                "TBA"
            }
        }

    companion object {

        fun select(where: String, orderBy: String = "", limit: Int = 0): Ring {
            val ring=Ring()
            ring.select(where, orderBy, limit)
            return ring
        }



        fun update() {
            // fill in gaps in ring table
            Global.connection.execute("""
            INSERT IGNORE INTO
                ring (idCompetition, date, ringNumber)
            SELECT DISTINCT
                idCompetition, classDate AS date, ringNumber
            FROM
                agilityclass
            WHERE
                ringNumber > 0
            GROUP BY idCompetition , classDate , ringNumber
        """)

            // fill add first class to ring entries
            Global.connection.execute("""
            UPDATE ring
                    JOIN
                (SELECT
                    idCompetition,
                        classDate AS date,
                        ringNumber,
                        idAgilityClass,
                        SUBSTRING_INDEX(heightRunningOrder, ',', 1) AS heightCode
                FROM
                    agilityClass
                WHERE
                    ringOrder = 1) AS firstClass USING (idCompetition , date , ringNumber)
            SET
                ring.idAgilityClass = firstClass.idAgilityClass,
                ring.heightCode = firstClass.heightCode
            WHERE
                ring.date > curdate()
        """)

            // initialize start time of first class
            Global.connection.execute("""
            UPDATE ring
                    JOIN
                agilityClass USING (idAgilityClass)
            SET
                startTime = DATE_ADD(classDate,
                    INTERVAL '8:30' HOUR_MINUTE)
            WHERE
                startTime = 0 AND classProgress = 0
        """)
        }
    }


}