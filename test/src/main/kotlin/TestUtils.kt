import TestShow.baseDate
import org.egility.library.api.ApiUtils
import org.egility.library.database.DbJdbcConnection
import org.egility.library.database.DbQuery
import org.egility.library.dbobject.*
import org.egility.library.dbobject.LedgerItem
import org.egility.library.general.*
import org.egility.linux.tools.NativeUtils
import org.egility.linux.tools.PlazaAdmin
import org.egility.linux.tools.PlazaData
import org.egility.linux.tools.UkaAdmin
import java.util.*
import kotlin.collections.ArrayList

object TestUtils {
    fun t() {
        when (Test.QUICK) {
            Test.QUICK -> {
                println(6)
                //   generateIndHeights(1211403141)
                //uploadRunningOrders(1466784191)
                //   addExtra()
                //   generateRawTable("height")
                //    quick()
                //     KcAccountPaperwork(1087321845).export()
                //   KcAccountPaperwork(2050106425).export()
                //    allocateShowPayment(145460, 1816611119)
            }
            Test.HOLD -> {
                //   testNedlo()
                //  printResults(1477184376, finalize = true)
                //   fixRegistration()
//            Reports.printResults(1723988386, subResultsFlag = 1+2+4+8)
                //   splitClassByGrade(2099560997, 3, "KC05")

                //    addExtra()
                //   generateAcuDat()
                //  KcAccountPaperwork(1928723416).export(markFirst = false, all=true)
                //    KcAccountPaperwork(1761752493).export()
                //   KcAccountPaperwork(1802400721).export()
                //  fuelPayment("BBR-7236-EZZ", 2000)
                //  setUpTryOutGamblers(2033936567)
                //  fixRegistration()
                //  populateJuniorLeague()
                //   KcAccountPaperwork(1120514657, notes = "/home/mbrickman/Documents/Sandstone/Show Documents/Blackdown Sept 19.pdf").export()
                //KcDocuments(WKC).champCatalogues(marked=false)
                //  champRunningOrders(WKC)
                //  clivePost(2950)
                //  quick()
                //  println(arp().toJson(pretty = true))
                // checkShowActions()
                //SysUtils.process("/home/mbrickman/acu010.log")

                // fixGames()
                //   addAccountTransfer("MBR-9283-MYG", "MNE-1467-YWP", 3000)
                //  runningOrders(1319178624)
                // KcAccountPaperwork(1502532547).quick()
                //  EntryFormReport.generate(1679974589)
                //closeShowNow(Competition(1221785713))
                // KcAccountPaperwork(1502532547).exportItineraries()
                //     KcAccountPaperwork(1968772086).export(markFirst = false, all=true)
                //   fixRegistration()
                //  KcAccountPaperwork(1823617200, notes = "/home/mbrickman/Documents/Sandstone/Show Documents/Chatsworth Saturday.pdf").export(markFirst = false)
                //KcAccountPaperwork(1170965083).export()
                //  importWorkbook("/home/mbrickman/Downloads/K9Brat19_template.xls", force = true)
                //      checkSubClasses(1928723416)
                //  fixRegistration()
                //  splitFromAccount("KM6823")
                //val x=execStr("bash -c 'realpath /sys/class/net/wl* | grep usb | rev | cut -d\"/\" -f1 | rev'")
                //  val x=execStr("bash -c 'realpath /sys/class/net/wl* | grep -v usb | rev | cut -d\"/\" -f1 | rev'")
                // println(x)
                // quick()
                //  CallingListReport.generate(TUFFLEY)
                //  deleteEntry(2012423074, "TST-1844-YAS")
                //  deleteEntry(2012423074, "RBA-8514-AUW")
                //  runningOrders(1594112541)
                //  generateMacList()
                // EmergencyScoreReport.generate(1312363294)
                //Starling.processTransactions(today.addYears(-2))
                //  KcDocuments(1312363294).champCatalogues(marked=true)
                //   CampingListReport.generate(1312363294)
                //  payKevin(1679974589, 165000)
                //  fixRegistration()
                // closeShow(Competition(2065223521))
                //splitFromAccount("JS2539")
                //  printResults(1974548706, finalize = true)
                // printResults(1139796804, finalize = true)
                //jsonCompileTest()

                //  runningOrders(1140258180, pdf=true)
                //  do_simulateCruftsTeam(WRAXALL, host = "acu240.local")
                //    addAssets()
                //     KcAccountPaperwork(2037643434).export()
            }
            Test.RAW -> {
                generateRawTable("z_agilitynetshow")
            }
            else -> {
                doNothing()
            }
        }
    }


    fun deDuplicate() {
        Global.databaseHost = LIVE
        //PlazaData.deDuplicateAccountDogs(1905091660)
        PlazaData.deDuplicateName("Jenny ", "Guscott")
    }

    fun deDuplicateName(givenName: String, familyName: String) {
        Global.databaseHost = LIVE
        PlazaData.deDuplicateName(givenName, familyName)
    }

    fun deDuplicateDogs(accountCode: String) {
        Global.databaseHost = LIVE
        Account.select("accountCode=${accountCode.quoted}").withFirst {
            PlazaData.deDuplicateAccountDogs(it.id)
        }
    }


    fun tidyAccount(idAccount: Int) {
        Global.databaseHost = LIVE
        val c = Competitor.select("idAccount = $idAccount")
        c.forEach {
            DbQuery("SELECT GROUP_CONCAT(idCompetitor) AS list FROM competitor WHERE aliasFor=${c.id}").withFirst {
                val list = it.getString("list")
                if (list.isNotEmpty()) {
                    dbExecute("UPDATE dog SET idCompetitor=${c.id}, idAccount=$idAccount WHERE idCompetitor IN ($list)")
                }
            }
        }
        PlazaData.deDuplicateAccountDogs(idAccount)
    }


    fun waverunners() {
        Global.databaseHost = LIVE
        Global.allEmailsTo = ""
        setDebugExcludeClasses("*")
//    dbExecute("truncate emailqueue")
        Ledger().join { account }.where("idCompetition=$WAVE AND type IN ($LEDGER_ENTRY_FEES, $LEDGER_ENTRY_FEES_PAPER)") {
            PlazaMessage.prepare(idAccount, account.emailList, "Waverunners - Date Change to 15/16th Sept") {
                para {
                    "In case you have not heard already, we are emailing everyone who has entered Waverunners to tell them that the " +
                            "show is being brought forward one week from 22/23 Sept to 15/16 Sept. This is due to a major cycling " +
                            "event that will cause widespread traffic disruption over the original weekend."
                }
                para {
                    "Your entry will be moved automatically to the new date. If you can’t attend you can cancel your " +
                            "entry by selecting 'Shows I have entered' and clicking on 'Waverunners' and then the " +
                            "'Cancel Entry' button. If you need help, please email support (address below)."
                }
            }

        }
        PlazaAdmin.dequeuePlaza()
    }

    fun sherborne() {
        Global.databaseHost = LIVE
        Global.allEmailsTo = ""
        setDebugExcludeClasses("*")
        //  dbExecute("truncate emailqueue")
        Ledger().join { account }
            .where("idCompetition=$SHERBORNE AND type IN ($LEDGER_ENTRY_FEES, $LEDGER_ENTRY_FEES_PAPER)") {
                PlazaMessage.prepare(idAccount, account.emailList, "Sherborne Vale - Important Information") {
                    para {
                        "We are writing to you because you entered the Sherborne Vale DTC agility show this Sunday. If you " +
                                "do not follow our facebook group, you may not be aware that due to an administrative error " +
                                "we did not receive the correct ring plan for the show. As a result we published the one " +
                                "we did receive and mailed out personal itineraries based on this incorrect information. We " +
                                "now have the correct order of classes and have updated Agility Plaza accordingly."
                    }
                    para {
                        "If you downloaded your show documents or a ring plan BEFORE TODAY (Wednesday 18th July), please " +
                                "download again. If you received a personal itinerary in the post last week, please " +
                                "disregard. We are in the process of mailing out revised personal itineraries with the " +
                                "correct order of classes. In case there is any doubt the correct mail out will be " +
                                "marked 'REVISED Personal Itinerary'."
                    }
                }

            }
        PlazaAdmin.dequeuePlaza()
    }


    fun revisedRunningOrders(competition: Competition, idAccount: Int) {
        val account = Account(idAccount)
        PlazaMessage.prepareHtml(
            idAccount,
            account.emailList,
            "${competition.briefNiceName} - Revised Running Orders"
        ) { body ->

            body.block {
                p {
                    +"We have decided to alter the way the YKC classes will be run so that all three heights will run as a "
                    +"single class (with separate results) rather than as three classes (a, b & c). As a consequence we have "
                    +"had to re-generate the running orders for YKC. As your household is affected we are sending you a new "
                    +"set of running orders."
                }
                PlazaMessage.showEntry(account, competition, this, runningOrders = true)
            }

        }
    }

    fun starling2() {
        Global.databaseHost = LIVE
        Global.allEmailsTo = ""
        //  Starling.refundAccount(Account.codeToId("MBR-9283-MYG"), "Mike Brickman", "602127", "54725259", 1)
    }

    fun showAdvance(idCompetition: Int, amount: Int) {
        Global.databaseHost = LIVE
//    Global.allEmailsTo = ""
        BankPaymentRequest.showAdvance(idCompetition, amount)
//    dequeuePlaza()
    }

    fun starlingAddPayee(idCompetiton: Int = 1962675019) {
        Global.databaseHost = LIVE
        Competition().seek(idCompetiton) {
            Starling.findOrAddPayee(bankAccountName, bankAccountNumber, bankAccountSort)
        }
    }

    fun addExtra() {
        Global.databaseHost = LIVE
/*
    CompetitionExtra().withAppendPost {
        idCompetition = 2116305137
        ledgerItemType = LEDGER_ITEM_ADMISSION_TICKET
        description = "Additional (Non-Handler) Tickets"
        unitPrice = 2100
        needsQuantity = true
    }
    CompetitionExtra().withAppendPost {
        idCompetition = 1823617200
        ledgerItemType = LEDGER_ITEM_ADMISSION_TICKET
        description = "Additional (Non-Handler) Tickets"
        unitPrice = 2100
        needsQuantity = true
    }
        CompetitionExtra().withAppendPost {
        idCompetition = 1211403141
        ledgerItemType = LEDGER_ITEM_MEAL_TICKET
        description = "Additional Meal Tickets"
        unitPrice = 300
        needsQuantity = true
    }

 */

        CompetitionExtra().withAppendPost {
            idCompetition = 2116305137
            ledgerItemType = LEDGER_ITEM_ADMISSION_TICKET
            description = "Additional (Non-Handler) Tickets"
            unitPrice = 900
            needsQuantity = true
        }
        CompetitionExtra().withAppendPost {
            idCompetition = 2116305137
            ledgerItemType = LEDGER_ITEM_ADMISSION_TICKET_CHILD
            description = "Additional (Child) Tickets"
            unitPrice = 500
            needsQuantity = true
        }
        CompetitionExtra().withAppendPost {
            idCompetition = 2116305137
            ledgerItemType = LEDGER_ITEM_ADMISSION_TICKET_FAMILY
            description = "Additional (Family) Tickets"
            unitPrice = 3500
            needsQuantity = true
        }

    }

    fun checkCamping() {
        //Global.databaseHost = LIVE
        Camping().join { competition }.join { account }.join { account.competitor }
            .where("NOT freeCamping AND dateStart>curDate() and NOT rejected", "dateStart, camping.idCompetition") {
                if (fee != calculateFee2()) {
                    println("${competition.uniqueName} ${account.code} ${account.competitor.email} $fee, ${calculateFee2()}")
                }
            }
    }

