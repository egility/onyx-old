/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.linux.tools

import com.sun.mail.smtp.SMTPTransport
import org.egility.library.general.Global
import java.security.Security
import java.util.*
import javax.mail.Message
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage


object GoogleMail {

    val SSL_FACTORY = "javax.net.ssl.SSLSocketFactory"

    data class Connection(val session: Session, val transport: SMTPTransport)

    fun withAccount(emailAccount: String, password: String, body: (Connection) -> Unit) {
        Security.addProvider(com.sun.net.ssl.internal.ssl.Provider())

        val props = System.getProperties()
        props.setProperty("mail.smtps.host", "smtp.gmail.com")
        props.setProperty("mail.smtp.socketFactory.class", SSL_FACTORY)
        props.setProperty("mail.smtp.socketFactory.fallback", "false")
        props.setProperty("mail.smtp.port", "465")
        props.setProperty("mail.smtp.socketFactory.port", "465")
        props.setProperty("mail.smtps.auth", "true")
        props.put("mail.smtps.quitwait", "false")

        val session = Session.getInstance(props, null)
        val transport = session.getTransport("smtps") as SMTPTransport
        transport.connect("smtp.gmail.com", emailAccount, password)

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
        withAccount("donotreply@agilityplaza.com", "\\b;X8GkR^WfQ,D-M", { connection ->
            val extendedMessage = message + "<p>DO NOT REPLY to this email, if you have any questions, please email support@agilityplaza.com.</p>"
            send(connection, "donotreply@agilityplaza.com", recipientEmail, ccEmail, "Agility Plaza - " + title, extendedMessage)
        })
    }

    fun sendUka(recipientEmail: String, ccEmail: String, title: String, message: String) {
        withAccount("donotreply@agilityplaza.com", "\\b;X8GkR^WfQ,D-M", { connection ->
            val extendedMessage = message + "<p>DO NOT REPLY to this email, if you have any questions, please email enquiries@ukagility.com.</p>"
            send(connection, "donotreply@mail.ukagility.com", recipientEmail, ccEmail, "Agility Plaza - " + title, extendedMessage)
        })
    }

}

