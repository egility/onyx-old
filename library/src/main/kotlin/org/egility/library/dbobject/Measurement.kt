/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.library.dbobject

import org.egility.library.database.*
import org.egility.library.general.Json
import org.egility.library.general.dbExecute
import java.util.*

open class MeasurementRaw<T: DbTable<T>>(_connection: DbConnection? = null, vararg columnNames: String) : DbTable<T>(_connection, "measurement", *columnNames) {
    open var id: Int by DbPropertyInt("idMeasurement")
    open var idCompetition: Int by DbPropertyInt("idCompetition")
    open var idDog: Int by DbPropertyInt("idDog")
    open var idCompetitorMeasurer: Int by DbPropertyInt("idCompetitorMeasurer")
    open var idOrganization: Int by DbPropertyInt("idOrganization")
    open var type: Int by DbPropertyInt("type")
    open var value: Int by DbPropertyInt("value")
    open var flags: Int by DbPropertyInt("flags")
    open var extra: Json by DbPropertyJson("extra")
    open var dateCreated: Date by DbPropertyDate("dateCreated")
    open var deviceCreated: Int by DbPropertyInt("deviceCreated")
    open var dateModified: Date by DbPropertyDate("dateModified")
    open var deviceModified: Int by DbPropertyInt("deviceModified")
    open var dateDeleted: Date by DbPropertyDate("dateDeleted")

    val competition: Competition by DbLink<Competition>({ Competition() })
    val dog: Dog by DbLink<Dog>({ Dog() })

    val measurer: Competitor by DbLink<Competitor>(
        { Competitor() },
        label = "measurer",
        keyNames = *arrayOf("idCompetitorMeasurer")
    )


}

class Measurement(vararg columnNames: String) : MeasurementRaw<Measurement>(null, *columnNames) {
    
    companion object {

        fun fixIds() {
            dbExecute(
                """
                    SELECT 
                        @id:=if(MAX(idMeasurement) IS NULL, 0, MAX(idMeasurement))
                    FROM
                        measurement
                    WHERE
                        idMeasurement < ${Int.MAX_VALUE / 2};    
                """.trimIndent()
            )

            dbExecute(
                """
                    UPDATE measurement 
                    SET 
                        idMeasurement = (@id:=@id + 1)
                    WHERE
                        idMeasurement > @id AND dateCreated < CurDate() - INTERVAL 14 DAY 
                    ORDER BY dateCreated, idMeasurement; 
                """.trimIndent()
            )
        }

    }
}