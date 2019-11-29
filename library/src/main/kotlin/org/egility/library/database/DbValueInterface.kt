package org.egility.library.database

import org.egility.library.general.Json
import org.egility.library.general.Variant
import java.util.*

interface DbValueInterface {

    fun isColumn(columnName: String): Boolean
    fun setVariant(columnName: String, value: Variant)
    fun getVariant(columnName: String): Variant

    fun isNull(columnName: String): Boolean {
        return getVariant(columnName).isNull
    }
    fun isInt(columnName: String): Boolean {
        return getVariant(columnName).isInteger
    }
    fun isFloat(columnName: String): Boolean {
        return getVariant(columnName).isInteger
    }
    fun isBoolean(columnName: String): Boolean {
        return getVariant(columnName).isBoolean
    }
    fun isString(columnName: String): Boolean {
        return getVariant(columnName).isString
    }
    fun isDate(columnName: String): Boolean {
        return getVariant(columnName).isDate
    }
    fun isBlob(columnName: String): Boolean {
        return getVariant(columnName).isBlob
    }

    fun getInt(columnName: String): Int {
        return getLong(columnName).toInt()
    }
    fun getLong(columnName: String): Long {
        return getVariant(columnName).long
    }
    fun getDouble(columnName: String): Double {
        return getVariant(columnName).float.toDouble()
    }
    fun getBoolean(columnName: String): Boolean {
        return getVariant(columnName).boolean
    }
    fun getString(columnName: String): String {
        return getVariant(columnName).string
    }
    fun getJson(columnName: String): Json {
        return getVariant(columnName).json
    }
    fun getDate(columnName: String): Date {
        return getVariant(columnName).date
    }
    fun getBlob(columnName: String): ByteArray {
        return getVariant(columnName).blob
    }

    fun setNull(columnName: String) {
        setVariant(columnName, Variant.nullValue())
    }
    fun setValue(columnName: String, value: Int) {
        setVariant(columnName, Variant(value))
    }
    fun setValue(columnName: String, value: Long) {
        setVariant(columnName, Variant(value))
    }
    fun setValue(columnName: String, value: Double) {
        setVariant(columnName, Variant(value))
    }
    fun setValue(columnName: String, value: Boolean) {
        setVariant(columnName, Variant(value))
    }
    fun setValue(columnName: String, value: String) {
        setVariant(columnName, Variant(value))
    }
    fun setValue(columnName: String, value: Json) {
        setVariant(columnName, Variant(value))
    }
    fun setValue(columnName: String, value: Date) {
        setVariant(columnName, Variant(value))
    }
    fun setValue(columnName: String, value: ByteArray) {
        setVariant(columnName, Variant(value))
    }
    
}