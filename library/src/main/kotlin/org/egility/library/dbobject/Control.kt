/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.dbobject

import org.egility.library.database.*
import org.egility.library.general.isBitSet
import java.util.*

open class ControlRaw<T : DbTable<T>>(_connection: DbConnection? = null, vararg columnNames: String) : DbTable<T>(_connection, "control", *columnNames) {
    var id: Int by DbPropertyInt("idControl")
    var idCompetition: Int by DbPropertyInt("idCompetition")
    var idSite: Int by DbPropertyInt("idSite")
    var effectiveDate: Date by DbPropertyDate("effectiveDate")
    var pinGeneral: String by DbPropertyString("pinGeneral")
    var pinSecretary: String by DbPropertyString("pinSecretary")
    var pinShowAccounts: String by DbPropertyString("pinShowAccounts")
    var pinMeasure: String by DbPropertyString("pinMeasure")
    var pinSystemManager: String by DbPropertyString("pinSystemManager")
    var graniteVersion: String by DbPropertyString("graniteVersion")
    var flags: Int by DbPropertyInt("flags")

    val liveLinkDisabled: Boolean by DbPropertyBit("flags", 0)
    
    val useIdUka: Boolean by DbPropertyBit("flags", 1)
    val useOldRegistrationRule: Boolean by DbPropertyBit("flags", 2)
    val option3: Boolean by DbPropertyBit("flags", 3)
    val option4: Boolean by DbPropertyBit("flags", 4)
    val option5: Boolean by DbPropertyBit("flags", 5)
    val option6: Boolean by DbPropertyBit("flags", 6)
    val option7: Boolean by DbPropertyBit("flags", 7)
    val option8: Boolean by DbPropertyBit("flags", 8)
    val option9: Boolean by DbPropertyBit("flags", 9)
    val option10: Boolean by DbPropertyBit("flags", 10)
    val option11: Boolean by DbPropertyBit("flags", 11)
    val option12: Boolean by DbPropertyBit("flags", 12)
    val option13: Boolean by DbPropertyBit("flags", 13)
    val option14: Boolean by DbPropertyBit("flags", 14)
    val option15: Boolean by DbPropertyBit("flags", 15)


}

class Control(vararg columnNames: String) : ControlRaw<Control>(null, *columnNames) {

}

private var _control: Control? = null

val control: Control
    get() {
        val existing = _control
        if (existing == null) {
            val created = Control()
            created.find(1)
            _control = created
            return created
        } else {
            return existing
        }
    }

fun resetControl() {
    _control = null
}