    fun tuffleyHelpers() {
        // Global.databaseHost = LIVE
        CompetitionCompetitor().where("idCompetition=1312363294") {
            var fri = Json("{\"date\":\"2019-07-05\"}") as JsonNode
            var sat = Json("{\"date\":\"2019-07-06\"}") as JsonNode
            var sun = Json("{\"date\":\"2019-07-07\"}") as JsonNode
            for (day in helpDays) {
                if (day["date"].asString == "2019-07-29") {
                    day["date"] = "2019-07-06"
                }
                when (day["date"].asDate.dayOfWeek()) {
                    6 -> fri = day
                    7 -> sat = day
                    1 -> sun = day
                    else -> {
                        println(day.toJson())
                    }
                }
            }
            helpDays.clear()
            helpDays.addElement(fri)
            helpDays.addElement(sat)
            helpDays.addElement(sun)
            post()
        }
    }

    fun deleteEntry(idCompetition: Int, accountCode: String) {
        Global.databaseHost = LIVE
        val competition = Competition(idCompetition)
        competition.cancelEntry(Account.codeToId(accountCode), reasonDeleted = ENTRY_DELETED_SHOW_SECRETARY)
    }

    fun fabFunds() {
        Global.databaseHost = LIVE
        setDebugExcludeClasses("*")
        Competition().where("uniqueName LIKE '%FAB%'") {
            println(uniqueName)
            bankAccountName = "Miss D E Weave"
            bankAccountNumber = "202662"
            bankAccountSort = "70927554"
            post()
            PlazaAdmin.releaseFunds(this)
        }
    }

    fun quick() {
        Global.databaseHost = LIVE
        voidRun(34568, 2013615093)
    }

    fun fixMembership() {
        Global.databaseHost = LIVE
        Global.allEmailsTo = ""
        setDebugExcludeClasses("*")
        Ledger().join { account }.where("type=300 and idLedger between 118911 and 129382") {
            charge = amount
            amount = 0
            post()
            payFromFunds(Ledger.balance(idAccount))
            if (amountOwing > 0) {
                val items = ArrayList<String>()
                println("$id, ${account.code}, $amountOwing")
                LedgerItem().where("idLedger=$id") {
                    items.add("$description (${amount.toCurrency()})")
                    if (type == 120) {
                        println("   $type, $description, $idDog")
                        Dog().seek(idDog) {
                            ukaDateConfirmed = nullDate
                            extra["uka"].clear()
                            post()
                        }
                    }
                    if (type == 110) {
                        println("   $type, $description, $idCompetitor")
                        Competitor().seek(idCompetitor) {
                            ukaMembershipExpires =
                                if (description.contains("Join")) nullDate else ukaMembershipExpires.addYears(-5).addDays(-1)
                            if (description.contains("Join")) ukaDateConfirmed = nullDate
                            extra["uka"].clear()
                            post()
                        }
                    }
                }
                PlazaMessage.ukaRegistrationCorrection(idAccount, Account(idAccount).emailList, charge, Ledger.balance(idAccount), items)
            }
        }
    }

    fun donation() {
        Global.databaseHost = LIVE
//    accountDonation(Account.codeToId("MBR-9283-MYG"), "M Brickman", "608371", "75934134", 1, "Mike's test charity")
        BankPaymentRequest.accountDonation(Account.codeToId("TST-1844-YAS"), "T. Kenward", "601648", "84049596", 5380, "Barney's Small Breed Rescue")
        BankPaymentRequest.accountDonation(Account.codeToId("RBA-8514-AUW"), "T. Kenward", "601648", "84049596", 3260, "Barney's Small Breed Rescue")
    }

    fun clivePost(amount: Int) {
        Global.databaseHost = LIVE
        Ledger.addMiscExpense(Account.codeToId("CHI-8021-XFU"), amount, LEDGER_POSTAGE)
    }

    fun fuelPayment(accountCode: String, amount: Int) {
        Global.databaseHost = LIVE
        Ledger.addMiscExpense(Account.codeToId(accountCode), amount, LEDGER_FUEL)
    }

    fun quick929() {
        Global.databaseHost = LIVE
        Competition().seek(1466784191) {
            society = "Bretford DTS"
            secretary =
                "Graham Taylor, WKC 2 Medley Grove Leamington Spa CV31 2GA, countryshowsagility@gmail.com, Tel 01926 315335"
            guarantors = """Chair: Dawn Bott, Birchley Wood Farmhouse, Brinklow Road, Coventry, CV3 2AB 
Hon Treasurer: Patrice Taylor 47, Westlea Road, Leamington Spa, CV31 3JJ 
Comm. Member: Jane Cockburn, Hargreaves Hse, Church Clse, Bishops Itchington, CV47 2QH Comm. Member: Julie Whitney, 7 Bentley Close, Leamington Spa, CV32 7SR 
Comm. Member: Di Joyce, 2 Pound Way, Southam, Warcs 
Hon.Sec: Graham Taylor, 2 Medley Grove, Royal Leamington Spa, CV31 2GA, 01926 315335           
        """.replace("\n", ";")
            parkingPermit = true
            post()
        }
    }

    fun payKevin(idCompetition: Int, amount: Int) {
        Global.databaseHost = LIVE
        BankPaymentRequest.showThirdPartyAdvance(idCompetition, amount, "K T Lampitt", "404410", "01167715", "On Behalf of FAB")
    }


    fun syncRunningOrders() {
        Global.databaseHost = "10.8.1.1"

        val local = DbJdbcConnection(Sandstone(databaseHost = "localhost"))

        val entry = Entry(_connection = local)
        Entry().where("idAgilityClass IN (1600643687,1907151184,1321381099,1765278036)") {
            println("${cursor + 1} of $rowCount")
            if (entry.find(id)) {
                runningOrder = entry.runningOrder
                post()
            }
        }
    }

    fun releaseFunds(idCompetition: Int) {
        Global.databaseHost = LIVE
        PlazaAdmin.releaseFunds(Competition(idCompetition))
    }

    fun forceWorkbook() {
        Global.databaseHost = LIVE
        PlazaAdmin.importWorkbook("/home/mbrickman/Downloads/Copy of FAB_Wilts1_19_template1.xls", force = true)
    }


    fun fixMeasurements() {
        Global.databaseHost = LIVE
        Measurement().join { dog }.where("measurement.dateCreated>'2019-05-31'") {
            if (dog.ukaMeasure3 == dog.ukaMeasure2) {
                val measure1 = dog.ukaMeasure1
                val measure2 = dog.ukaMeasure1
                dog.ukaMeasure.clear()
                dog.ukaMeasure1 = measure1
                dog.ukaMeasure2 = measure2
            }
            if (dog.ukaMeasure2 == dog.ukaMeasure1) {
                val measure1 = dog.ukaMeasure1
                dog.ukaMeasure.clear()
                dog.ukaMeasure1 = measure1
            }
            dog.post()
        }
    }

    fun resetMeasuring() {
        Global.databaseHost = "10.8.1.13"
        Measurement().where("idCompetition=1097575704") {
            val dog = Dog(idDog)
            val measure1 = dog.ukaMeasure1
            dog.ukaMeasure.clear()
            if (measure1 != value) dog.ukaMeasure1 = measure1
            dog.post()
            delete(reposition = false)
        }
    }


    fun gf() {
        println(PlazaAdmin.exportUkaFinalsInvited())
        //   importWorkbook("/data/e-gility/documents/finals_invites.xls")
        //   println(exportUkaFinalsInvites())
    }


    fun testEmails() {
        Global.allEmailsTo = ""
        PlazaAdmin.dequeuePlaza()
    }

    fun quick22() {
        Global.databaseHost = "10.8.1.26"
        AgilityClass().join { competition }
            .where("agilityClass.idCompetition=1868263964", "classDate, agilityClass.idCompetition") {

                //        .where("idOrganization=1 and classDate>'2019-02-01'", "classDate, agilityClass.idCompetition") {

                val jumpDelimiter = if (jumpHeightCodes.contains(";")) ";" else ","

                var _jumpHeightCodes = ""
                var _heightRunningOrder = ""
                val jumpHeights = ArrayList<String>()
                for (height in heightOptions.split(",")) {
                    val options = height.substringAfter(":").split("|")
                    for (option in options) {
                        if (!jumpHeights.contains(option)) {
                            jumpHeights.add(option)
                        }
                    }
                }

                var lowerFirst = ""
                var lowerLast = ""

                Collections.sort(jumpHeights) { a, b -> a.compareTo(b) }
                jumpHeights.forEach { _jumpHeightCodes = _jumpHeightCodes.append(it, jumpDelimiter) }
                jumpHeights.forEach { lowerLast = lowerLast.commaAppend(it) }
                Collections.sort(jumpHeights) { a, b -> b.compareTo(a) }
                jumpHeights.forEach { lowerFirst = lowerFirst.commaAppend(it) }

                when (competition._lhoOrder) {
                    "first" -> _heightRunningOrder = lowerFirst
                    "last" -> _heightRunningOrder = lowerLast
                    else -> {
                        if (_heightRunningOrder != lowerFirst && _heightRunningOrder != lowerLast) {
                            _heightRunningOrder = lowerLast
                        }
                    }
                }

                jumpHeightCodes = _jumpHeightCodes
                heightRunningOrder = _heightRunningOrder
                post()

            }
    }

    fun restoreDeletedEntry(idCompetition: Int, accountCode: String) {
        Global.databaseHost = LIVE
        val idAccount = Account.codeToId(accountCode)
        val webTransaction = WebTransaction()
        webTransaction.seekEntry(idAccount, idCompetition)
        WebTransaction.confirm(webTransaction)
        if (webTransaction.found()) {
            Entry().join { agilityClass }
                .where("entry.idAccount=$idAccount AND agilityClass.idCompetition=$idCompetition") {
                    runningOrder = agilityClass.nextRunningOrder(jumpHeightCode)
                    runningOrderJumpHeightCode = jumpHeightCode
                    post()
                }
        }
    }

    fun fixRunningOrders(idCompetition: Int, accountCode: String) {
        Global.databaseHost = LIVE
        val idAccount = Account.codeToId(accountCode)
        Entry().join { agilityClass }.where("entry.idAccount=$idAccount AND agilityClass.idCompetition=$idCompetition") {
            if (runningOrder == 0) {
                runningOrder = agilityClass.nextRunningOrder(jumpHeightCode)
                runningOrderJumpHeightCode = jumpHeightCode
                post()
            }
        }
    }

    fun quick9() {
        setDebugExcludeClasses("*")
        Entry().join { agilityClass }.join { team }.join { team.dog }.join { team.competitor }.join { team.dog.breed }
            .where("agilityClass.classNumber=7 and agilityClass.idCompetition=2098150906", "givenName, familyName") {
                if (team.dog.breed.kcGroup == "Gundog Group" || team.dog.idBreed == 1006) {
                    println("$teamDescription (${team.dog.registeredName}) - ${team.dog.breed.name}")
                }
            }
    }

    fun quick2() {
        dbExecute("truncate emailqueue")
        dbExecute("truncate mutex")

        Dog().where("idUka=26154") {
            processUkaLevel(PROGRAMME_PERFORMANCE, "UKA04")
        }

        PlazaAdmin.dequeuePlaza()
    }


    fun revertGrade(idCompetition: Int, dogCode: Int, grade: Int, date: String = "2018-05-05") {
        Global.databaseHost = LIVE
        Global.allEmailsTo = ""

        //dbExecute("truncate emailqueue")
        Dog().seek("dogCode=$dogCode") {
            kcSetGradeCode("KC0$grade", date.toDate())
            post()
            CompetitionDog().where("idCompetition=$idCompetition AND idDog=$id") {
                processKcGradeChange("KC0${grade - 1}", force = true)
            }
        }
        PlazaAdmin.dequeuePlaza()
    }

    fun forceGrade(idCompetition: Int, dogCode: Int, grade: Int) {
        Global.databaseHost = LIVE
        Global.allEmailsTo = ""

        //dbExecute("truncate emailqueue")
        Dog().seek("dogCode=$dogCode") {
            post()
            CompetitionDog().where("idCompetition=$idCompetition AND idDog=$id") {
                processKcGradeChange("KC0${grade}", force = true)
            }
        }
        PlazaAdmin.dequeuePlaza()
    }

