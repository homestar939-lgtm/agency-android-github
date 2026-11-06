package com.agency.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String,
    val buyerName: String,
    val totalAmount: Double,
    val status: String,
    val createdAt: Long
)
