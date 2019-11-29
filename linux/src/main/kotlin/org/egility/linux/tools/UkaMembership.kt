/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.linux.tools

import org.egility.library.dbobject.Breed
import org.egility.library.dbobject.Dog
import org.egility.library.dbobject.Grade
import org.egility.library.general.*

/**
 * Created by mbrickman on 15/03/18.
 */

object UkaMembership {
    fun exportExcel(): String {
        val path = "${Global.documentsFolder}/uka_membership.xls"
        val workbook = createWorkbook(path)

        var sheetIndex = 0

        val membersSheet = workbook.headedSheet("Members", sheetIndex++, PlazaAdmin.SHEET_UKA_MEMBERS, 1, "Members")
        val dogsSheet = workbook.headedSheet("Dogs", sheetIndex++, PlazaAdmin.SHEET_UKA_DOGS, 1, "Dogs")
        var row = 3
        var column = 0

        with(membersSheet) {
            addHeading(column++, 2, "UKA", 0.6)
            addHeading(column++, 2, "Merged", 1.0)
            addHeading(column++, 2, "Code", 0.8)
            addHeading(column++, 2, "Given Name", 1.5)
            addHeading(column++, 2, "Family Name", 1.5)
            addHeading(column++, 2, "Email", 2.5)
            addHeading(column++, 2, "Mobile", 1.0)
            addHeading(column++, 2, "Phone", 1.0)
            addHeading(column++, 2, "Address", 4.5)
            addHeading(column++, 2, "Registered", 0.85)
            addHeading(column++, 2, "Paid Until", 0.85)
            addHeading(column++, 2, "Dogs", 4.0)
        }
        
        dbQuery("""
                SELECT competitor.*, account.*, json_extract(competitor.extra, "$.uka") AS uka, GROUP_CONCAT(dog.dogCode ORDER BY dog.dogCode) AS dogs, alias.idCompetitor AS aliasId, alias.idUka AS aliasUka
                FROM competitor
                    LEFT JOIN account USING (idAccount)
                    LEFT JOIN dog ON dog.idCompetitor=competitor.idCompetitor AND dog.idUka>0 
                    LEFT JOIN competitor AS alias ON alias.idCompetitor=competitor.aliasFor
                WHERE competitor.idUka BETWEEN 10001 AND 49999 GROUP BY competitor.idUka
            """) {
            val idUka = getInt("idUka")
            val idAccount = getInt("idAccount")
            val givenName = getString("givenName")
            val familyName = getString("familyName")
            val email = getString("email")
            val phoneMobile = getString("phoneMobile")
            val phoneOther = getString("phoneOther")
            val streetAddress = getString("streetAddress")
            val town = getString("town")
            val postcode = getString("postcode")
            val ukaMembershipExpires = getDate("ukaMembershipExpires")
            val competitorCode = getString("competitorCode")
            val uka = Json(getString("uka"))
            val ukaDateConfirmed = uka["dateConfirmed"].asDate
            val dogs = getString("dogs").replace(",", ", ")

            val aliasId= getInt("aliasId")
            val aliasUka= getInt("aliasUka")

            val merged = if (aliasId>0) (if (aliasUka>0) aliasUka.toString() else "AMO") else ""

            row++
            var column = 0
            with(membersSheet) {
                addCell(column++, row, idUka)
                addCell(column++, row, merged)
                if (idAccount > 0) {
                    addCell(column, row, competitorCode)
                }
                column++
                addCell(column++, row, givenName.naturalCase)
                addCell(column++, row, familyName.naturalCase)
                addCell(column++, row, email)
                addCell(column++, row, phoneMobile)
                addCell(column++, row, phoneOther)
                addCell(column++, row, streetAddress.append(town, ", ").append(postcode, ", ").replace("\r\n", ", "))
                addCell(column++, row, ukaDateConfirmed)
                if (ukaMembershipExpires<today) addCell(column++, row, ukaMembershipExpires, WorkbookFormats.default.dateFormatRed) else addCell(column++, row, ukaMembershipExpires)
                addCell(column++, row, dogs)
            }
            if (idUka==25195) row+=3

        }

        with(dogsSheet) {
            column = 0
            addHeading(column++, 2, "UKA", 1.0)
            addHeading(column++, 2, "Merged", 1.0)
            addHeading(column++, 2, "Pet Name", 1.0)
            addHeading(column++, 2, "Registered Name", 3.0)
            addHeading(column++, 2, "Birth", 1.0)
            addHeading(column++, 2, "Breed", 2.5)
            addHeading(column++, 2, "Performance", 1.0)
            addHeading(column++, 2, "Steeplechase", 1.0)
            addHeading(column++, 2, "Measure", 1.0)
            addHeading(column++, 2, "Owner", 1.0)
            addHeading(column++, 2, "Paid", 1.0)
        }


        row = 3
        Dog().join { owner }.where("dog.idUka BETWEEN 1 AND 49999 OR ukaDateConfirmed>0", "dog.idUka") {
            try {
                var merged=""
                if (aliasFor>0) {
                    Dog().seek(aliasFor){merged=if(idUka>0) idUka.toString() else "ANO"}
                }
                row++
                column = 0
                with(dogsSheet) {
                    addCell(column++, row, idUka)
                    addCell(column++, row, merged)
                    addCell(column++, row, petName.naturalCase)
                    addCell(column++, row, registeredName.naturalCase)
                    addCell(column++, row, dateOfBirth)
                    addCell(column++, row, Breed.getBreedName(idBreed, "${idBreed}?"))
                    addCell(column++, row, Grade.getGradeName(ukaPerformanceLevel))
                    addCell(column++, row, Grade.getGradeName(ukaSteeplechaseLevel))
                    addCell(column++, row, ukaMeasuredHeight)
                    addCell(column++, row, owner.idUka)
                    addCell(column++, row, if (ukaDateConfirmed.isNotEmpty()) ukaDateConfirmed.dateText else "")
                }
                if (idUka==33381) row+=3
            } catch (e: Throwable) {
                doNothing()
            }
        }

        workbook.quit()

        return path


    }


}