/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.library.dbobject

import org.egility.library.database.*
import org.egility.library.general.now
import org.egility.library.general.quoted
import java.util.*
import kotlin.collections.HashMap

/**
 * Created by mbrickman on 15/02/18.
 */


open class EmailQueueRaw<T: DbTable<T>>(_connection: DbConnection? = null, vararg columnNames: String) : DbTable<T>(_connection, "emailQueue", *columnNames) {

    open var id: Int by DbPropertyInt("idEmailQueue")
    open var idAccount: Int by DbPropertyInt("idAccount")
    open var emailAccount: String by DbPropertyString("emailAccount")
    open var emailTo: String by DbPropertyString("emailTo")
    open var emailFrom: String by DbPropertyString("emailFrom")
    open var emailCC: String by DbPropertyString("emailCC")
    open var subject: String by DbPropertyString("subject")
    open var message: String by DbPropertyString("message")
    open var attachments: String by DbPropertyString("attachments")
    open var sent: Date by DbPropertyDate("sent")
    open var status: Int by DbPropertyInt("status")
    open var error: String by DbPropertyString("error")
    open var retryCount: Int by DbPropertyInt("retryCount")
    open var dateCreated: Date by DbPropertyDate("dateCreated")
    open var dateModified: Date by DbPropertyDate("dateModified")
    open var dateDeleted: Date by DbPropertyDate("dateDeleted")

}

class EmailQueue(vararg columnNames: String) : EmailQueueRaw<EmailQueue>(null, *columnNames) {

    companion object {

        data class Account(val email: String, val password: String)

        val accounts = HashMap<String, Account>()

        init {
            accounts.put("Plaza", Account("donotreply@agilityplaza.com", "\\b;X8GkR^WfQ,D-M"))
        }

        fun add(idAccount: Int, emailAccount: String, emailFrom: String, emailTo: String, emailCC: String, subject: String, message: String, attachments: String="") {
            if (emailTo.isNotEmpty()) {
                val e = EmailQueue()
                e.append()
                e.idAccount = idAccount
                e.emailAccount = emailAccount
                e.emailTo = emailTo
                e.emailFrom = emailFrom
                e.emailCC = emailCC
                e.subject = subject
                e.message = message
                e.attachments = attachments
                e.post()
            }
        }

        fun addPlaza(idAccount: Int, emailTo: String, emailCC: String, subject: String, message: String, attachments: String="") {
            val extendedMessage = "<p>DO NOT REPLY to this email. If you have any questions, please email support@agilityplaza.com.</p>" + message
            add(idAccount, "Plaza", "donotreply@agilityplaza.com", emailTo, emailCC, subject, extendedMessage, attachments)
        }

        fun addSupport(idAccount: Int, emailTo: String, emailCC: String, subject: String, message: String, attachments: String="") {
            add(idAccount, "Plaza", "support@agilityplaza.com", emailTo, emailCC, subject, message, attachments)
        }

        fun addUka(idAccount: Int,emailTo: String, emailCC: String, subject: String, message: String, attachments: String="") {
            val extendedMessage = message + "<p>DO NOT REPLY to this email. If you have any questions, please email enquiries@ukagility.com.</p>"
            add(idAccount, "Plaza", "donotreply@mail.ukagility.com", emailTo, emailCC, subject, extendedMessage, attachments)
        }

    }
}