import org.egility.library.general.Global
import org.egility.linux.reports.MarketingReport
import org.egility.linux.tools.NativeServices
import org.egility.linux.tools.PlazaAdmin.generateFabHeights

fun main(args: Array<String>) {
    NativeServices.initialize(true)
    Global.databaseHost = LOCAL
    Global.allEmailsTo = "mike@egility.org"
    doTest()
}

fun doTest() {
    MarketingReport.generate(2050106425, pdf=false)
}



/*
  <component name="PropertiesComponent">
    <property name="not.eligible.for.single.variant.sync" value="true" />
  </component>
 */