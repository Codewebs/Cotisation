package com.indiza.cotisation.sync

interface Syncable<T> {
    val id: T
    val remoteId: String?
    val syncStatus: SyncStatus
    val updatedAt: Long
}
enum class SyncStatus {
    PENDING_CREATE,
    PENDING_UPDATE,
    PENDING_DELETE,
    SYNCED,
    ERROR
}
enum class SyncAction {
    CREATE,
    UPDATE,
    DELETE
}
