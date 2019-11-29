/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.library.dbobject

import org.egility.library.database.*
import org.egility.library.general.addDays
import org.egility.library.general.format
import org.egility.library.general.isSameMonth
import java.util.*

open class AgilitynetShowRaw<T: DbTable<T>>(_connection: DbConnection? = null, vararg columnNames: String) : DbTable<T>(_connection, "z_agilitynetshow", *columnNames) {
    open var key: Int by DbPropertyInt("key")
    open var name: String by DbPropertyString("name")
    open var dateStart: Date by DbPropertyDate("dateStart")
    open var dateEnd: Date by DbPropertyDate("dateEnd")
    open var type: Int by DbPropertyInt("type")
    open var entriesTo: String by DbPropertyString("entriesTo")
    open var venuePostcode: String by DbPropertyString("venuePostcode")
    open var processor: String by DbPropertyString("processor")
    open var clubName: String by DbPropertyString("clubName")
    open var idEntity: Int by DbPropertyInt("idEntity")

    val entity: Entity by DbLink({ Entity() })
    val geoData: GeoData by DbLink<GeoData>({ GeoData() }, keyNames = *arrayOf("venuePostcode"))
}

class AgilitynetShow(vararg columnNames: String) : AgilitynetShowRaw<AgilitynetShow>(null, *columnNames) {

    constructor(idAgilitynetShow: Int) : this() {
        find(idAgilitynetShow)
    }

    val weekNumber: Int
        get() = dateStart.addDays(-2).format("w").toInt()


    val dateRange: String
        get() = if (dateEnd == dateStart) {
            dateStart.format("EEE d MMM, yyyy")
        } else if (dateEnd.isSameMonth(dateStart)) {
            dateStart.format("EEE d") + " - " + dateEnd.format("EEE d MMM, yyyy")
        } else {
            dateStart.format("EEE d MMM") + " - " + dateEnd.format("EEE d MMM, yyyy")
        }

    companion object {

    }
}