package com.indiza.cotisation.users

data class NotificationResponse(
    val id: String,
    val userId: String,
    val title: String,
    val message: String,
    val type: String,
    val referenceId: String?,
    val isRead: Boolean,
    val createdAt: Long
)
