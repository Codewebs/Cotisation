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
import com.indiza.cotisation.users.Role
import com.indiza.cotisation.users.SessionDao
import com.indiza.cotisation.users.UserDao
import com.indiza.cotisation.users.UserEntity
import kotlinx.coroutines.flow.Flow
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
    private val apiService: TontineApi
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
                    respecterFrequenceStrictement = caisseDraft.respecterFrequenceStrictement,
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

        transactionDao.insertTransaction(
            TransactionEntity(
                caisseId = sourceCaisseId,
                memberId = currentUserId,
                cycleId = sourceCycle?.id ?: 0L,
                montant = -amount,
                type = TransactionType.TRANSFERT_SORTIE,
                motif = "Transfert vers Caisse de Prêt Basique"
            )
        )

        transactionDao.insertTransaction(
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
        val userId = getCurrentUserId()
        return try {
            val remoteResults = apiService.searchTontines(query, userId)
            remoteResults.map { dto ->
                // Map to TontineEntity and local sync
                val existing = dao.getByRemoteId(dto.id)
                val entity = TontineEntity(
                    id = existing?.id ?: 0L,
                    libelle = dto.libelle,
                    exercice = dto.exercice,
                    dateDebut = if (dto.dateDebut.isNotEmpty()) java.time.LocalDate.parse(dto.dateDebut) else java.time.LocalDate.now(),
                    dateFin = if (dto.dateFin.isNotEmpty()) java.time.LocalDate.parse(dto.dateFin) else java.time.LocalDate.now().plusYears(1),
                    remoteId = dto.id,
                    syncStatus = SyncStatus.SYNCED,
                    updatedAt = dto.updatedAt
                )
                dao.upsert(entity)
                TontineSearchResult(entity, dto.membershipStatus)
            }
        } catch (e: Exception) {
            println("SEARCH_ERROR: ${e.message}")
            dao.searchTontines(query).map { TontineSearchResult(it, "NONE") }
        }
    }

    suspend fun requestJoinTontine(tontineRemoteId: String) {
        val userId = getCurrentUserId() ?: throw Exception("Utilisateur non connecté")
        apiService.requestJoin(tontineRemoteId, mapOf("userId" to userId))
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
                        isVerified = true
                    )
                )
            }

            val existing = tontineMemberDao.getMemberByRemoteId(tontineId, remoteMemberRecordId)
                ?: tontineMemberDao.getMemberByPhone(tontineId, remoteMember.phone)

            if (existing == null) {
                println("DEBUG_REPO:   - Nouveau membre détecté: ${remoteMember.nomSaisi}. Insertion...")
            }

            tontineMemberDao.insertMember(
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

    suspend fun createOnlineTransaction(transactionDto: TransactionApiDto) {
        apiService.createTransaction(transactionDto)
    }

    suspend fun addOnlineParticipant(caisseRemoteId: String, memberRemoteId: String, parts: Int) {
        apiService.addCaisseParticipant(caisseRemoteId, mapOf("memberId" to memberRemoteId, "nombreParts" to parts.toString()))
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
        apiService.updateParticipantOrder(caisseRemoteId, memberUserId, mapOf("ordrePassage" to order.toString()))
    }

    suspend fun addOnlineMember(tontineRemoteId: String, nom: String, phone: String, role: String): TontineMemberApiDto {
        return apiService.addMember(tontineRemoteId, mapOf("nomSaisi" to nom, "phone" to phone, "role" to role))
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
        isAnonymous: Boolean = false
    ): Long {
        val remoteId = if (isOnline) java.util.UUID.randomUUID().toString() else null
        val userId = getCurrentUserId()

        if (isOnline && remoteId != null) {
            try {
                // DIRECT ONLINE CREATE for Tontine
                val apiDto = TontineApiDto(
                    id = remoteId,
                    libelle = libelle,
                    exercice = exercice,
                    dateDebut = dateDebut.toString(),
                    dateFin = dateFin.toString(),
                    creatorId = userId,
                    updatedAt = System.currentTimeMillis()
                )
                apiService.createTontine(apiDto)
                
                // Si réussi, on insère localement en tant que SYNCED
                val tontineId = dao.insertTontine(
                    TontineEntity(
                        libelle = libelle,
                        exercice = exercice,
                        dateDebut = dateDebut,
                        dateFin = dateFin,
                        isOnline = true,
                        isAnonymous = isAnonymous,
                        remoteId = remoteId,
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
            } catch (e: Exception) {
                // Fallback to Sync Queue if server unreachable
                println("SYNC_ERROR: Fallback to queue for tontine creation: ${e.message}")
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
        dateFin: LocalDate
    ) {
        dao.updateTontineInfo(tontineId, libelle, exercice, dateDebut, dateFin)
    }

    suspend fun updateOnlineStatus(tontineId: Long, isOnline: Boolean) {
        dao.updateOnlineStatus(tontineId, isOnline)
    }

    suspend fun addMemberToTontine(tontineId: Long, nom: String, phone: String, role: Role, matricule: String) {
        tontineMemberDao.insertMember(
            TontineMemberEntity(
                tontineId = tontineId,
                nomSaisi = nom,
                phone = phone,
                role = role,
                matricule = matricule
            )
        )
    }

    suspend fun generateInviteLink(tontineId: String, senderId: String): String {
        return apiService.generateInvite(tontineId, com.indiza.cotisation.invitations.InviteGenerationRequest(senderId)).code
    }

    suspend fun broadcastReport(tontineId: String, senderId: String) {
        apiService.broadcastReport(tontineId, mapOf("senderId" to senderId))
    }

    suspend fun getComments(tontineId: String): List<CommentResponse> {
        val userId = getCurrentUserId()
        return apiService.getComments(tontineId, userId)
    }
    
    suspend fun postComment(tontineId: String, userId: String, content: String, imageUrl: String?) = 
        apiService.postComment(tontineId, CommentRequest(userId, content, imageUrl))

    suspend fun clearLocalData() {
        dao.deleteAllTontines()
        // Room cascade handle others
    }
}
