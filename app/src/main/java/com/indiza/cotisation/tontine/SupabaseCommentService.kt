package com.indiza.cotisation.tontine

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.storage.storage
import com.indiza.cotisation.network.SupabaseConfiguration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID
import kotlin.time.Duration.Companion.seconds
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseCommentService @Inject constructor(
    private val supabase: SupabaseClient,
    private val configuration: SupabaseConfiguration
) {
    fun observeCommentChanges(tontineId: String): Flow<Unit> = flow {
        configuration.requireConfigured()
        val channel = supabase.channel("comments:$tontineId")
        val changes = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "comments"
            filter = "tontine_id=eq.$tontineId"
        }
        channel.subscribe()
        try {
            changes.collect { emit(Unit) }
        } finally {
            channel.unsubscribe()
        }
    }

    suspend fun getComments(tontineId: String): List<CommentResponse> {
        configuration.requireConfigured()
        val rows = supabase.from("comments").select {
            filter { eq("tontine_id", tontineId) }
        }.decodeList<CommentRow>()
        val authors = supabase.from("members").select {
            filter { eq("tontine_id", tontineId) }
        }.decodeList<MemberAuthorRow>()
            .mapNotNull { member -> member.userId?.let { it to member.name } }
            .toMap()
        return rows.map { row ->
            CommentResponse(
                id = row.id,
                tontineId = row.tontineId,
                userId = row.userId,
                content = row.content,
                imageUrl = row.imagePath?.let { signedImageUrl(it) },
                createdAt = row.createdAt,
                author = CommentAuthor(authors[row.userId].orEmpty().ifBlank { "Membre" })
            )
        }.sortedBy { it.createdAt }
    }

    suspend fun postComment(
        tontineId: String,
        content: String,
        imageBytes: ByteArray?
    ): CommentResponse {
        configuration.requireConfigured()
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("Sign in before posting a comment.")
        val imagePath = imageBytes?.let { bytes ->
            val filename = "${UUID.randomUUID()}.jpg"
            val path = "tontines/$tontineId/$userId/$filename"
            supabase.storage.from(COMMENT_BUCKET).upload(path, bytes, upsert = false)
            path
        }
        val row = supabase.from("comments").insert(
            CommentInsert(
                tontineId = tontineId,
                userId = userId,
                content = content,
                imagePath = imagePath
            )
        ) {
            select()
        }.decodeSingle<CommentRow>()
        return CommentResponse(
            id = row.id,
            tontineId = row.tontineId,
            userId = row.userId,
            content = row.content,
            imageUrl = row.imagePath?.let { signedImageUrl(it) },
            createdAt = row.createdAt,
            author = CommentAuthor(row.authorName.orEmpty())
        )
    }

    private suspend fun signedImageUrl(path: String): String =
        supabase.storage.from(COMMENT_BUCKET).createSignedUrl(path, 3600.seconds)

    @Serializable
    private data class CommentInsert(
        @SerialName("tontine_id") val tontineId: String,
        @SerialName("user_id") val userId: String,
        val content: String,
        @SerialName("image_url") val imagePath: String?
    )

    @Serializable
    private data class CommentRow(
        val id: String,
        @SerialName("tontine_id") val tontineId: String,
        @SerialName("user_id") val userId: String,
        val content: String,
        @SerialName("image_url") val imagePath: String? = null,
        @SerialName("created_at") val createdAt: Long,
        @SerialName("author_name") val authorName: String? = null
    )

    @Serializable
    private data class MemberAuthorRow(
        @SerialName("user_id") val userId: String?,
        @SerialName("nom_saisi") val name: String
    )

    private companion object {
        const val COMMENT_BUCKET = "comment-images"
    }
}
