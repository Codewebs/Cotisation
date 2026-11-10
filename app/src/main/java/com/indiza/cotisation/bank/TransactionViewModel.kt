package com.indiza.cotisation.bank

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indiza.cotisation.CaisseAction
import com.indiza.cotisation.caisses.CaisseDao
import com.indiza.cotisation.caisses.CaisseDetailUiState
import com.indiza.cotisation.caisses.CaisseEntity
import com.indiza.cotisation.caisses.FrequenceCotisation
import com.indiza.cotisation.caisses.TypeInteret
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
    val pretDao: com.indiza.cotisation.bank.PretDao,
    private val sessionDao: com.indiza.cotisation.users.SessionDao,
    private val repository: com.indiza.cotisation.tontine.TontineRepository,
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


    private suspend fun checkFrequencyStrict(caisse: CaisseEntity, memberId: Long, userId: String): Result<Unit> {
        if (!caisse.respecterFrequenceStrictement) return Result.success(Unit)

        val lastDate = transactionDao.getLastContributionDate(caisse.id, memberId)
        if (lastDate != null) {
            if (!isPeriodElapsed(caisse.frequence, lastDate)) {
                val tontine = caisseDao.getTontineByCaisse(caisse.id)
                val userRole = tontineMemberDao.getUserRoleInTontine(caisse.tontineId, userId)
                val isInitiator = tontine?.creatorId == userId
                val isTreasurer = userRole == com.indiza.cotisation.users.Role.TRESORIER || userRole == com.indiza.cotisation.users.Role.INITIATEUR

                if (!isInitiator && !isTreasurer) {
                    val remaining = getRemainingTimeMessage(caisse.frequence, lastDate)
                    return Result.failure(Exception("Membre ID $memberId : prochain encaissement possible dans $remaining. (Dernière le : ${formatDate(lastDate)})"))
                }
            }
        }
        return Result.success(Unit)
    }

    fun validerEnMasse(
        caisse: CaisseEntity,
        action: CaisseAction,
        participants: List<ParticipantWithParts>,
        cycleId: Long,
        onResult: (Result<Unit>) -> Unit = {}
    ) = viewModelScope.launch {
        val session = sessionDao.getCurrentSession()
        val userId = session?.userId ?: ""
        val montantUnitaire = caisse.montantFixe ?: 0L
        
        // 1. Calcul du nombre total de parts pour le montant de la cagnotte
        val allMembers = tontineMemberDao.getParticipantsByCaisseSnapshot(caisse.id)
        val totalParticipants = allMembers.size
        val montantCagnotteUnePart = montantUnitaire * totalParticipants

        if (action == CaisseAction.TOUR_ENCAISSER) {
            // ... (keep existing check)
            val soldeActuel = transactionDao.getSoldeActuel(caisse.id) ?: 0L
            val totalNecessaire = participants.sumOf { it.nombreParts * montantCagnotteUnePart }
            
            if (soldeActuel < totalNecessaire) {
                onResult(Result.failure(Exception("Solde insuffisant pour validation en masse")))
                return@launch
            }
        }

        // --- NOUVEAU : Vérification fréquence pour TOUS les participants avant de commencer ---
        if (action == CaisseAction.COTISATION && caisse.respecterFrequenceStrictement) {
            participants.forEach { p ->
                val check = checkFrequencyStrict(caisse, p.memberWithUser.member.id, userId)
                if (check.isFailure) {
                    withContext(Dispatchers.Main) { onResult(check) }
                    return@launch
                }
            }
        }

        participants.forEach { participant ->
            val memberId = participant.memberWithUser.member.id

            when (action) {
                CaisseAction.COTISATION -> {
                    val montant = montantUnitaire * participant.nombreParts
                    repository.recordTransaction(
                        TransactionEntity(
                            caisseId = caisse.id,
                            memberId = memberId,
                            montant = montant,
                            type = TransactionType.COTISATION,
                            cycleId = cycleId,
                            validatedById = userId.ifBlank { null },
                            motif = "Cotisation groupée (Cycle n°$cycleId, ${participant.nombreParts} parts)"
                        )
                    )
                }
                CaisseAction.TOUR_ENCAISSER -> {
                    val parts = participant.nombreParts
                    val fraisCaisseTotal = (caisse.fraisCaisse ?: 0L) * parts
                    val montantBrutMembre = montantCagnotteUnePart * parts
                    val montantNetMembre = montantBrutMembre - fraisCaisseTotal

                    // 1. Chercher le tour planifié
                    val tourPlanifieId = tourDao.getTourIdForMemberInCycleSnapshot(memberId, cycleId, 0)
                    
                    if (tourPlanifieId != null) {
                        tourDao.markTourAsTermine(tourPlanifieId)
                        tourDao.updateMontantTour(tourPlanifieId, montantNetMembre)
                        tourDao.updateMontantBeneficiaire(tourPlanifieId, memberId, montantNetMembre)
                    } else {
                        // Créer un tour ponctuel si aucun plan n'existe
                        val prochainNumero = tourDao.countToursByCycle(cycleId) + 1
                        val tourId = tourDao.insertTour(
                            TourEntity(
                                cycleId = cycleId,
                                numeroTour = prochainNumero,
                                montantTotalDistribue = montantNetMembre,
                                estTermine = true
                            )
                        )
                        tourDao.insertBeneficiaire(
                            TourBeneficiaireEntity(
                                tourId = tourId,
                                memberId = memberId,
                                nombrePartsAttribuees = parts,
                                montantRecu = montantNetMembre
                            )
                        )
                    }

                    // 2. Transaction financière
                    repository.recordTransaction(
                        TransactionEntity(
                            caisseId = caisse.id,
                            memberId = memberId,
                            montant = -montantBrutMembre,
                            type = TransactionType.TOUR_ENCAISSER,
                            cycleId = cycleId,
                            validatedById = userId.ifBlank { null },
                            motif = "Remise de cagnotte groupée (Cycle n°$cycleId, $parts parts)"
                        )
                    )

                    // 3. Frais de caisse
                    if (fraisCaisseTotal > 0) {
                        repository.recordTransaction(
                            TransactionEntity(
                                caisseId = caisse.id,
                                memberId = memberId,
                                montant = fraisCaisseTotal,
                                type = TransactionType.FRAIS_CAISSE,
                                cycleId = cycleId,
                                validatedById = userId.ifBlank { null },
                                motif = "Frais de caisse collectés ($parts parts)"
                            )
                        )
                    }
                }
                else -> {}
            }
        }
    }

    fun cloturerEtPreparerSuivant(caisseId: Long, cycleActuel: CycleEntity) = viewModelScope.launch {
        if (cycleActuel.estCloture) return@launch

        // 1. On récupère d'abord l'objet Caisse
        val caisse = caisseDao.getCaisseById(caisseId) ?: return@launch
        
        // --- NOUVEAU : Vérification de la clôture définitive (Redistribution complète) ---
        if (caisse.redistribution) {
            val totalPartsCaisse = transactionDao.getTotalPartsCaisse(caisseId) ?: 0
            val partsAttribuees = tourDao.getTotalPartsDistribueesGlobal(caisseId).first() ?: 0
            
            if (partsAttribuees >= totalPartsCaisse && totalPartsCaisse > 0) {
                cloturerCaisseDefinitivement(cycleActuel) {}
                return@launch
            }
        }

        // 1. Clôturer le cycle actuel
        cycleDao.updateCycle(cycleActuel.copy(estCloture = true, dateFin = System.currentTimeMillis()))

        // 2. Vérifier si un cycle suivant existe déjà (ex: Cycle 2 ou Cycle 3 existait déjà)
        val nextNum = cycleActuel.numeroCycle + 1
        val allCycles = cycleDao.getAllCycles(caisseId).first()
        val existingNext = allCycles.find { it.numeroCycle == nextNum }
        
        if (existingNext != null) {
            _nouveauCycleId.value = existingNext.id
            return@launch
        }

        // 3. Sinon, créer le nouveau cycle suivant automatiquement
        val frequence = caisse.frequence
        val dateSuivante = frequence.prochaineDate(cycleActuel.dateDebut)

        val nouveauCycle = CycleEntity(
            caisseId = caisseId,
            numeroCycle = cycleActuel.numeroCycle + 1,
            dateDebut = dateSuivante,
            estCloture = false
        )
        val id = cycleDao.insertCycle(nouveauCycle)

        _nouveauCycleId.value = id
    }

    fun cloturerCaisseDefinitivement(cycleActuel: CycleEntity, onSuccess: () -> Unit) = viewModelScope.launch {
        cycleDao.updateCycle(cycleActuel.copy(estCloture = true, dateFin = System.currentTimeMillis()))
        
        // Mettre à jour le statut de la caisse
        val caisse = caisseDao.getById(cycleActuel.caisseId)
        if (caisse != null) {
            caisseDao.updateCaisse(caisse.copy(statut = com.indiza.cotisation.caisses.CaisseStatus.CLOTUREE))
        }
        
        withContext(Dispatchers.Main) { onSuccess() }
    }

    fun reouvrirCycle(cycleToOpen: CycleEntity) = viewModelScope.launch {
        val allCycles = cycleDao.getAllCycles(cycleToOpen.caisseId).first()
        allCycles.filter { !it.estCloture && it.id != cycleToOpen.id }.forEach { openCycle ->
            cycleDao.updateCycle(openCycle.copy(estCloture = true, dateFin = System.currentTimeMillis()))
        }
        cycleDao.updateCycle(cycleToOpen.copy(estCloture = false, dateFin = null))
    }

    fun clearNavigationRequest() {
        _nouveauCycleId.value = null
    }

    private fun isPeriodElapsed(frequence: FrequenceCotisation, lastDate: Long): Boolean {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply { timeInMillis = lastDate }
        
        when (frequence) {
            FrequenceCotisation.JOURNALIER -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            FrequenceCotisation.HEBDOMADAIRE -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            FrequenceCotisation.MENSUEL -> calendar.add(Calendar.MONTH, 1)
            FrequenceCotisation.TRIMESTRIEL -> calendar.add(Calendar.MONTH, 3)
            FrequenceCotisation.SEMESTRIEL -> calendar.add(Calendar.MONTH, 6)
            FrequenceCotisation.ANNUEL -> calendar.add(Calendar.YEAR, 1)
            FrequenceCotisation.LIBRE -> return true
        }
        
        return now >= calendar.timeInMillis
    }

    private fun getRemainingTimeMessage(frequence: FrequenceCotisation, lastDate: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = lastDate }
        when (frequence) {
            FrequenceCotisation.JOURNALIER -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            FrequenceCotisation.HEBDOMADAIRE -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            FrequenceCotisation.MENSUEL -> calendar.add(Calendar.MONTH, 1)
            FrequenceCotisation.TRIMESTRIEL -> calendar.add(Calendar.MONTH, 3)
            FrequenceCotisation.SEMESTRIEL -> calendar.add(Calendar.MONTH, 6)
            FrequenceCotisation.ANNUEL -> calendar.add(Calendar.YEAR, 1)
            FrequenceCotisation.LIBRE -> return ""
        }
        
        val nextPossible = calendar.timeInMillis
        val diff = nextPossible - System.currentTimeMillis()
        if (diff <= 0) return ""
        
        val hours = diff / (3600 * 1000)
        val minutes = (diff % (3600 * 1000)) / (60 * 1000)
        val seconds = (diff % (60 * 1000)) / 1000
        
        return "${hours}h ${minutes}min ${seconds}s"
    }

    private fun formatDate(timestamp: Long): String {
        return java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
    }

    suspend fun getInfoFinanciereCagnotte(caisseId: Long, cycleId: Long): Pair<Long, Long> {
        val cotisations = transactionDao.getSommeCotisationsCycle(caisseId, cycleId) ?: 0L
        val soldeCaisse = transactionDao.getSoldeActuel(caisseId) ?: 0L
        return cotisations to soldeCaisse
    }

    suspend fun getCagnotteDetails(
        caisse: CaisseEntity,
        participant: ParticipantWithParts,
        cycleId: Long,
        nbPartsAliquider: Int = 1
    ): CagnotteDetails {
        val memberId = participant.member.id
        val montantFixe = caisse.montantFixe ?: 0L
        println("DEBUG_DETAILS: memberId=$memberId, caisseId=${caisse.id}, cycleId=$cycleId")

        // 1. BRUT (Jackpot théorique du cycle pour le nombre de parts choisi)
        val totalParticipants = tontineMemberDao.getParticipantsCount(caisse.id)
        val montantBrut = montantFixe * nbPartsAliquider * totalParticipants

        // 2. ECHECS (Retards de cotisations sur la durée de la caisse)
        val nbCyclesEcoules = transactionDao.getNombreCyclesEcoules(caisse.id)
        val attenduCumule = nbCyclesEcoules.toLong() * (montantFixe * participant.nombreParts)
        val reelCumule = transactionDao.getTotalCotiseReel(caisse.id, memberId) ?: 0L
        val echecs = if (attenduCumule > reelCumule) attenduCumule - reelCumule else 0L
        println("DEBUG_DETAILS: nbCyclesEcoules=$nbCyclesEcoules, attenduCumule=$attenduCumule, reelCumule=$reelCumule, echecs=$echecs")

        // 3. DETTES (Prêts et intérêts en cours)
        val detteSum = transactionDao.getEtatDetteMembre(caisse.id, memberId).first() ?: 0L
        val dettes = if (detteSum < 0) -detteSum else 0L
        println("DEBUG_DETAILS: dettes=$dettes (Sum was $detteSum)")

        // 4. PENALITES (Déjà enregistrées dans l'historique)
        val penalitesSum = transactionDao.getSommePenalitesMembre(memberId, caisse.id).first() ?: 0L
        var totalPenalites = if (penalitesSum < 0) -penalitesSum else 0L
        println("DEBUG_DETAILS: penalitesSum=$penalitesSum")

        // 5. AJOUT DES PÉNALITÉS "DÉDUCTIBLES" (Potentielles pour le tour actuel)
        
        // A. Pénalité d'échec
        if (caisse.penaliteEchecFixe != null && echecs > 0) {
            val aDejaEchec = transactionDao.aDejaPenaliteTypeCycle(memberId, caisse.id, cycleId, TransactionType.PENALITE_ECHEC)
            println("DEBUG_DETAILS: aDejaEchecTypePENALITE_ECHEC=$aDejaEchec")
            if (!aDejaEchec) {
                totalPenalites += caisse.penaliteEchecFixe
            }
        }

        // B. Pénalité de retard (Découplée des échecs)
        if (caisse.penaliteRetardFixe != null && caisse.dateLimiteJour != null) {
            val estEnRetard = isLate(caisse, System.currentTimeMillis())
            if (estEnRetard) {
                val aDejaRetard = transactionDao.aDejaPenaliteTypeCycle(memberId, caisse.id, cycleId, TransactionType.PENALITE_RETARD)
                if (!aDejaRetard) {
                    totalPenalites += caisse.penaliteRetardFixe
                }
            }
        }

        println("DEBUG_DETAILS: Final totalPenalites=$totalPenalites")

        return CagnotteDetails(
            brut = montantBrut,
            dettes = dettes,
            echecs = echecs,
            penalites = totalPenalites
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
        appliquerEchec: Boolean,
        onResult: (Result<Unit>) -> Unit = {}
    ) = viewModelScope.launch {
        val session = sessionDao.getCurrentSession()
        val userId = session?.userId ?: ""
        
        // --- VÉRIFICATION DE LA FRÉQUENCE STRICTE ---
        val freqCheck = checkFrequencyStrict(caisse, memberId, userId)
        if (freqCheck.isFailure) {
            withContext(Dispatchers.Main) { onResult(freqCheck) }
            return@launch
        }

        // 1. Transaction principale locale (Toujours pour le cache/historique rapide)
        repository.recordTransaction(
            TransactionEntity(
                caisseId = caisse.id,
                memberId = memberId,
                montant = montant,
                type = TransactionType.COTISATION,
                cycleId = cycleId,
                validatedById = userId
            )
        )

        // 2. Pénalités
        if (appliquerRetard && caisse.penaliteRetardFixe != null) {
            repository.recordTransaction(createPenaltyEntry(caisse, memberId, cycleId, caisse.penaliteRetardFixe!!, TransactionType.PENALITE_RETARD))
        }
        if (appliquerEchec && caisse.penaliteEchecFixe != null) {
            repository.recordTransaction(createPenaltyEntry(caisse, memberId, cycleId, caisse.penaliteEchecFixe!!, TransactionType.PENALITE_ECHEC))
        }

        withContext(Dispatchers.Main) { onResult(Result.success(Unit)) }
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
            totalParticipants = allParticipants.size,
            ordreFige = caisse?.ordreFige ?: false
        )
    }



    fun enregistrerCagnotte(
        caisseId: Long,
        memberId: Long,
        montantCaisse: Long, // Montant "Source" (Brut - Echecs si inclus)
        cycleId: Long,
        nbParts: Int = 1,
        montantDettes: Long = 0L,
        montantPenalites: Long = 0L,
        montantEchecs: Long = 0L,
        isOnline: Boolean = false,
        onSuccess: () -> Unit
    ) = viewModelScope.launch {
        val caisse = caisseDao.getCaisseById(caisseId) ?: return@launch
        val fraisCaisseTotal = (caisse.fraisCaisse ?: 0L) * nbParts
        
        // On ne soustrait PAS montantEchecs ici car montantCaisse est déjà (Brut - Echecs)
        val montantNet = montantCaisse - montantDettes - montantPenalites - fraisCaisseTotal

        logCagnotte("DEBUT_REMISE", "Membre $memberId, Montant Brut: $montantCaisse, Net: $montantNet", isOnline)

        // 1. Vérification du solde (le montantCaisse doit être disponible)
        val soldeActuel = transactionDao.getSoldeActuel(caisseId) ?: 0L
        if (soldeActuel < montantCaisse) {
            logCagnotte("ERREUR_SOLDE", "Solde insuffisant ($soldeActuel < $montantCaisse)", isOnline)
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
                    montantTotalDistribue = montantNet,
                    estTermine = true 
                )
            )
        }

        if (tourPlanifieId == null) {
            tourDao.insertBeneficiaire(
                TourBeneficiaireEntity(
                    tourId = tourId,
                    memberId = memberId,
                    nombrePartsAttribuees = nbParts,
                    montantRecu = montantNet
                )
            )
        } else {
             // Mettre à jour le montant reçu si c'était planifié
             tourDao.updateMontantTour(tourPlanifieId, montantNet)
             tourDao.updateMontantBeneficiaire(tourPlanifieId, memberId, montantNet)
        }

        val session = sessionDao.getCurrentSession()

        // 4. Transaction financière : Sortie du montant réel de la caisse pour le tour
        repository.recordTransaction(
            TransactionEntity(
                caisseId = caisseId,
                memberId = memberId,
                montant = -montantCaisse, 
                type = TransactionType.TOUR_ENCAISSER,
                cycleId = cycleId,
                validatedById = session?.userId,
                motif = "Remise de la cagnotte (Cycle n°$cycleId, $nbParts parts) - Montant décaissé"
            )
        )

        // 5. Récupération des frais de caisse
        if (fraisCaisseTotal > 0) {
            repository.recordTransaction(
                TransactionEntity(
                    caisseId = caisseId,
                    memberId = memberId,
                    montant = fraisCaisseTotal,
                    type = TransactionType.FRAIS_CAISSE,
                    cycleId = cycleId,
                    validatedById = session?.userId,
                    motif = "Frais de caisse collectés ($nbParts parts)"
                )
            )
        }

        // 6. Remboursement des dettes si choisies
        if (montantDettes > 0) {
            repository.recordTransaction(
                TransactionEntity(
                    caisseId = caisseId,
                    memberId = memberId,
                    montant = montantDettes,
                    type = TransactionType.REMBOURSEMENT,
                    cycleId = cycleId,
                    validatedById = session?.userId,
                    motif = "Remboursement de dette lors de la remise de cagnotte"
                )
            )
        }

        // 7. Paiement des pénalités si choisies
        if (montantPenalites > 0) {
            repository.recordTransaction(
                TransactionEntity(
                    caisseId = caisseId,
                    memberId = memberId,
                    montant = montantPenalites,
                    type = TransactionType.PENALITE_RETARD, // On utilise un type générique
                    cycleId = cycleId,
                    validatedById = session?.userId,
                    motif = "Paiement des pénalités lors de la remise de cagnotte"
                )
            )
        }

        // 8. Régularisation des échecs si choisis
        if (montantEchecs > 0) {
            repository.recordTransaction(
                TransactionEntity(
                    caisseId = caisseId,
                    memberId = memberId,
                    montant = montantEchecs,
                    type = TransactionType.COTISATION,
                    cycleId = cycleId,
                    a_cotise = true,
                    validatedById = session?.userId,
                    motif = "Régularisation des échecs lors de la remise de cagnotte"
                )
            )
            logCagnotte("ECHECS_REGULARISES", "Montant: $montantEchecs", isOnline)
        }

        logCagnotte("FIN_REMISE", "Succès pour membre $memberId", isOnline)
        withContext(Dispatchers.Main) { onSuccess() }
    }

    private fun logCagnotte(motif: String, details: String, isOnline: Boolean) {
        val tag = "CAGNOTTE_LOG"
        val fullMsg = "[$motif] $details"
        
        // Log Kotlin (Android Logcat)
        println("$tag: $fullMsg")
        
        // Log Web (si en ligne)
        if (isOnline) {
            println("WEB_LOG: $fullMsg")
            // Simulation d'envoi vers un service web de logging
            // repository.sendRemoteLog(motif, details)
        }
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

        val session = sessionDao.getCurrentSession()
        // 2. Enregistrement de la transaction financière (Négatif car sortie de caisse)
        val transaction = TransactionEntity(
            caisseId = caisse.id,
            memberId = beneficiaire.memberWithUser.member.id,
            montant = -montantTotalRemise,
            type = TransactionType.TOUR_ENCAISSER,
            cycleId = cycleId,
            validatedById = session?.userId,
            motif = "Remise de $nombrePartsABouffer parts de cagnotte"
        )
        repository.recordTransaction(transaction)

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
        estInteret: Boolean = false,
        originCaisseId: Long? = null
    ) = viewModelScope.launch {
        val session = sessionDao.getCurrentSession()
        
        // 1. Transaction dans la caisse actuelle (Réduit la dette)
        repository.recordTransaction(
            TransactionEntity(
                caisseId = caisseId,
                memberId = memberId,
                montant = montantVerse,
                cycleId = cycleId,
                type = if (estInteret) TransactionType.INTERET else TransactionType.REMBOURSEMENT,
                motif = if (estInteret) "Paiement des intérêts" else "Remboursement principal",
                validatedById = session?.userId
            )
        )

        // 2. Si c'est du principal et qu'il y a une caisse d'origine, on transfère
        if (!estInteret && originCaisseId != null && originCaisseId != caisseId) {
            // Sortie de la caisse de prêt
            repository.recordTransaction(
                TransactionEntity(
                    caisseId = caisseId,
                    memberId = memberId,
                    montant = -montantVerse,
                    cycleId = cycleId,
                    type = TransactionType.TRANSFERT_SORTIE,
                    motif = "Retour du principal à la caisse d'origine",
                    validatedById = session?.userId
                )
            )
            
            // Entrée dans la caisse d'origine
            repository.recordTransaction(
                TransactionEntity(
                    caisseId = originCaisseId,
                    memberId = memberId,
                    montant = montantVerse,
                    cycleId = cycleId, // TODO: find active cycle of target caisse?
                    type = TransactionType.TRANSFERT_ENTREE,
                    motif = "Récupération du principal d'un prêt",
                    validatedById = session?.userId
                )
            )
        }
    }

    fun enregistrerNonCotisation(
        caisse: CaisseEntity,
        memberId: Long,
        cycleId: Long,
        appliquerPenalite: Boolean
    ) = viewModelScope.launch {
        val session = sessionDao.getCurrentSession()
        val userId = session?.userId
        // 1. Enregistre une transaction à 0 FCFA marquée "a_cotise = false"
        repository.recordTransaction(
            TransactionEntity(
                caisseId = caisse.id,
                memberId = memberId,
                montant = 0L,
                type = TransactionType.COTISATION,
                cycleId = cycleId,
                a_cotise = false,
                validatedById = userId,
                motif = "Non cotisé pour ce tour"
            )
        )

        // 2. On applique quand même les pénalités si nécessaire
        if (appliquerPenalite && caisse.penaliteRetardFixe != null) {
            repository.recordTransaction(
                TransactionEntity(
                    caisseId = caisse.id,
                    memberId = memberId,
                    montant = -caisse.penaliteRetardFixe!!,
                    type = TransactionType.PENALITE_RETARD,
                    cycleId = cycleId,
                    validatedById = userId,
                    motif = "Pénalité pour RETARD non-cotisation"
                )
            )
        }
        if (appliquerPenalite && caisse.penaliteEchecFixe != null) {
            repository.recordTransaction(
                TransactionEntity(
                    caisseId = caisse.id,
                    memberId = memberId,
                    montant = -caisse.penaliteEchecFixe!!,
                    type = TransactionType.PENALITE_ECHEC,
                    cycleId = cycleId,
                    validatedById = userId,
                    motif = "Pénalité pour Echec non-cotisation"
                )
            )
        }
    }

    private fun isLate(caisse: CaisseEntity, dateInMillis: Long): Boolean {
        val jourLimite = caisse.dateLimiteJour ?: return false
        val heureLimite = caisse.heureLimite ?: 23
        val calendar = Calendar.getInstance().apply { timeInMillis = dateInMillis }

        return when (caisse.frequence) {
            FrequenceCotisation.HEBDOMADAIRE -> {
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                val currentDayIso = if (dayOfWeek == Calendar.SUNDAY) 7 else dayOfWeek - 1
                val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                currentDayIso > jourLimite || (currentDayIso == jourLimite && currentHour >= heureLimite)
            }
            FrequenceCotisation.MENSUEL -> {
                val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
                val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                currentDay > jourLimite || (currentDay == jourLimite && currentHour >= heureLimite)
            }
            else -> {
                val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
                val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                currentDay > jourLimite || (currentDay == jourLimite && currentHour >= heureLimite)
            }
        }
    }

    private suspend fun checkAndApplyPenalties(caisse: CaisseEntity, memberId: Long, dateSaisie: Long, cycleId: Long) {
        if (caisse.penaliteRetardFixe != null && isLate(caisse, dateSaisie)) {
            val jourLimite = caisse.dateLimiteJour ?: 0
            val heureLimite = caisse.heureLimite ?: 23
            repository.recordTransaction(
                TransactionEntity(
                    caisseId = caisse.id,
                    memberId = memberId,
                    cycleId = cycleId,
                    montant = -caisse.penaliteRetardFixe!!,
                    type = TransactionType.PENALITE_RETARD,
                    motif = "Retard de cotisation (Limite : Jour $jourLimite à ${heureLimite}h)"
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
        repository.recordTransaction(
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
        originCaisseId: Long? = null,
        onSuccess: () -> Unit
    ) = viewModelScope.launch(Dispatchers.IO) {
        // 1. Transaction dans la caisse de prêt
        val transaction = TransactionEntity(
            caisseId = caisseId,
            memberId = memberId,
            cycleId = cycleId,
            montant = -montant,
            type = TransactionType.PRET,
            motif = if (moisDiffere > 0) "Prêt avec différé de $moisDiffere mois" else "Prêt simple"
        )
        repository.recordTransaction(transaction)

    fun envoyerRelances(caisseId: Long, memberIds: List<Long>, cycleId: Long) = viewModelScope.launch(Dispatchers.IO) {
        val caisse = caisseDao.getCaisseById(caisseId) ?: return@launch
        val session = sessionDao.getCurrentSession() ?: return@launch
        
        memberIds.forEach { memberId ->
            // Enregistrement de la relance (SMS et Push)
            repository.recordTransaction(
                TransactionEntity(
                    caisseId = caisseId,
                    memberId = memberId,
                    cycleId = cycleId,
                    montant = 0,
                    type = TransactionType.COTISATION, // Utilisation d'un type neutre ou créer une nouvelle entrée
                    motif = "Relance envoyée par l'administrateur"
                )
            )
            // L'envoi effectif des SMS/Push se ferait ici via une API dédiée
        }
    }

    // 2. Création de l'entité Prêt pour suivi
        pretDao.insertPret(
            PretEntity(
                caisseId = caisseId,
                memberId = memberId,
                montantPrincipal = montant,
                montantInterets = 0L, // Calculé lors du remboursement ou via taux
                tauxInteretApplique = 0.0, // TODO: fetch from caisse
                typeInteret = TypeInteret.SIMPLE,
                dateEcheance = System.currentTimeMillis() + (30L * 24 * 3600 * 1000 * (moisDiffere + 1)),
                moisDiffere = moisDiffere,
                originCaisseId = originCaisseId
            )
        )

        // 3. Si origine différente, on sort de l'origine pour entrer dans la caisse de prêt
        if (originCaisseId != null && originCaisseId != caisseId) {
            repository.recordTransaction(
                TransactionEntity(
                    caisseId = originCaisseId,
                    memberId = memberId,
                    cycleId = cycleId,
                    montant = -montant,
                    type = TransactionType.TRANSFERT_SORTIE,
                    motif = "Financement prêt caisse"
                )
            )
            repository.recordTransaction(
                TransactionEntity(
                    caisseId = caisseId,
                    memberId = memberId,
                    cycleId = cycleId,
                    montant = montant,
                    type = TransactionType.TRANSFERT_ENTREE,
                    motif = "Import fonds financement prêt"
                )
            )
        }

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

    suspend fun getOriginCaisseId(memberId: Long, caisseId: Long): Long? {
        return pretDao.getActivePret(memberId, caisseId)?.originCaisseId
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
        
        // 1. Calculer le nombre de participants dans la caisse
        val allParticipants = tontineMemberDao.getParticipantsByCaisseSnapshot(caisseId)
        val totalParticipants = allParticipants.size

        // 2. Créer un tour PAR bénéficiaire pour permettre une validation individuelle ultérieure
        selections.forEach { (participant, partsLiquidees) ->
            val prochainNumero = tourDao.countToursByCycle(cycleId) + 1
            val montantMembre = montantUnitaire * totalParticipants * partsLiquidees
            
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

    fun getTontineMembers(tontineId: Long): Flow<List<TontineMemberWithUser>> {
        return tontineMemberDao.getTontineMembersWithUsers(tontineId)
    }

    fun enregistrerMouvementManuel(
        caisseId: Long,
        cycleId: Long,
        montant: Long,
        type: TransactionType,
        motif: String,
        memberId: Long? = null,
        onSuccess: () -> Unit
    ) = viewModelScope.launch {
        val session = sessionDao.getCurrentSession()
        val userId = session?.userId
        
        // Si c'est une sortie, le montant doit être négatif
        val finalAmount = if (type == TransactionType.SORTIE || type == TransactionType.TRANSFERT_SORTIE || type == TransactionType.SORTIE_SECOURS) -montant else montant
        
        // Si memberId n'est pas fourni, on prend l'utilisateur actuel comme "responsable"
        val effectiveMemberId = memberId ?: tontineMemberDao.getMemberEntity(
            tontineId = caisseDao.getCaisseById(caisseId)?.tontineId ?: 0L,
            userId = userId ?: ""
        )?.id ?: 0L

        repository.recordTransaction(
            TransactionEntity(
                caisseId = caisseId,
                memberId = effectiveMemberId,
                cycleId = cycleId,
                montant = finalAmount,
                type = type,
                motif = motif,
                validatedById = userId
            )
        )
        
        withContext(Dispatchers.Main) { onSuccess() }
    }
}
