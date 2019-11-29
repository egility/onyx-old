/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.database

import com.mysql.jdbc.ConnectionImpl
import org.egility.library.general.*
import net.sourceforge.jtds.jdbc.JtdsConnection
import java.sql.*
import java.util.*

private val CONNECT_TIMEOUT = 30000
private val QUERY_TIMEOUT = 300 * 1000
private val RECHECK_TIME = (5 * 1000000000).toLong()


private var mySqlReady = false
private fun readyMySql() {
    if (!mySqlReady) {
        Class.forName("com.mysql.jdbc.Driver").newInstance()
        mySqlReady = true
    }
}

private var sqlServerReady = false
fun readySqlServer() {
    if (!sqlServerReady) {
//        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver").newInstance()
        sqlServerReady = true
    }
}

fun buildMySqlUrl(databaseHost: String, databasePort: Int, databaseName: String): String {
//    return "jdbc:mysql://%s:%d/%s".format(databaseHost, databasePort, databaseName) + "?zeroDateTimeBehavior=convertToNull&useCompression=true"
    return "jdbc:mysql://$databaseHost:$databasePort/$databaseName?zeroDateTimeBehavior=convertToNull"
}

fun buildSqlServerUrl(databaseHost: String, databasePort: Int, databaseName: String): String {
    return "jdbc:jtds:sqlserver://$databaseHost:$databasePort/$databaseName"
  //  return "jdbc:sqlserver://$databaseHost:$databasePort;databaseName=$databaseName"
}

internal data class TransactionState(var nestLevel: Int=0, var rollbackError: Throwable?=null)

class DbJdbcConnection(val dbBuilder: DbBuilder) : DbConnection() {

    init {
        when (dbBuilder.dialect) {
            DbConnection.Dialect.MYSQL -> {
                readyMySql()
            }
            DbConnection.Dialect.SQLITE -> {
                /* not supported */
            }
            DbConnection.Dialect.SQL_SERVER -> {
                readySqlServer()
            }
        }
        dialect = dbBuilder.dialect
    }

    var maxConnectionRetries = 90
    var connectionRetrySleep = 1000L
    private val lastConnectionCheck = 0
    private var _connection: Connection? = null

    private var transactionState=TransactionState()

    val isConnected: Boolean
        get()=_connection != null

    override fun createDataset(sql: String, silent: Boolean, reference: String): DbDataset {
        val queryThread = ThreadQuery(sql, silent, reference)
        val statement = queryThread.execute(QUERY_TIMEOUT)
        return DbDataset(this, statement)
    }

    override fun execute(sql: String): Boolean {
        val executeThread = ThreadExecute(sql, "n/a")
        return executeThread.execute(QUERY_TIMEOUT)
    }

    override fun execute(sql: String, vararg args: Any): Boolean {
        return execute(sql.format(*args))
    }

    override fun transaction(body: () -> Unit) {
        startTransaction()
        try {
            body()
        } catch (e: Throwable) {
            rollBack(e)
            throw Exception(e.message, e)
        }
        commit()
    }

    private fun startTransaction() {
        synchronized(transactionState) {
            if (transactionState.nestLevel==0) {
                transactionState.rollbackError=null
                execute("START TRANSACTION")
            }
            transactionState.nestLevel++
        }
    }

    private fun commit() {
        synchronized(transactionState) {
            transactionState.nestLevel--
            if (transactionState.nestLevel==0) {
                if (transactionState.rollbackError!=null) {
                    execute("ROLLBACK")
                    throw Wobbly("Attempt to commit a rolling back transaction", cause=transactionState.rollbackError)
                } else {
                    execute("COMMIT")
                }
            }
        }
    }

    private fun rollBack(e: Throwable) {
        synchronized(transactionState) {
            transactionState.nestLevel--
            if (transactionState.nestLevel==0) {
                execute("ROLLBACK")
            } else {
                transactionState.rollbackError=e
            }
        }
    }

    override fun close() {
        val connection = _connection
        if (connection != null) {
            try {
                connection.close()
            } finally {
                _connection = null
            }
        }
    }

