/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.general

import org.egility.library.database.DbQuery
import org.egility.library.dbobject.*
import java.util.*

/**
 * Created by mbrickman on 28/10/15.
 */

object MeasuringServicesData {

    var idCompetitorMeasurer = -1
    
    val selectedDog = Dog()

    fun selectDog(idDog: Int) {
        selectedDog.find(idDog)  
    }
}

