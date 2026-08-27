package com.indiza.cotisation.bank

import androidx.room.Embedded
import androidx.room.Relation
import com.indiza.cotisation.tontine.TontineMemberEntity

data class TransactionWithMember(
    @Embedded val transaction: TransactionEntity,
    @Relation(
        parentColumn = "memberId",
        entityColumn = "id"
    )
    val member: TontineMemberEntity
)