    fun quick99() {
        Global.databaseHost = SHOW
        setDebugExcludeClasses("*")

        Team().where("idAgilityClass=1451616327") {
            if (member(1)["dogCode"].asInt == 24539) {
                member(2)["idDog"] = Dog.codeToIdDog(24582)
            }
            if (member(1)["dogCode"].asInt == 32947) {
                member(2)["idDog"] = Dog.codeToIdDog(32407)
                member(2)["heightCode"] = "UKA550"
            }
            member(2)["idCompetitor"] = Dog(member(2)["idDog"].asInt).idCompetitor
            refreshMembers()
            if (member(1)["dogCode"].asInt == 32407) {
                member(2)["heightCode"] = "UKA550"
            }
            post()
        }


        printEntries(1451616327)

    }

    fun void() {
        Global.databaseHost = LIVE
        UkaAdmin.voidRuns(44284, "UKA04", steeplechase = false)
        UkaAdmin.voidRuns(44284, "UKA04", steeplechase = true)
        UkaAdmin.voidRuns(78557, "UKA04", steeplechase = false)
        UkaAdmin.voidRuns(78557, "UKA04", steeplechase = true)
    }

    fun voidRun(dogCode: Int, idAgilityClass: Int) {
        Global.databaseHost = LIVE
        UkaAdmin.voidRun(dogCode, idAgilityClass)
    }


    fun gradeChanges(idCompetition: Int) {
//    setDebugExcludeClasses("*")
        val c = Competition(idCompetition)
        c.checkGradeChanges()
        //Global.databaseHost = LIVE
    }


    fun quick72() {
        Ledger().where("type=$LEDGER_PAPER_ENTRY_CHEQUE") {
            val chequeAmount = amount
            Ledger().seek("idCompetition=$idCompetition AND idAccount=$idAccount AND type=$LEDGER_ENTRY_FEES_PAPER") {
                paperCheque = chequeAmount
                post()
            }
        }
        Ledger().where("type=$LEDGER_PAPER_ENTRY_CASH") {
            val cashAmount = amount
            Ledger().seek("idCompetition=$idCompetition AND idAccount=$idAccount AND type=$LEDGER_ENTRY_FEES_PAPER") {
                paperCash = cashAmount
                post()
            }
        }

    }
/*
#'HXTEPV','WBGDNB','KAXEUB','DSJBSY','DZWPDP','KWYJDS','RGKKCF','UMKGWT','YTMDYH'
#HXTEPV,WBGDNB,KAXEUB,DSJBSY,DZWPDP,KWYJDS,RGKKCF,UMKGWT,YTMDYH
#1823862657,1321068036,1600193003,2091973478,1378522209,1853700400,2130884775,1790156779,1293260555;
#1412364417,2038664833,2132148343
 */

    fun payAllOverdues() {
        Global.databaseHost = LIVE
        DbQuery(
            """
        SELECT
            Ledger.idAccount, Ledger.amount, t.balance
        FROM
            Ledger
                JOIN
            (SELECT
                idAccount,
                    SUM(IF(credit = 1000, amount, - amount)) AS balance
            FROM
                ledger
            WHERE
                NOT pending
                    AND (debit = 1000 OR credit = 1000)
            GROUP BY idAccount) AS t USING (idAccount)
        WHERE
            Ledger.dateEffective < CURDATE()
                AND Ledger.pending
                AND Ledger.amount <= t.balance
    """
        ).forEach {
            Ledger.payOverdue(it.getInt("idAccount"))
        }
    }

    fun quickMail() {
        val a = Account()
        a.select(
            """
        registrationComplete
            AND (postcode LIKE 'TR%'
            OR postcode LIKE 'PL%'
            OR postcode LIKE 'EX%'
            OR postcode LIKE 'TQ%'
            OR postcode LIKE 'TA%'
            OR postcode LIKE 'DT%'
            OR postcode LIKE 'BA%'
            OR postcode LIKE 'BS%'
            OR postcode LIKE 'SP%'
            OR postcode LIKE 'BH%')
        """
        )
    }

    fun quickRegistration(code: String, email: String) {
        Global.databaseHost = LIVE
        Competitor.select("competitorCode=${code.quoted}").withFirst {
            val token = it.generateRegistrationToken(email)
            println(it.fullName)
            println("http://agilityplaza.com/register_uka?token=$token")
        }
    }


    fun ykc() {
        val agilityClass = AgilityClass()
        agilityClass.competition.joinToParent()
        agilityClass.select("classCode BETWEEN ${ClassTemplate.KC_YKC_AGILITY_U12.code} AND ${ClassTemplate.KC_YKC_TEAM.code} && classDate>curdate()")
        while (agilityClass.next()) {
            println(agilityClass.competition.name)
            agilityClass.ageBaseDate = agilityClass.competition.dateStart
            agilityClass.post()
        }
    }

    fun surchargeCalc() {
        for (pence in 1..100000) {
            val surcharge = calcSurcharge(pence, 20, 0.01)
            val fee = calcFee(pence + surcharge, 20, 0.01)
            if (pence + surcharge - fee != pence)
                println("price=$pence, surchage=$surcharge, fee=$fee, receive=${pence + surcharge - fee}")
        }
    }


    fun do_granite_test(show: String) {
        Global.databaseHost = "acu240.local"
        val control = Control()
        control.find(1)
        if (control.found()) {
            when (show) {
                "blackdown" -> {
                    control.idCompetition = 1152565374
                    control.effectiveDate = "2017-09-09".toDate()
                }
                "blackdownSUN" -> {
                    control.idCompetition = 1152565374
                    control.effectiveDate = "2017-09-10".toDate()
                }
                "base" -> {
                    control.idCompetition = 611
                    control.effectiveDate = "2017-09-09".toDate()
                }
                "aa" -> {
                    control.idCompetition = 605
                    control.effectiveDate = "2017-09-01".toDate()
                }
            }
        }
        control.post()
    }

    fun doNetworkCheck(host: String = "10.8.1.31", brief: Boolean = false) {
        Global.databaseHost = LIVE
        setDebugExcludeClasses("*")
        NetworkCheck.load(host, brief)
        println(NetworkCheck.log)
    }


/*
fun addTuppy() {
    showLateEntry(1744817404, 1394955945, 50, "KC350", ENTRY_IMPORTED_LIVE)
    showLateEntry(1744817404, 1394955945, 51, "KC350", ENTRY_IMPORTED_LIVE)
    showLateEntry(1744817404, 1394955945, 52, "KC350", ENTRY_IMPORTED_LIVE)
    swapRunningOrders(1744817404)
}
*/

    fun doTrophies() {
        println(PlazaAdmin.exportPlaceSheet(1693336254))
        //importPlacesSheet("/home/mbrickman/Downloads/places_SouthDevon0617.xls")
    }

    fun getAverageLoad(): Int {
        val load = fileToString("/proc/loadavg").split(" ")
        val mins5 = (load[1].toDoubleDef(0.0) / 4.0 * 100.0).toInt()
        return mins5
    }

    fun endOfDayReport() {
        val day = CompetitionDay()
        day.seek(548, today.addDays(0))
        day.print(false)
    }

    fun doReportClasses(classes: String) {
        val agilityClass = AgilityClass()
        agilityClass.select("idCompetition=1724374455 and classNumber IN ($classes)")
        while (agilityClass.next()) {
            Reports.printResults(agilityClass.id, true);
        }
    }

    fun test() {
        println(realToday.toString())
    }

    fun test2() {
        for (i in 1..20) {
            DbQuery("select * from agilityClass where idCompetition=516 and classDate=\"2016-04-30\" order by classDate, ringNumber, ringOrder")
        }
    }

    fun generateRawTable(tableName: String) {
        Global.connection._getTableSchema(tableName, "sandstone")?.generateRawCode()
    }

    fun tidyCompetition(idCompetition: Int) {
        val competition = Competition()
        competition.find(idCompetition)
        competition.tidy()
    }

    fun closeForEntries(idAgilityClass: Int) {
        var agilityClass = AgilityClass()
        agilityClass.find(idAgilityClass)
        if (agilityClass.found()) {
            agilityClass.entriesClosed()
        }
    }

    fun closeClass(idAgilityClass: Int) {
        var agilityClass = AgilityClass()
        agilityClass.find(idAgilityClass)
        if (agilityClass.found()) {
            agilityClass.closeClass(true)
        }
    }

    private fun generateResults(idAgilityClass: Int, finalize: Boolean = false) {
        if (finalize) {
            AgilityClass.doFinalizeClass(idAgilityClass);
        }
        Reports.printResults(idAgilityClass, false);

    }

    private fun runningOrders(idAgilityClass: Int, pdf: Boolean = false, tournament: Boolean = false) {
        Reports.printRunningOrders(idAgilityClass, pdf, tournament = tournament);

    }

    private fun entryList(idAgilityClass: Int) {
        Reports.printEntries(idAgilityClass, false);

    }

    private fun fixRelayHeightCodes(idAgilityClass: Int) {
        val agilityClass = AgilityClass()
        agilityClass.find(idAgilityClass)
        if (agilityClass.found()) {
            agilityClass.fixRelayHeightCodes()
        }
        Reports.printRunningOrders(idAgilityClass, true)
    }


    private fun prepareClass(idAgilityClass: Int) {
        val agilityClass = AgilityClass()
        agilityClass.find(idAgilityClass)
        if (agilityClass.found()) {
            agilityClass.prepareClass()
        }
        Reports.printRunningOrders(idAgilityClass, true)
    }


    fun fixQualifier(idAgilityClass: Int = 1090587961) {
        //Global.databaseHost = LIVE

        AgilityClass().where("idAgilityClassParent=$idAgilityClass") {
            finalizeClass()
        }
        printResults(idAgilityClass, finalize = true)
    }

    private fun finalizeClass(idAgilityClass: Int) {
//    Global.databaseHost="10.8.1.35"
        val agilityClass = AgilityClass()
        agilityClass.find(idAgilityClass)
        if (agilityClass.found()) {
            agilityClass.finalizeClass()
        }
    }


    private fun quickFix() {
    }

    private fun printEntries(idAgilityClass: Int) {
        Reports.printEntries(idAgilityClass, false)
    }


    fun printResults(idAgilityClass: Int, finalize: Boolean = false, tournament: Boolean = false) {
        Reports.printResults(idAgilityClass, false, finalize = finalize, tournament = tournament);
    }


    fun printAwards(idAgilityClass: Int) {
        Reports.printAwards(idAgilityClass, false);
    }


    fun jsonTest() {

        val json = Json("{}")
        debug("json", json.toJson(compact = true))

    }


    fun setupSubResults() {
        val agilityClass = AgilityClass()
        agilityClass.select("idAgilityClassParent>0")
        while (agilityClass.next()) {
            val entry = Entry()

            entry.select("entry.idAgilityClass=${agilityClass.id}")
            while (entry.next()) {
                if (entry.progress != PROGRESS_TRANSFERRED) {
                    entry.UpdateParentSubResult(agilityClass.idAgilityClassParent, agilityClass)
                }

            }
        }
    }


    fun testMoveUp() {
        val dog = Dog()
        dog.find("idUka", 28521)
        dog.moveToGradeUkaAtShow("UKA04", 2, 1)
    }

