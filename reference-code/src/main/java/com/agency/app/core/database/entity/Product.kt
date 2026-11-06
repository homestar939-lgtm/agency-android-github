// core/database/entity/Product.kt
package com.agency.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate

@Entity(tableName = "products")
data class Product(
    @PrimaryKey
    val id: String,
    val name: String,
    val categoryId: String,
    val price: Double,
    val unit: String,
    val stock: Int,
    val minOrderQuantity: Int,
    val expiryDate: LocalDate?,
    val originCountry: String?,
    val imageUrl: String?,
    val description: String?,
    val barcode: String?,
    val showPriceToBuyer: Boolean = true,
    val showUnitToBuyer: Boolean = true,
    val showExpiryToBuyer: Boolean = false,
    val showMinOrderToBuyer: Boolean = false,
    val isActive: Boolean = true
)
