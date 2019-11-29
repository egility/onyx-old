package org.egility.library.dbobject

import org.egility.library.database.*
import org.egility.library.general.quoted

open class CountryRaw<T: DbTable<T>>(_connection: DbConnection? = null, vararg columnNames: String) : DbTable<T>(_connection, "country", *columnNames) {
    open var code: String by DbPropertyString("countryCode")
    open var name: String by DbPropertyString("countryName")
    open var townTitle: String by DbPropertyString("townTitle")
    open var regionTitle: String by DbPropertyString("regionTitle")
    open var postcodeTitle: String by DbPropertyString("postcodeTitle")
    open var addressFormat: String by DbPropertyString("addressFormat")
    open var stripeEurope: Boolean by DbPropertyBoolean("stripeEurope")
}

class Country(vararg columnNames: String) : CountryRaw<Country>(null, *columnNames) {

    constructor(countryCode: String) : this() {
        find("countryCode = ${countryCode.quoted}")
    }

    companion object {

        private var codes = HashMap<String, String>()

        fun select(where: String, orderBy: String = "", limit: Int = 0): Country {
            val country = Country()
            country.select(where, orderBy, limit)
            return country
        }

        fun getCountryName(countryCode: String, unknown: String = "UNKNOWN"): String {
            if (codes.size == 0) {
                val country = Country.select("TRUE")
                while (country.next()) {
                    codes.put(country.code, country.name)
                }
            }
            return codes.get(countryCode) ?: unknown
        }
    }

}