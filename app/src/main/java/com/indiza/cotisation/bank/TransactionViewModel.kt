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
    private val transactionDao: TransactionDao,
    private val caisseDao: CaisseDao,
    private val tourDao: TourDao,
    val cycleDao: CycleDao,
    private val tontineMemberDao: TontineMemberDao,
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

    init {
        viewModelScope.launch {
            val caisse = caisseDao.getCaisseById(caisseId) ?: return@launch

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
        participants.forEach { participant ->
            val memberId = participant.memberWithUser.member.id

            when (action) {
                CaisseAction.COTISATION -> {
                    val montant = (caisse.montantFixe ?: 0L) * participant.nombreParts
                    transactionDao.insertTransaction(
                        TransactionEntity(
                            caisseId = caisse.id,
                            memberId = memberId,
                            montant = montant,
                            type = TransactionType.COTISATION,
                            cycleId = cycleId,
                            motif = "Cotisation groupée (${participant.nombreParts} parts)"
                        )
                    )
                }
                CaisseAction.TOUR_ENCAISSER -> {
                    // Pour la cagnotte, on enregistre une sortie par membre planifié
                    val montant = caisse.montantFixe ?: 0L
                    transactionDao.insertTransaction(
                        TransactionEntity(
                            caisseId = caisse.id,
                            memberId = memberId,
                            montant = -montant,
                            type = TransactionType.TOUR_ENCAISSER,
                            cycleId = cycleId,
                            motif = "Remise de cagnotte groupée"
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

    fun calculerMontantTheoriqueCagnotte(
        caisse: CaisseEntity,
        participant: ParticipantWithParts,
        appliquerDettes: Boolean,
        appliquerEchecs: Boolean,
        appliquerPenalites: Boolean
    ): Long {
        val memberId = participant.memberWithUser.member.id
        val montantFixe = caisse.montantFixe ?: 0L

        // 1. BRUT THEORIQUE (Total attendu sur toute la vie de la tontine pour ce membre)
        // Ici on prend le nombre de parts du membre * montant * total des participants (cycles)
        // À ajuster selon votre règle de "cycles théoriques"
        val nbCyclesTheoriques = _createTourState.value.participants.sumOf { it.nombreParts }
        val montantBrut = montantFixe * participant.nombreParts * nbCyclesTheoriques

        // 2. CALCUL DES ECHECS (Retards de cotisations ou cotisations partielles)
        // Somme attendue jusqu'ici vs Somme versée
        var deductionEchecs = 0L
        if (appliquerEchecs) {
            viewModelScope.launch {
                val nbCyclesEcoules = transactionDao.getNombreCyclesEcoules(caisse.id)
                val attenduCumule = nbCyclesEcoules * (montantFixe * participant.nombreParts)
                val reelCumule = transactionDao.getTotalCotiseReel(caisse.id, memberId) ?: 0L
                deductionEchecs = if (attenduCumule > reelCumule) attenduCumule - reelCumule else 0L
            }
        }

        // 3. DETTES (Prêts en cours)
        var deductionDettes = 0L
        if (appliquerDettes) {
            // On récupère le solde du compte prêt (positif = dette pour le membre dans notre logique)
            // Si getEtatDetteMembre renvoie -5000, c'est que le membre doit 5000.
        }

        // 4. PENALITES (Retards et Amendes)
        var deductionPenalites = 0L
        if (appliquerPenalites) {
            // Somme des transactions de type PENALITE
        }

        val resultat = montantBrut - deductionDettes - deductionEchecs - deductionPenalites
        return if (resultat > 0) resultat else 0L
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
        val caisse = caisseDao.getCaisseById(caisseId)

        // ✅ On utilise .first() pour extraire la List<ParticipantWithParts> du Flow
        val participants = tontineMemberDao.getParticipantsByCaisse(caisseId).first()

        _createTourState.value = CreateTourUiState(caisse, participants)
    }

    fun enregistrerCagnotte(
        caisseId: Long,memberId: Long,montant: Long,cycleId: Long,isClose: Boolean,onSuccess: () -> Unit
    ) = viewModelScope.launch {
        val transaction = TransactionEntity(
            caisseId = caisseId,
            memberId = memberId,
            montant = -montant, // Négatif car c'est une sortie de la caisse vers le membre
            type = TransactionType.TOUR_ENCAISSER,
            cycleId = cycleId,
            motif = "Remise de la cagnotte au bénéficiaire"
        )
        transactionDao.insertTransaction(transaction)

       // if(isClose)
            //cloturerEtPreparerSuivant(caisseId, cycleId)

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

    fun enregistrerSortieSecours(
        caisseId: Long,
        memberId: Long,
        montant: Long,
        cycleId: Long,
        motif: MotifSortie,
        description: String
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
                type = TransactionType.SORTIE_SECOURS,
                motif = "Sortie : ${motif.name} - $description"
            )
        )
    }

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
        // 1. Créer le TourEntity
        val tourId = tourDao.insertTour(TourEntity(cycleId = cycleId, numeroTour = 1)) // Le numéro peut être auto-incrémenté

        selections.forEach { (participant, partsLiquidees) ->
            val montantUnitaire = caisseDao.getCaisseById(caisseId)?.montantFixe ?: 0L
            val montantMembre = montantUnitaire * partsLiquidees

            // 2. Enregistrer le bénéficiaire du tour
            tourDao.insertBeneficiaire(
                TourBeneficiaireEntity(
                    tourId = tourId,
                    memberId = participant.memberWithUser.member.id,
                    nombrePartsAttribuees = partsLiquidees,
                    montantRecu = montantMembre
                )
            )


        }
        withContext(Dispatchers.Main) { onSuccess() }
    }

    fun annulerPositionnementCagnotte(
        caisseId: Long,
        memberId: Long,
        cycleId: Long
    ) = viewModelScope.launch {
        // 1. Trouver le tour concerné
        val tourId = tourDao.getTourIdForMember(memberId) ?: return@launch

        // 2. Vérifier si le tour n'est pas déjà verrouillé/terminé
        if (tourDao.isTourTermine(tourId)) {
            // Optionnel : émettre une erreur vers l'UI
            return@launch
        }


        tourDao.deleteBeneficiaire(tourId, memberId)

        // Rafraîchir les données
        loadCreateTourData(caisseId, cycleId)
    }

}