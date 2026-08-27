package com.indiza.cotisation.bank

import com.indiza.cotisation.caisses.TypeInteret
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

// Fonction pour calculer l'écart en mois
fun calculerMoisEntre(dateDebutMs: Long, dateFinMs: Long): Int {
    val debut = Instant.ofEpochMilli(dateDebutMs).atZone(ZoneId.systemDefault()).toLocalDate()
    val fin = Instant.ofEpochMilli(dateFinMs).atZone(ZoneId.systemDefault()).toLocalDate()
    return ChronoUnit.MONTHS.between(debut, fin).toInt()
}

// Fonction de calcul financier corrigée
fun calculerMontantDu(pret: PretEntity, dateActuelle: Long): Long {
    val moisEcoules = calculerMoisEntre(pret.datePret, dateActuelle)
    // On soustrait le différé sans descendre en dessous de 0
    val moisEffectifs = (moisEcoules - pret.moisDiffere).coerceAtLeast(0)

    if (moisEffectifs == 0) return pret.montantPrincipal

    return when (pret.typeInteret) {
        TypeInteret.SIMPLE -> {
            val interets = pret.montantPrincipal * (pret.tauxInteretApplique / 100) * moisEffectifs
            (pret.montantPrincipal + interets).toLong()
        }
        TypeInteret.COMPOSE -> {
            val total = pret.montantPrincipal * Math.pow(1 + (pret.tauxInteretApplique / 100), moisEffectifs.toDouble())
            total.toLong()
        }
    }
}