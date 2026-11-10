package com.indiza.cotisation.tontine

import androidx.room.Transaction
import com.indiza.cotisation.bank.CycleDao
import com.indiza.cotisation.bank.CycleEntity
import com.indiza.cotisation.bank.TransactionDao
import com.indiza.cotisation.bank.TransactionEntity
import com.indiza.cotisation.bank.TransactionType
import com.indiza.cotisation.caisses.CaisseDraft
import com.indiza.cotisation.caisses.CaisseEntity
import com.indiza.cotisation.caisses.CaisseType
import com.indiza.cotisation.caisses.FrequenceCotisation
import com.indiza.cotisation.caisses.CaisseDao
import com.indiza.cotisation.caisses.toEntity
import com.indiza.cotisation.sync.SyncAction
import com.indiza.cotisation.sync.SyncQueueDao
import com.indiza.cotisation.sync.SyncQueueEntity
import com.indiza.cotisation.sync.SyncStatus
import com.indiza.cotisation.network.SupabaseDiscoveryService
import com.indiza.cotisation.users.Role
import com.indiza.cotisation.users.SessionDao
import com.indiza.cotisation.users.UserDao
import com.indiza.cotisation.users.UserEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class TontineRepository @Inject constructor(
    private val dao: TontineDao,
    private val caisseDao: CaisseDao,
    private val tontineMemberDao: TontineMemberDao,
    private val userDao: UserDao,
    private val cycleDao: CycleDao,
    private val transactionDao: TransactionDao,
    private val sessionDao: SessionDao,
    private val syncQueueDao: SyncQueueDao,
    private val syncManager: com.indiza.cotisation.sync.SyncManager,
    private val apiService: TontineApi,
    private val commentService: SupabaseCommentService,
    private val discoveryService: SupabaseDiscoveryService
)
{

    suspend fun getUserTontinesFlow(userId: String): Flow<List<TontineWithCaisses>> {
        return tontineMemberDao.getUserTontinesWithCaisses(userId)
    }

    fun getTontineWithCaisses(tontineId: Long): Flow<TontineWithCaisses> {
        return dao.getTontineWithCaisses(tontineId)
    }

    suspend fun insertCaisse(tontineId: Long, caisseDraft: CaisseDraft) {
        val tontine = dao.getById(tontineId)
        
        if (tontine?.isOnline == true && tontine.remoteId != null && tontine.syncStatus == SyncStatus.SYNCED) {
            try {
                // DIRECT ONLINE MODE
                val apiDto = com.indiza.cotisation.caisses.CaisseApiDto(
                    id = java.util.UUID.randomUUID().toString(),
                    tontineId = tontine.remoteId,
                    type = caisseDraft.type.name,
                    libelle = caisseDraft.libelle,
                    montantFixe = caisseDraft.montantFixe,
                    montantMinimum = caisseDraft.montantMinimum,
                    frequence = caisseDraft.frequence.name,
                    regulariteImposee = caisseDraft.regulariteImposee,
                    redistribution = caisseDraft.redistribution,
                    tauxInteret = caisseDraft.tauxInteret,
                    typeInteret = caisseDraft.typeInteret?.name,
                    penaliteRetardFixe = caisseDraft.penaliteRetardFixe,
                    penaliteEchecFixe = caisseDraft.penaliteEchecFixe,
                    dateLimiteJour = caisseDraft.dateLimiteJour,
                    fraisCaisse = caisseDraft.fraisCaisse,
                    possibilitePret = caisseDraft.possibilitePret,
                    autoriserPretSurAmendes = caisseDraft.autoriserPretSurAmendes,
                    respecterFrequenceStrictement = caisseDraft.respecterFrequenceStrictement,
                    dureeCycles = caisseDraft.dureeCycles,
                    ordreRedistribution = null,
                    updatedAt = System.currentTimeMillis()
                )
                val created = apiService.createCaisse(apiDto)
                
                val localId = dao.insertCaisse(created.toEntity(tontineId))
                cycleDao.insertCycle(CycleEntity(caisseId = localId, numeroCycle = 1))
                return
            } catch (e: Exception) {
                println("SYNC_ERROR: Fallback to queue for caisse creation: ${e.message}")
            }
        }

        // LOCAL MODE or OFFLINE FALLBACK
        val caisseEntity = CaisseEntity(
            tontineId = tontineId,
            type = caisseDraft.type,
            libelle = caisseDraft.libelle,
            montantFixe = caisseDraft.montantFixe,
            montantMinimum = caisseDraft.montantMinimum,
            frequence = caisseDraft.frequence,
            dureeCycles = caisseDraft.dureeCycles,
            regulariteImposee = caisseDraft.regulariteImposee,
            redistribution = caisseDraft.redistribution,
            tauxInteret = caisseDraft.tauxInteret,
            typeInteret = caisseDraft.typeInteret,
            penaliteRetardFixe = caisseDraft.penaliteRetardFixe,
            penaliteEchecFixe = caisseDraft.penaliteEchecFixe,
            fraisCaisse = caisseDraft.fraisCaisse,
            dateLimiteJour = caisseDraft.dateLimiteJour,
            respecterFrequenceStrictement = caisseDraft.respecterFrequenceStrictement,
            remoteId = if (tontine?.isOnline == true) java.util.UUID.randomUUID().toString() else null,
            syncStatus = if (tontine?.isOnline == true) SyncStatus.PENDING_CREATE else SyncStatus.SYNCED
        )
        val newCaisseId = dao.insertCaisse(caisseEntity)

        if (tontine?.isOnline == true) {
            syncQueueDao.enqueue(
                SyncQueueEntity(
                    entityType = "CAISSE",
                    entityLocalId = newCaisseId,
                    action = SyncAction.CREATE
                )
            )
        }

        val premierCycle = CycleEntity(
            caisseId = newCaisseId,
            numeroCycle = 1,
            dateDebut = System.currentTimeMillis(),
            estCloture = false
        )
        cycleDao.insertCycle(premierCycle)
    }

    suspend fun deleteCaisse(caisseId: Long): Result<Unit> {
        return try {
            val transactionsCount = transactionDao.getTransactionsCount(caisseId)
            
            if (transactionsCount > 0) {
                return Result.failure(Exception("Impossible de supprimer une caisse ayant des transactions enregistrées."))
            }

            val caisse = caisseDao.getById(caisseId)
            if (caisse?.remoteId != null && caisse.syncStatus == SyncStatus.SYNCED) {
                syncQueueDao.enqueue(SyncQueueEntity(entityType = "CAISSE", entityLocalId = caisseId, action = SyncAction.DELETE))
                syncManager.triggerSync()
            }
            
            caisseDao.deleteCaisseById(caisseId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @Transaction
    suspend fun duplicateCaisse(caisseId: Long, newLibelle: String): Long {
        val original = caisseDao.getById(caisseId) ?: throw Exception("Caisse non trouvée")
        
        // 1. Créer la nouvelle caisse avec les mêmes propriétés
        val newCaisse = original.copy(
            id = 0L,
            libelle = newLibelle,
            statut = com.indiza.cotisation.caisses.CaisseStatus.ACTIVE,
            remoteId = if (original.remoteId != null) java.util.UUID.randomUUID().toString() else null,
            syncStatus = if (original.remoteId != null) SyncStatus.PENDING_CREATE else SyncStatus.SYNCED,
            updatedAt = System.currentTimeMillis()
        )
        val newId = caisseDao.insertCaisse(newCaisse)
        
        // 2. Copier les participants
        val participants = caisseDao.getParticipantsByCaisseRaw(caisseId)
        participants.forEach { p ->
            caisseDao.addParticipantToCaisse(p.copy(caisseId = newId, estBeneficiaire = false))
        }
        
        // 3. Créer le premier cycle
        cycleDao.insertCycle(CycleEntity(caisseId = newId, numeroCycle = 1))

        if (newCaisse.syncStatus == SyncStatus.PENDING_CREATE) {
            syncQueueDao.enqueue(SyncQueueEntity(entityType = "CAISSE", entityLocalId = newId, action = SyncAction.CREATE))
            syncManager.triggerSync()
        }
        
        return newId
    }

    suspend fun cloturerCaisseDefinitivement(caisseId: Long) {
        val caisse = caisseDao.getById(caisseId) ?: return
        caisseDao.updateCaisse(caisse.copy(statut = com.indiza.cotisation.caisses.CaisseStatus.CLOTUREE))
    }

    @Transaction
    suspend fun setupBasicLoanFund(tontineId: Long, amount: Long, sourceCaisseId: Long): Long {
        val allCaisses = caisseDao.getCaissesByTontine(tontineId).first()
        var caisseBasique = allCaisses.find { it.isBasique }
        
        val basiqueId = if (caisseBasique == null) {
            val newCaisse = CaisseEntity(
                tontineId = tontineId,
                type = CaisseType.PRET,
                libelle = "Caisse de Prêt Basique",
                frequence = FrequenceCotisation.MENSUEL,
                isBasique = true,
                possibilitePret = true,
                plafondPret = amount
            )
            val id = caisseDao.insertCaisse(newCaisse)
            cycleDao.insertCycle(CycleEntity(caisseId = id, numeroCycle = 1))
            
            // Sync check for basic loan fund
            val tontine = dao.getById(tontineId)
            if (tontine?.isOnline == true) {
                syncQueueDao.enqueue(
                    SyncQueueEntity(
                        entityType = "CAISSE",
                        entityLocalId = id,
                        action = SyncAction.CREATE
                    )
                )
                syncManager.triggerSync()
            }
            
            caisseBasique = newCaisse.copy(id = id)
            id
        } else {
            caisseDao.updateCaisse(caisseBasique.copy(plafondPret = (caisseBasique.plafondPret ?: 0L) + amount))
            caisseBasique.id
        }

        val sourceCycle = cycleDao.getCycleActifSnapshot(sourceCaisseId)
        val targetCycle = cycleDao.getCycleActifSnapshot(basiqueId)
        
        val currentUserId = tontineMemberDao.getTontineMembers(tontineId).find { it.role == Role.PRESIDENT || it.role == Role.TRESORIER }?.id ?: 0L

        recordTransaction(
            TransactionEntity(
                caisseId = sourceCaisseId,
                memberId = currentUserId,
                cycleId = sourceCycle?.id ?: 0L,
                montant = -amount,
                type = TransactionType.TRANSFERT_SORTIE,
                motif = "Transfert vers Caisse de Prêt Basique"
            )
        )

        recordTransaction(
            TransactionEntity(
                caisseId = basiqueId,
                memberId = currentUserId,
                cycleId = targetCycle?.id ?: 0L,
                montant = amount,
                type = TransactionType.TRANSFERT_ENTREE,
                motif = "Dotation initiale/augmentation depuis ${allCaisses.find { it.id == sourceCaisseId }?.libelle}"
            )
        )

        return basiqueId
    }

    @androidx.annotation.RequiresApi(android.os.Build.VERSION_CODES.O)
    suspend fun searchTontines(query: String): List<TontineSearchResult> {
        return try {
            val remoteResults = discoveryService.searchTontines(query)
            val statuses = discoveryService.getMembershipStatuses(remoteResults.map { it.id })
            remoteResults.map { dto ->
                // Map to TontineEntity and local sync
                val existing = dao.getByRemoteId(dto.id)
                val entity = TontineEntity(
                    id = existing?.id ?: 0L,
                    libelle = dto.libelle,
                    exercice = dto.exercice,
                    dateDebut = existing?.dateDebut ?: java.time.LocalDate.now(),
                    dateFin = existing?.dateFin ?: java.time.LocalDate.now().plusYears(1),
                    creatorId = dto.creatorId,
                    remoteId = dto.id,
                    syncStatus = SyncStatus.SYNCED,
                    updatedAt = System.currentTimeMillis()
                )
                val localId = if (existing == null) {
                    dao.insertTontine(entity)
                } else {
                    dao.upsert(entity)
                    existing.id
                }
                TontineSearchResult(
                    tontine = entity.copy(id = localId),
                    membershipStatus = statuses[dto.id] ?: dto.membershipStatus ?: "NONE",
                    memberCount = dto.memberCount
                )
            }
        } catch (e: Exception) {
            println("SEARCH_ERROR: ${e.message}")
            dao.searchTontines(query).map { TontineSearchResult(it, "NONE") }
        }
    }

    suspend fun requestJoinTontine(tontineRemoteId: String) {
        getCurrentUserId() ?: throw Exception("Utilisateur non connecté")
        discoveryService.requestToJoin(tontineRemoteId)
    }

    suspend fun claimEmptyDefaultTontine(tontine: TontineEntity): TontineEntity? {
        val remoteId = tontine.remoteId ?: return null
        val userId = getCurrentUserId() ?: throw Exception("Utilisateur non connecté")
        if (!discoveryService.claimEmptyDefaultTontine(remoteId)) return null

        val updatedTontine = tontine.copy(creatorId = userId)
        dao.upsert(updatedTontine)

        if (tontineMemberDao.getUserRoleInTontine(tontine.id, userId) == null) {
            val currentUser = userDao.getUserById(userId)
            tontineMemberDao.insertMember(
                TontineMemberEntity(
                    tontineId = tontine.id,
                    userId = userId,
                    nomSaisi = currentUser?.nom ?: "Administrateur",
                    phone = currentUser?.phone ?: "",
                    matricule = currentUser?.matricule ?: "",
                    role = Role.INITIATEUR,
                    isInitiator = true
                )
            )
        }
        return updatedTontine
    }

    suspend fun cancelJoinTontine(tontineRemoteId: String) {
        getCurrentUserId() ?: throw Exception("Utilisateur non connecté")
        discoveryService.cancelJoinRequest(tontineRemoteId)
    }

    suspend fun fetchMyOnlineTontines(): List<TontineWithCaissesAndRole> {
        val userId = getCurrentUserId() ?: return emptyList()
        try {
            val remoteTontines = apiService.getMyTontines(userId)
            
            return remoteTontines.map { dto ->
                // 1. Sauvegarder/Mettre à jour la tontine en base (pour avoir l'ID local)
                var existing = dao.getByRemoteId(dto.id)
                val entity = dto.toEntity().copy(
                    id = existing?.id ?: 0L,
                    isOnline = true,
                    syncStatus = SyncStatus.SYNCED
                )
                
                val localId = if (existing != null) {
                    dao.upsert(entity)
                    existing.id
                } else {
                    dao.insertTontine(entity)
                }

                // 2. Sauvegarder l'adhésion si absente
                val existingMember = tontineMemberDao.getUserRoleInTontine(localId, userId)
                if (existingMember == null) {
                    val currentUser = userDao.getUserById(userId)
                    tontineMemberDao.insertMember(
                        TontineMemberEntity(
                            tontineId = localId,
                            userId = userId,
                            nomSaisi = currentUser?.nom ?: "Membre",
                            phone = currentUser?.phone ?: "",
                            role = Role.MEMBRE,
                            joinedAt = System.currentTimeMillis()
                        )
                    )
                }

                // 3. Sauvegarder les caisses pour la navigation
                dto.caisses?.forEach { caisseDto ->
                    val existingCaisse = caisseDao.getByRemoteId(caisseDto.id)
                    if (existingCaisse == null) {
                        val caisseId = caisseDao.insertCaisse(caisseDto.toEntity(localId))
                        if (cycleDao.getCycleActifSnapshot(caisseId) == null) {
                            val maxNum = cycleDao.getMaxNumeroCycle(caisseId) ?: 0
                            cycleDao.insertCycle(CycleEntity(caisseId = caisseId, numeroCycle = maxNum + 1))
                        }
                    } else {
                        caisseDao.updateCaisse(caisseDto.toEntity(localId).copy(id = existingCaisse.id))
                    }
                }

                val role = tontineMemberDao.getUserRoleInTontine(localId, userId) ?: Role.MEMBRE
                
                TontineWithCaissesAndRole(
                    tontine = entity.copy(id = localId),
                    caisses = dto.caisses?.map { it.toEntity(localId) } ?: emptyList(),
                    role = role
                )
            }
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun getOnlineTontineDetails(remoteId: String): TontineApiDto {
        val userId = getCurrentUserId()
        if (userId == null) {
            println("SYNC_ERROR: userId is null in getOnlineTontineDetails")
        }
        return apiService.getTontineDetails(remoteId, userId)
    }

    suspend fun syncOnlineTontineMembers(
        tontineId: Long,
        remoteMembers: List<TontineMemberApiDto>
    ) {
        println("DEBUG_REPO: syncOnlineTontineMembers - Synchro de ${remoteMembers.size} membres pour tontine $tontineId")
        remoteMembers.forEach { remoteMember ->
            val remoteMemberRecordId = remoteMember.id
            val remoteAccountUserId = remoteMember.userId ?: remoteMember.id

            if (userDao.getUserById(remoteAccountUserId) == null) {
                println("DEBUG_REPO:   - Création profil utilisateur local pour ${remoteMember.nomSaisi} ($remoteAccountUserId)")
                userDao.upsertUser(
                    UserEntity(
                        id = remoteAccountUserId,
                        nom = remoteMember.nomSaisi,
                        phone = remoteMember.phone,
                        matricule = remoteAccountUserId,
                        remoteId = remoteAccountUserId,
                        syncStatus = SyncStatus.SYNCED,
                        isVerified = false
                    )
                )
            }

            val existing = tontineMemberDao.getMemberByRemoteId(tontineId, remoteMemberRecordId)
                ?: tontineMemberDao.getMemberByPhone(tontineId, remoteMember.phone)

            if (existing == null) {
                println("DEBUG_REPO:   - Nouveau membre détecté: ${remoteMember.nomSaisi}. Insertion...")
            }

            tontineMemberDao.upsertMember(
                (existing ?: TontineMemberEntity(tontineId = tontineId, nomSaisi = remoteMember.nomSaisi, phone = remoteMember.phone, role = remoteMember.role))
                    .copy(
                        remoteId = remoteMemberRecordId,
                        userId = remoteAccountUserId,
                        nomSaisi = remoteMember.nomSaisi,
                        phone = remoteMember.phone,
                        role = remoteMember.role,
                        isInitiator = remoteMember.isInitiator,
                        isActive = true
                    )
            )
        }
        println("DEBUG_REPO: syncOnlineTontineMembers - Synchro terminée.")
    }

    suspend fun syncOnlineTransactions(caisse: CaisseEntity) {
        val remoteCaisseId = caisse.remoteId ?: return
        try {
            println("DEBUG_REPO: syncOnlineTransactions - Récupération transactions en ligne pour caisse ${caisse.id}")
            val remoteTransactions = apiService.getTransactionsByCaisse(remoteCaisseId)
            println("DEBUG_REPO:   - ${remoteTransactions.size} transactions en ligne trouvées")

            remoteTransactions.forEach { txDto ->
                val type = runCatching { TransactionType.valueOf(txDto.type) }.getOrNull() ?: return@forEach

                val localMember = tontineMemberDao.getMemberByRemoteId(caisse.tontineId, txDto.memberId)
                    ?: tontineMemberDao.getMemberEntity(caisse.tontineId, txDto.memberId)
                    ?: return@forEach

                val cycleNum = txDto.cycleNumber ?: 1
                var localCycle = cycleDao.getCycleByNumero(caisse.id, cycleNum)
                if (localCycle == null) {
                    val newCycleId = cycleDao.insertCycle(
                        com.indiza.cotisation.bank.CycleEntity(
                            caisseId = caisse.id,
                            numeroCycle = cycleNum
                        )
                    )
                    localCycle = cycleDao.getCycleById(newCycleId)
                }
                val cycleId = localCycle?.id ?: return@forEach

                val exists = transactionDao.existsTransaction(caisse.id, localMember.id, type, txDto.dateTransaction) > 0
                if (!exists) {
                    println("DEBUG_REPO:   - Insertion transaction ${txDto.type} (${txDto.montant} F) pour membre ${localMember.nomSaisi}")
                    transactionDao.insertTransaction(
                        TransactionEntity(
                            caisseId = caisse.id,
                            memberId = localMember.id,
                            cycleId = cycleId,
                            montant = txDto.montant,
                            type = type,
                            dateTransaction = txDto.dateTransaction,
                            validatedById = txDto.validatedById,
                            motif = txDto.motif
                        )
                    )
                }
            }
            println("DEBUG_REPO: syncOnlineTransactions - Synchro des transactions terminée.")
        } catch (e: Exception) {
            println("DEBUG_REPO: ERREUR synchro transactions: ${e.message}")
        }
    }

    suspend fun addOnlineParticipant(caisseRemoteId: String, memberRemoteId: String, parts: Int) {
        apiService.addCaisseParticipant(caisseRemoteId, memberRemoteId, parts)
    }

    suspend fun removeOnlineParticipant(caisseRemoteId: String, memberRemoteId: String) {
        apiService.removeCaisseParticipant(caisseRemoteId, memberRemoteId)
    }

    suspend fun getCaisseRemoteId(caisseId: Long): String? {
        return caisseDao.getCaisseById(caisseId)?.remoteId
    }

    suspend fun getMemberRemoteId(memberId: Long): String? {
        // En mode Duo, le memberId local correspond à un membre dont le userId est sa clé de liaison en ligne
        return tontineMemberDao.getMemberById(memberId)?.userId
    }

    suspend fun getOnlineParticipants(caisseRemoteId: String): List<com.indiza.cotisation.tontinemembers.CaisseParticipantApiDto> {
        val userId = getCurrentUserId()
        return apiService.getCaisseParticipants(caisseRemoteId, userId)
    }

    suspend fun updateOnlineParticipantOrder(caisseRemoteId: String, memberUserId: String, order: Int) {
        apiService.updateParticipantOrder(caisseRemoteId, memberUserId, order)
    }

    suspend fun addOnlineMember(tontineRemoteId: String, nom: String, phone: String, role: String): TontineMemberApiDto {
        return apiService.addMember(tontineRemoteId, nom, phone, role)
    }

    suspend fun getCurrentUserId(): String? {
        return sessionDao.getCurrentSession()?.userId
    }

    suspend fun getUserTontines(): List<TontineWithCaisses> {
        val userId = getCurrentUserId() ?: return emptyList()
        return tontineMemberDao.getUserTontinesWithCaisses(userId).first()
    }

    suspend fun isTontineNameDuplicate(libelle: String): Boolean {
        val userId = getCurrentUserId() ?: return false
        return dao.existsByNameAndInitiator(libelle, userId)
    }

    @Transaction
    suspend fun insertTontine(
        libelle: String,
        exercice: String,
        dateDebut: LocalDate,
        dateFin: LocalDate,
        isOnline: Boolean = true,
        isAnonymous: Boolean = false,
        hideInfoForSimpleMembers: Boolean = false
    ): Long {
        val remoteId = if (isOnline) java.util.UUID.randomUUID().toString() else null
        val userId = getCurrentUserId()

        if (isOnline && remoteId != null) {
            val apiDto = TontineApiDto(
                id = remoteId,
                libelle = libelle,
                exercice = exercice,
                dateDebut = dateDebut.toString(),
                dateFin = dateFin.toString(),
                isAnonymous = isAnonymous,
                hideInfoForSimpleMembers = hideInfoForSimpleMembers,
                creatorId = userId,
                updatedAt = System.currentTimeMillis()
            )
            val createdRemotely = try {
                apiService.createTontine(apiDto)
            } catch (e: Exception) {
                val errorDetails = generateSequence<Throwable>(e) { it.cause }
                    .joinToString(" ") { it.message.orEmpty() }
                if (
                    errorDetails.contains("23505") ||
                    errorDetails.contains("already have a tontine with this name", ignoreCase = true) ||
                    errorDetails.contains("shared default tontine already exists", ignoreCase = true)
                ) {
                    throw IllegalArgumentException(
                        "Vous avez déjà créé une réunion portant ce nom.",
                        e
                    )
                }
                // Fallback to Sync Queue if server unreachable
                println("SYNC_ERROR: Fallback to queue for tontine creation: ${e.message}")
                null
            }

            if (createdRemotely != null) {
                val tontineId = dao.insertTontine(
                    TontineEntity(
                        libelle = libelle,
                        exercice = exercice,
                        dateDebut = dateDebut,
                        dateFin = dateFin,
                        isOnline = true,
                        isAnonymous = isAnonymous,
                        hideInfoForSimpleMembers = hideInfoForSimpleMembers,
                        creatorId = createdRemotely.creatorId ?: userId,
                        remoteId = createdRemotely.id,
                        syncStatus = SyncStatus.SYNCED
                    )
                )

                userId?.let { id ->
                    val currentUser = userDao.getUserById(id)
                    tontineMemberDao.insertMember(
                        TontineMemberEntity(
                            tontineId = tontineId,
                            userId = id,
                            nomSaisi = currentUser?.nom ?: "Administrateur",
                            phone = currentUser?.phone ?: "",
                            matricule = currentUser?.matricule ?: "",
                            role = Role.INITIATEUR,
                            isInitiator = true
                        )
                    )
                }
                return tontineId
            }
        }

        // LOCAL MODE or OFFLINE FIRST FALLBACK
        val tontineId = dao.insertTontine(
            TontineEntity(
                libelle = libelle,
                exercice = exercice,
                dateDebut = dateDebut,
                dateFin = dateFin,
                isOnline = isOnline,
                isAnonymous = isAnonymous,
                hideInfoForSimpleMembers = hideInfoForSimpleMembers,
                creatorId = userId,
                remoteId = remoteId,
                syncStatus = if (isOnline) SyncStatus.PENDING_CREATE else SyncStatus.SYNCED
            )
        )

        userId?.let { id->
            val currentUser = userDao.getUserById(id)
            tontineMemberDao.insertMember(
                TontineMemberEntity(
                    tontineId = tontineId,
                    userId = id,
                    nomSaisi = currentUser?.nom ?: "Administrateur",
                    phone = currentUser?.phone ?: "",
                    matricule = currentUser?.matricule ?: "",
                    role = Role.INITIATEUR,
                    isInitiator = true
                )
            )
        }

        if (isOnline) {
            syncQueueDao.enqueue(
                SyncQueueEntity(
                    entityType = "TONTINE",
                    entityLocalId = tontineId,
                    action = SyncAction.CREATE
                )
            )
        }

        return tontineId
    }

    suspend fun triggerSync() {
        syncManager.triggerSync()
    }

    suspend fun updateCaisseSettings(caisse: CaisseEntity) {
        caisseDao.updateCaisse(caisse)
        if (caisse.remoteId != null) {
            syncQueueDao.enqueue(
                SyncQueueEntity(
                    entityType = "CAISSE",
                    entityLocalId = caisse.id,
                    action = SyncAction.UPDATE
                )
            )
            syncManager.triggerSync()
        }
    }

    suspend fun getUserTontinesWithRole(userId: String): Flow<List<TontineWithCaissesAndRole>> {
        return tontineMemberDao.getUserTontinesWithCaisses(userId).map { list ->
            list.map { item ->
                val role = tontineMemberDao
                    .getUserRoleInTontine(item.tontine.id, userId)
                    ?: Role.MEMBRE

                TontineWithCaissesAndRole(
                    tontine = item.tontine,
                    caisses = item.caisses,
                    role = role
                )
            }
        }
    }

    suspend fun deleteTontineIfEmpty(tontineId: Long): Result<Unit> {
        return try {
            val memberCount = tontineMemberDao.getMemberCount(tontineId)
            val tontineWithCaisses = dao.getTontineWithCaisses(tontineId).first()
            if (memberCount > 1) {
                return Result.failure(Exception("Impossible de supprimer : des membres sont déjà inscrits."))
            }
            if (tontineWithCaisses.caisses.isNotEmpty()) {
                return Result.failure(Exception("Impossible de supprimer : des caisses sont déjà configurées."))
            }
            dao.deleteTontineById(tontineId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTontineBaseInfo(
        tontineId: Long,
        libelle: String,
        exercice: String,
        dateDebut: LocalDate,
        dateFin: LocalDate,
        isAnonymous: Boolean,
        hideInfoForSimpleMembers: Boolean
    ) {
        val tontine = dao.getById(tontineId) ?: error("Réunion introuvable.")
        val currentName = tontine.libelle.trim()
        val newName = libelle.trim()
        val userId = getCurrentUserId()
        if (
            !currentName.equals(newName, ignoreCase = true) &&
            userId != null &&
            dao.existsOtherNameForInitiator(newName, userId, tontineId)
        ) {
            throw IllegalArgumentException("Vous avez déjà créé une réunion portant ce nom.")
        }

        dao.updateTontineInfo(
            tontineId,
            libelle,
            exercice,
            dateDebut,
            dateFin,
            isAnonymous,
            hideInfoForSimpleMembers
        )
        if (tontine.isOnline && tontine.remoteId != null) {
            syncQueueDao.enqueue(
                SyncQueueEntity(
                    entityType = "TONTINE",
                    entityLocalId = tontineId,
                    action = SyncAction.UPDATE
                )
            )
            syncManager.triggerSync()
        }
    }

    suspend fun updateOnlineStatus(tontineId: Long, isOnline: Boolean) {
        dao.updateOnlineStatus(tontineId, isOnline)
    }

    suspend fun addMemberToTontine(
        tontineId: Long,
        nom: String,
        phone: String,
        role: Role,
        matricule: String,
        remoteId: String? = null,
        userId: String? = null
    ): Long {
        val localMemberId = tontineMemberDao.insertMember(
            TontineMemberEntity(
                tontineId = tontineId,
                remoteId = remoteId,
                userId = userId,
                nomSaisi = nom,
                phone = phone,
                role = role,
                matricule = matricule
            )
        )
        val tontine = dao.getById(tontineId)
        if (tontine?.isOnline == true && remoteId == null) {
            syncQueueDao.enqueue(
                SyncQueueEntity(
                    entityType = "MEMBER",
                    entityLocalId = localMemberId,
                    action = SyncAction.CREATE
                )
            )
            syncManager.triggerSync()
        }
        return localMemberId
    }

    suspend fun addOnlineMember(
        remoteTontineId: String,
        nom: String,
        phone: String,
        role: Role
    ): TontineMemberApiDto = apiService.addMember(remoteTontineId, nom, phone, role.name)

    suspend fun recordTransaction(transaction: TransactionEntity): Long {
        val localId = transactionDao.insertTransaction(transaction)
        val caisse = caisseDao.getById(transaction.caisseId) ?: return localId
        val tontine = dao.getById(caisse.tontineId) ?: return localId
        if (!tontine.isOnline) return localId

        val queueEntry = SyncQueueEntity(
            entityType = "TRANSACTION",
            entityLocalId = localId,
            action = SyncAction.CREATE
        )
        try {
            val remoteCaisseId = caisse.remoteId ?: error("Caisse non synchronisée")
            val member = tontineMemberDao.getMemberById(transaction.memberId)
                ?: error("Membre introuvable")
            val remoteMemberId = member.remoteId ?: member.userId
                ?: error("Membre non synchronisé")
            val cycle = cycleDao.getCycleById(transaction.cycleId)
                ?: error("Cycle introuvable")

            apiService.createTransaction(
                TransactionApiDto(
                    id = transactionRemoteId(localId),
                    caisseId = remoteCaisseId,
                    memberId = remoteMemberId,
                    cycleId = cycle.id.toString(),
                    montant = transaction.montant,
                    type = transaction.type.name,
                    motif = transaction.motif,
                    dateTransaction = transaction.dateTransaction,
                    validatedById = transaction.validatedById
                ),
                cycle.numeroCycle
            )
        } catch (_: Exception) {
            syncQueueDao.enqueue(queueEntry)
            syncManager.triggerSync()
        }
        return localId
    }

    suspend fun generateInviteLink(tontineId: String, senderId: String): String {
        return apiService.generateInvite(tontineId, senderId).code
    }

    suspend fun broadcastReport(tontineId: String, senderId: String) {
        apiService.broadcastReport(tontineId, senderId)
    }

    suspend fun getComments(tontineId: String): List<CommentResponse> {
        return commentService.getComments(tontineId)
    }

    fun observeCommentChanges(tontineId: String): Flow<Unit> =
        commentService.observeCommentChanges(tontineId)
    
    suspend fun postComment(tontineId: String, content: String, imageBytes: ByteArray?) =
        commentService.postComment(tontineId, content, imageBytes)

    suspend fun clearLocalData() {
        dao.deleteAllTontines()
        // Room cascade handle others
    }
}