    private open inner class ThreadJdbc : Thread() {

        var description = ""

        protected var _exception: Throwable? = null

        protected fun timeAsMilliseconds(time: Long): Double {
            return (time.toDouble()) / 1000000.0
        }

        fun checkNetwork(onConnectedListener: (String) -> Unit) {
            val now = System.nanoTime()
            if (now - lastConnectionCheck > RECHECK_TIME) {
                Global.services.checkNetwork()
            }
        }

        protected fun openConnection(attempt: Int): Connection {
            if (attempt > maxConnectionRetries) {
                throw Wobbly(Wobbly.Event.DB_CONNECTION)
            }
            checkNetwork({})
            val connection = _connection
            if (connection != null) {
                if (connection is JtdsConnection || connection.isValid(0)) {
                    return connection
                } else
                    try {
                        connection.close()
                    } catch (e: Throwable) {
                        /* ignore */
                    }
                _connection = null
            }
            val connectThread = ThreadConnect()
            try {
                connectThread.execute(QUERY_TIMEOUT)
            } catch (e: Throwable) {
                /* ignore */
            }
            debug("DbJdbcConnection", "Sleep for $connectionRetrySleep msec, attempt = $attempt")
            sleep(connectionRetrySleep)
            return openConnection(attempt + 1)
        }

        protected fun startIt(timeout: Int) {
            _exception = null
            start()
            join(timeout.toLong())
            val exception = _exception
            if (exception != null) {
                throw Wobbly("Error in JDBC thread when $description", cause = exception)
            }

            if (state != Thread.State.TERMINATED) {
                throw Wobbly("Thread timed out")
            }
        }
    }

    private inner class ThreadExecute(private var sql: String, val reference: String) : ThreadJdbc() {

        private var executeResult: Boolean = false

        fun execute(timeout: Int): Boolean {
            description = "execute: ${sql}"
            if (_connection is ConnectionImpl &&  (_connection as ConnectionImpl).activeMySQLConnection!=null) {
                debug("SQL", "Execute ${(_connection as ConnectionImpl).activeMySQLConnection.host.trim()}: ${sql.removeWhiteSpace}")
            } else {
                debug("SQL", "Execute ${reference}: %s", sql.removeWhiteSpace)
            }
            debugTime("SQL", "Execute") {
                startIt(timeout)
            }
            return executeResult
        }

        override fun run() {
            try {
                val statement = openConnection(0).createStatement()
                executeResult = statement.execute(sql)
            } catch (e: Throwable) {
                _exception = e
            }
        }
    }

    private inner class ThreadQuery(val sql: String, val silent: Boolean, val reference: String) : ThreadJdbc() {

        lateinit private var statement: Statement

        fun execute(timeout: Int): Statement {
            description = "Query: ${sql}"
            if (silent) {
                startIt(timeout)
            } else {
                if (_connection is ConnectionImpl &&  (_connection as ConnectionImpl).activeMySQLConnection!=null) {
                    debug("SQL", "Query ${(_connection as ConnectionImpl).activeMySQLConnection.host.trim()}: ${sql.removeWhiteSpace}")
                } else {
                    debug("SQL", "Query ${reference}: %s", sql.removeWhiteSpace)
                }
                debugTime("SQL", "Query") {
                    startIt(timeout)
                }
            }
            return statement
        }

        override fun run() {
            try {
                statement = openConnection(0).createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)
                statement.executeQuery(sql)
            } catch (e: Throwable) {
                _exception = e
            }
        }

    }

    private inner class ThreadConnect : ThreadJdbc() {

        fun execute(timeout: Int) {
            description = "Connect"
            url = dbBuilder.url
            debug("SQL", "Connect (START): %s", url)
            startIt(CONNECT_TIMEOUT * 10)
            debugTime("SQL", "Connect") {
                startIt(CONNECT_TIMEOUT * 10)
            }
        }

        override fun run() {
            try {
                val properties = Properties()
                if (dbBuilder.username.isNotEmpty()) {
                    properties["user"]=dbBuilder.username
                    properties["password"]=dbBuilder.password
                }
                //properties["useOldAliasMetadataBehavior"]=true
                DriverManager.setLoginTimeout(CONNECT_TIMEOUT / 1000)
                _connection = DriverManager.getConnection(url, properties)
            } catch (e: Throwable) {
                _exception = e
            }

        }


    }

}
