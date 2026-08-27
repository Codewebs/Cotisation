package com.indiza.cotisation.users

object MatriculeGenerator {
    private const val CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"

    fun generate(): String {
        val now = java.util.Calendar.getInstance()

        // 1. On récupère les secondes et minutes actuelles (0 à 3599)
        // Cela garantit que deux matricules générés à 1s d'intervalle sont différents
        val secondsInHour = (now.get(java.util.Calendar.MINUTE) * 60) +
                now.get(java.util.Calendar.SECOND)

        // 2. Variable aléatoire cryptographique pour l'entropie
        // 36^2 = 1296 possibilités pour ces deux seuls caractères
        val randomEntropy = java.security.SecureRandom().nextInt(1296)

        return buildString {
            append(toBase36(secondsInHour, 3)) // Temporel (ex: 2RF)
            append(toBase36(randomEntropy, 2)) // Aléatoire (ex: 4X)
        }
    }

    private fun toBase36(value: Int, padTo: Int): String {
        var res = ""
        var num = value
        if (num == 0) res = "0"
        while (num > 0) {
            res = CHARS[num % 36] + res
            num /= 36
        }
        return res.padStart(padTo, '0')
    }
}