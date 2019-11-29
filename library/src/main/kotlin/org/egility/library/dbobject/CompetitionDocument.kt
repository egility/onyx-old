/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.dbobject

import jdk.nashorn.internal.objects.Global
import org.egility.library.database.*
import org.egility.library.general.prepareFile
import org.egility.library.general.quoted
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*

open class CompetitionDocumentRaw<T: DbTable<T>>(_connection: DbConnection? = null, vararg columnNames: String) : DbTable<T>(_connection, "competitionDocument", *columnNames) {

    open var id: Int by DbPropertyInt("idCompetitionDocument")
    open var idCompetition: Int by DbPropertyInt("idCompetition")
    open var documentName: String by DbPropertyString("documentName")
    open var documentPath: String by DbPropertyString("documentPath")
    open var dateCreated: Date by DbPropertyDate("dateCreated")
    open var deviceCreated: Int by DbPropertyInt("deviceCreated")
    open var dateModified: Date by DbPropertyDate("dateModified")
    open var deviceModified: Int by DbPropertyInt("deviceModified")
    open var dateDeleted: Date by DbPropertyDate("dateDeleted")
}

class CompetitionDocument(vararg columnNames: String) : CompetitionDocumentRaw<CompetitionDocument>(null, *columnNames) {

    constructor(idCompetitionDocument: String) : this() {
        find(idCompetitionDocument)
    }

    companion object {

        fun select(where: String, orderBy: String = "", limit: Int = 0): CompetitionDocument {
            val competitionDocument = CompetitionDocument()
            competitionDocument.select(where, orderBy, limit)
            return competitionDocument
        }

    }

}