/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.library.dbobject

import org.egility.library.database.*
import org.egility.library.general.addHours
import org.egility.library.general.dbExecute
import org.egility.library.general.now
import org.egility.library.general.quoted
import java.util.*

open class MutexRaw<T: DbTable<T>>(_connection: DbConnection? = null, vararg columnNames: String) : DbTable<T>(_connection, "mutex", *columnNames) {
    open var idSite: Int by DbPropertyInt("idSite")
    open var keyword: String by DbPropertyString("keyword")
    open var time: Date by DbPropertyDate("time")

}

class Mutex(vararg columnNames: String) : MutexRaw<Mutex>(null, *columnNames) {

    constructor(idMutex: Int) : this() {
        find(idMutex)
    }

    companion object {

        fun select(where: String, orderBy: String = "", limit: Int = 0): Mutex {
            val mutex = Mutex()
            mutex.select(where, orderBy, limit)
            return mutex
        }

        private fun whileLocked(body: () -> Unit) {
            dbExecute("LOCK TABLES Mutex WRITE")
            try {
                body()
            } finally {
                dbExecute("UNLOCK TABLES")
            }

        }

        private fun aquire(keyword: String): Boolean {
            val idSite=control.idSite
            var result=false
            whileLocked {
                val mutex = Mutex()
                mutex.find("idSite=$idSite AND keyword=${keyword.quoted}")
                if (!mutex.found()) {
                    mutex.append()
                    mutex.idSite = idSite
                    mutex.keyword = keyword
                    mutex.post()
                    result = true
                } else if (mutex.time<now.addHours(-1)) {
                    mutex.time = now
                    mutex.post()
                    result=true
                }
            }
            return result
        }

        private fun release(keyword: String) {
            whileLocked {
                dbExecute("DELETE FROM Mutex WHERE keyword=${keyword.quoted}")
            }
        }

        fun ifAquired(keyword: String, body: () -> Unit) {
            if (aquire(keyword)) {
                try {
                    body()
                } finally {
                    release(keyword)
                }
            }
        }

    }
}