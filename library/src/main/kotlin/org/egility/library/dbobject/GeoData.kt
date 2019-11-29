/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.library.dbobject

import org.egility.library.database.*

open class GeoDataRaw<T: DbTable<T>>(_connection: DbConnection? = null, vararg columnNames: String) : DbTable<T>(_connection, "geoData", *columnNames) {
    open var postcode: String by DbPropertyString("postcode")
    open var latitude: Double by DbPropertyDouble("latitude")
    open var longitude: Double by DbPropertyDouble("longitude")
    open var id: Int by DbPropertyInt("id")}

class GeoData(vararg columnNames: String) : GeoDataRaw<GeoData>(null, *columnNames) {

    constructor(idPostcode: Int) : this() {
        find(idPostcode)
    }

    companion object {
        
        /* IMPORT
        
            data from: https://www.freemaptools.com/download-uk-postcode-lat-lng.htm
        
            LOAD DATA INFILE '/var/lib/mysql-files/ukpostcodes.csv' 
            IGNORE INTO TABLE postcode FIELDS TERMINATED BY ',' ENCLOSED BY '"'LINES TERMINATED BY '\n' IGNORE 1 ROWS
            (`id`, `postcode`, `latitude`, `longitude`);        
        
        
         */
        
    }
}