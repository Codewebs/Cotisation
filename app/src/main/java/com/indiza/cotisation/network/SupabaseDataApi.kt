package com.indiza.cotisation.network

import com.indiza.cotisation.caisses.CaisseApiDto
import com.indiza.cotisation.caisses.CaisseSyncDto
import com.indiza.cotisation.caisses.toApiDto
import com.indiza.cotisation.invitations.InviteGenerationResponse
import com.indiza.cotisation.invitations.JoinRequestResponse
import com.indiza.cotisation.invitations.UserProfileInfo
import com.indiza.cotisation.tontine.TontineApi
import com.indiza.cotisation.tontine.TontineApiDto
import com.indiza.cotisation.tontine.TontineMemberApiDto
import com.indiza.cotisation.tontine.TontineSummaryDto
import com.indiza.cotisation.tontine.TontineSyncDto
import com.indiza.cotisation.tontine.toApiDto
import com.indiza.cotisation.tontine.TransactionApiDto
import com.indiza.cotisation.tontinemembers.CaisseParticipantApiDto
import com.indiza.cotisation.tontinemembers.MemberShortDto
import com.indiza.cotisation.users.AdminSignatureStats
import com.indiza.cotisation.users.NotificationResponse
import com.indiza.cotisation.users.Role
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@RequiresApi(Build.VERSION_CODES.O)
@Singleton
class SupabaseDataApi @Inject constructor(
    private val supabase: SupabaseClient,
    private val configuration: SupabaseConfiguration,
    private val tontineDao: com.indiza.cotisation.tontine.TontineDao
) : TontineApi, RemoteApi {

    fun observeNotifications(userId: String): Flow<Unit> = flow {
        configuration.requireConfigured()
        val channel = supabase.channel("notifications:$userId")
        val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "notifications"
            filter = "user_id=eq.$userId"
        }
        channel.subscribe()
        try {
            changes.collect { emit(Unit) }
        } finally {
            channel.unsubscribe()
        }
    }

    override suspend fun createTontine(tontine: TontineApiDto): TontineApiDto {
        val actorId = requireUserId()
        val tontineId = tontine.id ?: UUID.randomUUID().toString()
        val created = supabase.postgrest.rpc(
            function = "create_tontine_with_initiator",
            parameters = buildJsonObject {
                put("p_id", tontineId)
                put("p_libelle", tontine.libelle)
                put("p_exercice", tontine.exercice)
                put("p_date_debut", tontine.dateDebut)
                put("p_date_fin", tontine.dateFin)
                put("p_is_online", true)
                put("p_updated_at", tontine.updatedAt)
                put("p_is_anonymous", tontine.isAnonymous)
                put("p_hide_info_for_simple_members", tontine.hideInfoForSimpleMembers)
            }
        ).decodeSingle<CreatedTontineRow>()
        return tontine.copy(id = created.id, creatorId = actorId)
    }

    override suspend fun createCaisse(caisse: CaisseApiDto): CaisseApiDto {
        configuration.requireConfigured()
        return supabase.from("caisses").insert(caisse.toWrite()) {
            select()
        }.decodeSingle<CaisseRecord>().toDto()
    }

    override suspend fun getCaisseParticipants(
        caisseId: String,
        userId: String?
    ): List<CaisseParticipantApiDto> {
        configuration.requireConfigured()
        val rows = supabase.from("caisse_participants").select {
            filter { eq("caisse_id", caisseId) }
        }.decodeList<ParticipantRecord>()
        return rows.mapNotNull { row ->
            val member = supabase.from("members").select {
                filter { eq("id", row.memberId) }
            }.decodeList<MemberRecord>().firstOrNull() ?: return@mapNotNull null
            row.toDto(member)
        }
    }

    override suspend fun addCaisseParticipant(
        caisseId: String,
        memberId: String,
        nombreParts: Int
    ): CaisseParticipantApiDto {
        configuration.requireConfigured()
        val tontineId = caisseTontineId(caisseId)
        val member = supabase.from("members").select {
            filter {
                eq("id", memberId)
                eq("tontine_id", tontineId)
            }
        }.decodeList<MemberRecord>().firstOrNull()
            ?: supabase.from("members").select {
                filter {
                    eq("user_id", memberId)
                    eq("tontine_id", tontineId)
                }
            }.decodeSingle<MemberRecord>()
        val participant = supabase.from("caisse_participants").upsert(
            ParticipantWrite(caisseId, member.id, nombreParts = nombreParts)
        ) { select() }.decodeSingle<ParticipantRecord>()
        return participant.toDto(member)
    }

    override suspend fun removeCaisseParticipant(caisseId: String, memberId: String) {
        configuration.requireConfigured()
        val tontineId = caisseTontineId(caisseId)
        val member = supabase.from("members").select {
            filter {
                eq("id", memberId)
                eq("tontine_id", tontineId)
            }
        }.decodeList<MemberRecord>().firstOrNull()
            ?: supabase.from("members").select {
                filter {
                    eq("user_id", memberId)
                    eq("tontine_id", tontineId)
                }
            }.decodeSingle<MemberRecord>()

        supabase.from("caisse_participants").delete {
            filter {
                eq("caisse_id", caisseId)
                eq("member_id", member.id)
            }
        }
    }

    override suspend fun updateParticipantOrder(caisseId: String, memberId: String, order: Int) {
        configuration.requireConfigured()
        val resolvedMemberId = supabase.from("members").select {
            filter {
                eq("id", memberId)
                eq("tontine_id", caisseTontineId(caisseId))
            }
        }.decodeList<MemberRecord>().firstOrNull()?.id
            ?: supabase.from("members").select {
                filter {
                    eq("user_id", memberId)
                    eq("tontine_id", caisseTontineId(caisseId))
                }
            }.decodeSingle<MemberRecord>().id
        supabase.from("caisse_participants").update(
            { set("ordre_passage", order) }
        ) {
            filter {
                eq("caisse_id", caisseId)
                eq("member_id", resolvedMemberId)
            }
        }
    }

    override suspend fun addMember(
        tontineId: String,
        nom: String,
        phone: String,
        role: String
    ): TontineMemberApiDto {
        configuration.requireConfigured()
        val existing = supabase.from("members").select {
            filter {
                eq("tontine_id", tontineId)
                eq("phone", phone)
            }
        }.decodeList<MemberRecord>().firstOrNull()
        if (existing != null) return existing.toApiDto()

        val row = supabase.from("members").insert(
            MemberWrite(
                tontineId = tontineId,
                userId = null,
                nomSaisi = nom,
                phone = phone,
                role = role
            )
        ) { select() }.decodeSingle<MemberRecord>()
        return row.toApiDto()
    }

    override suspend fun createTransaction(
        transaction: TransactionApiDto,
        cycleNumber: Int
    ): TransactionApiDto {
        configuration.requireConfigured()
        val tontineId = caisseTontineId(transaction.caisseId)
        val member = supabase.from("members").select {
            filter {
                eq("id", transaction.memberId)
                eq("tontine_id", tontineId)
            }
        }.decodeList<MemberRecord>().firstOrNull()
            ?: supabase.from("members").select {
                filter {
                    eq("user_id", transaction.memberId)
                    eq("tontine_id", tontineId)
                }
            }.decodeSingle<MemberRecord>()

        val cycle = supabase.from("cycles").select {
            filter {
                eq("caisse_id", transaction.caisseId)
                eq("numero_cycle", cycleNumber)
            }
        }.decodeList<CycleRecord>().firstOrNull()
            ?: supabase.from("cycles").insert(
                CycleWrite(
                    caisseId = transaction.caisseId,
                    numeroCycle = cycleNumber,
                    dateDebut = transaction.dateTransaction
                )
            ) { select() }.decodeSingle<CycleRecord>()

        val normalized = transaction.copy(memberId = member.id, cycleId = cycle.id)
        val existing = supabase.from("transactions").select {
            filter { eq("id", normalized.id) }
        }.decodeList<TransactionRecord>().firstOrNull()
        if (existing != null) return existing.toDto()

        return supabase.from("transactions").insert(normalized.toWrite()) {
            select()
        }.decodeSingle<TransactionRecord>().toDto()
    }

    override suspend fun getMyTontines(userId: String): List<TontineApiDto> {
        configuration.requireConfigured()
        val members = supabase.from("members").select {
            filter { eq("user_id", userId) }
        }.decodeList<MemberRecord>()
        return members.map { it.tontineId }.distinct().map { getTontineDetails(it, userId) }
    }

    override suspend fun getTontineDetails(id: String, userId: String?): TontineApiDto {
        configuration.requireConfigured()
        val row = supabase.from("tontines").select {
            filter { eq("id", id) }
        }.decodeSingle<TontineRecord>()
        val caisses = supabase.from("caisses").select {
            filter { eq("tontine_id", id) }
        }.decodeList<CaisseRecord>()
        val members = supabase.from("members").select {
            filter { eq("tontine_id", id) }
        }.decodeList<MemberRecord>()
        return row.toDto(
            caisses = caisses.map { it.toDto() },
            members = members.map { it.toApiDto() }
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun resolveTontineRemoteId(tontineId: String): String {
        return if (tontineId.toLongOrNull() != null) {
            tontineDao.getById(tontineId.toLong())?.remoteId ?: tontineId
        } else {
            tontineId
        }
    }

    override suspend fun generateInvite(tontineId: String, senderId: String): InviteGenerationResponse {
        configuration.requireConfigured()
        val remoteTontineId = resolveTontineRemoteId(tontineId)
        val code = UUID.randomUUID().toString().replace("-", "").take(12).uppercase()
        supabase.from("invite_links").insert(
            InviteLinkWrite(tontineId = remoteTontineId, senderId = senderId, code = code)
        )
        return InviteGenerationResponse(code)
    }

    override suspend fun broadcastReport(tontineId: String, senderId: String) {
        configuration.requireConfigured()
        val remoteTontineId = resolveTontineRemoteId(tontineId)
        supabase.postgrest.rpc(
            function = "broadcast_tontine_report",
            parameters = buildJsonObject { put("p_tontine_id", remoteTontineId) }
        )
    }

    override suspend fun createTontine(tontine: TontineSyncDto): String =
        createTontine(tontine.toApiDto()).id
            ?: error("Supabase did not return the tontine ID.")

    override suspend fun updateTontine(remoteId: String, tontine: TontineSyncDto) {
        configuration.requireConfigured()
        supabase.from("tontines").update(
            {
                set("libelle", tontine.libelle)
                set("exercice", tontine.exercice)
                set("date_debut", tontine.dateDebut)
                set("date_fin", tontine.dateFin)
                set("is_anonymous", tontine.isAnonymous)
                set("hide_info_for_simple_members", tontine.hideInfoForSimpleMembers)
                set("updated_at", tontine.updatedAt)
            }
        ) { filter { eq("id", remoteId) } }
    }

    override suspend fun deleteTontine(remoteId: String) {
        configuration.requireConfigured()
        supabase.from("tontines").delete { filter { eq("id", remoteId) } }
    }

    override suspend fun createCaisse(caisse: CaisseSyncDto): String =
        createCaisse(caisse.toApiDto()).id ?: error("Supabase did not return the caisse ID.")

    override suspend fun updateCaisse(remoteId: String, caisse: CaisseSyncDto) {
        configuration.requireConfigured()
        supabase.from("caisses").update(
            {
                set("tontine_id", caisse.tontineRemoteId!!)
                set("type", caisse.type)
                set("libelle", caisse.libelle)
                set("montant_fixe", caisse.montantFixe)
                set("montant_minimum", caisse.montantMinimum)
                set("frequence", caisse.frequence ?: "MENSUEL")
                set("regularite_imposee", caisse.regulariteImposee)
                set("redistribution", caisse.redistribution)
                set("ordre_redistribution", caisse.ordreRedistribution)
                set("taux_interet", caisse.tauxInteret)
                set("type_interet", caisse.typeInteret)
                set("penalite_retard_fixe", caisse.penaliteRetardFixe)
                set("penalite_echec_fixe", caisse.penaliteEchecFixe)
                set("date_limite_jour", caisse.dateLimiteJour)
                set("frais_caisse", caisse.fraisCaisse)
                set("statut", caisse.statut)
                set("respecter_frequence_strictement", caisse.respecterFrequenceStrictement)
                set("ordre_fige", caisse.ordreFige)
                set("possibilite_pret", caisse.possibilitePret)
                set("autoriser_pret_sur_amendes", caisse.autoriserPretSurAmendes)
                set("duree_cycles", caisse.dureeCycles)
                set("plafond_pret", caisse.plafondPret)
                set("is_basique", caisse.isBasique)
                set("updated_at", caisse.updatedAt)
            }
        ) {
            filter { eq("id", remoteId) }
        }
    }

    override suspend fun deleteCaisse(remoteId: String) {
        configuration.requireConfigured()
        supabase.from("caisses").delete { filter { eq("id", remoteId) } }
    }

    override suspend fun generateInviteLink(tontineId: String, senderId: String): String =
        generateInvite(tontineId, senderId).code

    override suspend fun getJoinRequests(tontineId: String): List<JoinRequestResponse> {
        configuration.requireConfigured()
        val remoteTontineId = resolveTontineRemoteId(tontineId)
        val requests = supabase.from("join_requests").select {
            filter {
                eq("tontine_id", remoteTontineId)
                eq("status", "PENDING")
            }
        }.decodeList<JoinRequestRecord>()
        return requests.map { request ->
            val profileRecord = profile(request.userId)
            val memberRecord = supabase.from("members").select {
                filter { eq("user_id", request.userId) }
                limit(1)
            }.decodeList<MemberRecord>().firstOrNull()

            val name = profileRecord?.nom?.takeIf { it.isNotBlank() } 
                ?: memberRecord?.nomSaisi?.takeIf { it.isNotBlank() } 
                ?: profileRecord?.email?.takeIf { it.isNotBlank() } 
                ?: "Membre"
            val phone = profileRecord?.phone?.takeIf { it.isNotBlank() } 
                ?: memberRecord?.phone?.takeIf { it.isNotBlank() } 
                ?: ""

            JoinRequestResponse(
                id = request.id,
                tontineId = request.tontineId,
                userId = request.userId,
                inviteCode = request.inviteCode,
                createdAt = request.createdAt,
                requester = UserProfileInfo(
                    id = request.userId,
                    nom = name,
                    email = profileRecord?.email.orEmpty(),
                    phone = phone,
                    matricule = profileRecord?.matricule.orEmpty()
                )
            )
        }
    }

    override suspend fun approveJoinRequest(requestId: String, targetMemberRemoteId: String?) {
        configuration.requireConfigured()
        supabase.postgrest.rpc(
            function = "approve_tontine_join_request",
            parameters = buildJsonObject {
                put("p_request_id", requestId)
                put(
                    "p_target_member_id",
                    targetMemberRemoteId?.let { kotlinx.serialization.json.JsonPrimitive(it) }
                        ?: kotlinx.serialization.json.JsonNull
                )
            }
        )
    }

    override suspend fun rejectJoinRequest(requestId: String) {
        configuration.requireConfigured()
        supabase.from("join_requests").delete {
            filter { eq("id", requestId) }
        }
    }

    override suspend fun getUserNotifications(userId: String): List<NotificationResponse> =
        run {
            configuration.requireConfigured()
            supabase.from("notifications").select {
                filter { eq("user_id", userId) }
            }.decodeList<NotificationRecord>().map { it.toDto() }
        }

    override suspend fun markNotificationAsRead(notificationId: String) {
        configuration.requireConfigured()
        supabase.from("notifications").update(
            { set("is_read", true) }
        ) { filter { eq("id", notificationId) } }
    }

    override suspend fun getTransactionDetails(transactionId: String): TransactionApiDto {
        configuration.requireConfigured()
        val transaction = supabase.from("transactions").select {
            filter { eq("id", transactionId) }
        }.decodeSingle<TransactionRecord>()
        val caisse = supabase.from("caisses").select {
            filter { eq("id", transaction.caisseId) }
        }.decodeSingle<CaisseRecord>()
        val tontine = supabase.from("tontines").select {
            filter { eq("id", caisse.tontineId) }
        }.decodeSingleOrNull<TontineRecord>()
        val validator = transaction.validatedById?.let { profile(it) }
        return transaction.toDto(caisse.toDto(), validator?.let {
            UserProfileInfo(it.id, it.nom.orEmpty(), it.email.orEmpty(), it.phone.orEmpty(), it.matricule.orEmpty())
        }, tontine?.libelle)
    }

    override suspend fun getTransactionsByCaisse(caisseRemoteId: String): List<TransactionApiDto> {
        configuration.requireConfigured()
        val rows = supabase.from("transactions").select {
            filter { eq("caisse_id", caisseRemoteId) }
        }.decodeList<TransactionRecord>()

        val cycleIds = rows.map { it.cycleId }.distinct()
        val remoteCycles = if (cycleIds.isNotEmpty()) {
            supabase.from("cycles").select {
                filter { isIn("id", cycleIds) }
            }.decodeList<CycleRecord>().associateBy { it.id }
        } else emptyMap()

        return rows.map { row ->
            val cycleNum = remoteCycles[row.cycleId]?.numeroCycle ?: 1
            row.toDto().copy(cycleNumber = cycleNum)
        }
    }

    override suspend fun getAdminSignatureStats(userId: String): AdminSignatureStats {
        configuration.requireConfigured()
        val from = YearMonth.now().atDay(1).atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant().toEpochMilli()
        val rows = supabase.from("transactions").select {
            filter {
                eq("validated_by_id", userId)
                gte("date_transaction", from)
            }
        }.decodeList<TransactionRecord>()
        return AdminSignatureStats(
            count = rows.size,
            totalAmount = rows.sumOf { it.montant },
            month = YearMonth.now().toString()
        )
    }

    private suspend fun caisseTontineId(caisseId: String): String {
        configuration.requireConfigured()
        return supabase.from("caisses").select {
            filter { eq("id", caisseId) }
        }.decodeSingle<CaisseRecord>().tontineId
    }

    private suspend fun profile(userId: String): ProfileRecord? =
        supabase.from("profiles").select {
            filter { eq("id", userId) }
        }.decodeList<ProfileRecord>().firstOrNull()

    private suspend fun requireUserId(): String {
        configuration.requireConfigured()
        supabase.auth.awaitInitialization()
        return supabase.auth.currentUserOrNull()?.id
            ?: error("Sign in to use online tontines.")
    }

    private fun TontineRecord.toDto(
        caisses: List<CaisseApiDto>? = null,
        members: List<TontineMemberApiDto>? = null
    ) = TontineApiDto(
        id = id,
        libelle = libelle,
        exercice = exercice,
        dateDebut = dateDebut ?: LocalDate.now().toString(),
        dateFin = dateFin ?: LocalDate.now().plusYears(1).toString(),
        isAnonymous = isAnonymous,
        hideInfoForSimpleMembers = hideInfoForSimpleMembers,
        creatorId = creatorId,
        updatedAt = updatedAt ?: System.currentTimeMillis(),
        caisses = caisses,
        members = members
    )

    private fun CaisseApiDto.toWrite() = CaisseWrite(
        id = id ?: UUID.randomUUID().toString(),
        tontineId = tontineId,
        type = type,
        libelle = libelle,
        montantFixe = montantFixe,
        montantMinimum = montantMinimum,
        frequence = frequence ?: "MENSUEL",
        regulariteImposee = regulariteImposee,
        redistribution = redistribution,
        ordreRedistribution = ordreRedistribution,
        tauxInteret = tauxInteret,
        typeInteret = typeInteret,
        penaliteRetardFixe = penaliteRetardFixe,
        penaliteEchecFixe = penaliteEchecFixe,
        dateLimiteJour = dateLimiteJour,
        jourCotisation = jourCotisation,
        heureLimite = heureLimite,
        fraisCaisse = fraisCaisse,
        respecterFrequenceStrictement = respecterFrequenceStrictement ?: false,
        ordreFige = ordreFige,
        dureeCycles = dureeCycles,
        plafondPret = plafondPret,
        isBasique = isBasique,
        statut = statut,
        possibilitePret = possibilitePret ?: false,
        autoriserPretSurAmendes = autoriserPretSurAmendes ?: false,
        updatedAt = updatedAt
    )

    private fun CaisseRecord.toDto() = CaisseApiDto(
        id = id,
        tontineId = tontineId,
        type = type.orEmpty(),
        libelle = libelle.orEmpty(),
        montantFixe = montantFixe,
        montantMinimum = montantMinimum,
        frequence = frequence,
        regulariteImposee = regulariteImposee,
        redistribution = redistribution,
        possibilitePret = possibilitePret,
        autoriserPretSurAmendes = autoriserPretSurAmendes,
        tauxInteret = tauxInteret,
        typeInteret = typeInteret,
        penaliteRetardFixe = penaliteRetardFixe,
        penaliteEchecFixe = penaliteEchecFixe,
        dateLimiteJour = dateLimiteJour,
        jourCotisation = jourCotisation,
        heureLimite = heureLimite,
        fraisCaisse = fraisCaisse,
        respecterFrequenceStrictement = respecterFrequenceStrictement ?: false,
        ordreFige = ordreFige,
        dureeCycles = dureeCycles,
        plafondPret = plafondPret,
        isBasique = isBasique,
        statut = statut,
        ordreRedistribution = ordreRedistribution,
        updatedAt = updatedAt ?: 0L
    )

    private fun MemberRecord.toApiDto() = TontineMemberApiDto(
        id = id,
        userId = userId,
        nomSaisi = nomSaisi,
        phone = phone.orEmpty(),
        role = runCatching { Role.valueOf(role.orEmpty()) }.getOrDefault(Role.MEMBRE),
        isInitiator = isInitiator
    )

    private fun ParticipantRecord.toDto(member: MemberRecord) = CaisseParticipantApiDto(
        caisseId = caisseId,
        memberId = memberId,
        ordrePassage = ordrePassage,
        nombreParts = nombreParts,
        member = MemberShortDto(
            id = member.id,
            userId = member.userId,
            nomSaisi = member.nomSaisi,
            phone = member.phone.orEmpty(),
            role = runCatching { Role.valueOf(member.role.orEmpty()) }.getOrDefault(Role.MEMBRE)
        )
    )

    private fun TransactionApiDto.toWrite() = TransactionWrite(
        id = id,
        caisseId = caisseId,
        memberId = memberId,
        cycleId = cycleId,
        validatedById = validatedById,
        montant = montant,
        type = type,
        motif = motif,
        dateTransaction = dateTransaction
    )

    private fun TransactionRecord.toDto(
        caisse: CaisseApiDto? = null,
        validator: UserProfileInfo? = null,
        tontineLibelle: String? = null
    ) = TransactionApiDto(
        id = id,
        caisseId = caisseId,
        memberId = memberId,
        cycleId = cycleId,
        montant = montant,
        type = type,
        motif = motif,
        dateTransaction = dateTransaction,
        validatedById = validatedById,
        validator = validator,
        Caisse = caisse,
        tontineLibelle = tontineLibelle
    )

    private fun NotificationRecord.toDto() = NotificationResponse(
        id = id,
        userId = userId,
        title = title,
        message = message.orEmpty(),
        type = type.orEmpty(),
        referenceId = referenceId,
        isRead = isRead,
        createdAt = createdAt
    )

    @Serializable
    private data class CreatedTontineRow(val id: String)

    @Serializable
    private data class TontineRecord(
        val id: String,
        val libelle: String,
        val exercice: String? = null,
        @SerialName("date_debut") val dateDebut: String? = null,
        @SerialName("date_fin") val dateFin: String? = null,
        @SerialName("is_anonymous") val isAnonymous: Boolean = false,
        @SerialName("hide_info_for_simple_members") val hideInfoForSimpleMembers: Boolean = false,
        @SerialName("creator_id") val creatorId: String? = null,
        @SerialName("updated_at") val updatedAt: Long? = null
    )

    @Serializable
    private data class MemberWrite(
        @SerialName("tontine_id") val tontineId: String,
        @SerialName("user_id") val userId: String?,
        @SerialName("nom_saisi") val nomSaisi: String,
        val phone: String,
        val role: String,
        @SerialName("is_initiator") val isInitiator: Boolean = false
    )

    @Serializable
    private data class MemberRecord(
        val id: String,
        @SerialName("tontine_id") val tontineId: String,
        @SerialName("user_id") val userId: String? = null,
        @SerialName("nom_saisi") val nomSaisi: String,
        val phone: String? = null,
        val role: String? = null,
        @SerialName("is_initiator") val isInitiator: Boolean = false
    )

    @Serializable
    private data class CaisseWrite(
        val id: String? = null,
        @SerialName("tontine_id") val tontineId: String,
        val type: String,
        val libelle: String,
        @SerialName("montant_fixe") val montantFixe: Long?,
        @SerialName("montant_minimum") val montantMinimum: Long?,
        val frequence: String,
        @SerialName("regularite_imposee") val regulariteImposee: Boolean,
        val redistribution: Boolean,
        @SerialName("ordre_redistribution") val ordreRedistribution: Int?,
        @SerialName("taux_interet") val tauxInteret: Double?,
        @SerialName("type_interet") val typeInteret: String?,
        @SerialName("penalite_retard_fixe") val penaliteRetardFixe: Long?,
        @SerialName("penalite_echec_fixe") val penaliteEchecFixe: Long?,
        @SerialName("date_limite_jour") val dateLimiteJour: Int?,
        @SerialName("jour_cotisation") val jourCotisation: String? = null,
        @SerialName("heure_limite") val heureLimite: Int? = null,
        @SerialName("frais_caisse") val fraisCaisse: Long?,
        @SerialName("respecter_frequence_strictement") val respecterFrequenceStrictement: Boolean,
        @SerialName("possibilite_pret") val possibilitePret: Boolean,
        @SerialName("autoriser_pret_sur_amendes") val autoriserPretSurAmendes: Boolean,
        @SerialName("statut") val statut: String,
        @SerialName("ordre_fige") val ordreFige: Boolean,
        @SerialName("duree_cycles") val dureeCycles: Int?,
        @SerialName("plafond_pret") val plafondPret: Long?,
        @SerialName("is_basique") val isBasique: Boolean,
        @SerialName("updated_at") val updatedAt: Long
    )

    @Serializable
    private data class CaisseRecord(
        val id: String,
        @SerialName("tontine_id") val tontineId: String,
        val type: String? = null,
        val libelle: String? = null,
        @SerialName("montant_fixe") val montantFixe: Long? = null,
        @SerialName("montant_minimum") val montantMinimum: Long? = null,
        val frequence: String? = null,
        @SerialName("regularite_imposee") val regulariteImposee: Boolean = false,
        val redistribution: Boolean = false,
        @SerialName("ordre_redistribution") val ordreRedistribution: Int? = null,
        @SerialName("taux_interet") val tauxInteret: Double? = null,
        @SerialName("type_interet") val typeInteret: String? = null,
        @SerialName("penalite_retard_fixe") val penaliteRetardFixe: Long? = null,
        @SerialName("penalite_echec_fixe") val penaliteEchecFixe: Long? = null,
        @SerialName("date_limite_jour") val dateLimiteJour: Int? = null,
        @SerialName("jour_cotisation") val jourCotisation: String? = null,
        @SerialName("heure_limite") val heureLimite: Int? = null,
        @SerialName("frais_caisse") val fraisCaisse: Long? = null,
        @SerialName("respecter_frequence_strictement") val respecterFrequenceStrictement: Boolean? = null,
        @SerialName("possibilite_pret") val possibilitePret: Boolean? = null,
        @SerialName("autoriser_pret_sur_amendes") val autoriserPretSurAmendes: Boolean? = null,
        @SerialName("statut") val statut: String = "ACTIVE",
        @SerialName("ordre_fige") val ordreFige: Boolean = false,
        @SerialName("duree_cycles") val dureeCycles: Int? = null,
        @SerialName("plafond_pret") val plafondPret: Long? = null,
        @SerialName("is_basique") val isBasique: Boolean = false,
        @SerialName("updated_at") val updatedAt: Long? = null
    )

    @Serializable
    private data class ParticipantWrite(
        @SerialName("caisse_id") val caisseId: String,
        @SerialName("member_id") val memberId: String,
        @SerialName("nombre_parts") val nombreParts: Int
    )

    @Serializable
    private data class ParticipantRecord(
        @SerialName("caisse_id") val caisseId: String,
        @SerialName("member_id") val memberId: String,
        @SerialName("ordre_passage") val ordrePassage: Int? = null,
        @SerialName("nombre_parts") val nombreParts: Int = 1
    )

    @Serializable
    private data class CycleWrite(
        @SerialName("caisse_id") val caisseId: String,
        @SerialName("numero_cycle") val numeroCycle: Int,
        @SerialName("date_debut") val dateDebut: Long
    )

    @Serializable
    private data class CycleRecord(
        val id: String,
        @SerialName("caisse_id") val caisseId: String,
        @SerialName("numero_cycle") val numeroCycle: Int
    )

    @Serializable
    private data class TransactionWrite(
        val id: String,
        @SerialName("caisse_id") val caisseId: String,
        @SerialName("member_id") val memberId: String,
        @SerialName("cycle_id") val cycleId: String,
        @SerialName("validated_by_id") val validatedById: String?,
        val montant: Long,
        val type: String,
        val motif: String?,
        @SerialName("date_transaction") val dateTransaction: Long
    )

    @Serializable
    private data class TransactionRecord(
        val id: String,
        @SerialName("caisse_id") val caisseId: String,
        @SerialName("member_id") val memberId: String,
        @SerialName("cycle_id") val cycleId: String,
        @SerialName("validated_by_id") val validatedById: String? = null,
        val montant: Long,
        val type: String,
        val motif: String? = null,
        @SerialName("date_transaction") val dateTransaction: Long
    )

    @Serializable
    private data class InviteLinkWrite(
        @SerialName("tontine_id") val tontineId: String,
        @SerialName("sender_id") val senderId: String,
        val code: String
    )

    @Serializable
    private data class JoinRequestRecord(
        val id: String,
        @SerialName("tontine_id") val tontineId: String,
        @SerialName("user_id") val userId: String,
        @SerialName("invite_code") val inviteCode: String? = null,
        val status: String,
        @SerialName("created_at") val createdAt: Long
    )

    @Serializable
    private data class ProfileRecord(
        val id: String,
        val nom: String? = null,
        val email: String? = null,
        val phone: String? = null,
        val matricule: String? = null
    )

    @Serializable
    private data class NotificationRecord(
        val id: String,
        @SerialName("user_id") val userId: String,
        val title: String,
        val message: String? = null,
        val type: String? = null,
        @SerialName("reference_id") val referenceId: String? = null,
        @SerialName("is_read") val isRead: Boolean,
        @SerialName("created_at") val createdAt: Long,
        @SerialName("group_key") val groupKey: String? = null,
        @SerialName("actor_id") val actorId: String? = null,
        @SerialName("actor_name") val actorName: String? = null,
        @SerialName("context_name") val contextName: String? = null
    )
}
