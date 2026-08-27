package com.indiza.cotisation.caisses

fun initialiserCaisseParType(type: CaisseType): CaisseDraft {
    return CaisseDraft(type = type).apply {
        when (type) {
            CaisseType.PRET -> {
                possibilitePret = true
                redistribution = false
                typeInteret = TypeInteret.SIMPLE
            }
            CaisseType.COTISATION_SIMPLE -> {
                libelle = "Cotisation"
                redistribution = true
                possibilitePret = false
                penaliteRetardFixe = 0
                penaliteEchecFixe = 0
            }
            else -> {
                possibilitePret = false
                redistribution = false
            }
        }
    }
}

