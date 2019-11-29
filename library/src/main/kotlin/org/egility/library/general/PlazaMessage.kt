/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.library.general

import org.egility.library.dbobject.*
import org.egility.library.dbobject.LedgerItem
import java.util.*

/**
 * Created by mbrickman on 25/02/18.
 */


class PlazaMessage(
    val emailTo: String,
    val subject: String,
    val idAccount: Int,
    val emailCC: String = "",
    val attachments: String = ""
) {

    var message = ""
    var account = Account(idAccount)

    fun send() {
        EmailQueue.addPlaza(idAccount, emailTo, emailCC, subject, message, attachments)
    }

    fun sendSupport() {
        EmailQueue.addSupport(idAccount, emailTo, emailCC, subject, message, attachments)
    }

    fun sendUka() {
        EmailQueue.addUka(idAccount, emailTo, emailCC, subject, message, attachments)
    }

    fun bold(f: () -> String): PlazaMessage {
        message += "<p><b>${f()}</b></p>"
        return this
    }

    fun h1(f: () -> String): PlazaMessage {
        message += "<h1>${f()}</h1>"
        return this
    }

    fun h2(f: () -> String): PlazaMessage {
        message += "<h2>${f()}</h2>"
        return this
    }

    fun h3(f: () -> String): PlazaMessage {
        message += "<h3>${f()}</h3>"
        return this
    }

    fun h4(f: () -> String): PlazaMessage {
        message += "<h4>${f()}</h4>"
        return this
    }

    fun para(f: () -> String): PlazaMessage {
        message += "<p>${f()}</p>"
        return this
    }

    fun paraCenter(f: () -> String): PlazaMessage {
        message += "<p style='text-align: center'>${f()}</p>"
        return this
    }

    fun table(f: () -> String): PlazaMessage {
        message += "<table>${f()}</table>"
        return this
    }

    fun row(f: () -> String): PlazaMessage {
        message += "<tr>${f()}</tr>"
        return this
    }

    fun cell(f: () -> String): PlazaMessage {
        message += "<td>${f()}</td>"
        return this
    }

    fun a(href: String, f: () -> String): PlazaMessage {
        message += "<a href=${href.quoted}>${f()}</a>"
        return this
    }

    fun button(href: String, caption: String): String {
        return """
<a class="button-link"
    href=${href.quoted}
    style="
        color: #ffffff !important;
        text-decoration: none !important;
        text-underline: none;
        word-wrap: break-word;
        font-size: 16px;
        font-weight: 700;
        text-transform: none;
        line-height: 16px;
        text-align: center;
        background-color: #4fbdbd;
        border-radius: 1px;
        padding: 1px 10px;
        border: 10px solid #4fbdbd"
>
        $caption
</a>
            """
    }


    companion object {

        fun prepare(idAccount: Int, emailTo: String, subject: String, emailCC: String = "", attachments: String = "", body: PlazaMessage.() -> Unit) {
            val plazaMessage = PlazaMessage(emailTo, subject, idAccount, emailCC, attachments)
            plazaMessage.h2 { subject }
            body(plazaMessage)
            plazaMessage.send()
        }

        fun prepareHtml(idAccount: Int, emailTo: String, subject: String, emailCC: String = "", attachments: String = "", messageBlock: (body: Body) -> Unit) {
            val plazaMessage = PlazaMessage(emailTo, subject, idAccount, emailCC, attachments)
            plazaMessage.message = html {
                body {
                    if (plazaMessage.account.isOnRow) {
                        p { +"ref: ${plazaMessage.account.code}" }
                    }
                    h2 { +subject }
                    messageBlock(this)
                }

            }.toString()
            plazaMessage.send()
        }

        fun prepareHtmlSupport(idAccount: Int, emailTo: String, subject: String, emailCC: String = "", attachments: String = "", messageBlock: (body: Body) -> Unit) {
            val plazaMessage = PlazaMessage(emailTo, subject, idAccount, emailCC, attachments)
            plazaMessage.message = html {
                body {
                    if (plazaMessage.account.isOnRow) {
                        p { +"ref: ${plazaMessage.account.code}" }
                    }
                    h2 { +subject }
                    messageBlock(this)
                }

            }.toString()
            plazaMessage.sendSupport()
        }

        fun prepareHtmlUka(idAccount: Int, emailTo: String, subject: String, emailCC: String = "", attachments: String = "", messageBlock: (body: Body) -> Unit) {
            val plazaMessage = PlazaMessage(emailTo, subject, idAccount, emailCC, attachments)
            plazaMessage.message = html {
                body {
                    if (plazaMessage.account.isOnRow) {
                        p { +"ref: ${plazaMessage.account.code}" }
                    }
                    h2 { +subject }
                    messageBlock(this)
                }

            }.toString()
            plazaMessage.sendUka()
        }

        fun entryOnHold(idAccount: Int, email: String, showName: String, amountOwing: Int, balance: Int) {
            prepare(idAccount, email, "$showName - Entry on Hold") {
                para {
                    "The ${showName} show has now closed but we are unable to confirm your entry as you " +
                            "do not have sufficient funds in your Agility Plaza account to cover the outstanding show fees " +
                            "of ${amountOwing.toCurrency()}. Your balance currently stands at ${balance.toCurrency()}. To " +
                            "avoid your entry being deleted please transfer at least ${(amountOwing - balance).toCurrency()} " +
                            "into your account <b>IMMEDIATELY</b> using any of the methods described on the FAQs " +
                            "page of the website. Ideally you should use the ‘Instant Top-Up’ facility to save time."
                }
                para {
                    "Over the next few days we will be reviewing your entry and if funds are still not " +
                            "available <b>IT MAY BE DELETED</b> without further warning."
                }
                para {
                    "Remember, it is your responsibility to ensure that the banking details are correct " +
                            "on any electronic payments and that you give sufficient time for funds to clear."
                }
            }
        }

        fun entryOnHold2(idAccount: Int, email: String, showName: String, amountOwing: Int, balance: Int) {
            prepare(idAccount, email, "$showName - Reminder") {
                para {
                    "We are still unable to confirm your entry as you do not have sufficient funds in your Agility " +
                            "Plaza account to cover the outstanding show fees of ${amountOwing.toCurrency()}. Your balance currently " +
                            "stands at ${balance.toCurrency()}. To avoid your entry being deleted please transfer at " +
                            "least ${(amountOwing - balance).toCurrency()} into your account <b>IMMEDIATELY</b> using any " +
                            "of the methods described on the FAQs page of the website. Ideally you should use the " +
                            "‘Instant Top-Up’ facility to save time."
                }
                para {
                    "Over the next few days we will be reviewing your entry and if funds are still not " +
                            "available <b>IT MAY BE DELETED</b> without further warning."
                }
                para {
                    "Remember, it is your responsibility to ensure that the banking details are correct " +
                            "on any electronic payments and that you give sufficient time for funds to clear."
                }
            }
        }

        fun entryOnHold3(idAccount: Int, email: String, showName: String, amountOwing: Int, balance: Int) {
            prepare(idAccount, email, "$showName - Final Reminder") {
                para {
                    "Despite various reminders, we are still unable to confirm your entry as you do not have " +
                            "sufficient funds in your Agility Plaza account to cover the outstanding show fees of " +
                            "${amountOwing.toCurrency()}. Your balance currently stands at ${balance.toCurrency()}. Your " +
                            "entry is about to be DELETED. To avoid this happening, we must receive at least " +
                            "${(amountOwing - balance).toCurrency()} within the next <b>24 HOURS</b> using any of the " +
                            "methods described on the FAQs page of the website. Ideally you should use the ‘Instant " +
                            "Top-Up’ facility to save time."
                }
                para { "You will get no further warnings. If funds are not forthcoming your entry will be deleted." }
                para {
                    "Remember, it is your responsibility to ensure that the banking details are correct " +
                            "on any electronic payments and that you give sufficient time for funds to clear."
                }
            }
        }

        fun paymentReceived(
            idAccount: Int,
            email: String,
            date: Date,
            amount: Int,
            reference: String,
            code: String,
            wrongReference: Boolean = false
        ) {
            prepare(idAccount, email, "Bank Transfer") {
                para {
                    "Thank you for transferring ${amount.toCurrency()} into your Agility Plaza account. This was " +
                            "received into our client funds bank account on ${date.dateText} with a reference " +
                            "of ${reference.quotedSingle}"
                }
                if (wrongReference) {
                    para {
                        "<b>VERY IMPORTANT:</b> Your transfer did not include the correct reference. To ensure that " +
                                "funds are correctly allocated to your account please include your reference " +
                                "<b>${code}</b> in future."
                    }
                }
            }
        }

        fun paymentRefund(idAccount: Int, email: String, date: Date, amount: Int) {
            prepare(idAccount, email, "Payment Refund") {
                para {
                    "As requested we have refunded ${amount.toCurrency()} from your Agility Plaza account back to " +
                            "your bank account using the details you supplied. This left our client funds bank " +
                            "account on ${date.dateText}."
                }
            }
        }

        fun donation(idAccount: Int, email: String, amount: Int, beneficiary: String) {
            prepare(idAccount, email, "Charitable Donation") {
                para {
                    "As requested we have made a charitable donation of ${amount.toCurrency()} from your Agility Plaza to $beneficiary."
                }
            }
        }

        fun topupRefund(idAccount: Int, email: String, amount: Int, card: String) {
            prepare(idAccount, email, "Instant Top-Up Refund") {
                para {
                    "As requested we have refunded ${amount.toCurrency()} from your Agility Plaza " +
                            "account back to your $card via our Stripe payment service. It will take between " +
                            "5 and 10 days for this to appear on your card statement."
                }
            }
        }

        fun ukaMemberFundsTransfer(idAccount: Int, email: String, date: Date, amount: Int) {
            prepare(idAccount, email, "UKA Credit Transfer") {
                para {
                    "We just wanted to let you know that UK Agility have transferred your credit balance of " +
                            "${amount.toCurrency()} to us and we have placed it in your Agility Plaza account. This " +
                            "amount was received into our client funds bank account on ${date.dateText}."
                }
            }
        }

        fun swapMemberFundsTransfer(idAccount: Int, email: String, date: Date, amount: Int) {
            prepare(idAccount, email, "SWAP Credit Transfer") {
                para {
                    "We just wanted to let you know that South West Agility Processing (SWAP) have transferred your credit balance of " +
                            "${amount.toCurrency()} to us and we have placed it in your Agility Plaza account. This " +
                            "amount was received on ${date.dateText}."
                }
            }
        }

        fun ukaRegistrationTransferConfirmation(email: String, date: Date, amount: Int) {
            prepare(0, email, "Registration Fees - Agility Plaza Funds Transfer") {
                para {
                    "Please be advised that ${amount.toCurrency()} left our bank account on ${date.dateText} " +
                            "with a reference UKA REGISTRATIONS"
                }
            }
        }

        fun ukaFundsTransferConfirmation(
            email: String,
            showName: String,
            idCompetition: Int,
            date: Date,
            amount: Int,
            reference: String
        ) {
            prepare(0, email, "$showName - Agility Plaza Funds Transfer") {
                para {
                    "Please be advised that ${amount.toCurrency()} left our bank account on ${date.dateText} " +
                            "with a reference $reference"
                }
            }
        }

        fun unableToCloseShow(showName: String) {
            prepare(0, "accounts@agilityplaza.com", "Unable to close ($showName)") {
                para {
                    "We are unable to close $showName because the bank transfers have not been processed in the " +
                            "last 12 hours."
                }
            }
        }

        fun competitionGradeChangeOld(
            idAccount: Int,
            email: String,
            showName: String,
            oldGrade: String,
            newGrade: String,
            petName: String,
            json: JsonNode
        ) {
            prepare(idAccount, email, "$showName - $petName change of grade") {
                para {
                    "We have reviewed your entries for $showName in light of your change of grade from $oldGrade " +
                            "to $newGrade and your classes have been adjusted as follows:"
                }
                para {
                    var rows = ""
                    for (dateNode in json) {
                        rows = rows.append("<b><u>${dateNode["date"].asDate.fullDate()}</u></b>", "<br>")
                        for (classNode in dateNode["classes"]) {
                            rows = rows.append(
                                "${classNode["name"].asString} - ${classNode["action"].asString}",
                                "<br>"
                            )
                        }
                    }
                    rows
                }
            }
        }

        fun competitionGradeChange(
            idAccount: Int,
            email: String,
            oldGrade: String,
            newGrade: String,
            petName: String,
            json: JsonNode
        ) {
            prepare(idAccount, email, "$petName change of grade") {
                if (json.size == 0) {
                    para {
                        "We have reviewed your entries in light of your change of grade from ${Grade.getGradeName(
                            oldGrade
                        )} " +
                                "to ${Grade.getGradeName(newGrade)} and there are no show entries to be changed"
                    }
                } else {
                    para {
                        "We have reviewed your entries in light of your change of grade from ${Grade.getGradeName(
                            oldGrade
                        )} " +
                                "to ${Grade.getGradeName(newGrade)} and your show entries have been adjusted as follows:"
                    }
                    para {
                        var rows = ""
                        for (competition in json) {
                            rows = rows.append("<h3>${competition["name"].asString}</h3>")
                            if (competition["dates"].size == 0) {
                                rows.append("No changes made.")
                            }
                            var first = true
                            for (dateNode in competition["dates"]) {
                                rows = rows.append(
                                    "<b><u>${dateNode["date"].asDate.fullDate()}</u></b>",
                                    if (first) "" else "<br>"
                                )
                                for (classNode in dateNode["classes"]) {
                                    rows = rows.append(
                                        "${classNode["name"].asString} - ${classNode["action"].asString}",
                                        "<br>"
                                    )
                                }
                                first = false
                            }
                        }
                        rows
                    }
                }
            }
        }

        fun ukaRegistrationCorrection(idAccount: Int, email: String, fee: Int, balance: Int, items: ArrayList<String>) {
            prepareHtml(idAccount, email, "UK Agility Registration - Correction") { body ->
                body.block {
                    p {
                        +"Unfortunately we accidentally confirmed your application before the fees were received. "
                        +"We have now reversed this mistake pending the receipt of the correct money."
                    }
                    if (balance == 0) {
                        p {
                            +"Total fees for this application come "
                            +"to ${fee.toCurrency()}. Please arrange to transfer this into your Agility Plaza "
                            +"account as soon as possible so we can confirm your application."
                        }
                    } else {
                        p {
                            +"Total fees for this application come "
                            +"to ${fee.toCurrency()}. Your Agility Plaza account currently has a balance "
                            +"of ${balance.toCurrency()} so you will need to transfer in at "
                            +"least ${(fee - balance).toCurrency()} as soon as possible so we can confirm "
                            +"your application."
                        }

                    }
                    p {
                        +"Note that registrations do not take effect until they have been confirmed. Although bank "
                        +"transfers may arrive at our bank within a couple of hours, they are not shown on your "
                        +"account until we process our bank statements which can take up to 3 working days. If "
                        +"you need this application to be confirmed very quickly so that you can enter or "
                        +"compete at a UKA show, we recommend that you use the “Instant Top-Up” service to add "
                        +"funds to your account."
                    }
                    p {
                        +"DO NOT send funds directly to UK Agility."
                    }
                    p {
                        +"Details of your application are as follows:"
                    }

                    p {
                        for (item in items) {
                            +"$item<BR>"
                        }
                    }
                }
            }
        }

        fun ukaRegistrationPending(idAccount: Int, email: String, fee: Int, balance: Int, items: ArrayList<String>) {
            prepareHtml(idAccount, email, "UK Agility Registration - Application") { body ->
                body.block {
                    if (balance == 0) {
                        p {
                            +"Thank you for your UK Agility application. Total fees for this application come "
                            +"to ${fee.toCurrency()}. Please arrange to transfer this into your Agility Plaza "
                            +"account as soon as possible so we can confirm your application."
                        }
                    } else {
                        p {
                            +"Thank you for your UK Agility application. Total fees for this application come "
                            +"to ${fee.toCurrency()}. Your Agility Plaza account currently has a balance "
                            +"of ${balance.toCurrency()} so you will need to transfer in at "
                            +"least ${(fee - balance).toCurrency()} as soon as possible so we can confirm "
                            +"your application."
                        }

                    }
                    p {
                        +"Note that registrations do not take effect until they have been confirmed. Although bank "
                        +"transfers may arrive at our bank within a couple of hours, they are not shown on your "
                        +"account until we process our bank statements which can take up to 3 working days. If "
                        +"you need this application to be confirmed very quickly so that you can enter or "
                        +"compete at a UKA show, we recommend that you use the “Instant Top-Up” service to add "
                        +"funds to your account."
                    }
                    p {
                        +"DO NOT send funds directly to UK Agility."
                    }
                    p {
                        +"Details of your application are as follows:"
                    }

                    p {
                        for (item in items) {
                            +"$item<BR>"
                        }
                    }
                }
            }
        }

        fun ukaRegistrationConfirmed(email: String, idAccount: Int) {
            prepareHtml(idAccount, email, "UK Agility Registration - Confirmed") { body ->

                body.block {
                    p {
                        +"Your UK Agility application has been confirmed and you will now be able to enter and compete at "
                        +"UK Agility shows with any of the members and dogs listed below (note the 5 digit "
                        +"membership/registration codes)."
                    }
                    p {
                        +"Please visit the UK Agility website "
                        a(href = "www.ukagility.com") { +"www.ukagility.com" }
                        +" to download the rules or for "
                        +"information about competing at UKA shows. Dogs marked with an asterisk next to their "
                        +"height need to be measured at their first UKA show. There is no need to book "
                        +"and you will not be charged for the service."

                    }
                    h3 { +"Members" }
                    table {
                        attributes["border"] = "1"
                        attributes["cellpadding"] = "5"
                        attributes["style"] = "border-collapse: collapse;"
                        tr {
                            th { +"Code" }
                            th { +"Name" }
                            th { +"Expires" }
                        }
                        Competitor().where(
                            "idAccount=$idAccount AND AliasFor=0",
                            "idUka=0, idUka, givenName, familyName"
                        ) {
                            if (ukaMembershipExpires.isNotEmpty() && ukaMembershipExpires >= today) {
                                tr {
                                    td { +"$idUka" }
                                    td { +"$fullName" }
                                    td { +"${ukaMembershipExpires.dateText}" }
                                }
                            }
                        }
                    }

                    h3 { +"Dogs" }
                    table {
                        attributes["border"] = "1"
                        attributes["cellpadding"] = "5"
                        attributes["style"] = "border-collapse: collapse;"
                        tr {
                            th { +"Code" }
                            th { +"Dog" }
                            th { +"Height" }
                            th { +"Performance" }
                            th { +"Steeplechase" }
                        }
                        Dog().where("idAccount=$idAccount  AND AliasFor=0", "dogCode=0, dogCode, petName") {
                            if (ukaDateConfirmed.isNotEmpty() && state < DOG_GONE) {
                                val asterisk = if (ukaHeightCode < "UKA650" && ukaMeasuredHeight == 0) "*" else ""
                                tr {
                                    td { +"$code" }
                                    td { +"$cleanedPetName" }
                                    td { +"${Height.getHeightName(ukaHeightCode)}$asterisk" }
                                    td { +"${Grade.getGradeName(ukaPerformanceLevel)}" }
                                    td { +"${Grade.getGradeName(ukaSteeplechaseLevel)}" }
                                }
                            }
                        }
                    }

                }
            }

        }

        fun showEntry(
            account: Account,
            competition: Competition,
            block: BLOCK,
            prefix: String = "",
            runningOrders: Boolean = false
        ) {
            val idCompetition = competition.id
            val idAccount = account.id
            val where = if (runningOrders && competition.isUka)
                "entry.idAccount=$idAccount AND (entryType<=$ENTRY_TRANSFER OR entryType=$ENTRY_DEPENDENT_CLASS) AND " +
                        "agilityClass.idCompetition=$idCompetition AND entry.progress<$PROGRESS_DELETED_LOW AND NOT agilityClass.classCode IN (${ClassTemplate.TRY_OUT.code})"
            else if (runningOrders)
                "entry.idAccount=$idAccount AND (entryType<=$ENTRY_TRANSFER OR entryType=$ENTRY_DEPENDENT_CLASS) AND " +
                        "agilityClass.idCompetition=$idCompetition AND entry.progress<$PROGRESS_DELETED_LOW AND " +
                        "agilityClass.ringNumber>0"
            else
                "entry.idAccount=$idAccount AND entryType<=$ENTRY_TRANSFER AND agilityClass.idCompetition=$idCompetition AND entry.progress<$PROGRESS_DELETED_LOW"

            val orderBy =
                "dog.petName, dog.registeredName, agilityClass.ClassDate, agilityClass.ClassNumber, agilityClass.classCode, agilityClass.suffix"
            val classDateMonitor = ChangeMonitor<Date>(nullDate)
            val petNameMonitor = ChangeMonitor<String>("")

            var campingText = ""
            if (runningOrders && competition.hasCamping) {
                Camping().seek("idCompetition = $idCompetition AND idAccount = $idAccount AND NOT rejected AND pitchNumber<>''") {
                    campingText = bookingText(true)
                }
            }
            with(block) {
                hr {}
                if (runningOrders) {
                    if (campingText.isNotEmpty()) {
                        h3 { +"Camping Booking" }
                        p { +campingText }
                        hr {}
                    }
                    h3 { +"Running Orders" }
                } else {
                    h3 { if (prefix.isNotEmpty()) +"$prefix Entry" else +"Entry" }
                }
                table {
                    attributes["border"] = "1"
                    attributes["cellpadding"] = "5"
                    attributes["style"] = "border-collapse: collapse;"

                    Entry().join { team }.join { team.dog }.join { team.competitor }.join { agilityClass }
                        .where(where, orderBy) {
                            if (!competition.grandFinals || (entered && !uninvited && !cancelled)) {
                                if (petNameMonitor.hasChanged(team.dog.petName)) {
                                    classDateMonitor.value = nullDate
                                    var dogHeading = "${team.dog.code} ${team.dog.petName}"
                                    if (competition.isKc) {
                                        CompetitionDog().seek("idCompetition=$idCompetition AND idDog=${team.idDog}") {
                                            dogHeading += " (${Grade.getGradeName(kcGradeCode)} / ${Height.getCombinedName(
                                                kcHeightCode,
                                                kcJumpHeightCode
                                            )})"
                                        }
                                    } else if (competition.isFab) {
                                        CompetitionDog().seek("idCompetition=$idCompetition AND idDog=${team.idDog}") {
                                            val heightText =
                                                "FAB: ${Height.getHeightName(fabHeightCode)}, IFCS: ${Height.getHeightName(ifcsHeightCode)}"
                                            val collieText = if (fabCollie) "Collie/X" else "ABC"
                                            if (runningOrders) {
                                                dogHeading += " ($collieText)"
                                            } else {
                                                dogHeading += " ($collieText, $heightText)"
                                            }
                                        }
                                    } else if (competition.isIndependent) {
                                        CompetitionDog().seek("idCompetition=$idCompetition AND idDog=${team.idDog}") {
                                            dogHeading += " (${options(competition.idOrganization)})"
                                        }
                                    }
                                    tr {
                                        td {
                                            attributes["colspan"] = "99"
                                            b { +dogHeading }
                                        }
                                    }
                                    tr {
                                        th { +"Date" }
                                        th { +"Class" }
                                        if (competition.isUka) {
                                            th { +"Height" }
                                        }
                                        if (competition.isKc) {
                                            th { +"Handler" }
                                        }
                                        if (competition.processed || runningOrders) {
                                            th { +"Ring" }
                                            if (competition.isFab) {
                                                th { +"Height" }
                                            }
                                            th { +"R/O" }
                                        }
                                    }

                                }
                                tr {
                                    if (classDateMonitor.hasChanged(agilityClass.date)) {
                                        td { +agilityClass.date.fullishDate() }
                                    } else {
                                        td {}
                                    }
                                    if (runsEntered>1) {
                                        td { +"${agilityClass.name} ($runsEntered runs)" }
                                    } else {
                                        td { +agilityClass.name }
                                    }
                                    if (competition.isUka) {
                                        td { +jumpHeightText }
                                    }
                                    if (competition.isKc) {
                                        td { +team.competitor.givenName.naturalCase }
                                    }
                                    if (competition.processed || runningOrders) {
                                        var ring =
                                            "Ring ${agilityClass.ringNumber} (${agilityClass.ringOrder.ordinal()})"
                                        if (agilityClass.ringNumber == 0 && agilityClass.hasChildren) {
                                            val agilityClass = agilityClass.children()
                                            agilityClass.first()
                                            ring =
                                                "Ring ${agilityClass.ringNumber} (${agilityClass.ringOrder.ordinal()})"
                                        }

                                        td { +ring }
                                        if (competition.isFab) {
                                            td { +Height.getHeightName(heightCode) }
                                        }
                                        td {
                                            attributes["align"] = "right"
                                            +runningOrder.toString()
                                        }
                                    }
                                }
                            }
                        }

                }

                if (runningOrders && competition.isFab) {
                    p {
                        +"Running orders are specific to each height."
                    }
                }

                if (!runningOrders) {
                    if (competition.hasCamping) {
                        Camping().seek("idCompetition = $idCompetition AND idAccount = $idAccount AND NOT rejected") {
                            val waitingList = competition.hasManagedCamping && !confirmed

                            var first = nullDate
                            var last = nullDate
                            var nights = 0
                            for (date in dateArray) {
                                nights++
                                if (first.isEmpty()) first = date
                                if (date > last) last = date
                            }
                            val entitlement = Account.getCampingEntitlement(idCompetition, idAccount)
                            val group =
                                if (entitlement.isNotEmpty()) entitlement else if (groupName.isNotEmpty()) "Group: $groupName" else ""

                            val arriving = first.format("EEEE")
                            val leaving = last.addDays(1).format("EEEE")
                            val period = "for $nights nights, arriving $arriving and leaving $leaving"

                            val refundClause = when (competition.campingRefundOption) {
                                1 -> """
                                    This fee is non-refundable and you will not 
                                    be able to cancel your camping booking. You may be permitted to sell on your pitch but please check 
                                    with the show secretary before doing so and let them know who will be taking your place.
                                """.trimIndent()
                                2 -> """
                                    You will only be allowed to cancel your booking if the 
                                    show has not yet closed and there are people on the waiting list who can take your place. Otherwise your 
                                    camping fees are non-refundable and you will not be able to cancel your booking. You may be permitted to 
                                    sell on your pitch but please check with the show secretary before doing so and let them know who 
                                    will be taking your place.
                                """.trimIndent()
                                3 -> """
                                    This fee will be refunded if you cancel your camping booking (or whole entry) before 
                                    the show closes. To cancel your booking just amend your entry and un-tick the 
                                    camping boxes (or cancel your whole entry in the usual way). Once the show closes 
                                    you may be permitted to sell your space on but please check with the show secretary 
                                    before doing so and let them know who will be taking your place.
                                """.trimIndent()
                                else -> ""
                            }

                            val accountBalance = if (pending) Ledger.balance(idAccount) else 0
                            val paymentNeeded = if (pending) deposit - accountBalance else 0

                            val text = when {
                                pending -> """
                                    <font color="red">Your camping application <b>is on hold</b> pending payment.
                                    In order for us to proceed, you must pay the 
                                    ${deposit.money} deposit in full. Your account balance is currently
                                    ${accountBalance.money} so you need to top up your account by 
                                    <b>at least ${paymentNeeded.money}</b>. We will process your camping application as
                                    soon at this is received.</font>
                                """.trimIndent()
                                waitingList && deposit == 0 -> """
                                    You have been placed on <b>the waiting list</b> $period.
                                """.trimIndent()
                                waitingList -> """
                                    You have been placed on <b>the waiting list</b> $period. A deposit of ${deposit.money} has 
                                    been deducted from your account. This will be refunded if you cancel your 
                                    camping application or it has not been accepted when the show closes. To cancel 
                                    your application just amend your entry and un-tick the camping boxes (or cancel your
                                    whole entry in the usual way). The show secretary will review the waiting list 
                                    regularly and allocate spaces as they become available using the criteria 
                                    detailed in the schedule (or 'first come first served' if not otherwise specified).
                                """.trimIndent()
                                confirmed && deposit == 0 -> """
                                    You have a <b>confirmed booking</b> $period.
                                """.trimIndent()
                                confirmed -> """
                                    You have a <b>confirmed booking</b> $period. The fee of ${deposit.money} has been deducted 
                                    from your account. $refundClause
                                """.trimIndent()
                                else -> "Booked $period"
                            }
                            val groupText = if (group.isNotEmpty()) " ($group)" else ""


                            p {
                                b { +"Camping: " }
                                +(text)
                            }
                        }.otherwise {
                            p {
                                b { +"Camping: " }
                                +"Not booked"
                            }
                        }
                    }

                    LedgerItem().where("idCompetition=$idCompetition AND idAccount=$idAccount AND type in ($LEDGER_ITEM_POSTAGE, $LEDGER_ITEM_PAPER)", limit = 1) {
                        p {
                            b { +"Running Orders: " }
                            +"Will be posted"
                        }
                    }.otherwise {
                        p {
                            b { +"Running Orders: " }
                            if (competition.noPosting) {
                                +"Will be emailed, and you can download from the website."
                            } else {
                                +"Postage not required (will download from website)"
                            }
                        }

                    }

                    if (competition.isUka) {
                        var help = ""
                        CompetitionCompetitor().join { competitor }
                            .where("idCompetition=$idCompetition AND competitor.idAccount=$idAccount")
                            {
                                val offer = helpOffer
                                if (offer.isNotEmpty()) {
                                    help = help.append("${competitor.givenName.naturalCase}: $offer", ". ")
                                }
                            }
                        if (help.isEmpty()) help = "Not offered to help"
                        p {
                            b { +"Help: " }
                            +help
                        }
                    }

                    if (competition.isKc) {
                        var nfc = ""
                        var vouchers = ""
                        var help = ""
                        CompetitionDog().join { dog }
                            .where("CompetitionDog.idCompetition=$idCompetition AND CompetitionDog.idAccount=$idAccount AND nfc") {
                                nfc = nfc.append(dog.cleanedPetName)
                            }
                        CompetitionCompetitor().join { competitor }
                            .where("idCompetition=$idCompetition AND competitor.idAccount=$idAccount") {
                                if (voucherCode.isNotEmpty())
                                    for (code in voucherCode.split(",")) {
                                        Voucher().seekByCode(idCompetition, code) {
                                            vouchers = vouchers.append(
                                                "$code - $description (${competitor.givenName.naturalCase})",
                                                ", "
                                            )
                                        }
                                    }

                                val offer = helpOffer
                                if (offer.isNotEmpty()) {
                                    help = help.append("${competitor.givenName.naturalCase}: $offer", ". ")
                                }
                            }
                        if (nfc.isNotEmpty()) {
                            p {
                                b { +"NFC Entries: " }
                                +nfc
                            }
                        }
                        if (vouchers.isEmpty()) vouchers = "None used"
                        p {
                            b { +"Vouchers: " }
                            +vouchers
                        }
                        if (help.isEmpty()) help = "Not offered to help"
                        p {
                            b { +"Help: " }
                            +help
                        }
                    }
                    hr {}
                    
                    var anyPrizes=false
                    if (competition.bonusCategoriesRaw.isNotEmpty()) {
                        h3 { +"Special Prize Categories" }
                        table() {
                            attributes["border"] = "1"
                            attributes["cellpadding"] = "5"
                            attributes["style"] = "border-collapse: collapse;"

                            tr {
                                th { +"Handler/Dog" }
                                th { +"Categories" }
                            }
                            CompetitionDog().join { dog }.where(
                                "competitionDog.idCompetition=$idCompetition AND competitionDog.idAccount=$idAccount", "petName") {
                                if (indBonusCategories.isNotEmpty()) {
                                    tr {
                                        td { +"${indHandler} / ${dog.cleanedPetName}" }
                                        td { +"${indBonusCategories.replace("_", " ").replace(",", ", ")}" }
                                    }
                                    anyPrizes = true
                                }
                                if (!anyPrizes) {
                                    tr {
                                        td {
                                            attributes["colspan"] = "99"
                                            +"No categories selected"
                                        }
                                    }
                                }
                            }
                        }
                        hr {}
                    }

                    h3 { +"Fees" }
                    table() {
                        attributes["border"] = "1"
                        attributes["cellpadding"] = "5"
                        attributes["style"] = "border-collapse: collapse;"

                        tr {
                            th { +"Item" }
                            th { +"Description" }
                            th { +"Cost" }
                        }

                        var index = 1
                        var total = 0
                        LedgerItem().where(
                            "idCompetition=$idCompetition AND idAccount=$idAccount",
                            "type, description, unitPrice"
                        ) {
                            total += amount
                            val text = describe()

                            tr {
                                td { +(index++).toString() }
                                td { +text }
                                td {
                                    attributes["align"] = "right"
                                    +amount.toCurrency()
                                }
                            }


                        }
                        tr {
                            td {
                                attributes["colspan"] = "2"
                                b { +"Total" }
                            }
                            td {
                                attributes["align"] = "right"
                                +total.toCurrency()
                            }
                        }

                    }
                }
                hr {}
            }

        }

        fun showEntryAcknowledged(idCompetition: Int, idAccount: Int, paper: Boolean = false) {
            val competition = Competition(idCompetition)
            val account = Account(idAccount)

            if (paper) {
                val email = account.emailList
                if (email.isNotEmpty()) {
                    prepareHtml(idAccount, account.emailList, "${competition.briefNiceName} - Paper Entry") { body ->
                        body.block {
                            if (competition.grandFinals) {
                                p {
                                    +"This is to confirm that we have received your paper entry for "
                                    +"${competition.briefName}. The details are shown below. Please check these carefully and "
                                    +"let us know if there are any problems. UK Agility terms and conditions apply for the " 
                                    +"Grand Finals. Note that fees are non-refundable in the event of cancellation"
                                }
                            } else {
                                p {
                                    +"This is to confirm that we have received your paper entry for "
                                    +"${competition.briefName}. The details are shown below. Please check these carefully and "
                                    +"let us know if there are any problems - bearing in mind that we cannot accept any"
                                    +"changes or cancellations after the show closes on ${competition.dateCloses.fullishDate()}"
                                }
                            }
                            p { +"Note that your paper entry will only be processed if you have sent the correct fees. We will try to contact you if this is not the case." }
                            showEntry(account, competition, this)
                        }
                    }
                }
            } else if (competition.hasClosed) {
                Ledger().seek("idCompetition=$idCompetition AND idAccount=$idAccount AND type=$LEDGER_ENTRY_FEES") {
                    val late = if (false && dateCreated.dateOnly() > competition.dateCloses) "Late " else ""
                    if (amountOwing>0) {
                        prepareHtml(
                            idAccount,
                            account.emailList,
                            "${competition.briefNiceName} - ${late}Entry on Hold"
                        ) { body ->
                            body.block {
                                ul {
                                    li { +"This is your acknowledgement that we have received your entry for ${competition.briefName} which has already closed." }
                                    li {
                                        +"Unfortunately you do not currently have sufficient funds in your account to "
                                        +"cover the outstanding ${amountOwing.toCurrency()} fees so we are unable to confirm your entry at "
                                        +"this stage but will do so as soon as the fees are received."
                                    }
                                    li {
                                        +"The details of your entry are shown below. "
                                    }
                                    li { +"This is the entirety of your entry for ${competition.briefName} and replaces any previous entries submitted by you." }
                                }
                                showEntry(account, competition, this)
                            }
                        }
                    } else {
                        prepareHtml(
                            idAccount,
                            account.emailList,
                            "${competition.briefNiceName} - ${late}Entry Confirmed"
                        ) { body ->
                            body.block {
                                ul {
                                    li { +"This is to confirm your entry for ${competition.briefName} which has already closed." }
                                    li {
                                        +"The details of your entry are shown below. "
                                    }
                                    li { +"This is the entirety of your entry for ${competition.briefName} and replaces any previous entries submitted by you." }
                                }
                                showEntry(account, competition, this)
                            }
                        }
                    }

                }
            } else {
                prepareHtml(idAccount, account.emailList, "${competition.briefNiceName} - Acknowledgement") { body ->

                    body.block {

                        ul {
                            li { +"This is your acknowledgement that we have received your entry for ${competition.briefName}." }
                            if (competition.grandFinals) {
                                li {
                                    b { +"This is not a confirmation" }
                                    +"– we will confirm your entry once the fees have been received."
                                }
                                li { +"UK Agility terms and conditions apply for the Grand Finals. Note that fees are non-refundable in the event of cancellation." }
                                li {
                                    +"The details of your entry are shown below. "
                                    b { +"Please check carefully." }
                                }
                            } else {
                                li {
                                    b { +"This is not a confirmation" }
                                    +"– we will confirm your entry once the show closes (on ${competition.dateCloses.fullishDate()}) and the fees have been deducted from your account."
                                }
                                li { +"Until the closing date you are free to amend or cancel your entry." }
                                li {
                                    +"The details of your entry are shown below. "
                                    b { +"Please check carefully" }
                                    +" as once the show closes, we cannot accept any changes."
                                }
                            }
                            li { +"This is the entirety of your entry for ${competition.briefName} and replaces any previous entries submitted by you." }
                        }
                        showEntry(account, competition, this)
                    }
                }

            }


        }

        fun showCampingAccepted(idCompetition: Int, idAccount: Int) {
            val competition = Competition(idCompetition)
            val account = Account(idAccount)

            prepareHtml(idAccount, account.emailList, "${competition.briefNiceName} - Camping accepted") { body ->

                body.block {

                    ul {
                        li { +"We are please to say that your camping application for ${competition.briefName} has now been accepted." }
                        li {
                            +"The details of your revised entry are shown below with the updated camping information. "
                            b { +"Please check carefully" }
                            +" as once the show closes, we cannot accept any changes."
                        }
                        li { +"This is the entirety of your entry for ${competition.briefName} and replaces any previous entries submitted by you." }
                    }
                    showEntry(account, competition, this)
                }
            }

        }

        fun showCampingDepositReceived(idCompetition: Int, idAccount: Int) {
            val competition = Competition(idCompetition)
            val account = Account(idAccount)

            prepareHtml(idAccount, account.emailList, "${competition.briefNiceName} - Camping deposit received") { body ->

                body.block {

                    ul {
                        li { +"We are please to say that your camping deposit for ${competition.briefName} has now been received." }
                        li {
                            +"The details of your revised entry are shown below with the updated camping information. "
                            b { +"Please check carefully" }
                            +" as once the show closes, we cannot accept any changes."
                        }
                        li { +"This is the entirety of your entry for ${competition.briefName} and replaces any previous entries submitted by you." }
                    }
                    showEntry(account, competition, this)
                }
            }

        }

        fun campingRejected(idCompetition: Int, idAccount: Int) {
            val competition = Competition(idCompetition)
            val account = Account(idAccount)

            prepareHtml(idAccount, account.emailList, "${competition.briefNiceName} - Camping deposit refunded") { body ->
                body.block {
                    p {
                        +"Unfortunately we have been unable to confirm your application for camping at ${competition.briefName} and "
                        if (competition.closed) +"the show is now closed. " else +"camping is now full. "
                        +"Camping has been removed from your entry and we have returned the deposit to your account."
                        +"We will keep your details on file in case we are contacted by anyone who wants to sell on their pitch."
                    }
                }
            }
        }

        fun campingTransferredTo(idCompetition: Int, idAccount: Int, toName: String) {
            val competition = Competition(idCompetition)
            val account = Account(idAccount)

            prepareHtml(idAccount, account.emailList, "${competition.briefNiceName} - Camping Transferred On") { body ->
                body.block {
                    p {
                        +"This is to confirm that your camping pitch for ${competition.briefName} has been transferred to ${toName}."
                    }
                }
            }
        }

        fun campingTransferredFrom(idCompetition: Int, idAccount: Int, fromName: String, period: String) {
            val competition = Competition(idCompetition)
            val account = Account(idAccount)

            prepareHtml(idAccount, account.emailList, "${competition.briefNiceName} - Camping Acquired") { body ->
                body.block {
                    p {
                        +"This is to confirm that you have acquired a camping pitch for ${competition.briefName} from ${fromName} and this has now been transferred into your name."
                    }
                    p {
                        +"Camping has been paid for $period. If the show is still open, you can change this in the usual way otherwise email support@agilityplaza.com."
                    }
                }
            }
        }

        fun closeWarning(competition: Competition, idAccount: Int, amountOwing: Int, balance: Int) {
            val account = Account(idAccount)

            prepareHtml(idAccount, account.emailList, "${competition.briefNiceName} - Closing Soon") { body ->

                body.block {
                    ul {
                        li { +"${competition.briefName} will be closing at midnight on ${competition.dateCloses.fullDate()}." }
                        if (balance < amountOwing) {
                            li {
                                +"The outstanding show fees of ${amountOwing.toCurrency()} become due at this time. "
                                if (balance == 0) {
                                    +"You do not currently have any funds in your account, so "
                                    b { +"you need to transfer at least ${amountOwing.toCurrency()}" }
                                    +" into your account to cover this amount."
                                } else {
                                    +"Your account balance currently stands at ${balance.toCurrency()}, so "
                                    b { +"you need to transfer at least ${(amountOwing - balance).toCurrency()}" }
                                    +" into your account."
                                }
                                +" See FAQs of methods of payment."
                            }
                            li {
                                +"It is your responsibility to ensure that funds are transferred using the correct banking "
                                +"details and that these are showing in your account when shows close. If funds are not "
                                +"showing at this time your entries may be deleted regardless of the circumstances."
                            }
                        } else {
                            li { +"The show fees of ${amountOwing.toCurrency()} will be deducted from your account at this time." }
                        }
                        li {
                            +"The details of your entry are shown below. "
                            b { +"Please check carefully" }
                            +" and make any necessary adjustments as once the show closes "
                            if (competition.isKc) {
                                +"we cannot accept any changes (other than grade win outs) and you cannot cancel your entry."
                            } else {
                                +"we cannot accept any changes and you cannot cancel your entry."
                            }
                        }

                    }
                    showEntry(account, competition, this)
                }

            }
        }

        fun entryConfirmed(competition: Competition, idAccount: Int, outstanding: Int, overdue: Boolean = false) {
            val account = Account(idAccount)

            val state = if (overdue) "Entry Now Confirmed" else "Entry Confirmed"
            prepareHtml(idAccount, account.emailList, "${competition.briefNiceName} - $state") { body ->

                body.block {
                    ul {
                        if (overdue) {
                            li { +"We have now received funds into your account and are thus able to confirm your entry for ${competition.briefName}." }
                        } else {
                            li { +"${competition.briefName} has now closed and we are able to confirm your entry." }
                        }
                        if (competition.grandFinals) {
                            li { +"Your Agility Plaza account has now been charged the ${outstanding.toCurrency()} entry fee (which is non-refundable)." }
                        } else {
                            li { +"Your Agility Plaza account has now been charged the ${outstanding.toCurrency()} outstanding entry fee (which is non-refundable)." }
                            if (competition.isKc) {
                                li { +"No changes may now be made to your entry (other than below) and it cannot be cancelled." }
                                li { +"If your dog wins out of a grade and you update its Agility Plaza record accordingly at least 14 days before the show's start date, we will automatically adjust your class entries." }
                            } else if (competition.isUka) {
                                li {
                                    +"No changes may now be made to your entry and it cannot be cancelled. If your dog "
                                    +"changes level or is measured into a different height, please report to the show "
                                    +"secretary when you arrive at the show and they will move you to the appropriate classes"
                                }
                            } else {
                                li { +"No changes may now be made to your entry and it cannot be cancelled." }
                            }
                        }

                        LedgerItem().where("idCompetition=${competition.id} AND idAccount=$idAccount AND type in ($LEDGER_ITEM_POSTAGE, $LEDGER_ITEM_PAPER)", limit = 1) {
                            li {
                                +"Your show documents will be posted out to you and you should receive these at least 7 days before the show starts. "
                                +"They can also be downloaded from ${competition.dateProcessing.fullDate()} (go to "
                                +"'Shows I have entered' and click on ${competition.briefName} then press the 'Documents' button)"
                            }
                        }.otherwise {
                            if (competition.noPosting) {
                                li {
                                    +"We will not be posting our documents for this show. "
                                    +"However these can be downloaded from ${competition.dateProcessing.fullDate()} (go to "
                                    +"'Shows I have entered' and click on ${competition.briefName} then press the 'Documents' button)"
                                }
                            } else {
                                li {
                                    +"You have not paid for show documents to be posted. "
                                    +"However these can be downloaded from ${competition.dateProcessing.fullDate()} (go to "
                                    +"'Shows I have entered' and click on ${competition.briefName} then press the 'Documents' button)"
                                }
                            }
                        }

                    }
                    showEntry(account, competition, this)
                }
            }
        }

        fun entryDeleted(competition: Competition, idAccount: Int) {
            val account = Account(idAccount)

            prepareHtml(idAccount, account.emailList, "${competition.briefNiceName} - Entry Deleted") { body ->
                body.block {
                    p {
                        +"Despite various reminders, you still do not have sufficient funds in your Agility Plaza account to cover the fees for ${competition.briefNiceName} so your entry has been deleted."
                    }
                    if (competition.isUka && !competition.lateEntryRestricted) {
                        p {
                            +"If you would still like to attend the show, you can enter classes using the 'Pay On The Day' facilities at the show."
                        }
                    }
                    p { +"For your records details of your deleted entry are displayed below." }
                    showEntry(account, competition, this, "Deleted")
                }
            }
        }

        fun entryUserCancelled(competition: Competition, idAccount: Int) {
            val account = Account(idAccount)

            prepareHtml(idAccount, account.emailList, "${competition.briefNiceName} - Entry Cancelled") { body ->
                body.block {
                    p {
                        +"As requested, your entry for ${competition.briefNiceName} has been cancelled. For your records the details were as displayed below."
                    }
                    showEntry(account, competition, this, "Cancelled")
                }
            }
        }

        fun runningOrders(competition: Competition, idAccount: Int) {
            val account = Account(idAccount)
            prepareHtml(idAccount, account.emailList, "${competition.briefNiceName} - Running Orders") { body ->

                body.block {
                    if (competition.importantNote.isNotEmpty()) {
                        p {
                            +"<font color='red'><b>IMPORTANT NOTE: ${competition.importantNote}</b></font>"
                        }
                    }
                    p {
                        +"Running orders have now been prepared for ${competition.briefName} and your's are listed below. "
                        LedgerItem().where("idCompetition=${competition.id} AND idAccount=$idAccount AND type in ($LEDGER_ITEM_POSTAGE, $LEDGER_ITEM_PAPER)", limit = 1) {
                            +"Your show documents will be posted out to you and you should receive these at least 7 days before the show starts. "
                            +"They can also be downloaded - go to "
                            +"'Shows I have entered' and click on ${competition.briefName} then press the 'Documents' button."
                        }.otherwise {
                            +"You have not paid for show documents to be posted. "
                            +"However these can be downloaded - go to "
                            +"'Shows I have entered' and click on ${competition.briefName} then press the 'Documents' button."
                        }

                    }
                    showEntry(account, competition, this, runningOrders = true)
                }

            }
        }

        fun runningOrdersRevised(competition: Competition, idAccount: Int) {
            val account = Account(idAccount)
            prepareHtml(idAccount, account.emailList, "${competition.briefNiceName} - REVISED Running Orders") { body ->

                body.block {
                    p {
                        +"Running orders have been REVISED for ${competition.briefName} and your's are listed below. PLEASE IGNORE ANY PREVIOUS RUNNING ORDERS."
                        LedgerItem().where("idCompetition=${competition.id} AND idAccount=$idAccount AND type in ($LEDGER_ITEM_POSTAGE, $LEDGER_ITEM_PAPER)", limit = 1) {
                            +"Your show documents will be posted out to you and you should receive these at least 7 days before the show starts. "
                            +"They can also be downloaded - go to "
                            +"'Shows I have entered' and click on ${competition.briefName} then press the 'Documents' button."
                        }.otherwise {
                            +"You have not paid for show documents to be posted. "
                            +"However these can be downloaded - go to "
                            +"'Shows I have entered' and click on ${competition.briefName} then press the 'Documents' button."
                        }

                    }
                    showEntry(account, competition, this, runningOrders = true)
                }

            }
        }

        fun ukaLevelChange(
            idAccount: Int,
            programme: Int,
            oldGrade: String,
            newGrade: String,
            dog: Dog,
            json: JsonNode
        ) {

            val programmeText = if (programme == PROGRAMME_PERFORMANCE) "performance" else "steeplechase"

            val account = Account(idAccount)

            if (idAccount > 0 && account.found()) {

                prepareHtml(idAccount, account.emailList, "${dog.petName} - UK Agility Level Change") { body ->

                    body.block {
                        p {
                            if (newGrade > oldGrade) {
                                +"Congratulations our records show that ${dog.cleanedPetName} is now eligible to go "
                            } else {
                                +"Our records show that the level has been changed for ${dog.cleanedPetName} "
                            }
                            +"from ${Grade.getGradeName(oldGrade)} to ${Grade.getGradeName(newGrade)} in the UKA $programmeText programme. "
                            if (json.has("competitions")) {
                                +"As a consequence we have reviewed your show entries and have made the following notes/adjustments:"
                            } else {
                                +" As you do not currently have any UKA entries booked, there are no adjustments to make."
                            }
                        }
                        for (competitionNode in json["competitions"]) {
                            h3 { +competitionNode["name"].asString }
                            when (competitionNode["state"].asString) {
                                "inProgress" -> {
                                    p { +"This show is in progress so we cannot adjust your classes remotely - please report to the show secretary's area and they will adjust your classes on their tablet system." }
                                }
                                "processed" -> {
                                    p { +"This show has been processed so we cannot adjust your classes automatically - please report to the show secretary's area when you arrive and they will adjust your classes on their tablet system." }
                                }
                                else -> {
                                    table {
                                        attributes["border"] = "1"
                                        attributes["cellpadding"] = "5"
                                        attributes["style"] = "border-collapse: collapse;"
                                        tr {
                                            th { +"Date" }
                                            th { +"Class" }
                                            th { +"Action" }
                                        }
                                        val dateMonitor = ChangeMonitor<Date>(nullDate)
                                        for (node in competitionNode["actions"]) {
                                            tr {
                                                if (dateMonitor.hasChanged(node["classDate"].asDate)) {
                                                    td { +node["classDate"].asDate.fullishDate() }
                                                } else {
                                                    td {}
                                                }
                                                td { +node["className"].asString }
                                                td { +node["action"].asString }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            }
        }

        fun campingAllocation(
            account: Account,
            showName: String,
            place: Int,
            fee: Int,
            balance: Int,
            allocationTime: String
        ) {

            prepareHtml(account.id, account.emailList, "$showName - Camping Allocation") { body ->

                body.block {
                    p {
                        +"We are writing to remind you that we will be processing camping applications for $showName $allocationTime.  "
                    }
                    p {
                        +"Priority will be given to show officials and ring parties and then to early bookers in strict "
                        +"chronological order. Camping fees will be deducted from the accounts of successful "
                        +"competitors at this time so can only be offered to those with sufficient funds in their account."
                    }
                    p {
                        +"It is your responsibility to ensure that you have at "
                        +"least ${fee.toCurrency()} in your account on the day, taking into account any show fees that "
                        +"may be deducted for other shows that close beforehand. Your account balance is currently ${balance.toCurrency()}."
                    }
                    p {
                        +"As things stand, you are number ${place} on the camping list."
                    }
                    p {
                        +"Once allocated, camping fees are strictly non-refundable. However if for some reason you "
                        +"are unable to use your pitch, you can give (or sell) it to another competitor. "
                        +"If you no longer require camping then please remove the option from your entry before we do the processing."

                    }
                }
            }
        }

        fun campingConfirmed(account: Account, showName: String, fee: Int) {
            prepareHtml(account.id, account.emailList, "$showName - Camping Confirmed") { body ->
                body.block {
                    p {
                        +"Congratulations your application for camping at $showName has been confirmed and the "
                        +"fee of ${fee.toCurrency()} has been deducted from your account."
                    }
                    p {
                        +"Camping fees are non-refundable. However if for any reason you decide not to camp or "
                        +"wish to cancel your show entry entirely, you may give (or sell) your pitch to another "
                        +"competitor. Please email the show secretary with your account details quoting your "
                        +"account code (${account.code}) and give them details of who will be taking your place."
                    }

                }
            }
        }

        fun campingConfirmedNoFunds(account: Account, showName: String, fee: Int) {
            prepareHtml(account.id, account.emailList, "$showName - Camping Confirmed") { body ->
                body.block {
                    p {
                        +"Congratulations your application for camping at $showName has been confirmed and the "
                        +"fee of ${fee.toCurrency()} will be deducted from your account as soon as there are sufficient funds."
                    }
                    p {
                        +"Once paid, camping fees are non-refundable. However if for any reason you decide not to camp or "
                        +"wish to cancel your show entry entirely, you may give (or sell) your pitch to another "
                        +"competitor. Please email the show secretary with your account details quoting your "
                        +"account code (${account.code}) and give them details of who will be taking your place."
                    }

                }
            }
        }

        fun cancelledShowRefund(idAccount: Int, email: String, showName: String, amount: Int) {
            prepareHtml(idAccount, email, "$showName - Refund") { body ->
                body.block {
                    p {
                        +"We just wanted to let you know that we have now received your refund from the recently "
                        +"cancelled $showName show and ${amount.toCurrency()} has been credited to your Agility "
                        +"Plaza account."
                    }
                }
            }
        }


        fun campingFurtherAllocation(
            account: Account,
            showName: String,
            fee: Int,
            balance: Int,
            places: Int,
            waitingList: Int
        ) {
            prepareHtml(account.id, account.emailList, "$showName - Unable to confirm camping") { body ->
                body.block {
                    p {
                        +"Unfortunately we have been unable to confirm your application for camping at $showName at "
                        +"this time. However we still have $places pitches available and will be doing a further "
                        +"round of allocations shortly. You are currently number $waitingList on the waiting list."
                    }
                    p {
                        +"To participate in the next round you must have at least ${fee.toCurrency()} in your "
                        +"account. Your account balance is currently ${balance.toCurrency()}."
                    }
                }
            }
        }

        fun refundRequest(
            account: Account,
            accountName: String,
            sortCode: String,
            accountNumber: String,
            amount: Int,
            link: String
        ) {
            prepareHtml(account.id, account.emailList, "Refund Request") { body ->
                body.block {
                    p {
                        +"You have requested a refund with details as follows:"
                    }
                    table {
                        tr {
                            td { +"Account Name:" }
                            td { +accountName }
                        }
                        tr {
                            td { +"Sort Code:" }
                            td { +sortCode }
                        }
                        tr {
                            td { +"Account Number:" }
                            td { +accountNumber }
                        }
                        tr {
                            td { +"Amount:" }
                            td { +amount.money }
                        }
                    }
                    p {
                        +"Please check the details carefully and click the button below to confirm your request. You "
                        +"are responsible for ensuring that the bank account information is correct."
                    }
                    p { +"&nbsp;" }
                    p {
                        button(link) { +"Click here to confirm" }
                    }

                }
            }
        }

        fun refundRequested(
            accountName: String,
            amount: Int,
            reference: String
        ) {
            prepare(0, "accounts@agilityplaza.com", "Refund Requested ($accountName)") {
                para { "Please pay ${amount.toCurrency()} to $accountName with reference '$reference'" }
            }
        }

        fun ukaFinalsPreInvite(account: Account, team: Team, event: String, date: Date) {
            prepareHtmlUka(account.id, account.emailList, "Grand Finals Qualification") { body ->
                body.block {
                    p {
                        +"Congratulations to <b>${team.description}</b> for qualifying on ${date.fullDate()} for the <b>$event</b> event at this year's Grand Finals."
                    }
                    p {
                        +"""
                            All processing will be run through Agility Plaza this year, including 
                            clothing selections.  Entries will open on 1st July, at which point you will receive an 
                            email explaining how enter. To book your place you need to ENTER and PAY for the event within 14 days of 
                            receiving that email. If an entry is not confirmed and payment is not received within 14 
                            DAYS OF THE EMAIL BEING SENT the system will cancel the ability for you to enter, you will 
                            forfeit your place at the final and a reserve will be called.
                        """.trimIndent()
                    }
                    p {
                        +"""
                            Please ensure your contact details are up to date on your Agility Plaza account and emails 
                            from donotreply@agilityplaza.com, enquiries@ukagility.com and becky@ukagility.com are set 
                            as trusted senders in your email settings. 
                        """.trimIndent()
                    }
                }
            }

        }

        fun ukaFinalsInvite(account: Account, team: Team, event: String, date: Date) {
            prepareHtmlUka(account.id, account.emailList, "Grand Finals - Invite") { body ->
                body.block {
                    p {
                        +"This invitation is for <b>${team.description}</b> who due to their performance on ${date.fullDate()} are eligible to participate in the <b>$event</b> event at this year's Grand Finals."
                    }
                    p {
                        +"""
                            Agility Plaza is open for online Grand Final entries. To book 
                            your place you need to ENTER and PAY for the event within 14 days of receiving this 
                            email. If an entry is not confirmed and payment is not 
                            received within that period the system will cancel the ability for you to enter, you will 
                            forfeit your place at the final and a reserve will be called. Note that all payments are 
                            non-refundable so your fees and clothing costs will be forfeited if you choose not to attend.
                        """.trimIndent()
                    }
                    p {
                        +"""
                            Entries will be accepted in the normal way using the "Enter a show" button on Agility Plaza.
                        """.trimIndent()
                    }
                    p {
                        +"""
                            Please ensure your contact details are up to date on your Agility Plaza account and emails 
                            from donotreply@agilityplaza.com, enquiries@ukagility.com and becky@ukagility.com are set 
                            as trusted senders in your email settings. 
                        """.trimIndent()
                    }
                }
            }

        }

        fun ukaFinalsCancelled(account: Account, team: Team, event: String) {
            prepareHtmlUka(account.id, account.emailList, "Grand Finals - Cancellation") { body ->
                body.block {
                    p {
                        +"""
                            This is to confirm that the entry for <b>${team.description}</b> in the <b>$event</b> event 
                            at this year's Grand Finals had been cancelled. If you were not expecting this, please 
                            contact UK Agility at the email address below.
                        """.trimIndent()
                    }
                }
            }
        }

        fun ukaFinalsUninvited(account: Account, team: Team, event: String) {
            prepareHtmlUka(account.id, account.emailList, "Grand Finals - Invitation Withdrawn") { body ->
                body.block {
                    p {
                        +"""
                            This is to confirm that the invite for <b>${team.description}</b> to enter the <b>$event</b> 
                            event at this year's Grand Finals had been withdrawn. If you were not expecting this, please 
                            contact UK Agility at the email address below.
                        """.trimIndent()
                    }
                }
            }
        }

        fun ukaFinalsUnCancelled(account: Account, team: Team, event: String) {
            prepareHtmlUka(account.id, account.emailList, "Grand Finals - Entry Restored") { body ->
                body.block {
                    p {
                        +"""
                            This is to confirm that the entry for <b>${team.description}</b> in the <b>$event</b> event 
                            at this year's Grand Finals which had been cancelled, has now been restored. If you were not expecting this, please 
                            contact UK Agility at the email address below.
                        """.trimIndent()
                    }
                }
            }
        }

        fun ukaFinalsReInvited(account: Account, team: Team, event: String) {
            prepareHtmlUka(account.id, account.emailList, "Grand Finals - Invitation Reinstated") { body ->
                body.block {
                    p {
                        +"""
                            This is to confirm that the invitation for <b>${team.description}</b> to enter the <b>$event</b> 
                            event at this year's Grand Finals had been re-instated. If you were not expecting this, please 
                            contact UK Agility at the email address below.
                        """.trimIndent()
                    }
                }
            }
        }

        fun chaseEntries(competition: Competition, idAccount: Int) {
            val account = Account(idAccount)

            prepareHtml(idAccount, account.emailList, "${competition.briefNiceName} - Last Chance to Enter") { body ->

                body.block {
                    p {
                        +"As you entered ${competition.briefName} last year, we thought you would appreciate a reminder that "
                        +"you are running out of time to enter this year as the show will be closing at midnight on ${competition.dateCloses.fullDate()}."
                    }
                }
            }

        }

        fun confirmCruftsTeamDetails(competition: Competition, team: Team, idAccount: Int) {
            val account = Account(idAccount)

            prepareHtml(idAccount, account.emailList, "${competition.briefNiceName} - Crufts Team Entry") { body ->

                body.block {
                    p {
                        +"Below are details of your ${competition.briefName} Crufts Team Qualifier entry. Please check "
                        +"VERY carefully as you may be disqualified if they do not comply with the Kennel Club rules. "
                        +"<b>In particular check the name of the club you are representing as we had to 'guess' some of " 
                        +"these.</b> Also make sure that you have at least 4 dogs and 4 handlers in your team."
                    }
                    p {
                        +"If you need anything corrected, email our support team using the email address above ASAP."
                    }
                    var index = 0
                    table {
                        tr {
                            td { b { +"Team Name:" } }
                            td {
                                if (team.teamName.isEmpty()) +"*** NO TEAM NAME ***" else +team.teamName
                            }
                        }
                        tr {
                            td { b { +"Representing Club:" } }
                            td { 
                                if (team.clubName.isEmpty()) +"*** NO CLUB NAME ***" else +team.clubName 
                            }
                        }
                    }

                    table {
                        attributes["border"] = "1"
                        attributes["cellpadding"] = "5"
                        attributes["style"] = "border-collapse: collapse;"

                        tr {
                            th { +"Role" }
                            th { +"Handler" }
                            th { +"Code" }
                            th { +"Pet Name" }
                            th { +"Registered Name" }
                        }
                        for (member in team.members) {
                            index++
                            val role = if (index <= 4) "Dog $index" else "Reserve ${index - 4}"
                            tr {
                                td { +role }
                                td { +member["competitorName"].asString }
                                td { +member["dogCode"].asString }
                                td { +member["petName"].asString }
                                td { +member["registeredName"].asString }
                            }
                        }
                    }

                }
            }

        }

    }



}