    fun createSpecialClasses() {
        /*
        createSplitPairs(597, 2017, 4, 23) // Agility Antics
        createSplitPairs(593, 2017, 6, 18) // Aldon
        createSplitPairs(603, 2017, 7, 8) // Just Sox
        createSplitPairs(590, 2017, 7, 9) // LAPS
        createSplitPairs(601, 2017, 7, 15) // Captain Jacks
        createSplitPairs(602, 2017, 8, 12) // Agility Rocks Teejay

        createTeam(587, 2017, 6, 3) // Quad Paws
        createTeam(595, 2017, 6, 24) // GT
        createTeam(603, 2017, 7, 9) // Just Sox
        createTeam(599, 2017, 7, 22) // RUFFS
        createTeam(602, 2017, 8, 13) // Agility Rocks Teejay

        createGrandPrix(588, 2017, 5, 21) // Red Run
        createGrandPrix(594, 2017, 6, 11) // Sutton Weavers
        createGrandPrix(593, 2017, 6, 17) // Aldon
        createGrandPrix(595, 2017, 6, 25) // GT
        createGrandPrix(601, 2017, 7, 16) // Captain Jack's
        createGrandPrix(602, 2017, 8, 12) // Agility Rocks Teejay

        createChallenge(588, 2017, 5, 20) // Red Run
        createChallenge(587, 2017, 5, 28) // Quad Paws
        createChallenge(584, 2017, 6, 3) // Agility Rocks Devon
        createChallenge(592, 2017, 6, 17) // MADS
        createChallenge(596, 2017, 7, 16) // Agility Antics


        createSplitPairs(606, 2017, 8, 27) //Hawbridge
        createSplitPairs(608, 2017, 9, 2) //Crooked Oak
        createTeam(609, 2017, 9, 3) // Agility Antics
        createChallenge(608, 2017, 9, 3) // Crooked Oak



        createSplitPairs(611, 2017, 9, 9) //UKA BASE
        createChallenge(611, 2017, 9, 9) // UKA BASE


        createTeam(615, 2017, 9, 16) // Lechlade
        createGrandPrix(615, 2017, 9, 16) // Lechlade


        createTeam(605, 2017, 9, 3) // Lechlade




        createBeginnersHeat(586, 2017, 5, 27) // Phoenix
        createBeginnersHeat(595, 2017, 6, 24) // GT Agility
        createBeginnersHeat(590, 2017, 7, 8) // LAPS
        createBeginnersHeat(599, 2017, 7, 23) // RUFFS
        createBeginnersHeat(602, 2017, 8, 13) // Agility Rocks Teejay
        createBeginnersHeat(605, 2017, 9, 2) // Agility Antics
        createBeginnersHeat(606, 2017, 8, 20) // Hawbridge

        createTryout(604, 2017, 9, 22, 27237)
        */

    }


    fun doKCClassTest(idCompetition: Int) {
        Global.databaseHost = "10.8.1.26"
        val agilityClass = AgilityClass()
        val entry = Entry()
        agilityClass.select("idCompetition=$idCompetition", "classNumber")
        while (agilityClass.next()) {
            entry.select("idAgilityClass=${agilityClass.id}")
            while (entry.next()) {
                entry.subClass = agilityClass.chooseSubClass(entry.gradeCode, entry.heightCode, entry.jumpHeightCode, 0)
                entry.post()
            }
        }
    }


/*
fun doGradeChange(registeredName: String, gradeCode: String, vararg classNumbers: Int) {
    val dog = Dog()
    dog.select("registeredName=${registeredName.quoted} AND dataProvider='SWAP'")
    if (dog.found()) {
        showGradeChange(1724374455, dog.id, gradeCode, *classNumbers)
    }
}

fun gradeChanges() {
    setDebugExcludeClasses("*")
    doGradeChange("Devongem Freekin Wild", "KC06", 7, 6, 11)
    doGradeChange("So Goode Mini Me The Minx", "KC04", 23, 21, 25)

    doGradeChange("Hawksflight The Phoenix", "KC04", 5, 4, 10, 28)
    doGradeChange("Shadowsquad Noble Jack", "KC03", 4, 3, 9, 27)
    doGradeChange("Magical Trevor", "KC04", 5, 4, 10, 28)
    doGradeChange("Aedan Small Talk", "KC03", 20, 24, 31)
    doGradeChange("Penny Bowes Valarie", "KC06", 15, 16, 18, 30)
    doGradeChange("Baby Bel Bonkers Boo", "KC04", 23, 21, 25, 32)
    doGradeChange("Little Miss Blue Skye Aw(b)", "KC04", 0)
    doGradeChange("Once You Pop", "KC02", 0)
//    doAdjust("Princess Chertsey", 35)

}
*/

/*
fun entryChanges() {
    showWithdrawAll(1724374455, 1275918234)
    showLateEntry(1724374455, 1189693862, 35, "KC650")
    showListDogEntries(1724374455, 1189693862)
}
*/

    fun listChanges() {
        fun doList(registeredName: String) {
            val dog = Dog()
            dog.select("registeredName=${registeredName.quoted} AND dataProvider='SWAP'")
            if (dog.found()) {
//            kcListDogEntries(1724374455, dog.id)
            }
        }

        setDebugExcludeClasses("*")

        doList("Devongem Freekin Wild")
        doList("So Goode Mini Me The Minx")

        doList("Hawksflight The Phoenix")
        doList("Shadowsquad Noble Jack")
        doList("Magical Trevor")
        doList("Aedan Small Talk")
        doList("Penny Bowes Valarie")
        doList("Baby Bel Bonkers Boo")
        doList("Little Miss Blue Skye Aw(b)")
        doList("Once You Pop")
        doList("Princess Chertsey")

    }

    fun listEntries() {
        setDebugExcludeClasses("*")

        val dogs = "1543252064,1201541166,1920820274,1850584117,1173595062".split(",")
        for (idDog in dogs) {
            //kcListDogEntries(1724374455, idDog.toInt())

        }
    }

    fun fixGamblers() {
//    val entry=Entry()
//    entry.select("idAgilityClass=1308110541")
//    while (entry.next()) {
//        entry.scoreCodes = entry.scoreCodes.replace(OBSTACLE_6, OBSTACLE_4).replace(OBSTACLE_12, OBSTACLE_6)
//        entry.post()
//    }
        generateResults(1308110541, true)
    }

/*fun fixRegistration(temp: Int = 94054, permanent: Int = 31403) {
    val dog = Dog()
    dog.find("idUka", temp)
    val dogTo = dog.id
    var competitorTo = dog.idCompetitor
    competitorTo = 1994168847 // already de-duplicated
    dog.delete()
    dog.find("idUka", permanent)
    val dogFrom = dog.id
    val competitorFrom = dog.idCompetitor
    Dog.renumber(dogFrom, dogTo)
    Competitor.renumber(competitorFrom, competitorTo)
}*/

    fun generateResults(idCompetition: Int) {
        setDebugExcludeClasses("*")

        println(NativeUtils.zipCompetitionResults(idCompetition))

    }

    fun finalizeResults(idCompetition: Int) {
        val agilityClass = AgilityClass()
        agilityClass.select("idCompetition=$idCompetition")
        while (agilityClass.next()) {
            if (agilityClass.progress == CLASS_CLOSED) {
                agilityClass.finalizeClass()
            }
        }
    }

    fun checkSubClasses(idCompetition: Int) {
        Global.databaseHost = LIVE

        setDebugExcludeClasses("*")
        var count = 0
        val entry = Entry()
        entry.agilityClass.joinToParent()
        entry.select("agilityClass.idCompetition=$idCompetition", "classDate, classCode, agilityClass.gradeCodes, entry.idAgilityClass")
        while (entry.next()) {
            count++
            val proposed =
                entry.agilityClass.chooseSubClass(entry.gradeCode, entry.heightCode, entry.jumpHeightCode, entry.subDivision)
            if (proposed != entry.subClass) {
                println("${entry.idAgilityClass}, ${entry.agilityClass.date.shortText}, ${entry.agilityClass.name}, ${entry.gradeCode}, ${entry.heightCode}, ${entry.jumpHeightCode}, ${entry.subDivision}, ${entry.subClass} => $proposed (${entry.place})")
                entry.subClass = proposed
                entry.post()
            }
        }
        println(count)
    }

    fun checkSubClassesFab(idCompetition: Int) {
        Global.databaseHost = "10.8.1.27"

        setDebugExcludeClasses("*")
        var count = 0
        val entry = Entry()
        entry.agilityClass.joinToParent()
        entry.select("agilityClass.idCompetition=$idCompetition", "classDate, classCode, agilityClass.gradeCodes, entry.idAgilityClass")
        while (entry.next()) {
            count++
            val proposed =
                entry.agilityClass.chooseSubClass(if (entry.agilityClass.gradeCodes.contains(",")) entry.gradeCode else entry.agilityClass.gradeCodes, entry.heightCode, entry.jumpHeightCode, entry.subDivision)
            if (proposed != entry.subClass) {
                println("${entry.idAgilityClass}, ${entry.agilityClass.date.shortText}, ${entry.agilityClass.name}, ${entry.gradeCode}, ${entry.heightCode}, ${entry.jumpHeightCode}, ${entry.subDivision}, ${entry.subClass} => $proposed (${entry.place})")
                entry.subClass = proposed
                entry.post()
            }
        }
        println(count)
    }

    fun moveRunningOrder(idEntry: Int, newRunningOrder: Int) {
        val entry = Entry()
        entry.agilityClass.joinToParent()
        entry.team.joinToParent()
        entry.team.dog.joinToParent()
        entry.team.competitor.joinToParent()

        entry.find(idEntry)
        if (entry.found()) {
            println("${entry.agilityClass.name}: ${entry.competitorName} & ${entry.dogName} (${entry.jumpHeightText}) moved from r/o ${entry.runningOrder} to $newRunningOrder")
            entry.moveToRunningOrder(newRunningOrder)
        }

    }

    fun simulateGrandFinals(host: String = "localhost") {
        Global.databaseHost=host
        Global.testMode = true
        Global.alwaysToPdf = true
        Global.runningOrderCopies = 1

        TestShow.setup(GRAND_FINALS, "2018-10-27")

        closeForEntries(MASTERS)
        TestShow.simulateRing(1)
        TestShow.closeClass(1)

        closeForEntries(GAMES_CHALLENGE)
        TestShow.simulateRing(1)
        TestShow.closeClass(1)

        closeForEntries(SPLIT_PAIRS)
        TestShow.simulateRing(1)
        TestShow.closeClass(1)

        // Gamblers
        TestShow.simulateRing(1)
        TestShow.closeClass(1)

        // Masters Agility
        TestShow.simulateRing(1)
        TestShow.closeClass(1)

        closeForEntries(BEGINNERS_STEEPLECHASE_SEMI_FINAL)
        TestShow.simulateRing(1)
        TestShow.closeClass(1)

        closeForEntries(CIRCULAR_KNOCKOUT)
//    TestShow.simulateRing(1)
        TestShow.closeClass(1)

        // Steeplechase Final
        TestShow.simulateRing(1)
        TestShow.closeClass(1)

        TestShow.setup(GRAND_FINALS, "2018-10-28")

        closeForEntries(CHALLENGE_FINAL)
        TestShow.simulateRing(1)
        TestShow.closeClass(1)

        closeForEntries(JUNIOR_MASTERS)
        TestShow.simulateRing(1)
        TestShow.closeClass(1)

        // Challenge Agility
        TestShow.simulateRing(1)
        TestShow.closeClass(1)

        // Junior Masters Agility
        TestShow.simulateRing(1)
        TestShow.closeClass(1)

        closeForEntries(GRAND_PRIX_SEMI_FINAL)
        TestShow.simulateRing(1)
        TestShow.closeClass(1)

        closeForEntries(TEAM)
        TestShow.simulateRing(1)
        TestShow.closeClass(1)

        // Grand Prix Final
        TestShow.simulateRing(1)
        TestShow.closeClass(1)

        // Team Relay
        TestShow.simulateRing(1)
        TestShow.closeClass(1)

    }


    fun do_simulateSw(host: String = "localhost") {
        Global.alwaysToPdf = true
        Global.runningOrderCopies = 1
        Global.databaseHost = host

        TestShow.setup(SW_FINALS, "2018-02-25")

        closeForEntries(SW_P2)
        TestShow.simulateRing(1)
        TestShow.closeClass(1)

        closeForEntries(SW_P1)
        TestShow.simulateRing(1)
        TestShow.closeClass(1)

        closeForEntries(SW_GAMES)
        TestShow.simulateRing(2)
        TestShow.closeClass(2)

        closeForEntries(SW_ST1)
        TestShow.simulateRing(2)
        TestShow.closeClass(2)

        closeForEntries(SW_ST2)
        TestShow.simulateRing(2)
        TestShow.closeClass(2)

        //gambler
        TestShow.simulateRing(3)
        TestShow.closeClass(3)

        TestShow.simulateRing(3)
        TestShow.closeClass(3)

        TestShow.simulateRing(3)
        TestShow.closeClass(3)

        TestShow.simulateRing(3)
        TestShow.closeClass(3)

        TestShow.simulateRing(3)
        TestShow.closeClass(3)

    }


