/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.dbobject

import org.egility.library.database.*
import org.egility.library.general.*

open class HeightRaw<T : DbTable<T>>(_connection: DbConnection? = null, vararg columnNames: String) :
    DbTable<T>(_connection, "height", *columnNames) {

    var code: String by DbPropertyString("heightCode")
    var organization: String by DbPropertyString("organization")
    var name: String by DbPropertyString("name")
    var abbreviation: String by DbPropertyString("abbreviation")
    var jumpName: String by DbPropertyString("jumpName")
    var minHeight: Int by DbPropertyInt("minHeight")
    var maxHeight: Int by DbPropertyInt("maxHeight")
    var jumpMax: Int by DbPropertyInt("jumpMax")
    var jumpMin: Int by DbPropertyInt("jumpMin")
    var casualCode: String by DbPropertyString("casualCode")
    var jumpHeightOnly: Boolean by DbPropertyBoolean("jumpHeightOnly")
    var clearRoundOnlyOption: Boolean by DbPropertyBoolean("ClearRoundOnlyOption")
    var classHeightOnly: Boolean by DbPropertyBoolean("classHeightOnly")
    var kcFrom: String by DbPropertyString("kcFrom")
    var kcTo: String by DbPropertyString("kcTo")
    var selector: String by DbPropertyString("selector")

}

class Height(vararg columnNames: String) : HeightRaw<Height>(null, *columnNames) {

    fun selectClassHeights(idAgilityClass: Int, runningOrder: Boolean) {
        release()
        val agilityClass = AgilityClass()
        agilityClass.find(idAgilityClass)
        if (agilityClass.found()) {
            val heightRunningOrder = agilityClass.heightRunningOrder
            val where = "FIND_IN_SET(heightCode, %s) > 0"
            var orderBy = "heightCode DESC"
            if (runningOrder) {
                orderBy = "FIND_IN_SET(heightCode, %s)"
            }
            select(where, orderBy, heightRunningOrder.quoted)
        }
    }

