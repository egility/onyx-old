/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.dbobject

import org.egility.library.database.*
import org.egility.library.general.Global
import org.egility.library.general.Wobbly
import org.egility.library.general.dbExecute

import java.io.PrintWriter
import java.io.StringWriter
import java.util.*

/**
 * Created by mbrickman on 03/10/15.
 */
open class PanicRaw<T: DbTable<T>>(_connection: DbConnection? = null, vararg columnNames: String) : DbTable<T>(_connection, "panic", *columnNames) {

    var id: Int by DbPropertyInt("idPanic")
    var idDevice: Int by DbPropertyInt("idDevice")
    var panicTime: Date by DbPropertyDate("panicTime")
    var panicClass: String by DbPropertyString("panicClass")
    var message: String by DbPropertyString("message")
    var stack: String by DbPropertyString("stack")
    var log: String by DbPropertyString("log")
    var version: String by DbPropertyString("version")
    var dateCreated: Date by DbPropertyDate("dateCreated")
    var deviceCreated: Int by DbPropertyInt("deviceCreated")
    var dateModified: Date by DbPropertyDate("dateModified")
    var deviceModified: Int by DbPropertyInt("deviceModified")

}

class Panic(vararg columnNames: String) : PanicRaw<Panic>(null, *columnNames) {
    companion object {
        fun fixIds() {
            dbExecute(
                """
                    SELECT 
                        @id:=if(MAX(idPanic) IS NULL, 0, MAX(idPanic))
                    FROM
                        panic
                    WHERE
                        idPanic < ${Int.MAX_VALUE / 2};    
                """.trimIndent()
            )

            dbExecute(
                """
                    UPDATE panic 
                    SET 
                        idPanic = (@id:=@id + 1)
                    WHERE
                        idPanic > @id ORDER BY dateCreated, idPanic; 
                """.trimIndent()
            )
        }
    }
}


fun logPanic(panicTime: Date, panicClass: String, message: String, stack: String, log: String) {
    val panic = Panic()
    panic.append()
    panic.idDevice = Device.thisDevice.id
    panic.panicTime = panicTime
    panic.panicClass = panicClass
    panic.message = message
    panic.stack = stack
    panic.log = log
    panic.version = Global.version
    panic.post()
}