    fun do_bmark() {
        TestShow.testMasters()
    }

    fun do_sumulate_masters() {

        var MASTERS = 1816243659

        Global.runningOrderCopies = 1
        TestShow.setup(LYDIARD, "2019-03-30")

        closeForEntries(MASTERS)
        TestShow.simulateRing(6)
        TestShow.closeClass(6)
        TestShow.simulateRing(6)
        TestShow.closeClass(6)
        TestShow.simulateRing(6)
        TestShow.closeClass(6)
    }

    fun do_sumulate_gt_team() {

        var TEAM = 2123998252
        val GT = 1279284822

        Global.runningOrderCopies = 1
        TestShow.setup(GT, "2019-04-07")

        closeForEntries(TEAM)
        TestShow.closeClass(5)
/*
    TestShow.simulateRing(5)
    TestShow.closeClass(5)
    TestShow.simulateRing(5)
    TestShow.closeClass(5)

 */
    }

/*fun do_sumulate_ks_challenge() {

    var KS = 1962675019
    var jumping = 1913224766
    var agility = 1261913947
    var heat = 1414257513

    AgilityClass().seek(jumping) {
        ringNumber = 3
        ringOrder = 1
        post()
    }
    AgilityClass().seek(1261913947) {
        ringNumber = 3
        ringOrder = 2
        post()
    }

    Ring().withAppendPost {
        idCompetition = KS
        date = "2019-03-30".toDate()
        number = 3
        judge = "Dalton Meredith"
        heightCode = "KC650"
        idAgilityClass = jumping
    }

    // dbExecute("DELETE FROM entry WHERE idAgilityClass=$agility")

    Global.runningOrderCopies = 1
    TestShow.setup(KS, "2019-03-30")

    TestShow.simulateRing(3)
    TestShow.closeClass(3)
    TestShow.simulateRing(3)
    TestShow.closeClass(3)
}*/


    fun do_simulateDay1(host: String = "localhost") {
        Global.runningOrderCopies = 1
        Global.databaseHost = host

        TestShow.setup(GRAND_FINALS, "2017-11-25")

/*
    //Challenge Jumping
    closeForEntries(CHALLENGE_FINAL)
    TestShow.simulateRing(1)
    TestShow.closeClass(1)

    //Junior Open Jumping
    closeForEntries(JUNIOR_OPEN)
    TestShow.simulateRing(1)
    TestShow.closeClass(1)

    //Challenge Agility
    TestShow.simulateRing(1)
    TestShow.closeClass(1)

*/
        //Junior Open Agility
        TestShow.simulateRing(1)
        TestShow.closeClass(1)

        //Team Individual
        closeForEntries(TEAM)
        TestShow.simulateRing(1)
        TestShow.closeClass(1)

        //Grand Prix Semi-Final
        closeForEntries(GRAND_PRIX_SEMI_FINAL)
        TestShow.simulateRing(1)
        TestShow.closeClass(1)

        //Team Relay
        TestShow.simulateRing(1)
        TestShow.closeClass(1)

        //Grand Prix Final
        TestShow.simulateRing(1)
        TestShow.closeClass(1)

    }

    fun do_simulateDay2(host: String = "localhost") {
        Global.runningOrderCopies = 1
        Global.databaseHost = host

        TestShow.setup(GRAND_FINALS, "2017-11-26")

        //Masters Jumping
        closeForEntries(MASTERS)
        TestShow.simulateRing(1)
        TestShow.closeClass(1)

        //Games Challenge Snooker
        closeForEntries(GAMES_CHALLENGE)
        TestShow.simulateRing(1)
        TestShow.closeClass(1)

        //Split Pairs
        closeForEntries(SPLIT_PAIRS)
        TestShow.simulateRing(1)
        TestShow.closeClass(1)

        //Games Challenge Gamblers
        TestShow.simulateRing(1)
        TestShow.closeClass(1)

        //Masters Agility
        TestShow.simulateRing(1)
        TestShow.closeClass(1)

        //Beginners Steeplechase Semi
        closeForEntries(BEGINNERS_STEEPLECHASE_SEMI_FINAL)
        TestShow.simulateRing(1)
        TestShow.closeClass(1)

        //Circular Knockout
        closeForEntries(CIRCULAR_KNOCKOUT)
        TestShow.closeClass(1)

        //Beginners Steeplechase Final
        TestShow.simulateRing(1)
        TestShow.closeClass(1)
    }

    fun do_simulate() {
        Global.databaseHost = "acu240.local"
        val FAB = 2143917698

        Global.runningOrderCopies = 1

        TestShow.setup(FAB, "2019-05-04")

        loop(1) {
            TestShow.simulateRing(1)
            //    TestShow.closeClass(1)
        }

    }

/*fun openDocumentTest() {
    Global.testMode = true
    //Global.alwaysToPdf = true

    val query =
        DbQuery("SELECT GROUP_CONCAT(DISTINCT idAgilityClass) AS list FROM entry JOIN agilityClass USING (idAgilityClass) WHERE idCompetition=607 AND classCode<9000 AND classCode<>${ClassTemplate.TEAM.code}")
    query.first()

    val entry = Entry()
    entry.agilityClass.joinToParent()
    entry.team.joinToParent()
    entry.team.dog.joinToParent()
    entry.team.competitor.joinToParent()
    entry.select(
        "entry.idAgilityClass IN (${query.getString("list")}) AND progress<${PROGRESS_REMOVED}",
        "agilityClass.classDate, agilityClass.ringOrder, classCode, FIND_IN_SET(entry.jumpHeightCode, agilityClass.heightRunningOrder), entry.runningOrder"
    )

    var idAgilityClass = 0
    var jumpHeightCode = ""

    while (entry.next()) {
        if (entry.idAgilityClass != idAgilityClass) {
            idAgilityClass = entry.idAgilityClass
            jumpHeightCode = ""
            println(entry.agilityClass.name)
        }
        if (entry.jumpHeightCode != jumpHeightCode) {
            jumpHeightCode = entry.jumpHeightCode
            println("  " + entry.jumpHeightText)
        }
        println("    ${entry.runningOrder} ${entry.team.getTeamDescription(entry.teamMember, extended = true)}")
    }
}*/


/*
fun fixTeam() {

    dbExecute("update entry join agilityClass using (idAgilityClass) join team using (idTeam) set teamType=30 where agilityClass.classCode=150")
    dbExecute("update entry join agilityClass using (idAgilityClass) join team using (idTeam) set teamType=40 where agilityClass.classCode=160")
    dbExecute("update entry join agilityClass using (idAgilityClass) join team using (idTeam) set teamType=50 where agilityClass.classCode in (1061, 1062, 1063)")

    dbExecute("""
        update team left join competitor using (idCompetitor) set competitorName="" where competitorName<>"" and teamType<=20
        and replace(competitorName, ' ', '') = replace(concat(givenName, familyName), ' ', '')
    """)

    dbExecute("""
        update team left join competitor using (idCompetitor) set teamType=0 where competitorName="" and teamType<=20
    """)

    dbExecute("""
        update team left join competitor using (idCompetitor) set teamType=20 where competitorName<>"" and teamType<=20
    """)

    dbExecute("""
        update team left join dog using (idDog) set teamType=10 where team.idCompetitor<>dog.idCompetitor
    """)

}

fun deDuplicateTeam() {

    fun deDuplicateList(where: String, groupBy: String) {
        val query = DbQuery("select group_concat(idTeam) as list, count(*) as total from team where $where group by $groupBy having total>1")
        while (query.next()) {
            val list = query.getString("list")
            val first = list.split(",")[0]
            dbExecute("UPDATE entry SET idTeam=$first WHERE idTeam IN ($list)")
            dbExecute("DELETE FROM team WHERE idTeam IN ($list) AND NOT idTeam=$first")
        }
    }


    deDuplicateList("teamType=0", "idCompetitor, idDog")
    deDuplicateList("teamType=20", "idDog, competitorName")

    dbExecute("update team set teamCode = 0")
    dbExecute("update team join dog using (idDog) set teamCode = dog.idUka where teamType=0")
}


fun TeamsAndPairs() {
    val team = Team()
    team.select("teamType>=$TEAM_UKA_SPLIT_PAIR")
    while (team.next()) {
        when (team.type) {
            TEAM_UKA_SPLIT_PAIR -> team.classCode = ClassTemplate.SPLIT_PAIRS.code
            TEAM_UKA_TEAM -> team.classCode = ClassTemplate.TEAM.code
            TEAM_KC_PAIR -> team.classCode = ClassTemplate.KC_PAIRS_MIXI.code
        }
        if (team.type == TEAM_UKA_TEAM) {
            team.teamName = team.name
        }
        Team.updateMemberNode(team.member(1), idDog = team.idDog, idCompetitor = team.idCompetitor, heightCode = team.heightCode, competitorName = team.competitorName)
        Team.updateMemberNode(team.member(2), idDog = team.idDog2, idCompetitor = team.idCompetitor2, heightCode = team.heightCode2, competitorName = team.competitorName2)
        if (team.type == TEAM_UKA_TEAM) {
            Team.updateMemberNode(team.member(3), idDog = team.idDog3, idCompetitor = team.idCompetitor3, heightCode = team.heightCode3, competitorName = team.competitorName3)
        }
        team.type = TEAM_MULTIPLE
        team.refreshMembers()
        team.post()
    }
}

*/

    fun allocateReceipt(idLedger: Int, code: String, wrongReference: Boolean = true) {
        Global.databaseHost = LIVE
        Global.allEmailsTo = ""

        val account = Account()
        if (account.seekCode(code)) {
            Ledger.allocateElectronicReceipt(idLedger, ACCOUNT_USER, account.id, 0, wrongReference)
        }
    }

    fun allocateShowReceipt(idLedger: Int, idCompetition: Int) {
        Global.databaseHost = LIVE
        Global.allEmailsTo = ""
        Ledger.allocateElectronicReceipt(idLedger, ACCOUNT_SHOW_HOLDING, 0, idCompetition)
    }


    fun allocateShowPayment(idLedger: Int, idCompetition: Int) {
        Global.databaseHost = LIVE
        Global.allEmailsTo = ""
        Ledger.allocateElectronicPayment(idLedger, ACCOUNT_SHOW_HOLDING, 0, idCompetition)
    }

    fun addAccountTransfer(fromCode: String, toCode: String, amount: Int) {
        Global.databaseHost = LIVE
        Global.allEmailsTo = ""
        Ledger.addAccountTransfer(Account.codeToId(fromCode), Account.codeToId(toCode), amount)
    }

    fun stripRefund(code: String, amount: Int, fee: Int, card: String) {
        // stripRefund("AOX-2703-TFM", 4500, 0, "Visa ending 7143")


        Global.databaseHost = LIVE
        Global.allEmailsTo = ""

        val account = Account()
        if (account.seekCode(code)) {
            Ledger.addStripeRefund(account.id, amount, fee, card)
        }
    }

/*
fun fixClassNames(idCompetition: Int) {
    val c = Competition.select("idOrganization=2 AND dateStart >= CURDATE()")
    while (c.next()) {
        println(c.name)
        val a = AgilityClass.select("idCompetition=${c.id}")
        while (a.next()) {
            a.name = a.shortDescription
            a.nameLong = a.description
            a.post()
        }
    }
}
*/

/*fun removeDuplicateTeams() {
    val q =
        DbQuery("select teamType, idCompetitor, idDog, competitorName, count(*) as total, group_concat(idTeam) as list from team where teamType<30 group by teamType, idCompetitor, idDog, competitorName having total>1")
    while (q.next()) {
        val first = q.getString("list").substringBefore(",")
        val rest = q.getString("list").substringAfter(",")
        dbExecute("UPDATE entry SET idTeam=$first WHERE idTeam IN ($rest)")
        dbExecute("DELETE FROM team WHERE idTeam IN ($rest)")
    }
}*/


