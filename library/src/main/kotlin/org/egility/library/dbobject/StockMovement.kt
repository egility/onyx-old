/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.library.dbobject

import org.egility.library.database.*
import org.egility.library.general.Json
import org.egility.library.general.now
import java.util.*

open class StockMovementRaw<T: DbTable<T>>(_connection: DbConnection? = null, vararg columnNames: String) : DbTable<T>(_connection, "stockMovement", *columnNames) {

    open var id: Int by DbPropertyInt("idStockMovement")
    open var idTransaction: Int by DbPropertyInt("idTransaction")
    open var type: Int by DbPropertyInt("stockMovementType")
    open var idDevice: Int by DbPropertyInt("idDevice")
    open var time: Date by DbPropertyDate("time")
    open var locationType: Int by DbPropertyInt("locationType")
    open var idCompetitionStock: Int by DbPropertyInt("idCompetitionStock")
    open var locationText: String by DbPropertyString("locationText")
    open var comment: String by DbPropertyString("comment")
    open var extra: Json by DbPropertyJson("extra")
    
}

class StockMovement(vararg columnNames: String) : StockMovementRaw<StockMovement>(null, *columnNames) {

    constructor(idStockMovement: Int) : this() {
        find(idStockMovement)
    }

    companion object {
        
        fun add(idTransaction: Int, type: Int, idDevice: Int, locationType: Int, idCompetitionStock: Int, locationText: String, comment: String) {
            
            StockMovement().withAppendPost { 
                this.idTransaction = idTransaction
                this.type = type
                this.idDevice = idDevice
                this.locationType = locationType
                this.idCompetitionStock = idCompetitionStock
                this.locationText = locationText
                this.comment = comment
                this.time = now
            }
            
            Device().seek(idDevice) {
                this.locationType = locationType
                this.idCompetitionStock = idCompetitionStock
                this.locationText = locationText
                this.post()
            }
            
        }

    }
}