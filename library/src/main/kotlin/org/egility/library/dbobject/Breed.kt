/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.dbobject

import org.egility.library.database.*
import org.egility.library.general.quoted
import java.util.*

/**
 * Created by mbrickman on 01/12/15.
 */
open class BreedRaw<T: DbTable<T>>(_connection: DbConnection? = null, vararg columnNames: String) : DbTable<T>(_connection, "breed") {

    var id: Int by DbPropertyInt("idBreed")
    var name: String by DbPropertyString("breedName")
    var dateCreated: Date by DbPropertyDate("dateCreated")
    var deviceCreated: Int by DbPropertyInt("deviceCreated")
    var dateModified: Date by DbPropertyDate("dateModified")
    var deviceModified: Int by DbPropertyInt("deviceModified")
    var kcName: String by DbPropertyString("kcName")
    var kcGroup: String by DbPropertyString("kcGroup")

}

class Breed(vararg columnNames: String) : BreedRaw<Breed>(null, *columnNames) {

    companion object {

        private var _allBreeds: Breed? = null

        fun getBreedName(idBreed: Int, unknown: String = "UNKNOWN"): String {
            if (_allBreeds == null) {
                val create = Breed()
                create.select("TRUE")
                _allBreeds = create
            }
            val allBreeds = _allBreeds

            if (allBreeds != null) {
                allBreeds.beforeFirst()
                while (allBreeds.next()) {
                    if (allBreeds.id == idBreed) {
                        return allBreeds.name
                    }
                }
            }
            return unknown
        }

        fun getIdBreed(breedName: String, create: Boolean = false): Int {
            when (breedName) {
                "" -> return 0
                "Cross" -> return 9000
                "Collie X" -> return 9001
                "Working Sheepdog" -> return 252
                "Toy Poodle" -> return 189
                "Standard Poodle" -> return 188
                "Miniature Poodle" -> return 186
                "Labrador" -> return 140
                "Hungarian Wirehaired Vizsla" -> return 236
                "Hungarian Vizsla" -> return 236
                "German Wire Haired Pointer" -> return 110
                "German Short Haired Pointer" -> return 109
                "Belgian Shepherd Dog" -> return 35
                "Kelpie" -> return 24
                "Mexican Hairless" -> return 155
                "Fox Terrier (Wire)" -> return 245
                "Flat Coated Retriever" -> return 105
                "Brittany" -> return 58

            }
            val breed = Breed()
            if (breed.find("breedName=${breedName.quoted}")) {
                return breed.id
            }
            if (create) {
                breed.append()
                breed.name = breedName
                breed.post()
                return breed.id
            }
            return -1
        }


    }

}