    fun fixRegistration() {
        Global.databaseHost = LIVE
        // Global.databaseHost = "10.8.1.30"

//    "select * from account where idAccount in (1936275397, 1485190658)"

        dbTransaction {
            // Account.merge(good = Account.codeToId(""), bad = 1894590707)
            //  Account.merge(good = 1980867482, bad = 1674209473)
            //UkaImport.deDupeCompetitors(2004668629)
            //UkaImport.deDupeDogs(2004668629)
            // Competitor.merge("NB1079", "NB8752", "NB3712")
            //Competitor.merge("HL5638", "HL1762")
            // Dog.renumberLinked(1291858228, 1381782409)


            Dog.merge(12165, 1754489061)
            //Dog.merge(326, 6055, 54020)
        }

    }

    fun addMissingStripePayments() {
        Global.databaseHost = LIVE

/*
    Ledger.addStripePayment(Account.codeToId("CLA-8236-AWN"), 2355, "2018-05-16".toDate(), "ch_1CSNqcCH2tJOrkrBixTWXb3f", "card_1CSNpTCH2tJOrkrBu1bTctGc", "Visa", "7317", 53, 2302, "2018-05-23".toDate())
    Ledger.addStripePayment(Account.codeToId("ACA-7902-XCK"), 2084, "2018-09-13".toDate(), "ch_1D9xnKCH2tJOrkrB9F57iEGz", "card_1D9xnFCH2tJOrkrBAy0YUL5l", "Visa", "3014", 49, 2035, "2018-09-20".toDate())
    Ledger.addStripePayment(Account.codeToId("ACU-8403-ACG"), 629, "2018-12-27".toDate(), "ch_1Dm8h5CH2tJOrkrBIF4qMOB3", "card_1Dm8h0CH2tJOrkrBGRNHaSiO", "Visa", "4513", 29, 600, "2019-01-03".toDate())
    Ledger.addStripePayment(Account.codeToId("MAF-6127-FEH"), 1847, "2019-01-06".toDate(), "ch_1Dpj1zCH2tJOrkrBlL2f4qdH", "card_1Dpj1vCH2tJOrkrByOuHUxhW", "Visa", "6139", 46, 1801, "2019-01-13".toDate())
    Ledger.addStripePayment(Account.codeToId("CWA-9746-ZYW"), 629, "2018-10-28".toDate(), "ch_1DQA21CH2tJOrkrBPjsFrzIa", "card_1DQA1wCH2tJOrkrBH61gUPMm", "Visa", "6041", 29, 600, "2018-11-04".toDate())
    Ledger.addStripePayment(Account.codeToId("KHA-9200-MWK"), 1796, "2019-05-28".toDate(), "ch_1EexbuCH2tJOrkrBVElyJrwk", "card_1EexbrCH2tJOrkrBek7k1sHg", "Visa", "8645", 45, 1751, "2019-06-04".toDate())
    Ledger.addStripePayment(Account.codeToId("KHA-9200-MWK"), 1796, "2019-05-28".toDate(), "ch_1EexXlCH2tJOrkrBX4seSpCJ", "card_1EexXiCH2tJOrkrB9w2oHydX", "Visa", "8645", 45, 1751, "2019-06-04".toDate())
    Ledger.addStripePayment(Account.codeToId("KHA-9200-MWK"), 1796, "2019-05-28".toDate(), "ch_1EexY0CH2tJOrkrBJecYRzhU", "card_1EexXxCH2tJOrkrBBUSFarJg", "Visa", "8645", 45, 1751, "2019-06-04".toDate())
    Ledger.addStripePayment(Account.codeToId("KHA-9200-MWK"), 1796, "2019-05-28".toDate(), "ch_1EexYlCH2tJOrkrB1zpn0FXu", "card_1EexYjCH2tJOrkrBRCYsNQvg", "Visa", "8645", 45, 1751, "2019-06-04".toDate())

*/

    }

    fun doFixHeight(idCompetition: Int, dogCode: Int, correctHeight: String) {
        val idDog = Dog.codeToIdDog(dogCode)
        dbTransaction {
            Entry().join { team }.join { agilityClass }
                .where("agilityClass.idCompetition=$idCompetition AND team.idDog=$idDog") {
                    jumpHeightCode = correctHeight
                    subClass = agilityClass.chooseSubClass(gradeCode, heightCode, jumpHeightCode, 0)
                    post()
                }
            CompetitionDog().where("idCompetition=$idCompetition AND idDog=$idDog") {
                kcJumpHeightCode = correctHeight
                post()
            }
        }
    }


/*
fun doFix(idTeam: Int, idDog: Int, wrongGrade: String, correctGrade: String, wrongHeight: String, correctHeight: String) {
    Entry().eitherway { it.join(it.agilityClass) }.select("idTeam=$idTeam AND idCompetition=$KERNOW").forEach {
        it.gradeCode = correctGrade
        it.heightCode = correctHeight
        if (it.agilityClass.template.oneOf(ClassTemplate.KC_ANY_JUMPING, ClassTemplate.KC_ANY_AGILITY)) {
            when (correctHeight) {
                "KC650" -> it.jumpHeightCode = "X350"
                else -> it.jumpHeightCode = "X250"
            }
        } else {
            it.jumpHeightCode = correctHeight
        }
        it.subClass = it.agilityClass.chooseSubClass(it.gradeCode, it.heightCode, it.jumpHeightCode)
        it.post()
    }

}
*/



    fun challengeTest() {
        Global.databaseHost = LOCAL
        TestShow.testChallenge()
    }

    fun splitClassByHeight(idCompetition: Int, classNumber: Int) {
        Global.databaseHost = LIVE
        AgilityClass().where("idCompetition=$idCompetition and classNumber=$classNumber") {
            KcUtils.splitClassByHeight(id)
        }
    }

    fun splitClassByGrade(idCompetition: Int, classNumber: Int, gradeCodePartB: String) {
        Global.databaseHost = LIVE
        AgilityClass().where("idCompetition=$idCompetition and classNumber=$classNumber") {
            KcUtils.splitClassByGrade(id, gradeCodePartB)
        }
    }

    val x = "select JSON_EXTRACT(extra, '$.kc.gradeHold') from dog where JSON_CONTAINS_PATH(extra, 'one', '$.kc.gradeHold')"

    fun gradeIssues() {
        //Global.databaseHost = LIVE
        //Global.allEmailsTo = ""
        setDebugExcludeClasses("*")
        dbExecute("truncate emailqueue")

        var lines = ""

        fun logIt(line: String) {
            lines += if (lines.isEmpty()) line else "\r$line"
        }

        WebTransaction().join { dog }.where("type=20", "WebTransaction.dateCreated") {
            lines = ""
            var report = false
            val kcGradeCodeOld = log["kcGradeCodeOld"].asString
            val kcGradeCodeNew = log["kcGradeCodeNew"].asString
            val wonOut = log["wonOut"].asDate
            logIt("${dog.id} ${dog.petName} (${dog.code}): from $kcGradeCodeOld to $kcGradeCodeNew on ${wonOut.dateText} (notified ${dateCreated.dateText})")
            for (competitionNode in log["competitions"]) {
                if (competitionNode.has("dates")) {
                    val idCompetition = competitionNode["idCompetition"].asInt
                    val name = competitionNode["name"].asString
                    logIt("  $name ($idCompetition)")
                    for (dateNode in competitionNode["dates"]) {
                        val date = dateNode["date"].asDate
                        for (classNode in dateNode["classes"]) {
                            val className = classNode["name"].asString
                            val action = classNode["action"].asString
                            if (action.contains("error", ignoreCase = true)) {
                                dog.kcGradeHoldArray.setValue(dog.kcGradeArray)
                                dog.updateKcGrade()
                                report = true
                            }
                            logIt("    ${date.dateText} $className: $action")

                        }

                    }
                }
            }
            if (report) {
                for (line in lines.split("\r")) {
                    println(line)
                }
            }

        }

//    dequeuePlaza()

    }

    fun champTest() {
        //Global.alwaysToPdf = true
        Global.testMode = true
        Global.runningOrderCopies = 1

        TestShow.setup(TUFFLEY, "2019-07-6")

        for (i in 1..6) {
            TestShow.simulateRing(1); TestShow.closeClass(1)
        }

        TestShow.setup(TUFFLEY, "2019-07-01")
        for (i in 1..3) {
            TestShow.simulateRing(1); TestShow.closeClass(1)
        }

    }

    fun fixTuffley() {
        Global.databaseHost = SHOW42
        setDebugExcludeClasses("*")
        addKcLate(TUFFLEY, 28070, false, 24, 23, 25)
        //  addKcLate(TUFFLEY, 23615, true, 43, 46)
        //  addKcLate(TUFFLEY, 17704, true, 46, 45)
    }

    fun switchResultsDog(idAgilityClass: Int, fromDogCode: Int, toDogCode: Int) {
        Global.databaseHost = LIVE
        var idTeamNew = 0
        Dog().where("dogCode=$toDogCode") {
            idTeamNew = Team.getIndividualId(idCompetitor, id)
        }
        if (idTeamNew > 0) {
            Entry().join { team }.join { team.dog }
                .where("entry.idAgilityClass=$idAgilityClass AND dog.dogCode=$fromDogCode") {
                    idTeam = idTeamNew
                    post()
                }
        }

    }

    fun fixAsoChamp(idCompetition: Int) {
        Global.databaseHost = LIVE
        AgilityClass().where("idCompetition=$idCompetition AND classCode=${ClassTemplate.KC_CHAMPIONSHIP_JUMPING.code}") {

            fun addClass(classCode: Int): AgilityClass {
                val new = AgilityClass()
                new.append()
                new.idCompetition = idCompetition
                new.code = classCode
                new.date = date
                new.number = number
                new.heightCodes = heightCodes
                new.gradeCodes = gradeCodes
                new.heightOptions = heightOptions
                new.jumpHeightCodes = jumpHeightCodes
                new.heightRunningOrder = heightRunningOrder
                new.name = new.describeClass()
                new.nameLong = new.describeClass(short = false)
                new.flag = false
                new.post()
                return new
            }

            val agility = AgilityClass()
            agility.find("idCompetition=$idCompetition AND classCode=${ClassTemplate.KC_CHAMPIONSHIP_AGILITY.code} AND heightCodes=${heightCodes.quoted}")
            val top = addClass(ClassTemplate.KC_CHAMPIONSHIP.code)
            val heat = addClass(ClassTemplate.KC_CHAMPIONSHIP_HEAT.code)
            val final = addClass(ClassTemplate.KC_CHAMPIONSHIP_FINAL.code)
            heat.idAgilityClassParent = top.id
            final.idAgilityClassParent = top.id
            agility.idAgilityClassParent = heat.id
            idAgilityClassParent = heat.id

            agility.numberSuffix = ""
            agility.name = agility.describeClass()
            agility.nameLong = agility.describeClass(short = false)

            numberSuffix = ""
            name = describeClass()
            nameLong = describeClass(short = false)

            heat.post()
            final.post()
            agility.post()
            post()

            duplicateEntries(top)
            duplicateEntries(heat)


        }

    }

    fun do_simulateBretford(host: String = "localhost") {
        //Global.alwaysToPdf = true
        Global.runningOrderCopies = 1
        Global.databaseHost = host

        TestShow.setup(BRETFORD, "2018-10-27")

        for (i in 1..3) {
            TestShow.simulateRing(1)
            TestShow.closeClass(1)
            TestShow.simulateRing(2)
            TestShow.closeClass(2)
        }
        for (i in 1..3) {
            TestShow.simulateRing(1)
            TestShow.closeClass(1)
        }

    }

