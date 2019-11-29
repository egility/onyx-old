/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.linux.tools

import com.sun.mail.smtp.SMTPTransport
import org.egility.library.general.Global
import java.util.*
import javax.mail.Message
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

object PlazaMail {

    val HOST = "10.8.0.1"

    data class Connection(val session: Session, val transport: SMTPTransport)

    fun withAccount(emailAccount: String, password: String, body: (Connection) -> Unit) {
        val session = Session.getInstance(System.getProperties(), null)
        val transport = session.getTransport("smtp") as SMTPTransport
        transport.connect(HOST, 25, emailAccount, password)

        body(Connection(session, transport))
    }

    fun send(connection: Connection, emailFrom: String, emailTo: String, emailCC: String, subject: String, message: String) {
        val msg = MimeMessage(connection.session)
        msg.setFrom(InternetAddress(emailFrom))

        if (Global.allEmailsTo.isNotEmpty()) {
            msg.subject = "$subject ($emailTo)"
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(Global.allEmailsTo, false))
        } else {
            msg.subject = subject
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailTo, false))
            if (emailCC.length > 0) {
                msg.setRecipients(Message.RecipientType.CC, InternetAddress.parse(emailCC, false))
            }
        }
        msg.setText(message, "utf-8", "html");
        msg.sentDate = Date()

        connection.transport.sendMessage(msg, msg.allRecipients)
    }

    fun sendAgilityPlaza(recipientEmail: String, ccEmail: String, title: String, message: String) {
        withAccount("auto@agilityplaza.com", "\\b;X8GkR^WfQ,D-M", { connection ->
            val extendedMessage = message + "<p>DO NOT REPLY to this email, if you have any questions, please email support@agilityplaza.com."
            send(connection, "auto@mail.agilityplaza.com", recipientEmail, ccEmail, "Agility Plaza - " + title, extendedMessage)
        })
    }

    fun sendUka(recipientEmail: String, ccEmail: String, title: String, message: String) {
        withAccount("donotreply@agilityplaza.com", "\\b;X8GkR^WfQ,D-M", { connection ->
            val extendedMessage = message + "<p>DO NOT REPLY to this email, if you have any questions, please email enquiries@ukagility.com."
            send(connection, "donotreply@mail.ukagility.com", recipientEmail, ccEmail, "Agility Plaza - " + title, extendedMessage)
        })
    }

}