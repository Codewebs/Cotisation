package com.indiza.cotisation.tontine

data class CommentResponse(
    val id: String,
    val tontineId: String,
    val userId: String,
    val content: String,
    val imageUrl: String?,
    val createdAt: Long,
    val author: CommentAuthor
)

data class CommentAuthor(
    val nom: String
)

data class CommentRequest(
    val userId: String,
    val content: String,
    val imageUrl: String? = null
)