    fun fixPembrooke() {
        Global.databaseHost = SHOW
        setDebugExcludeClasses("*")
        addKcLate(PEMBROKE, 32097, false, 102, 108, 204, 208, 301, 308)
        //  addKcLate(TUFFLEY, 23615, true, 43, 46)
        //  addKcLate(TUFFLEY, 17704, true, 46, 45)
    }

    fun fixBarnstaple() {
        Global.databaseHost = LIVE
        setDebugExcludeClasses("*")
        addKcLate(2068458849, 74957, true, 2, 6, 9)
        //  addKcLate(TUFFLEY, 23615, true, 43, 46)
        //  addKcLate(TUFFLEY, 17704, true, 46, 45)
    }

    fun fixSWAT() {
        Global.databaseHost = LIVE
        setDebugExcludeClasses("*")
//    addKcLate(CAC, 21652, false, 19, 20, 22, 52, 53, 55)


        addKcLate(SWAT3, 19856, true, 4, 7, 10, 13, 38, 41, 45, 48)
        addKcLate(SWAT3, 31733, true, 2, 5, 9, 11, 37, 39, 43, 46)
        addKcLate(SWAT3, 29695, true, 3, 6, 9, 12, 37, 40, 44, 47)

    }

    fun fixWave() {


        Global.databaseHost = "10.8.1.42"
        setDebugExcludeClasses("*")

        kcListDogEntries(WAVE, Dog.codeToIdDog(21460))
        kcListDogEntries(WAVE, Dog.codeToIdDog(29558))

    }



    fun removeUkaShowDuplicates() {
        Global.databaseHost = LIVE
        dbQuery("select idUka from dog where idUka>0 group by idUka having count(*) >1") {
            val idUka = getInt("idUka")
            Dog().where("idUka=$idUka and DeviceCreated=0") {
                val masterId = id
                val masterCompetitorId = idCompetitor
                Dog().where("idUka=$idUka and DeviceCreated<>0") {
                    Dog.renumberLinked(id, masterId)
                    if (this.idCompetitor != masterCompetitorId) {
                        Competitor.renumberLinked(this.idCompetitor, masterCompetitorId, basic = true)
                    }
                    this.idAccount = 0
                    this.idUka = 0
                    post()
                }
            }
        }
    }

    fun fixGF() {
        Global.databaseHost = LIVE
        AgilityClass().seek(GAMES_CHALLENGE_GAMBLERS) {
            gambleBonusObstacles = "$OBSTACLE_6$OBSTACLE_8$OBSTACLE_10$OBSTACLE_12"
            gambleBonusScore = 8
            post()
        }
    }

    fun fixGames() {
        Global.databaseHost = "10.8.1.2"
//    Global.databaseHost = "acu240.local"
        AgilityClass().seek(2126296289) {
            gambleBonusScore = 8
            gamble1 = 10
            gamble2 = 20
            post()
        }
    }

    fun do_test_try_out() {
        baseDate = "2018-09-22".toDate()
        TestShow.TEST_ID = A4E
        Global.databaseHost = "acu240.local"
        TestShow.simulateRing(1)
    }

    fun quick17() {
        Global.databaseHost = LIVE
        Global.allEmailsTo = ""

        val competition = Competition(CHIPPENHAM)

        dbQuery("SELECT DISTINCT idAccount FROM entry WHERE progress<>$PROGRESS_REMOVED AND idAgilityClass IN (1689958842, 1500567581, 1763521208, 1216711512)") {
            val idAccount = getInt("idAccount")
            revisedRunningOrders(competition, idAccount)
        }
    }


    fun fixChippenham() {
        Global.databaseHost = LIVE
        Entry().join { team }.join { team.competitor }.join { team.dog }.join { agilityClass }.join { account }.where(
            "entry.progress<>$PROGRESS_REMOVED AND entry.idAgilityClass IN (1689958842, 1500567581, 1763521208, 1216711512)",
            "entry.idAccount, entry.idTeam"
        ) {
            if (!team.competitor.ykcMember) {
                println("${agilityClass.name}: $teamDescription (${account.code}, ${team.competitor.code})")
                val idCompetitor = when (team.competitor.code) {
                    "SP7551" -> Competitor.competitorCodeToIdCompetitor("EP4175")
                    "BG4390" -> Competitor.competitorCodeToIdCompetitor("MG1902")
                    // "TR7085" -> Competitor.competitorCodeToIdCompetitor("EP4175")
                    "JT3765" -> Competitor.competitorCodeToIdCompetitor("RT2828")
                    "SM2778" -> Competitor.competitorCodeToIdCompetitor("CB5194")
                    else -> 0
                }
                if (idCompetitor > 0) {
                    idTeam = Team.getIndividualId(idCompetitor, team.dog.id)
                    post()
                }
            }
        }
    }

    fun fixSouthdown() {
        // Global.databaseHost = LIVE
        setDebugExcludeClasses("*")
//    addKcLate(CAC, 21652, false, 19, 20, 22, 52, 53, 55)


        addKcLate(SOUTHDOWNS, 22072, false, 4, 5, 10, 11, 29)
        addKcLate(SOUTHDOWNS, 31420, false, 3, 7, 8, 11, 29)

    }


    fun updateCompetitionLedger() {
        Global.databaseHost = "10.8.1.9"
        dbExecute(
            """
        ALTER TABLE `sandstone`.`competitionledger` 
        ADD COLUMN `idAccount` INT(11) NOT NULL DEFAULT '0' AFTER `idCompetition`,
        ADD INDEX `account` (`idCompetition` ASC, `idAccount` ASC, `type` ASC, `expireDate` ASC);
    """
        )
    }

    fun freeReplicationBlock() {

        fun skip(relayLogFile: String, relayLogPos: Int, channelName: String) {
            dbQuery("SHOW RELAYLOG EVENTS IN '$relayLogFile' FROM $relayLogPos LIMIT 1 FOR CHANNEL '$channelName'") {
                val eventType = getString("Event_type")
                val info = getString("Info")
                if (eventType.eq("Gtid")) {
                    val gtidNext = info.substringAfter("=").replace("'", "").trim()
                    dbExecute("STOP SLAVE FOR CHANNEL '$channelName'")
                    dbExecute("SET GTID_NEXT='$gtidNext'")
                    dbExecute("BEGIN")
                    dbExecute("COMMIT")
                    dbExecute("SET GTID_NEXT=\"AUTOMATIC\"")
                    dbExecute("START SLAVE FOR CHANNEL '$channelName'")
                }
            }
        }

        Global.databaseHost = "10.8.1.30"
        dbQuery("SHOW SLAVE STATUS") {
            val ioRunning = getString("Slave_IO_Running") == "Yes"
            val sqlRunning = getString("Slave_SQL_Running") == "Yes"
            val lastSqlError = getString("Last_SQL_Error")
            val relayLogFile = getString("Relay_Log_File")
            val relayLogPos = getInt("Relay_Log_Pos")
            val channelName = getString("Channel_Name")

            if (ioRunning && !sqlRunning && lastSqlError.isNotEmpty()) {
                skip(relayLogFile, relayLogPos, channelName)
            }
        }


    }

    fun syncAccount(dogCode: Int) {
        Global.databaseHost = "acu240.local"
        ApiUtils.syncAccount(dogCode)
    }


    fun fixShowRegistrations() {
        Global.databaseHost = LIVE
        CompetitionLedger().where("type=160") {
            if (description.startsWith("Register")) {
                val dogCode = description.substringAfter("(").substringBefore(")").toInt()
                var idDog = Dog.codeToIdDog(dogCode)
                if (idDog == -1) {
                    val petName = description.substringAfter("Register").substringBefore("(").trim()
                    Dog().where("idAccount=$idAccount AND petName=${petName.quoted} AND AliasFor=0") {
                        idDog = id
                    }
                }
                this.idDog = idDog
                this.idCompetitor = 0
                post()
            } else if (description.contains(" - Extend")) {
                val fullName = description.substringBefore(" - Extend")
                val givenName = fullName.substringBefore(" ")
                val familyName = fullName.substringAfter(" ")
                var idCompetitor = 0
                Competitor().where("idAccount=$idAccount AND givenName=${givenName.quoted} AND familyName=${familyName.quoted} AND AliasFor=0") {
                    idCompetitor = id
                }
                this.idCompetitor = idCompetitor
                post()
            }
        }

    }


    fun fixFab() {
        Global.databaseHost = LIVE

        AgilityClass().join { competition }.where("competition.IdOrganization=4") {
            subDivisions = template.subDivisions
            post()
        }

        CompetitionDog().join { dog }.join { competition }.where("competition.IdOrganization=4") {
            fabCollie = dog.fabCollie
            post()
            if (fabCollie) {
                Entry().join { team }.join { agilityClass }
                    .where("agilityClass.idCompetition=$idCompetition AND team.idDog=$idDog") {
                        if (agilityClass.subDivisions.toLowerCase().contains("collie")) {
                            subDivision = 1
                            post()
                        }
                    }
            }
        }
        Competition().where("competition.IdOrganization=$ORGANIZATION_FAB") {
            checkSubClasses(id)
        }
    }

    fun addLateDog(idCompetition: Int, dogCode: Int) {
        Global.databaseHost = "acu240.local"
        Dog().where("dogCode=$dogCode") {
            val competitionDog = CompetitionDog(idCompetition, id)
            if (!competitionDog.found()) {
                competitionDog.append()
                competitionDog.idCompetition = idCompetition
                competitionDog.idDog = id
                competitionDog.entryType = ENTRY_AT_SHOW
                competitionDog.idAccount = idAccount
                competitionDog.ringNumber = Dog(competitionDog.idDog).code
                competitionDog.nfc = false
                competitionDog.kcHandlerId = idCompetitorHandler
                competitionDog.kcHandler = handlerName
                competitionDog.kcHeightCode = kcHeightCode
                competitionDog.kcGradeCode = kcGradeCode
                competitionDog.kcJumpHeightCode = kcEntryOption
            }
            competitionDog.post()
        }
    }

    fun campingSoldTest(idCompetition: Int, fromAccount: String, toAccount: String) {
//    Global.databaseHost = LIVE
        dbExecute("truncate emailqueue")
        Camping().where("idCompetition=$idCompetition AND idAccount=${Account.codeToId(fromAccount)}") {
            val error = transferToAccount(Account.codeToId(toAccount))
            println("ERROR = $error")
        }
        PlazaAdmin.dequeuePlaza()
    }

    fun campingSold(idCompetition: Int, fromAccount: String, toAccount: String) {
        Global.databaseHost = LIVE
        Camping().where("idCompetition=$idCompetition AND idAccount=${Account.codeToId(fromAccount)}") {
            val error = transferToAccount(Account.codeToId(toAccount))
            println("ERROR = $error")
        }
    }

    fun updateCamping() {
        Competition().where("dateStart>curDate()", "dateStart") {
            println("$uniqueName, $campingCapSystem, $id")
            for (block in camping) {
                val start = block["start"].asDate
                val end = block["end"].asDate
                val dayBooking = block["dayBooking"].asBoolean
                val dayRate = block["dayRate"].asInt
                val blockRate = block["blockRate"].asInt
                println("   ${start.dateText}, ${end.dateText}, ${end.daysSince(start) + 1}, $blockRate, $dayRate, $dayBooking")
            }
        }
    }


    /********************************************************************
    CSJ Test
     *
     */

    fun csjPrimeEntries() {
        /* run once groups assigned to CompetitionCompetitor entries */
        Global.databaseHost = LIVE
        UkOpenUtils.generateRunningOrders(CSJ)
    }

    fun csjChangeHeight(dogCode: Int, newHeight: String) {
//    Global.databaseHost = LIVE
        UkOpenUtils.changeHeight(CSJ, Dog.codeToIdDog(dogCode), newHeight)

    }

    fun csjRemoveDog(dogCode: Int) {
//    Global.databaseHost = LIVE
        UkOpenUtils.drop(CSJ, Dog.codeToIdDog(dogCode))

    }

