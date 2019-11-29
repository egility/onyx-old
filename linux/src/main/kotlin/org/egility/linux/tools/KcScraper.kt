package org.egility.linux.tools

import org.egility.library.dbobject.Entity
import org.egility.library.dbobject.KcLicence
import org.egility.library.dbobject.KcShow
import org.egility.library.general.*
import org.jsoup.Jsoup
import java.util.*
import kotlin.collections.ArrayList

object KcScraper {

    fun scrapeShows() {

        val dataMap = HashMap<String, String>()
        val url = "https://www.thekennelclub.org.uk/services/public/findashow/"

        Jsoup.connect(url).run {
            val document = get()
            document.run {
                select("form").forEach { input ->
                    if (input.attr("id").eq("form1")) {
                        input.select("input").forEach {
                            dataMap[it.attr("name")] = it.attr("value")
                        }
                        dataMap["ctl00\$MainContent\$DdlDefaultDistance"] = "0"
                        dataMap["ctl00\$MainContent\$DdlDefaultTime"] = "1y+"
                        dataMap["ctl00\$MainContent\$DdlDefaultShowType"] = "Agility"
                    }
                }
            }
        }


        Jsoup.connect(url).run {
            dataMap.forEach { key, value ->
                data(key, value)
            }
            post().run {
                select("tbody").forEach { tbody ->
                    tbody.select("tr").forEach { tr ->
                        var index = 0
                        var date = nullDate
                        var showName = ""
                        var idLicence = 0
                        var idClub = 0
                        var type = ""
                        var venue = ""
                        var secretary = ""
                        var phone = ""
                        tr.select("td").forEach { td ->
                            when (index) {
                                0 -> date = td.text().toDate("dd/MM/yyyy")
                                2 -> {
                                    showName = td.text()
                                    td.select("a").forEach { a ->
                                        idLicence = a.attr("href").substringAfter("=").toIntDef(0)
                                    }
                                }
                                3 -> type = td.text()
                            }
                            index++
                        }
                        Jsoup.connect("https://www.thekennelclub.org.uk/services/public/findashow/display.aspx?id=$idLicence")
                            .get().run {
                                select("div#MainContent_DivName").forEach {
                                    secretary = it.text()
                                }
                                select("p#MainContent_ContentVenueAddress").forEach {
                                    venue = it.html().replace("<br>", ", ")
                                }
                                select("span#MainContent_LabelPhoneDay").forEach {
                                    phone = it.text()
                                }
                                select("div.SideMenu").forEach {
                                    it.select("a").forEach {
                                        idClub = it.attr("href").substringAfter("=").toIntDef(0)
                                    }
                                }

                            }
                        
                        KcLicence().seekOrAppend("idKcLicence=$idLicence", {id=idLicence}) { 
                            idKcClub = idClub
                            this.type = type
                            this.date = date
                            this.club = showName
                            this.secretary = secretary
                            this.phone = phone
                            this.venue = venue
                            post()
                        }
                    }
                }
            }
        }
    }
    
    fun scrapeClubs() {
        dbQuery("SELECT DISTINCT idKcClub FROM kcLicence WHERE idKcClub>0") { scrapeClub(getInt("idKcClub"), true) }
    }
    
    fun scrapeClub(idKcClub: Int, hasShows: Boolean = true) {
        val url = "https://www.thekennelclub.org.uk/services/public/findaclub/display.aspx?id=$idKcClub"
        var clubName = ""
        var secretary = ""
        var phone = ""
        var tests = ""
        var contact = ""
        var venue = ""
        var activities = ""
        var type = ""
        Jsoup.connect(url).get().run {
            select("h1#MainContent_headClubName").forEach {
                clubName = it.text()
            }
            select("div#MainContent_DivName").forEach {
                secretary = it.text()
            }
            select("span#MainContent_LabelPhoneDay").forEach {
                phone = it.text()
            }
            select("table.intab").forEach {
                val headings=ArrayList<String>()
                it.select("th").forEach {
                    headings.add(it.text())
                }
                if (headings.size>0) {
                    var table = when (headings[0]) {
                        "Tests Conducted" -> "Good Citizen"
                        "Type" -> "Training"
                        else -> ""
                    }
                    var column = 0
                    it.select("tr.lightrow").forEach {
                        it.select(("td")).forEach {
                            if (column < headings.size) {
                                when (headings[column]) {
                                    "Scheme Name" -> doNothing()
                                    "Tests Conducted" -> tests = it.text().replace("<br />", ", ")
                                    "Contact" -> contact = contact.append(it.text().replace("<br />", ", "), ";")
                                    "Venue" -> venue = venue.append(it.text().replace("<br />", ", "), ";")
                                    "Type" -> type = it.text().replace("<br />", ", ")
                                    "Location" -> venue = venue.append(it.text().replace("<br />", ", "), ";")
                                    else -> println("********* ${headings[column]}=${it.text()}")
                                }
                            }
                            column++
                        }
                    }
                }
            }
        }
        Entity().seekOrAppend("idKcClub=$idKcClub OR name=${clubName.quoted} OR legalName=${clubName.quoted}", {this.idKcClub=idKcClub}) {
            this.name = clubName
            this.secretary = secretary
            this.kcContactDetails = contact
            this.phone = phone
            if (hasShows) this.hasShows = true
            //this.venue = venue
            post()
        }
    }
    
    fun processLicences() {
        
        class Show() {
            var idKcClub: Int=-1
            var club: String=""
            var secretary: String=""
            var phone: String=""
            var venue: String=""
            var type: String=""
            var dateStart: Date= nullDate
            var dateEnd: Date= nullDate
            var kcLicenceList: String=""
            
            fun save(){
                if (idKcClub!=-1) {
                    KcShow().seekOrAppend("idKcClub=$idKcClub AND ${dateStart.sqlDate} <= dateEnd AND ${dateEnd.sqlDate} >= dateStart", {}) {
                        idKcClub = this@Show.idKcClub
                        dateStart = this@Show.dateStart
                        dateEnd = this@Show.dateEnd
                        name = this@Show.club
                        secretary = this@Show.secretary
                        phone = this@Show.phone
                        venue = this@Show.venue
                        kcLicenceList = this@Show.kcLicenceList
                        type = this@Show.type
                        post()
                    }
                    
                }
                idKcClub = -1
            }
        }
        
        val show=Show()
        
        KcLicence().where("true", "idKcClub, date") {
            if (show.idKcClub!=idKcClub || date>show.dateEnd.addDays(6)) {
                println("${show.idKcClub}/$idKcClub, ${date.dateText}/${show.dateEnd.dateText}")
                show.save()
                show.idKcClub=idKcClub
                show.club=club
                show.secretary=secretary
                show.phone=phone
                show.venue=venue
                show.type=type
                show.dateStart=date
                show.dateEnd=date
                show.kcLicenceList = id.toString()
            } else {
                show.dateEnd = date
                show.kcLicenceList = show.kcLicenceList.append(id.toString())
                if (!type.split(",").contains(type)) {
                    show.type = show.type.append(type)
                }
            }
        }
        show.save()
    }
    
}