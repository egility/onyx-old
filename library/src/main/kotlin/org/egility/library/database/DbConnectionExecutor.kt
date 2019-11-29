/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.database

import org.egility.library.general.DbBuilder
import org.egility.library.general.debug
import java.io.Closeable
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Created by mbrickman on 01/12/16.
 */

class DbConnectionExecutor(val builder: DbBuilder, threadPoolSize: Int) : ThreadPoolExecutor(threadPoolSize,
        Int.MAX_VALUE,
        60L,
        TimeUnit.SECONDS,
        SynchronousQueue<Runnable>(),
        DbConnectionThreadFactory(builder))

class DbConnectionThread(val connection: DbJdbcConnection, runnable: Runnable) : Thread(runnable) {

    override fun run() {
        try {
            super.run()
        } finally {
            debug("DbConnectionThread", "Closing thread")
            connection.close()
        }
    }

}

class DbConnectionThreadFactory(val builder: DbBuilder) : ThreadFactory {

    override fun newThread(runnable: Runnable): Thread {
        val thread = DbConnectionThread(DbJdbcConnection(builder), runnable)
        debug("DbConnectionThreadFactory", "newThread = ${thread.id}")
        return thread
    }

}