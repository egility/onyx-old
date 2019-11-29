/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.general

/**
 * Created by mbrickman on 04/09/16.
 */

import java.util.*

object Cryptography {

    fun encrypt(message: String, keyPhrase: String): String {
        var encoder = Base64.getUrlEncoder()
        val preparedMessage = prepareMessage(message)
        val transformedMessage = transform(preparedMessage, keyPhrase)
        return encoder.encodeToString(transformedMessage)
    }

    fun decrypt(message: String, keyPhrase: String): String {
        var decoder = Base64.getUrlDecoder()
        val decoded = decoder.decode(message)
        val unTransformedMessage = unTransform(decoded, keyPhrase)
        return unPrepareMessage(unTransformedMessage)
    }

    fun test(secretMessage: String) {
        var phrase = "the wrong trousers"
        var cypher = encrypt(secretMessage, phrase)
        println("cypher = $cypher")
        var message = decrypt(cypher, phrase)
        println("message = $message")
    }


    // convert message to an array of byte with a random length, zero delineates the end of string
    private fun prepareMessage(message: String): ByteArray {
        var length = if (message.length < 10) random(20, 12) else random(message.length * 2, message.length + message.length / 3)
        length = (length + 2) / 3 * 3 // make divisible by 3 to avoid Base64 padding

        var array = ByteArray(length)

        for (i in 0..array.size - 1) {
            when (i.compareTo(message.length)) {
                -1 -> {
                    array[i] = message[i].toByte()
                }
                0 -> {
                    array[i] = 0
                }
                1 -> {
                    array[i] = random(255, 0).toByte()
                }
            }
        }
        return array
    }

    private fun unPrepareMessage(array: ByteArray): String {
        var message = ""
        for (i in 0..array.size - 1) {
            if (array[i] == 0.toByte()) {
                return message
            }
            message += array[i].toChar()
        }
        return message
    }

    private fun createTransformTable(size: Int, seed: Long): IntArray {
        val random = Random(seed)
        val pickFrom = ArrayList<Int>()
        for (i in 0..size - 1) {
            pickFrom.add(i)
        }
        val transformTable = IntArray(size)
        for (i in 0..size - 1) {
            val pick = random.nextInt(pickFrom.size)
            transformTable[i] = pickFrom[pick]
            pickFrom.removeAt(pick)
        }
        return transformTable
    }

    private fun keyToSeed(keyPhrase: String): Long {
        var hash = 1125899906842597L
        for (i in 0..keyPhrase.length - 1) {
            hash = 31 * hash + keyPhrase[i].toInt()
        }
        return hash
    }

    // re-arrange the bits to create the transformed message
    private fun transform(preparedMessage: ByteArray, keyPhrase: String): ByteArray {
        val transformedMassage = ByteArray(preparedMessage.size)
        val transformTable = createTransformTable(preparedMessage.size * 8, keyToSeed(keyPhrase))
        for (i in 0..preparedMessage.size * 8 - 1) {
            val thisValue = testBit(preparedMessage, i)
            setBit(transformedMassage, transformTable[i], thisValue)
        }
        return transformedMassage
    }

    private fun unTransform(preparedMessage: ByteArray, keyPhrase: String): ByteArray {
        val transformedMassage = ByteArray(preparedMessage.size)
        val transformTable = createTransformTable(preparedMessage.size * 8, keyToSeed(keyPhrase))
        for (i in 0..preparedMessage.size * 8 - 1) {
            val thisValue = testBit(preparedMessage, transformTable[i])
            setBit(transformedMassage, i, thisValue)
        }
        return transformedMassage
    }


    private fun bitCount(array: ByteArray): Int {
        return array.size * 8
    }

    private fun testBit(array: ByteArray, bit: Int): Boolean {
        if (bit < bitCount(array)) {
            return array[bit / 8].toInt().and(1.shl(bit % 8)) != 0
        } else {
            return false
        }
    }

    private fun setBit(array: ByteArray, bit: Int, state: Boolean) {
        if (bit < bitCount(array)) {
            var value = array[bit / 8].toInt()
            val oldValue = value
            var mask = 1.shl(bit % 8)
            if (state) {
                value = value.or(mask)
            } else {
                value = value.and(mask.xor(255))
            }
            array[bit / 8] = value.toByte()
        }
    }

}

