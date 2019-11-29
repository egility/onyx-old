/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.library.dbobject

import org.egility.library.database.*
import org.egility.library.general.Json
import org.egility.library.general.dbExecute
import java.util.*

open class RadioRaw<T: DbTable<T>>(_connection: DbConnection? = null, vararg columnNames: String) : DbTable<T>(_connection, "radio", *columnNames) {

    open var id: Int by DbPropertyInt("idRadio")
    open var idCompetition: Int by DbPropertyInt("idCompetition")
    open var ringNumber: Int by DbPropertyInt("ringNumber")
    open var idAgilityClass: Int by DbPropertyInt("idAgilityClass")
    open var messageTemplate: Int by DbPropertyInt("messageTemplate")
    open var heightCode: String by DbPropertyString("heightCode")
    open var heightText: String by DbPropertyString("heightText")
    open var dogs: Int by DbPropertyInt("dogs")
    open var callingTo: Int by DbPropertyInt("callingTo")
    open var atTime: Date by DbPropertyDate("atTime")
    open var resumeTime: Date by DbPropertyDate("resumeTime")
    open var inMinutes: Int by DbPropertyInt("inMinutes")
    open var fullText: String by DbPropertyString("fullText")
    open var timeAnnounced: Date by DbPropertyDate("timeAnnounced")
    open var extra: Json by DbPropertyJson("extra")
    open var dateCreated: Date by DbPropertyDate("dateCreated")
    open var deviceCreated: Int by DbPropertyInt("deviceCreated")
    open var dateModified: Date by DbPropertyDate("dateModified")
    open var deviceModified: Int by DbPropertyInt("deviceModified")

    val competition: Competition by DbLink<Competition>({ Competition() })
    val agilityClass: AgilityClass by DbLink<AgilityClass>({ AgilityClass() })

}

class Radio(vararg columnNames: String) : RadioRaw<Radio>(null, *columnNames) {

    constructor(idRadio: Int) : this() {
        find(idRadio)
    }

    companion object {

        fun fixIds() {
            dbExecute(
                """
                SELECT 
                    @id:=if(MAX(idRadio) IS NULL, 0, MAX(idRadio))
                FROM
                    radio
                WHERE
                    idRadio < ${Int.MAX_VALUE / 2};    
            """.trimIndent()
            )

            dbExecute(
                """
                UPDATE radio 
                SET 
                    idRadio = (@id:=@id + 1)
                WHERE
                    idRadio > @id AND dateCreated < CurDate() - INTERVAL 1 DAY 
                ORDER BY dateCreated, idRadio; 
            """.trimIndent()
            )
        }


    }
}

