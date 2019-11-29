/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.dbobject

import org.egility.library.database.*
import org.egility.library.general.quoted

open class RegionRaw<T: DbTable<T>>(_connection: DbConnection? = null, vararg columnNames: String) : DbTable<T>(_connection, "region", *columnNames) {

    open var code: String by DbPropertyString("regionCode")
    open var countryCode: String by DbPropertyString("countryCode")
    open var name: String by DbPropertyString("regionName")
}

class Region(vararg columnNames: String) : RegionRaw<Region>(null, *columnNames) {

    constructor(regionCode: String) : this() {
        find("regionCode = ${regionCode.quoted}")
    }

    companion object {

        private var codes = HashMap<String, String>()

        fun select(where: String, orderBy: String = "", limit: Int = 0): Region {
            val region = Region()
            region.select(where, orderBy, limit)
            return region
        }

        fun getRegionName(regionCode: String, unknown: String = "UNKNOWN"): String {
            if (codes.size == 0) {
                val region = Region.select("TRUE")
                while (region.next()) {
                    codes.put(region.code, region.name)
                }
            }
            return codes.get(regionCode) ?: unknown
        }
    }

}