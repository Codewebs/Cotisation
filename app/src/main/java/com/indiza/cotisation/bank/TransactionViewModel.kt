package com.indiza.cotisation.bank

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indiza.cotisation.CaisseAction
import com.indiza.cotisation.caisses.CaisseDao
import com.indiza.cotisation.caisses.CaisseDetailUiState
import com.indiza.cotisation.caisses.CaisseEntity
import com.indiza.cotisation.caisses.FrequenceCotisation
import com.indiza.cotisation.caisses.prochaineDate
import com.indiza.cotisation.tontine.TontineMemberDao
import com.indiza.cotisation.tontine.TontineMemberWithUser
import com.indiza.cotisation.tontinemembers.ParticipantWithParts
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class TransactionViewModel @Inject constructor(
    val transactionDao: TransactionDao,
    val caisseDao: CaisseDao,
    val tourDao: TourDao,
    val cycleDao: CycleDao,
    val tontineMemberDao: TontineMemberDao,
    savedStateHandle: SavedStateHandle
) : ViewModel()
{

    private val _createTourState = MutableStateFlow(CreateTourUiState())
    val createTourState: StateFlow<CreateTourUiState> = _createTourState.asStateFlow()

    private val caisseId: Long = savedStateHandle.get<Long>("caisseId") ?: 0L
    private val memberId: Long? = savedStateHandle.get<Long>("memberId")
    val cycleId: Long = savedStateHandle.get<Long>("cycleId") ?: 0L

    private val _nouveauCycleId = MutableStateFlow<Long?>(null)
    val nouveauCycleId = _nouveauCycleId.asStateFlow()

    private val _uiState = MutableStateFlow<Pair<CaisseEntity, ParticipantWithParts>?>(null)
    val uiState: StateFlow<Pair<CaisseEntity, ParticipantWithParts>?> = _uiState

    private val _soldeActuel = MutableStateFlow(0L)
    val soldeActuel = _soldeActuel.asStateFlow()

    init {
        viewModelScope.launch {
            val caisse = caisseDao.getCaisseById(caisseId) ?: return@launch
            
            launch {
                transactionDao.getSoldeActuel(caisseId)?.let {
                    _soldeActuel.value = it
                }
            }

            val participant = memberId?.let {
                tontineMemberDao.getParticipantWithParts(it, caisseId)
            }

            if (participant != null) {
                _uiState.value = caisse to participant
            }
        }
    }


    fun validerEnMasse(
        caisse: CaisseEntity,
        action: CaisseAction,
        participants: List<ParticipantWithParts>,
        cycleId: Long
    ) = viewModelScope.launch {
        val montantUnitaire = caisse.montantFixe ?: 0L
        
        // Calcul du nombre total de parts pour le montant de la cagnotte
        val allMembers = tontineMemberDao.getParticipantsByCaisseSnapshot(caisse.id)
        val totalPartsCaisse = allMembers.sumOf { it.nombreParts }
        val montantCagnotte = montantUnitaire * totalPartsCaisse

        if (action == CaisseAction.TOUR_ENCAISSER) {
            // Vérification du solde global avant validation en masse
            val soldeActuel = transactionDao.getSoldeActuel(caisse.id) ?: 0L
            val totalNecessaire = participants.size * montantCagnotte
            
            if (soldeActuel < totalNecessaire) {
                println("DEBUG_CAGNOTTE: Solde insuffisant pour validation en masse ($soldeActuel < $totalNecessaire)")
                return@launch
            }
        }

        participants.forEach { participant ->
            val memberId = participant.memberWithUser.member.id

            when (action) {
                CaisseAction.COTISATION -> {
                    val montant = montantUnitaire * participant.nombreParts
                    transactionDao.insertTransaction(
                        TransactionEntity(
                            caisseId = caisse.id,
                            memberId = memberId,
                            montant = montant,
                            type = TransactionType.COTISATION,
                            cycleId = cycleId,
                            motif = "Cotisation groupée (Cycle n°$cycleId, ${participant.nombreParts} parts)"
                        )
                    )
                }
                CaisseAction.TOUR_ENCAISSER -> {
                    // 1. Chercher le tour planifié
                    val tourPlanifieId = tourDao.getTourIdForMemberInCycleSnapshot(memberId, cycleId, 0)
                    
                    if (tourPlanifieId != null) {
                        tourDao.markTourAsTermine(tourPlanifieId)
                    } else {
                        // Créer un tour ponctuel si aucun plan n'existe
                        val prochainNumero = tourDao.countToursByCycle(cycleId) + 1
                        val tourId = tourDao.insertTour(
                            TourEntity(
                                cycleId = cycleId,
                                numeroTour = prochainNumero,
                                montantTotalDistribue = montantCagnotte,
                                estTermine = true
                            )
                        )
                        tourDao.insertBeneficiaire(
                            TourBeneficiaireEntity(
                                tourId = tourId,
                                memberId = memberId,
                                nombrePartsAttribuees = 1,
                                montantRecu = montantCagnotte
                            )
                        )
                    }

                    // 2. Transaction financière
                    transactionDao.insertTransaction(
                        TransactionEntity(
                            caisseId = caisse.id,
                            memberId = memberId,
                            montant = -montantCagnotte,
                            type = TransactionType.TOUR_ENCAISSER,
                            cycleId = cycleId,
                            motif = "Remise de cagnotte groupée (Cycle n°$cycleId)"
                        )
                    )
                }
                else -> {}
            }
        }
    }

    fun cloturerEtPreparerSuivant(caisseId: Long, cycleActuel: CycleEntity) = viewModelScope.launch {
        // 1. On récupère d'abord l'objet Caisse pour connaître sa fréquence
        val caisse = caisseDao.getCaisseById(caisseId) ?: return@launch
        // 1. Clôturer le cycle actuel
        cycleDao.updateCycle(cycleActuel.copy(estCloture = true, dateFin = System.currentTimeMillis()))

        val frequence = caisse.frequence

        // 2. Calculer la date du prochain cycle
        val dateSuivante = frequence.prochaineDate(cycleActuel.dateDebut)

        // 3. Créer le nouveau cycle
        val nouveauCycle = CycleEntity(
            caisseId = caisseId,
            numeroCycle = cycleActuel.numeroCycle + 1,
            dateDebut = dateSuivante,
            estCloture = false
        )
        val id = cycleDao.insertCycle(nouveauCycle)

        _nouveauCycleId.value = id
    }

    fun clearNavigationRequest() {
        _nouveauCycleId.value = null
    }

    suspend fun getInfoFinanciereCagnotte(caisseId: Long, cycleId: Long): Pair<Long, Long> {
        val cotisations = transactionDao.getSommeCotisationsCycle(caisseId, cycleId) ?: 0L
        val soldeCaisse = transactionDao.getSoldeActuel(caisseId) ?: 0L
        return cotisations to soldeCaisse
    }

    suspend fun getCagnotteDetails(
        caisse: CaisseEntity,
        participant: ParticipantWithParts,
        cycleId: Long
    ): CagnotteDetails {
        val memberId = participant.memberWithUser.member.id
        val montantFixe = caisse.montantFixe ?: 0L

        // 1. BRUT (Jackpot théorique du cycle)
        // Somme des parts des membres participant à cette caisse
        val totalPartsCaisse = transactionDao.getTotalPartsCaisse(caisse.id) ?: 0
        val montantBrut = montantFixe * participant.nombreParts * totalPartsCaisse

        // 2. ECHECS (Retards de cotisations sur la durée de la caisse)
        val nbCyclesEcoules = transactionDao.getNombreCyclesEcoules(caisse.id)
        val attenduCumule = nbCyclesEcoules * (montantFixe * participant.nombreParts)
        val reelCumule = transactionDao.getTotalCotiseReel(caisse.id, memberId) ?: 0L
        val echecs = if (attenduCumule > reelCumule) attenduCumule - reelCumule else 0L

        // 3. DETTES (Prêts et intérêts en cours)
        val detteSum = transactionDao.getEtatDetteMembre(memberId, caisse.id).first() ?: 0L
        val dettes = if (detteSum < 0) -detteSum else 0L

        // 4. PENALITES
        val penalitesSum = transactionDao.getSommePenalitesMembre(memberId, caisse.id).first() ?: 0L
        val penalites = if (penalitesSum < 0) -penalitesSum else 0L

        return CagnotteDetails(
            brut = montantBrut,
            dettes = dettes,
            echecs = echecs,
            penalites = penalites
        )
    }


    private fun createPenaltyEntry(caisse: CaisseEntity, memberId: Long, cycleId: Long, montant: Long, type: TransactionType) =
        TransactionEntity(
            caisseId = caisse.id,
            memberId = memberId,
            montant = -montant,
            type = type,
            cycleId = cycleId,
            motif = "Pénalité ${if(type == TransactionType.PENALITE_RETARD) "Retard" else "Échec"} appliquée par le trésorier"
        )

    fun enregistrerCotisation(
        caisse: CaisseEntity,
        memberId: Long,
        montant: Long,
        cycleId: Long,
        appliquerRetard: Boolean,
        appliquerEchec: Boolean
    ) = viewModelScope.launch {

        // 1. Transaction principale (Cotisation)
        transactionDao.insertTransaction(
            TransactionEntity(
                caisseId = caisse.id,
                memberId = memberId,
                montant = montant,
                type = TransactionType.COTISATION,
                cycleId = cycleId,
            )
        )

        // 2. Pénalité Retard
        if (appliquerRetard && caisse.penaliteRetardFixe != null) {
            transactionDao.insertTransaction(createPenaltyEntry(caisse, memberId, cycleId, caisse.penaliteRetardFixe!!, TransactionType.PENALITE_RETARD))
        }
        // 3. Pénalité Échec
        if (appliquerEchec && caisse.penaliteEchecFixe != null) {
            transactionDao.insertTransaction(createPenaltyEntry(caisse, memberId, cycleId, caisse.penaliteEchecFixe!!, TransactionType.PENALITE_ECHEC))
        }

    }

    fun loadCreateTourData(caisseId: Long, cycleId: Long) = viewModelScope.launch {
        println("DEBUG_TOUR: === Chargement données création tour ===")
        println("DEBUG_TOUR: Caisse ID: $caisseId, Cycle ID: $cycleId")
        
        val caisse = caisseDao.getCaisseById(caisseId)
        val cycle = cycleDao.getCycleById(cycleId)
        val currentCycleNum = cycle?.numeroCycle ?: 1
        val solde = transactionDao.getSoldeActuel(caisseId) ?: 0L

        // 1. Récupérer tous les participants de la caisse avec leur nombre de parts
        val allParticipants = tontineMemberDao.getParticipantsByCaisseSnapshot(caisseId)
        val totalPartsCaisse = allParticipants.sumOf { it.nombreParts }

        // 2. Récupérer les parts déjà encaissées CUMULÉES jusqu'à ce cycle (Tours terminés)
        val partsEncaisseesCumulees = tourDao.getPartsEncaisseesJusquauCycleSnapshot(caisseId, currentCycleNum)
            .associate { it.memberId to it.count }

        // 3. Récupérer les parts déjà "planifiées" (Tours non terminés dans le cycle actuel)
        val partsPlanifiees = tourDao.getPartsPlanifieesParMembreDuCycleSnapshot(cycleId)
            .associate { it.memberId to it.count }

        // 4. Filtrer pour ne garder que ceux qui ont encore des parts à percevoir
        // On compare le total des parts du membre avec (encaissées cumulées + planifiées actuelles)
        val participantsRestants = allParticipants.filter { p ->
            val memberId = p.member.id
            val dejaRecu = partsEncaisseesCumulees[memberId] ?: 0
            val dejaPlanifie = partsPlanifiees[memberId] ?: 0
            val totalAttribue = dejaRecu + dejaPlanifie
            
            totalAttribue < p.nombreParts
        }

        _createTourState.value = CreateTourUiState(
            caisse = caisse,
            cycle = cycle,
            participants = participantsRestants,
            partsEncaisseesMap = partsEncaisseesCumulees,
            soldeGlobal = solde,
            totalPartsCaisse = totalPartsCaisse,
            ordreFige = caisse?.ordreFige ?: false
        )
    }



    fun enregistrerCagnotte(
        caisseId: Long,
        memberId: Long,
        montant: Long,
        cycleId: Long,
        nbParts: Int = 1,
        isClose: Boolean,
        onSuccess: () -> Unit
    ) = viewModelScope.launch {
        // 1. Vérification du solde
        val soldeActuel = transactionDao.getSoldeActuel(caisseId) ?: 0L
        if (soldeActuel < montant) {
            println("DEBUG_CAGNOTTE: Solde insuffisant ($soldeActuel < $montant)")
            return@launch
        }

        // 2. Chercher s'il existe un tour planifié (non terminé) pour ce membre dans ce cycle
        val tourPlanifieId = tourDao.getTourIdForMemberInCycleSnapshot(memberId, cycleId, 0)
        
        val tourId = if (tourPlanifieId != null) {
            println("DEBUG_CAGNOTTE: Tour planifié trouvé ($tourPlanifieId). Finalisation...")
            tourDao.markTourAsTermine(tourPlanifieId)
            tourPlanifieId
        } else {
            println("DEBUG_CAGNOTTE: Aucun tour planifié. Création d'un nouveau tour direct...")
            val prochainNumero = tourDao.countToursByCycle(cycleId) + 1
            tourDao.insertTour(
                TourEntity(
                    cycleId = cycleId,
                    numeroTour = prochainNumero,
                    montantTotalDistribue = montant,
                    estTermine = true // Attribution directe
                )
            )
        }

        // 3. Enregistrer le bénéficiaire si c'est un nouveau tour (sinon il y est déjà)
        if (tourPlanifieId == null) {
            tourDao.insertBeneficiaire(
                TourBeneficiaireEntity(
                    tourId = tourId,
                    memberId = memberId,
                    nombrePartsAttribuees = nbParts,
                    montantRecu = montant
                )
            )
        }

        // 4. Transaction financière (C'est ICI que l'argent est réellement déduit)
        val transaction = TransactionEntity(
            caisseId = caisseId,
            memberId = memberId,
            montant = -montant, 
            type = TransactionType.TOUR_ENCAISSER,
            cycleId = cycleId,
            motif = "Remise de la cagnotte (Cycle n°$cycleId, $nbParts parts) - Finalisation"
        )
        transactionDao.insertTransaction(transaction)

        withContext(Dispatchers.Main) { onSuccess() }
    }

    fun enregistrerRemiseCagnotteMultiParts(
        caisse: CaisseEntity,
        beneficiaire: ParticipantWithParts,
        nombrePartsABouffer: Int,
        cycleId: Long,
        tourId: Int,
        onSuccess: () -> Unit
    ) = viewModelScope.launch {
        // Calcul du montant basé sur les parts
        val montantUnitaire = caisse.montantFixe ?: 0L
        val montantTotalRemise = montantUnitaire * nombrePartsABouffer

        // 1. Création du Tour
        val tourId = tourDao.insertTour(
            TourEntity(cycleId = cycleId, numeroTour = tourId)
        )

        // 2. Enregistrement de la transaction financière (Négatif car sortie de caisse)
        val transaction = TransactionEntity(
            caisseId = caisse.id,
            memberId = beneficiaire.memberWithUser.member.id,
            montant = -montantTotalRemise,
            type = TransactionType.TOUR_ENCAISSER,
            cycleId = cycleId,
            motif = "Remise de $nombrePartsABouffer parts de cagnotte"
        )
        transactionDao.insertTransaction(transaction)

        // 3. Liaison bénéficiaire/tour
        tourDao.insertBeneficiaire(
            TourBeneficiaireEntity(
                tourId = tourId,
                memberId = beneficiaire.memberWithUser.member.id,
                nombrePartsAttribuees = nombrePartsABouffer,
                montantRecu = montantTotalRemise
            )
        )

        withContext(Dispatchers.Main) { onSuccess() }
    }

    suspend fun estPretPourCagnotte(caisseId: Long, cycleId: Long): Boolean {
        val totalParticipants = tontineMemberDao.getParticipantsCount(caisseId)
        val cotisationsRecues = transactionDao.getNombreCotisationsEnregistrees(caisseId, cycleId)

        // Le cycle est prêt si chaque membre a une entrée "COTISATION"
        return cotisationsRecues >= totalParticipants && totalParticipants > 0
    }

    fun enregistrerRemboursement(
        caisseId: Long,
        memberId: Long,
        montantVerse: Long,
        cycleId: Long,
        estInteret: Boolean = false
    ) = viewModelScope.launch {
        transactionDao.insertTransaction(
            TransactionEntity(
                caisseId = caisseId,
                memberId = memberId,
                montant = montantVerse,
                cycleId = cycleId,
                type = if (estInteret) TransactionType.INTERET else TransactionType.REMBOURSEMENT,
                motif = if (estInteret) "Paiement des intérêts" else "Remboursement partiel/total capital"
            )
        )
    }

    fun enregistrerNonCotisation(
        caisse: CaisseEntity,
        memberId: Long,
        cycleId: Long,
        appliquerPenalite: Boolean
    ) = viewModelScope.launch {
        // 1. Enregistre une transaction à 0 FCFA marquée "a_cotise = false"
        transactionDao.insertTransaction(
            TransactionEntity(
                caisseId = caisse.id,
                memberId = memberId,
                montant = 0L,
                type = TransactionType.COTISATION,
                cycleId = cycleId,
                a_cotise = false,
                motif = "Non cotisé pour ce tour"
            )
        )

        // 2. On applique quand même les pénalités si nécessaire
        if (appliquerPenalite && caisse.penaliteRetardFixe != null) {
            transactionDao.insertTransaction(
                TransactionEntity(
                    caisseId = caisse.id,
                    memberId = memberId,
                    montant = -caisse.penaliteRetardFixe!!,
                    type = TransactionType.PENALITE_RETARD,
                    cycleId = cycleId,
                    motif = "Pénalité pour RETARD non-cotisation"
                )
            )
        }
        if (appliquerPenalite && caisse.penaliteEchecFixe != null) {
            transactionDao.insertTransaction(
                TransactionEntity(
                    caisseId = caisse.id,
                    memberId = memberId,
                    montant = -caisse.penaliteEchecFixe!!,
                    type = TransactionType.PENALITE_ECHEC,
                    cycleId = cycleId,
                    motif = "Pénalité pour Echec non-cotisation"
                )
            )
        }
    }

    private suspend fun checkAndApplyPenalties(caisse: CaisseEntity, memberId: Long, dateSaisie: Long, cycleId: Long) {
        val jourLimite = caisse.dateLimiteJour ?: return
        val calendar = Calendar.getInstance().apply { timeInMillis = dateSaisie }
        val jourActuel = calendar.get(Calendar.DAY_OF_MONTH)

        if (jourActuel > jourLimite && caisse.penaliteRetardFixe != null) {
            transactionDao.insertTransaction(
                TransactionEntity(
                    caisseId = caisse.id,
                    memberId = memberId,
                    cycleId = cycleId,
                    montant = -caisse.penaliteRetardFixe!!, // Les pénalités sont des dettes/sorties
                    type = TransactionType.PENALITE_RETARD,
                    motif = "Retard de cotisation (Limite : le $jourLimite)"
                )
            )
        }
    }

    fun enregistrerSortie(
        caisseId: Long,
        memberId: Long,
        montant: Long,
        cycleId: Long,
        motif: MotifSortie,
        description: String,
        type: TransactionType = TransactionType.SORTIE_SECOURS
    ) = viewModelScope.launch {
        // 1. Enregistrer le détail de la sortie
        val sortie = SortieCaisseEntity(
            caisseId = caisseId,
            memberId = memberId,
            montant = montant,
            motif = motif,
            description = description
        )

        // 2. Enregistrer la transaction financière (Négative car l'argent sort de la caisse)
        transactionDao.insertTransaction(
            TransactionEntity(
                caisseId = caisseId,
                memberId = memberId,
                cycleId = cycleId,
                montant = -montant, // L'argent quitte la caisse
                type = type,
                motif = "Sortie : ${motif.name} - $description"
            )
        )
    }

    // Garder pour compatibilité si nécessaire, ou rediriger
    fun enregistrerSortieSecours(
        caisseId: Long,
        memberId: Long,
        montant: Long,
        cycleId: Long,
        motif: MotifSortie,
        description: String
    ) = enregistrerSortie(caisseId, memberId, montant, cycleId, motif, description, TransactionType.SORTIE_SECOURS)

    fun enregistrerPret(
        caisseId: Long,
        memberId: Long,
        montant: Long,
        cycleId: Long,
        moisDiffere: Int,
        onSuccess: () -> Unit
    ) = viewModelScope.launch(Dispatchers.IO) {
        val transaction = TransactionEntity(
            caisseId = caisseId,
            memberId = memberId,
            cycleId = cycleId,
            montant = -montant, // Un prêt est une sortie d'argent
            type = TransactionType.PRET,
            motif = if (moisDiffere > 0) "Prêt avec différé de $moisDiffere mois" else "Prêt simple"
        )
        transactionDao.insertTransaction(transaction)
        withContext(Dispatchers.Main) {
            onSuccess()
        }
    }

    /*fun getJournalTransactions(caisseId: Long): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsByCaisse(caisseId)
    }*/

    fun getEtatDetteMembre(caisseId: Long, memberId: Long): Flow<Long> {
        return transactionDao.getEtatDetteMembre(caisseId, memberId)
            .map { it ?: 0L } // Convertir null en 0L
    }

    fun getJournalTransactions(caisseId: Long): Flow<List<TransactionWithMember>> {
        return transactionDao.getTransactionsWithMemberByCaisse(caisseId)
    }

    fun validerTourComplet(
        caisseId: Long,
        cycleId: Long,
        selections: List<Pair<ParticipantWithParts, Int>>,
        onSuccess: () -> Unit
    ) = viewModelScope.launch {
        println("DEBUG_TOUR: === Validation du tour complet ===")
        println("DEBUG_TOUR: Caisse ID: $caisseId, Cycle ID: $cycleId")
        println("DEBUG_TOUR: Nombre de sélections: ${selections.size}")
        
        val caisse = caisseDao.getCaisseById(caisseId) ?: return@launch
        val montantUnitaire = caisse.montantFixe ?: 0L
        
        // 1. Calculer le nombre total de parts dans la caisse
        val allParticipants = tontineMemberDao.getParticipantsByCaisseSnapshot(caisseId)
        val totalPartsCaisse = allParticipants.sumOf { it.nombreParts }

        // 2. Créer un tour PAR bénéficiaire pour permettre une validation individuelle ultérieure
        selections.forEach { (participant, partsLiquidees) ->
            val prochainNumero = tourDao.countToursByCycle(cycleId) + 1
            val montantMembre = montantUnitaire * totalPartsCaisse * partsLiquidees
            
            println("DEBUG_TOUR: Création Tour n°$prochainNumero pour ${participant.member.nomSaisi}")
            println("DEBUG_TOUR:   - Montant total prévu: $montantMembre")
            println("DEBUG_TOUR:   - Parts liquidées: $partsLiquidees")

            val tourId = tourDao.insertTour(
                TourEntity(
                    cycleId = cycleId,
                    numeroTour = prochainNumero,
                    montantTotalDistribue = montantMembre,
                    estTermine = false // Planification
                )
            )

            tourDao.insertBeneficiaire(
                TourBeneficiaireEntity(
                    tourId = tourId,
                    memberId = participant.member.id,
                    nombrePartsAttribuees = partsLiquidees,
                    montantRecu = montantMembre
                )
            )
            println("DEBUG_TOUR: Tour inséré avec ID: $tourId")
        }
        println("DEBUG_TOUR: =========================================")
        
        withContext(Dispatchers.Main) { onSuccess() }
    }

    fun annulerPositionnementCagnotte(
        caisseId: Long,
        memberId: Long,
        cycleId: Long
    ) = viewModelScope.launch {
        // 1. Trouver le tour concerné
        val tourId = tourDao.getTourIdForMember(memberId) ?: return@launch
        val tour = tourDao.getTourByIdSnapshot(tourId) ?: return@launch

        // 2. Vérification temporelle (moins d'une heure)
        val uneHeureEnMillis: Long = 3600L * 1000L
        val estRecent = (System.currentTimeMillis() - tour.dateTour) < uneHeureEnMillis

        if (!estRecent) {
            // Optionnel: Notifier l'UI que le délai est dépassé
            return@launch
        }

        // 3. Vérifier si le tour n'est pas déjà verrouillé/terminé
        if (tourDao.isTourTermine(tourId)) {
            return@launch
        }


        tourDao.deleteBeneficiaire(tourId, memberId)

        // 4. Supprimer la transaction financière associée pour rétablir le solde
        transactionDao.deleteTransactionCagnotte(caisseId, memberId, cycleId)

        // Rafraîchir les données
        loadCreateTourData(caisseId, cycleId)
    }

    suspend fun getPartsPlanifiees(memberId: Long, cycleId: Long): Int? {
        return tourDao.getPartsPlanifieesParMembre(memberId, cycleId)
    }

}