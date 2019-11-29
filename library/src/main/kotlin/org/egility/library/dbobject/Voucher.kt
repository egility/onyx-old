/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.library.dbobject

import org.egility.library.database.*
import org.egility.library.general.*
import java.util.*

open class VoucherRaw<T : DbTable<T>>(_connection: DbConnection? = null, vararg columnNames: String) : DbTable<T>(_connection, "voucher", *columnNames) {

    open var id: Int by DbPropertyInt("idVoucher")
    open var idCompetition: Int by DbPropertyInt("idCompetition")
    open var idAccount: Int by DbPropertyInt("idAccount")
    open var code: String by DbPropertyString("voucherCode")
    open var instances: Int by DbPropertyInt("instances")
    open var instancesUsed: Int by DbPropertyInt("instancesUsed")
    open var type: Int by DbPropertyInt("type")
    open var validFrom: Date by DbPropertyDate("validFrom")
    open var validTo: Date by DbPropertyDate("validTo")
    open var flags: Int by DbPropertyInt("flags")
    open var description: String by DbPropertyString("description")
    open var extra: Json by DbPropertyJson("extra")
    open var dateCreated: Date by DbPropertyDate("dateCreated")
    open var deviceCreated: Int by DbPropertyInt("deviceCreated")
    open var dateModified: Date by DbPropertyDate("dateModified")
    open var deviceModified: Int by DbPropertyInt("deviceModified")

    open var memberRates: Boolean by DbPropertyJsonBoolean("extra", "memberRates")
    open var allRunsFree: Boolean by DbPropertyJsonBoolean("extra", "allRunsFree")
    open var allCampingFree: Boolean by DbPropertyJsonBoolean("extra", "allCampingFree")
    open var freeRuns: Int by DbPropertyJsonInt("extra", "freeRuns")
    open var campingCredit: Int by DbPropertyJsonInt("extra", "campingCredit")
    open var campingNightsFree: Int by DbPropertyJsonInt("extra", "campingNightsFree")
    open var generalCredit: Int by DbPropertyJsonInt("extra", "generalCredit")
    open var campingPermit: Boolean by DbPropertyJsonBoolean("extra", "campingPermit")
    open var campingPriority: Boolean by DbPropertyJsonBoolean("extra", "campingPriority")
    open var ringPartyName: String by DbPropertyJsonString("extra", "ringPartyName")

    val competition: Competition by DbLink<Competition>({ Competition() })


}

class VoucherBenefits() {

    var memberRates: Boolean = false
    var allRunsFree: Boolean = false
    var allCampingFree: Boolean = false
    var freeRuns: Int = 0
    var campingCredit: Int = 0
    var campingNightsFree: Int = 0
    var generalCredit: Int = 0
    var campingPermit: Boolean = false
    var campingPriority: Boolean = false

    val asJson: Json
        get() {
            val result = Json()
            result["memberRates"] = memberRates
            result["allRunsFree"] = allRunsFree
            result["allCampingFree"] = allCampingFree
            result["freeRuns"] = freeRuns
            result["campingCredit"] = campingCredit
            result["campingNightsFree"] = campingNightsFree
            result["generalCredit"] = generalCredit
            result["campingPermit"] = campingPermit
            result["campingPriority"] = campingPriority
            return result
        }
}

class Voucher(vararg columnNames: String) : VoucherRaw<Voucher>(null, *columnNames) {

    constructor(idVoucher: Int) : this() {
        find(idVoucher)
    }

    fun seekByCode(idCompetition: Int, voucherCode: String, body: (Voucher.() -> Unit)? = null): Voucher {
        val voucher = Voucher()
        return voucher.seek("idCompetition=$idCompetition AND voucherCode=${voucherCode.quoted}", body) as Voucher
    }

    fun generateCode() {
        if (code.isEmpty()) {
            var suggestion = generateBaseCode(6)
            do {
                val query = DbQuery("SELECT voucherCode FROM voucher WHERE voucherCode = ${suggestion.quoted}")
                if (!query.found()) {
                    code = suggestion
                } else {
                    suggestion = generateBaseCode(6)
                }
            } while (code.isEmpty())
        }
    }

    val specification: String
        get() {
            var result = ""
            if (memberRates) result = result.append("Members rates", " & ")
            if (allRunsFree) result = result.append("Free runs", " & ")
            if (allCampingFree) result = result.append("Free camping", " & ")
            if (freeRuns == 1) result = result.append("$freeRuns free run", " & ")
            if (freeRuns > 1) result = result.append("$freeRuns free runs", " & ")
            if (campingCredit > 0) result = result.append("${campingCredit.toCurrency()} camping credit", " & ")
            if (campingNightsFree == 1) result = result.append("$campingNightsFree night free camping", " & ")
            if (campingNightsFree > 1) result = result.append("$campingNightsFree nights free camping", " & ")
            if (generalCredit > 0) result = result.append("${generalCredit.toCurrency()} credit", " & ")
            if (campingPermit) result = result.append("Camping permit", " & ")
            if (campingPriority) result = result.append("Priority camping", " & ")
            return result
        }


    companion object {

        fun select(where: String, orderBy: String = "", limit: Int = 0): Voucher {
            val voucher = Voucher()
            voucher.select(where, orderBy, limit)
            return voucher
        }

        fun getBenefits(voucherList: String): VoucherBenefits {
            val result = VoucherBenefits()
            if (voucherList.isNotEmpty()) {
                Voucher().where("voucherCode IN (${voucherList.listToQuotedList()})") {
                    if (memberRates) result.memberRates = true
                    if (allRunsFree) result.allRunsFree = true
                    if (allCampingFree) result.allCampingFree = true
                    if (freeRuns > 0) result.freeRuns += freeRuns
                    if (campingCredit > 0) result.campingCredit += campingCredit
                    if (campingNightsFree > 0) result.campingNightsFree += campingNightsFree
                    if (generalCredit > 0) result.generalCredit += generalCredit
                    if (campingPermit) result.campingPermit = true
                    if (campingPriority) result.campingPriority = true
                }
            }
            return result
        }

    }
}