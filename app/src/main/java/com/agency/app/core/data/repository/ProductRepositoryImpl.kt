package com.agency.app.core.data.repository

import com.agency.app.core.data.local.dao.ProductDao
import com.agency.app.core.data.local.mapper.toDomain
import com.agency.app.core.data.local.mapper.toEntity
import com.agency.app.core.domain.model.AppError
import com.agency.app.core.domain.model.Outcome
import com.agency.app.core.domain.model.Product
import com.agency.app.core.domain.repository.ProductRepository
import com.agency.app.core.util.TimeSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * تنفيذ كتالوج المنتجات والمخزون.
 *
 * قاعدة المخزون: لا يوجد مسار يكتب كمية سالبة. كل خصم يمرّ عبر
 * [decrementStock] الذي يتحقق من الكمية المتاحة قبل اللمس.
 */
@Singleton
class ProductRepositoryImpl @Inject constructor(
    private val productDao: ProductDao
) : ProductRepository {

    override fun observeActiveProducts(): Flow<List<Product>> =
        productDao.observeActive().map { rows -> rows.map { it.toDomain() } }

    override fun observeAllProducts(): Flow<List<Product>> =
        productDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeProduct(id: String): Flow<Product?> =
        productDao.observeById(id).map { it?.toDomain() }

    override fun search(query: String): Flow<List<Product>> {
        val normalized = query.trim()
        return if (normalized.isEmpty()) {
            observeActiveProducts()
        } else {
            productDao.search(normalized).map { rows -> rows.map { it.toDomain() } }
        }
    }

    override suspend fun getProduct(id: String): Outcome<Product> = guarded {
        val entity = productDao.findById(id)
            ?: return@guarded Outcome.Failure(AppError.NotFound("منتج", id))
        Outcome.Success(entity.toDomain())
    }

    override suspend fun upsert(product: Product): Outcome<Unit> = guarded {
        if (product.name.isBlank()) {
            return@guarded Outcome.Failure(AppError.Validation("name", "اسم المنتج مطلوب"))
        }
        if (product.sku.isBlank()) {
            return@guarded Outcome.Failure(AppError.Validation("sku", "رمز المنتج (SKU) مطلوب"))
        }
        if (product.price.cents <= 0L) {
            return@guarded Outcome.Failure(
                AppError.Validation("price", "سعر المنتج يجب أن يكون أكبر من صفر")
            )
        }
        if (product.minOrderQuantity < 1) {
            return@guarded Outcome.Failure(
                AppError.Validation("minOrderQuantity", "أقل كمية للطلب لا تقل عن 1")
            )
        }
        if (product.stockQuantity < 0) {
            return@guarded Outcome.Failure(
                AppError.Validation("stockQuantity", "كمية المخزون لا تكون سالبة")
            )
        }
        productDao.upsert(product.toEntity(TimeSource.now()))
        Outcome.Success(Unit)
    }

    override suspend fun setActive(productId: String, active: Boolean): Outcome<Unit> = guarded {
        val entity = productDao.findById(productId)
            ?: return@guarded Outcome.Failure(AppError.NotFound("منتج", productId))
        productDao.upsert(
            entity.toDomain().copy(isActive = active).toEntity(TimeSource.now())
        )
        Outcome.Success(Unit)
    }

    /** جرد المخزون: كتابة كمية مطلقة (يستخدمه موظف المخزن عند الجرد). */
    override suspend fun setStock(productId: String, quantity: Int): Outcome<Unit> = guarded {
        if (quantity < 0) {
            return@guarded Outcome.Failure(
                AppError.Validation("quantity", "الكمية لا تكون سالبة")
            )
        }
        val entity = productDao.findById(productId)
            ?: return@guarded Outcome.Failure(AppError.NotFound("منتج", productId))
        productDao.updateStock(entity.id, quantity, TimeSource.now().toEpochMilliseconds())
        Outcome.Success(Unit)
    }

    /**
     * المسار الوحيد لخصم المخزون عند البيع.
     * نتحقق داخل العملية من الكمية المتاحة، ونستخدم تحديثاً ذرّياً في SQL.
     */
    override suspend fun decrementStock(productId: String, quantity: Int): Outcome<Unit> = guarded {
        if (quantity <= 0) {
            return@guarded Outcome.Failure(
                AppError.Validation("quantity", "كمية الخصم يجب أن تكون أكبر من صفر")
            )
        }
        val entity = productDao.findById(productId)
            ?: return@guarded Outcome.Failure(AppError.NotFound("منتج", productId))
        if (entity.stockQuantity < quantity) {
            return@guarded Outcome.Failure(
                AppError.Conflict(
                    "المخزون غير كافٍ للمنتج «${entity.name}»: المتاح ${entity.stockQuantity}"
                )
            )
        }
        productDao.decrementStock(
            id = productId,
            quantity = quantity,
            at = TimeSource.now().toEpochMilliseconds()
        )
        Outcome.Success(Unit)
    }

    override suspend fun delete(productId: String): Outcome<Unit> = guarded {
        val entity = productDao.findById(productId)
            ?: return@guarded Outcome.Failure(AppError.NotFound("منتج", productId))
        // لا نحذف منتجاً سبق بيعه: نعطّله فقط حفاظاً على سلامة الفواتير التاريخية.
        productDao.upsert(
            entity.toDomain().copy(isActive = false).toEntity(TimeSource.now())
        )
        Outcome.Success(Unit)
    }
}