    companion object {

        private var _allHeights: Height? = null

        val allHeights: Height 
        get() {
            val result = _allHeights
            if (result == null) {
                val create = Height()
                create.select("TRUE")
                val height = Height().where("TRUE", "heightCode") as Height
                _allHeights = height
                return height
            }
            return result
        }
        
        val ifcsHeights: ArrayList<String> = ArrayList()
            get() {
                if (field.isEmpty()) {
                    allHeights.forEach {
                        if (it.organization == "IFCS") field.add(it.code)
                    }
                }
                return field
            }

        val ukOpenHeights: ArrayList<String> = ArrayList()
            get() {
                if (field.isEmpty()) {
                    allHeights.forEach {
                        if (it.organization == "OP") field.add(it.code)
                    }
                }
                return field
            }

        val fabHeights: ArrayList<String> = ArrayList()
            get() {
                if (field.isEmpty()) {
                    allHeights.forEach {
                        if (it.organization == "FAB") field.add(it.code)
                    }
                }
                return field
            }

        fun indHeights(key: String):ArrayList<String> {
            val list=ArrayList<String>()
            if (list.isEmpty()) {
                allHeights.forEach {
                    if (it.organization == "IND" && it.selector.split(",").contains(key)) list.add(it.code)
                }
            }
            return list
        }

        fun indHeightFromKc(key: String, kcHeightCode: String): String {
            var result="unknown"
            allHeights.forEach {
                if (it.organization == "IND" && it.selector.split(",").contains(key)) {
                    if (kcHeightCode>=it.kcFrom && kcHeightCode<=it.kcTo) result = it.code
                }
            }
            return result
        }

        fun indHeightValues(key: String, clearRoundOnly: Boolean): JsonNode {
            val node = Json()
            for (kcHeightCode in "KC650,KC500,KC450,KC350,unknown".split(",")) {
                val options = node[kcHeightCode]
                allHeights.forEach {
                    if (it.organization == "IND" && it.selector.split(",").contains(key)) {
                        if (kcHeightCode=="unknown") {
                            val option = options.addElement()
                            option["value"] = it.code
                            option["description"] = it.name
                            if (clearRoundOnly && it.clearRoundOnlyOption) {
                                val option = options.addElement()
                                option["value"] = it.code +"C"
                                option["description"] = "${it.name} CRO"
                            }
                        } else if (kcHeightCode >= it.kcFrom && kcHeightCode <= it.kcTo) {
                            val option = options.addElement()
                            option["value"] = it.code
                            option["description"] = it.name
                        } else if (kcHeightCode > it.kcFrom && clearRoundOnly && it.clearRoundOnlyOption) {
                            val option = options.addElement()
                            option["value"] = it.code +"C"
                            option["description"] = "${it.name} CRO"
                        }
                    }
                }
                options.sortBy("value", descending = true)
            }
            return node
        }

        fun indDogHeightValues(key: String, clearRoundOnly: Boolean, kcHeightCode: String, ukaHeightCode: String): JsonNode {
            var kcJumpHeight = getJumpHeight(kcHeightCode)
            var ukaJumpHeight = getJumpHeight(ukaHeightCode)
            if (kcJumpHeight==0) kcJumpHeight = ukaJumpHeight
            if (ukaJumpHeight==0) ukaJumpHeight = kcJumpHeight
            val max= maxOf(kcJumpHeight, ukaJumpHeight)
            val min= minOf(kcJumpHeight, ukaJumpHeight)
            val node = Json()
            if (key.eq("aa")) {
                allHeights.forEach {
                    if (it.organization == "IND" && it.selector.split(",").contains(key)) {
                        if (max==0 && min==0) {
                            if (it.jumpMax>200) {
                                val option1 = node.addElement()
                                option1["value"] = it.code
                                option1["description"] = it.name
                            }
                            if (it.jumpMax<600) {
                                val option2 = node.addElement()
                                option2["value"] = it.code + "C"
                                option2["description"] = "${it.name} CRO"
                            }
                        } else if (it.jumpMax < min) {
                            val option = node.addElement()
                            option["value"] = it.code + "C"
                            option["description"] = "${it.name} CRO"
                        } else if (it.jumpMax <= max) {
                            val option = node.addElement()
                            option["value"] = it.code
                            option["description"] = it.name
                        }
                    }
                }
            }
            return node
        }



        fun indHeightsMap(key: String):Map<String,String> {
            val map=HashMap<String, String>()
                if (map.isEmpty()) {
                    allHeights.forEach {
                        if (it.organization == "IND" && it.selector.split(",").contains(key)) map[it.code]=it.abbreviation
                    }
                }
                return map
            }

        fun indHeightsReverseMap(key: String):Map<String,String> {
            val map=HashMap<String, String>()
            if (map.isEmpty()) {
                allHeights.forEach {
                    if (it.organization == "IND" && it.selector.split(",").contains(key)) map[it.abbreviation]=it.code
                }
            }
            return map
        }

        fun getHeightName(heightCodes: String, unknown: String = "UNKNOWN", short: Boolean = false): String {
            var result = ""
            val heightItems = heightCodes.split(",")

            allHeights.forEach {
                if (heightItems.contains(it.code)) {
                    result = result.append(if (short) it.abbreviation else it.name, "/")
                }
            }
            return if (result.isEmpty()) unknown else result
        }

        fun getHeightShort(heightCodes: String, unknown: String = "UNKNOWN"): String {
            return getHeightName(heightCodes, unknown, short = true)
        }

        fun getHeightJumpName(heightCodes: String, unknown: String = "UNKNOWN"): String {
            var result = ""
            val heightItems = heightCodes.split(",")

            allHeights.forEach {
                if (heightItems.contains(it.code)) {
                    result = result.append(it.jumpName, "/")
                }
            }
            return if (result.isEmpty()) unknown else result
        }

        fun getJumpHeight(heightCode: String): Int {
            var result = 0
            allHeights.forEach {
                if (it.code == heightCode) {
                    result = it.jumpMax
                }
            }
            return result
        }

        fun getHeightJumpNameEx(heightCodes: String, unknown: String = "UNKNOWN"): String {
            val heightName = getHeightName(heightCodes, unknown)
            val jumpHeightName = getHeightJumpName(heightCodes, unknown)
            when (jumpHeightName) {
                "FH" -> return heightName + " FH"
                "LHO" -> return heightName
                else -> return jumpHeightName
            }

        }


        fun getCombinedName(heightCode: String, jumpHeightCode: String, short: Boolean = false): String {
            return getHeightName(heightCode, short = short) + " " + getHeightJumpName(jumpHeightCode)
        }

        fun getCasualHeightCode(heightCode: String): String {
            var result = ""
            allHeights.forEach {
                if (it.code == heightCode) {
                    result = it.casualCode
                }
            }
            return result
        }

        fun toBase(heightCode: String): String {
            if (heightCode.endsWith("L")) {
                return heightCode.dropLast(1)
            } else {
                return heightCode
            }
        }

        fun isLho(heightCode1: String, heightCode2: String): Boolean {
            return heightCode1 == heightCode2 + "L" || heightCode1 + "L" == heightCode2
        }

        fun getHeightCode(name: String, organization: String = "", key: String = ""): String {
            var result = ""

            allHeights.forEach {
                if (key.isEmpty() || it.selector.split(",").contains(key)) {
                    if ((it.name.eq(name) || it.abbreviation.eq(name)) && (organization.isEmpty() || organization == it.organization)) {
                        result = it.code
                    }
                }
            }
            return result
        }


    }

}