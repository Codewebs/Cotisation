package com.indiza.cotisation.network

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseDiscoveryService @Inject constructor(
    private val supabase: SupabaseClient,
    private val configuration: SupabaseConfiguration
) {
    suspend fun searchTontines(searchTerm: String): List<SearchTontineRow> =
        run {
            configuration.requireConfigured()
            supabase.postgrest.rpc(
                function = "search_tontines",
                parameters = buildJsonObject { put("search_term", searchTerm) }
            ).decodeList()
        }

    suspend fun getMembershipStatuses(tontineIds: List<String>): Map<String, String> {
        configuration.requireConfigured()
        val userId = supabase.auth.currentUserOrNull()?.id ?: return emptyMap()
        if (tontineIds.isEmpty()) return emptyMap()

        val memberships = supabase.from("members").select {
            filter {
                eq("user_id", userId)
                isIn("tontine_id", tontineIds)
            }
        }.decodeList<TontineUserRow>()
        val requests = supabase.from("join_requests").select {
            filter {
                eq("user_id", userId)
                isIn("tontine_id", tontineIds)
            }
        }.decodeList<TontineUserRow>()
        return buildMap {
            memberships.forEach { put(it.tontineId, "MEMBER") }
            requests.forEach { request ->
                if (request.status.equals("PENDING", ignoreCase = true)) {
                    putIfAbsent(request.tontineId, "PENDING")
                }
            }
        }
    }

    suspend fun getTontineInvite(inviteCode: String): TontineInviteRow =
        run {
            configuration.requireConfigured()
            supabase.postgrest.rpc(
                function = "get_tontine_invite",
                parameters = buildJsonObject { put("invite_code", inviteCode) }
            ).decodeSingle()
        }

    suspend fun requestToJoin(tontineId: String) {
        configuration.requireConfigured()
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("Sign in before requesting to join.")
        supabase.from("join_requests").insert(
            JoinRequestInsert(tontineId = tontineId, userId = userId)
        )
    }

    suspend fun claimEmptyDefaultTontine(tontineId: String): Boolean {
        configuration.requireConfigured()
        return supabase.postgrest.rpc(
            function = "claim_empty_default_tontine",
            parameters = buildJsonObject { put("p_tontine_id", tontineId) }
        ).decodeSingle()
    }

    suspend fun cancelJoinRequest(tontineId: String) {
        configuration.requireConfigured()
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("Sign in before cancelling a join request.")
        supabase.from("join_requests").delete {
            filter {
                eq("tontine_id", tontineId)
                eq("user_id", userId)
                eq("status", "PENDING")
            }
        }
    }

    suspend fun joinTontineWithInvite(inviteCode: String) {
        configuration.requireConfigured()
        supabase.postgrest.rpc(
            function = "join_tontine_with_invite",
            parameters = buildJsonObject { put("invite_code", inviteCode) }
        )
    }

    @Serializable
    data class SearchTontineRow(
        val id: String,
        val libelle: String,
        val exercice: String? = null,
        @SerialName("creator_id") val creatorId: String? = null,
        @SerialName("member_count") val memberCount: Long = 0,
        @SerialName("membership_status") val membershipStatus: String? = null
    )

    @Serializable
    data class TontineInviteRow(
        @SerialName("tontine_id") val tontineId: String,
        @SerialName("tontine_name") val tontineName: String,
        @SerialName("member_count") val memberCount: Long = 0
    )

    @Serializable
    data class TontineUserRow(
        @SerialName("tontine_id") val tontineId: String,
        val status: String? = null
    )

    @Serializable
    data class JoinRequestInsert(
        @SerialName("tontine_id") val tontineId: String,
        @SerialName("user_id") val userId: String
    )
}