    fun fixCSJ(host: String = "localhost") {
        Global.databaseHost = host
        UkOpenUtils.swapDog(CSJ, 1804447419, Dog.codeToIdDog(23247), Dog.codeToIdDog(19890))
        UkOpenUtils.swapDog(CSJ, 1804447419, Dog.codeToIdDog(31761), Dog.codeToIdDog(31266))
        UkOpenUtils.swapDog(CSJ, 1804447419, Dog.codeToIdDog(31056), Dog.codeToIdDog(27388))
        UkOpenUtils.swapDog(CSJ, 1804447419, Dog.codeToIdDog(61489), Dog.codeToIdDog(26550))
    }

    fun csjAdjustRunningOrders(idCompetition: Int) {
        dbTransaction {
            val groupHeightCode = ChangeMonitor<String>("?")
            var proposedRunningOrder = 0

            Entry().join { agilityClass }
                .where("agilityClass.idCompetition=$idCompetition", "entry.idAgilityClass, entry.group, entry.jumpHeightCode") {
                    if (groupHeightCode.hasChanged(idAgilityClass.toString() + group + jumpHeightCode)) {
                        proposedRunningOrder = 1
                    }
                    runningOrder = proposedRunningOrder++
                    post()
                }
        }
    }

    fun do_simulateOpen(idCompetition: Int = CSJ, reset: Boolean = false, host: String = "localhost") {

        Global.alwaysToPdf = true
        Global.testMode = true
        Global.runningOrderCopies = 1
        Global.databaseHost = host

        val competition = Competition(idCompetition)


        if (reset) {
            dbExecute("DELETE entry.* FROM entry JOIN agilityClass USING (idAgilityClass) WHERE idCompetition=$idCompetition")
            dbExecute("UPDATE agilityClass SET classProgress=$CLASS_PENDING, finalized=false, runningOrdersGenerated=false WHERE idCompetition=$idCompetition")

            competition.ukOpenLocked = false
            competition.post()

            AgilityClass().join { ring }.where("agilityClass.idCompetition=$idCompetition AND ringOrder=1") {
                ring.idAgilityClass = id
                ring.heightCode = firstJumpHeightCode
                ring.group = firstGroup
                ring.post()
            }
            UkOpenUtils.generateRunningOrders(idCompetition, print = true)
        }

//    fixCSJ()

        UkOpenUtils.lockEntries(idCompetition)

        TestShow.setup(idCompetition, competition.dateStart.softwareDate)

        // Thurs
        for (i in 1..2) {
            TestShow.simulateRing(1); TestShow.closeClass(1)
            TestShow.simulateRing(2); TestShow.closeClass(2)
        }

        // Fri
        TestShow.nextDay()
        for (i in 1..2) {
            TestShow.simulateRing(1); TestShow.closeClass(1)
            TestShow.simulateRing(2); TestShow.closeClass(2)
        }
        TestShow.simulateRing(1); TestShow.closeClass(1)

        // Sat
        TestShow.nextDay()
        for (i in 1..3) {
            TestShow.simulateRing(1); TestShow.closeClass(1)
        }

        // Sun
        TestShow.nextDay()
        for (i in 1..3) {
            TestShow.simulateRing(1); TestShow.closeClass(1)
        }
    }

    fun do_simulateCruftsTeam(idCompetition: Int = WRAXALL, host: String = "localhost") {

        Global.alwaysToPdf = true
        Global.testMode = true
        Global.runningOrderCopies = 1
        Global.databaseHost = host

        val competition = Competition(idCompetition)

        TestShow.setup(idCompetition, competition.dateStart.softwareDate)

        TestShow.simulateRing(4, remain = 5)

        /*
        for (i in 1..3) {
            TestShow.simulateRing(4); TestShow.closeClass(4)
        }

         */
    }

    fun do_simulateOpenPart(idCompetition: Int = CSJ, reset: Boolean = false, host: String = "localhost") {

        Global.alwaysToPdf = true
        Global.testMode = true
        Global.runningOrderCopies = 1
        Global.databaseHost = host

        val competition = Competition(idCompetition)


        if (reset) {
            dbExecute("DELETE entry.* FROM entry JOIN agilityClass USING (idAgilityClass) WHERE idCompetition=$idCompetition")
            dbExecute("UPDATE agilityClass SET classProgress=$CLASS_PENDING, finalized=false, runningOrdersGenerated=false WHERE idCompetition=$idCompetition")

            competition.ukOpenLocked = false
            competition.post()

            AgilityClass().join { ring }.where("agilityClass.idCompetition=$idCompetition AND ringOrder=1") {
                ring.idAgilityClass = id
                ring.heightCode = firstJumpHeightCode
                ring.group = firstGroup
                ring.post()
            }
            UkOpenUtils.generateRunningOrders(idCompetition)
        }

//    fixCSJ()

        UkOpenUtils.lockEntries(idCompetition)

        TestShow.setup(idCompetition, competition.dateStart.softwareDate)

        // Thurs
        for (i in 1..2) {
            TestShow.simulateRing(1); TestShow.closeClass(1)
            TestShow.simulateRing(2); TestShow.closeClass(2)
        }

        // Fri
        TestShow.nextDay()
        for (i in 1..2) {
            TestShow.simulateRing(1); TestShow.closeClass(1)
            TestShow.simulateRing(2); TestShow.closeClass(2)
        }
        TestShow.simulateRing(1); TestShow.closeClass(1)

        // Sat
        TestShow.nextDay()
        TestShow.simulateRing(1)
        TestShow.closeClass(1)

    }

    fun do_prepare_gamblers(host: String = "acu240.local") {

        Global.alwaysToPdf = true
        Global.testMode = true
        Global.runningOrderCopies = 1
        Global.databaseHost = host

        val competition = Competition(CSJ)

        TestShow.setup(CSJ, competition.dateStart.softwareDate)

        UkOpenUtils.lockEntries(CSJ)

        for (i in 1..2) {
            TestShow.simulateRing(1); TestShow.closeClass(1)
            TestShow.simulateRing(2); TestShow.closeClass(2)
        }

        TestShow.nextDay()
        for (i in 1..2) {
            TestShow.simulateRing(1); TestShow.closeClass(1)
            TestShow.simulateRing(2); TestShow.closeClass(2)
        }
        TestShow.simulateRing(1); TestShow.closeClass(1)

        TestShow.nextDay()
        for (i in 1..1) {
            TestShow.simulateRing(1); TestShow.closeClass(1)
        }

        control.effectiveDate = TestShow.baseDate
        control.post()

    }

    fun do_simulateGrampian(host: String = "localhost") {

        Global.alwaysToPdf = true
        Global.testMode = true
        Global.runningOrderCopies = 1
        Global.databaseHost = host

        val competition = Competition(2104139165)

        TestShow.setup(2104139165, competition.dateStart.addDays(1).softwareDate)
        TestShow.noWithdraws = true

        for (i in 1..12) {
            TestShow.simulateRing(1); TestShow.closeClass(1)
        }
    }

    fun addAssets() {
        //Global.databaseHost = LIVE

        /*
        for (i in 10650..10662) {
            Device().withAppendPost {
                type = ASSET_CHARGE_BRICK
                model = "Anker Charge Brick"
                tag = "cha00${i-10650}"
                assetCode = "A$i"
            }
        }

            for (i in 10680..10680) {
            Device().withAppendPost {
                type = ASSET_MAINS_EXTENSION
                model = "6 Way Mains Extension"
                tag = "cha00${i-10680}"
                assetCode = "A$i"
            }
        }
        */
/*
    for (i in 10700..10743) {
        Device().withAppendPost {
            type = ASSET_POWER_PACK
            model = "Anker PowerCore 20100"
            tag = "bat0${i-10700+1}"
            assetCode = "A$i"
        }
    }



    for (i in 10681..10682) {
        Device().withAppendPost {
            type = ASSET_MAINS_EXTENSION
            model = "6 Way Mains Extension"
            tag = "cha00${i - 10680}"
            assetCode = "A$i"
        }
    }
    for (i in 10632..10633) {
        Device().withAppendPost {
            type = ASSET_DONGLE
            model = "MF823 Black"
            tag = "d00${i - 10632}"
            assetCode = "A$i"
        }
    }
 */
        for (i in 10800..10801) {
            Device().withAppendPost {
                type = ASSET_CRATE
                model = "Collapsible Crate"
                tag = "a$i"
                assetCode = "A$i"
            }
        }


    }

    fun jsonCompileTest() {
        /*
        for (i in 1..10) {
            val x = Json(
                "{\"classCode\":1270,\"members\":[{\"idDog\":1205541076,\"competitorName\":\"Louise Eden\",\"petName\":\"Fuze\",\"dogCode\":91797,\"heightCode\":\"KC350\",\"registeredName\":\"Obay The Boyz High Voltage\"},{\"idDog\":1551196982,\"dogCode\":32197,\"petName\":\"Snazzy\",\"competitorName\":\"Marc Wingate-Wynne\",\"heightCode\":\"KC350\",\"registeredName\":\"Obay Thatz Entertainment\"},{\"idDog\":1475557410,\"dogCode\":32362,\"petName\":\"Ziji\",\"competitorName\":\"M Adams\",\"heightCode\":\"KC350\",\"registeredName\":\"Obay Thatz The Truth\"},{\"idDog\":1433563849,\"dogCode\":31041,\"petName\":\"Zoa\",\"competitorName\":\"Bernadette Bay\",\"heightCode\":\"KC350\",\"registeredName\":\"Obay Letz Go Girlz\"},{\"idDog\":1331309230,\"dogCode\":62962,\"petName\":\"Pretzel\",\"competitorName\":\"Louise Eden\",\"heightCode\":\"KC350\",\"registeredName\":\"Obay Thatz Shocking\"},{\"idDog\":1168198405,\"dogCode\":99659,\"petName\":\"Flux\",\"competitorName\":\"Oliver Eden\",\"heightCode\":\"KC350\",\"registeredName\":\"Shenaja Magic Frigg\"}],\"teamName\":\"SCOC Shelties\",\"clubName\":\"Somerset Canine Obedience Club\"}"
            )
            if (i==10) println(x.toJson())
        }

         */
        val agilityClass = AgilityClass(1675457055)
        agilityClass.setHeightCallingTo("KC350", CALLING_TO_END)
        var entry =
            Entry("idEntry", "idAgilityClass", "idTeam", "teamMember", "jumpHeightCode", "progress", "scoreCodes", "time", "noTime", "runningOrder")
        entry.selectWaitingFor(agilityClass, "", "KC350", false, true)
        while (entry.next()) {
            println("${entry.runningOrder} ${entry.team.teamName}")
        }
        println("end")
    }


    fun champRunningOrders(idCompetition: Int) {
        AgilityClass().where("idCompetition=$idCompetition AND classCode IN (${ClassTemplate.KC_CHAMPIONSHIP_AGILITY.code}, ${ClassTemplate.KC_CHAMPIONSHIP_JUMPING.code})") {
            runningOrders(id, pdf = true)
        }

    }


    fun setUpTryOutGamblers(idCompetition: Int, host: String = "localhost") {
        Global.databaseHost = host
        with(UkOpenUtils.getAgilityClass(idCompetition, ClassTemplate.TRY_OUT_GAMES_GAMBLERS)) {
            openingTime = 30000
            gambleTimeLarge = 11000
            gambleTimeSmall = 11000
            courseTime = openingTime + gambleTimeLarge
            courseTimeSmall = openingTime + gambleTimeSmall
            obstaclePoints = "554322222222111111111"
            gambleBonusObstacles = "$OBSTACLE_9$OBSTACLE_10$OBSTACLE_11$OBSTACLE_12"
            gambleBonusScore = 8
            post()
        }
    }

    fun testNedlo(host: String = LOCAL) {

        val NEDLO = 2033185920

        Global.testMode = true
        Global.databaseHost = host

        val competition = Competition(NEDLO)

        TestShow.setup(NEDLO, competition.dateStart.softwareDate)


        for (i in 1..4) {
            TestShow.simulateRing(4); TestShow.closeClass(4)
        }

    }


    fun fixReplication() {
        Global.databaseHost = LIVE
        ReplicationFault.fixWebsiteReplication()
    }


}