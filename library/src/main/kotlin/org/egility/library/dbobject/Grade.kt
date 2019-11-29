/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.dbobject

import org.egility.library.database.DbConnection
import org.egility.library.database.DbPropertyBoolean
import org.egility.library.database.DbPropertyString
import org.egility.library.database.DbTable
import org.egility.library.general.dropLeft

open class GradeRaw<T : DbTable<T>>(_connection: DbConnection? = null, vararg columnNames: String) :
    DbTable<T>(_connection, "grade", *columnNames) {

    var code: String by DbPropertyString("gradeCode")
    var organization: String by DbPropertyString("organization")
    var name: String by DbPropertyString("name")
    var short: String by DbPropertyString("short")
    var classGradeOnly: Boolean by DbPropertyBoolean("classGradeOnly")
    var kcFrom: String by DbPropertyString("kcFrom")
    var kcTo: String by DbPropertyString("kcTo")
    var selector: String by DbPropertyString("selector")

}

class Grade(vararg columnNames: String) : GradeRaw<Grade>(null, *columnNames) {

    companion object {

        private var _allGrades: Grade? = null

        val allGrades: Grade
            get() {
                val result = _allGrades
                if (result == null) {
                    val create = Grade()
                    create.select("TRUE")
                    val grade = Grade().where("TRUE", "gradeCode") as Grade
                    _allGrades = grade
                    return grade
                }
                return result
            }


        val fabGrades: ArrayList<String> = ArrayList()
            get() {
                if (field.isEmpty()) {
                    allGrades.forEach {
                        if (it.organization == "FAB") field.add(it.code)
                    }
                }
                return field
            }

        fun indGrades(key: String):ArrayList<String> {
            val list=ArrayList<String>()
            if (list.isEmpty()) {
                allGrades.forEach {
                    if (it.organization == "IND" && it.selector.split(",").contains(key)) list.add(it.code)
                }
            }
            return list
        }

        fun indGradeFromKc(key: String, kcGradeCode: String): String {
            var result="unknown"
            allGrades.forEach {
                if (it.organization == "IND" && it.selector.split(",").contains(key)) {
                    if (kcGradeCode>=it.kcFrom && kcGradeCode<=it.kcTo) result = it.code
                }
            }
            return result
        }

        fun indGradesMap(key: String):Map<String,String> {
            val map=HashMap<String, String>()
            if (map.isEmpty()) {
                allGrades.forEach {
                    if (it.organization == "IND" && it.selector.split(",").contains(key)) map[it.code]=it.short
                }
            }
            return map
        }

        fun indGradesReverseMap(key: String):Map<String,String> {
            val map=HashMap<String, String>()
            if (map.isEmpty()) {
                allGrades.forEach {
                    if (it.organization == "IND" && it.selector.split(",").contains(key)) map[it.short]=it.code
                }
            }
            return map
        }
        
        fun getGradeName(gradeCode: String): String {
            var result = "UNKNOWN"
            allGrades.forEach {
                if (it.code.equals(gradeCode, ignoreCase = true)) {
                    result = it.name
                }
            }
            return result
        }

        fun getGradeShort(gradeCode: String): String {
            var result = "UNKNOWN"
            allGrades.forEach {
                if (it.code.equals(gradeCode, ignoreCase = true)) {
                    result = it.short
                }
            }
            return result
        }

        fun getGradeNumber(gradeCode: String): String {
            return gradeCode.dropLeft(3)
        }

    }


}