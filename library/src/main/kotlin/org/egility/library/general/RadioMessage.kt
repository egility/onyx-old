/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.library.general

import org.egility.library.dbobject.AgilityClass
import org.egility.library.dbobject.Radio
import java.util.*

/**
 * Created by mbrickman on 30/09/18.
 */


enum class RadioTemplate(var code: Int) {
    UNDEFINED(0),
    WALKING_SHORTLY(10), WALKING_NOW(11), WALKING_OVER_LUNCH(12),
    CALLING_ALL(20), CALLING_FIRST(21), CALLING_TO(22), CALLING_END(23), CALLING_REMAINING(24), CALLING_FINAL(25),
    CLOSING_SHORTLY(30), CLOSED(35),
    LUNCH_BETWEEN(40), CLOSED_FOR_LUNCH(41), NOT_BREAKING(42)

    ;

    val isWalking: Boolean
        get() = (WALKING_SHORTLY..WALKING_OVER_LUNCH).contains(this)
    val isLunch: Boolean
        get() = (LUNCH_BETWEEN..NOT_BREAKING).contains(this)
    val isCalling: Boolean
        get() = (CALLING_ALL..CALLING_FINAL).contains(this)

}

open class RadioMessage(var radioTemplate: RadioTemplate = RadioTemplate.UNDEFINED) {

    var idCompetition: Int = -1
    var ringNumber: Int = -1
    var idAgilityClass: Int = -1
    var heightCode: String = ""
    var heightText: String = ""
    var finalHeight: Boolean = true
    var inMinutes: Int = -1
    var atTime: Date = nullDate
    var resumeTime: Date = nullDate
    var callingTo: Int = 0
    var dogs: Int = -1

    val isFinalHeight: Boolean
        get() = heightCode.isEmpty() || finalHeight


    val text: String
        get() {
            val result = when (radioTemplate) {
                RadioTemplate.UNDEFINED -> heightText
                RadioTemplate.WALKING_SHORTLY -> "Walking shortly$startingClause"
                RadioTemplate.WALKING_NOW -> "Walking now$startingClause"
                RadioTemplate.WALKING_OVER_LUNCH -> "Walking over lunch$startingClause"
                RadioTemplate.CALLING_ALL -> "All $heightPhrase dogs to the ring"
                RadioTemplate.CALLING_FIRST -> "First $dogs $heightPhrase dogs to the ring"
                RadioTemplate.CALLING_TO -> {
                    if (callingTo == 0) {
                        "$heightPhrase calling to ..."
                    } else {
                        "$heightPhrase calling to $callingTo".trim().initialUpper
                    }
                }
                RadioTemplate.CALLING_END -> "$heightPhrase calling to end".trim().initialUpper
                RadioTemplate.CALLING_REMAINING -> "All remaining $heightPhrase dogs to the ring"
                RadioTemplate.CALLING_FINAL -> "Final call for all remaining $heightPhrase dogs"
                RadioTemplate.LUNCH_BETWEEN -> "Breaking for lunch $lunchClause"
                RadioTemplate.CLOSED_FOR_LUNCH -> "Closed for lunch$resumingClause (No Walking)"
                RadioTemplate.NOT_BREAKING -> "Not breaking for Lunch"
                RadioTemplate.CLOSING_SHORTLY -> "$heightPhrase closing in $inMinutes min".trim().initialUpper
                RadioTemplate.CLOSED -> "$heightPhrase is now closed".trim().initialUpper
            }
            return result.replace("  ", " ")
        }

    val startingClause: String
        get() = if (inMinutes > 0)
            ", starting in $inMinutes minutes"
        else if (atTime.isNotEmpty())
            ", starting at ${atTime.timeText}"
        else
            ""

    val resumingClause: String
        get() = if (resumeTime.isNotEmpty()) ", resuming at ${resumeTime.timeText}" else ""

    val lunchClause: String
        get() = if (inMinutes > 0) " in $inMinutes minutes"
        else if (atTime.isNotEmpty() && resumeTime.isNotEmpty()) " between ${atTime.timeText} and ${resumeTime.timeText}"
        else if (atTime.isNotEmpty()) " at ${atTime.timeText}"
        else ""

    val heightPhrase: String
        get() = if (heightText.isNotEmpty()) " $heightText " else ""

    val classHeight: String
        get() = if (isFinalHeight) " class " else heightPhrase

    fun save() {
        dbTransaction {
            val radio = Radio()
            radio.append()
            radio.idCompetition = idCompetition
            radio.ringNumber = ringNumber
            radio.idAgilityClass = idAgilityClass
            radio.messageTemplate = radioTemplate.code
            radio.heightCode = heightCode
            radio.heightText = heightText
            radio.dogs = dogs
            radio.callingTo = callingTo
            radio.atTime = atTime
            radio.resumeTime = resumeTime
            radio.inMinutes = inMinutes
            radio.fullText = text
            radio.post()

            AgilityClass().join { ring }.seek(idAgilityClass) {
                val t = atTime
                if (atTime.isNotEmpty() && !radioTemplate.isLunch) {
                    startTime = atTime
                }
                if (radioTemplate.isLunch) {
                    ring.lunchStart = atTime
                    ring.lunchEnd = resumeTime
                    ring.notBreaking = (radioTemplate == RadioTemplate.NOT_BREAKING)
                    ring.post()
                    if (radioTemplate == RadioTemplate.CLOSED_FOR_LUNCH) {
                        progress = CLASS_CLOSED_FOR_LUNCH
                    }
                }
                if (radioTemplate.isWalking) {
                    walkingOverLunch = radioTemplate == RadioTemplate.WALKING_OVER_LUNCH
                    if (radioTemplate != RadioTemplate.WALKING_SHORTLY) progress = CLASS_WALKING
                }
                if (radioTemplate.isCalling) {
                    val jumpHeightCode =
                        if (this@RadioMessage.heightCode.isNotEmpty()) this@RadioMessage.heightCode else lastHeightCode
                    setHeightCallingTo(jumpHeightCode, callingTo)
                }
                if (radioTemplate == RadioTemplate.CLOSING_SHORTLY) {
                    val jumpHeightCode =
                        if (this@RadioMessage.heightCode.isNotEmpty()) this@RadioMessage.heightCode else lastHeightCode
                    setClosingTime(jumpHeightCode, now.addMinutes(inMinutes))
                }
                if (radioTemplate == RadioTemplate.CLOSED) {
                    val jumpHeightCode =
                        if (this@RadioMessage.heightCode.isNotEmpty()) this@RadioMessage.heightCode else lastHeightCode
                    setAnnouncedClosed(jumpHeightCode)
                }
                post()
            }

        }
    }


    fun clone(newTemplate: RadioTemplate = RadioTemplate.UNDEFINED): RadioMessage {
        val result = RadioMessage(if (newTemplate != RadioTemplate.UNDEFINED) newTemplate else radioTemplate)
        result.idCompetition = idCompetition
        result.ringNumber = ringNumber
        result.idAgilityClass = idAgilityClass
        result.heightCode = heightCode
        result.heightText = heightText
        result.finalHeight = finalHeight
        result.inMinutes = inMinutes
        result.atTime = atTime
        result.resumeTime = resumeTime
        result.callingTo = callingTo
        result.dogs = dogs
        return result
    }
}



