package com.indiza.cotisation.caisses

import java.util.Calendar

enum class FrequenceCotisation {
    JOURNALIER, HEBDOMADAIRE, MENSUEL, TRIMESTRIEL, SEMESTRIEL, ANNUEL, LIBRE
}

fun FrequenceCotisation.prochaineDate(dateDepart: Long): Long {
    val calendar = Calendar.getInstance().apply { timeInMillis = dateDepart }

    when (this) {
        FrequenceCotisation.JOURNALIER -> calendar.add(Calendar.DAY_OF_YEAR, 1)
        FrequenceCotisation.HEBDOMADAIRE -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
        FrequenceCotisation.MENSUEL -> calendar.add(Calendar.MONTH, 1)
        FrequenceCotisation.TRIMESTRIEL -> calendar.add(Calendar.MONTH, 3)
        FrequenceCotisation.SEMESTRIEL -> calendar.add(Calendar.MONTH, 6)
        FrequenceCotisation.ANNUEL -> calendar.add(Calendar.YEAR, 1)
        FrequenceCotisation.LIBRE -> {}
    }
    return calendar.timeInMillis
}