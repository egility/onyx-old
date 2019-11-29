/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.library.dbobject

import org.egility.library.database.*
import org.egility.library.general.Json
import org.egility.library.general.addDays
import org.egility.library.general.format
import org.egility.library.general.isSameMonth
import java.util.*

open class KcShowRaw<T: DbTable<T>>(_connection: DbConnection? = null, vararg columnNames: String) : DbTable<T>(_connection, "kcShow", *columnNames) {

    open var id: Int by DbPropertyInt("idKcShow")
    open var idKcClub: Int by DbPropertyInt("idKcClub")
    open var type: String by DbPropertyString("type")
    open var dateStart: Date by DbPropertyDate("dateStart")
    open var dateEnd: Date by DbPropertyDate("dateEnd")
    open var name: String by DbPropertyString("name")
    open var secretary: String by DbPropertyString("secretary")
    open var phone: String by DbPropertyString("phone")
    open var email: String by DbPropertyString("email")
    open var venue: String by DbPropertyString("venue")
    open var venuePostcode: String by DbPropertyString("venuePostcode")
    open var kcLicenceList: String by DbPropertyString("kcLicenceList")
    open var processorHistoric: String by DbPropertyString("processorHistoric")
    open var processorConfirmed: String by DbPropertyString("processorConfirmed")
    open var marketingPriority: Int by DbPropertyInt("marketingPriority")
    open var rings: Int by DbPropertyInt("rings")
    open var contact: String by DbPropertyString("contact")
    open var contactEmail: String by DbPropertyString("contactEmail")
    open var notes: String by DbPropertyString("notes")
    open var extra: Json by DbPropertyJson("extra")
    open var dateCreated: Date by DbPropertyDate("dateCreated")
    open var dateModified: Date by DbPropertyDate("dateModified")
    open var dateDeleted: Date by DbPropertyDate("dateDeleted")

    val geoData: GeoData by DbLink<GeoData>({ GeoData() }, keyNames = *arrayOf("venuePostcode"))


}

class KcShow(vararg columnNames: String) : KcShowRaw<KcShow>(null, *columnNames) {

    constructor(idKcShow: Int) : this() {
        find(idKcShow)
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


    override fun whenBeforePost() {
        
        if (isModified("venue")) {
            val postcode = venue.reversed().replaceFirst(" ", "_").substringBefore(" ").reversed().replace("_", " ")
            if (postcode.matches(Regex("^([A-PR-UWYZ](([0-9](([0-9]|[A-HJKSTUW])?)?)|([A-HK-Y][0-9]([0-9]|[ABEHMNPRVWXY])?)) ?[0-9][ABD-HJLNP-UW-Z]{2})\$"))) {
                venuePostcode = postcode
            }
        }
        
        
        super.whenBeforePost()
    }

    companion object {

    }
}