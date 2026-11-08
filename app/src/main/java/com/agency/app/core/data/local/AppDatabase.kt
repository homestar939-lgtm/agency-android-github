package com.agency.app.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.agency.app.core.data.local.dao.BuyerDao
import com.agency.app.core.data.local.dao.InvoiceDao
import com.agency.app.core.data.local.dao.OrderDao
import com.agency.app.core.data.local.dao.PermissionDao
import com.agency.app.core.data.local.dao.ProductDao
import com.agency.app.core.data.local.dao.ReceiptDao
import com.agency.app.core.data.local.dao.SyncEventDao
import com.agency.app.core.data.local.dao.UserDao
import com.agency.app.core.data.local.entity.BuyerEntity
import com.agency.app.core.data.local.entity.InvoiceEntity
import com.agency.app.core.data.local.entity.InvoiceItemEntity
import com.agency.app.core.data.local.entity.OrderEntity
import com.agency.app.core.data.local.entity.OrderItemEntity
import com.agency.app.core.data.local.entity.OrderTimelineEntity
import com.agency.app.core.data.local.entity.PermissionRequestEntity
import com.agency.app.core.data.local.entity.ProductEntity
import com.agency.app.core.data.local.entity.ReceiptEntity
import com.agency.app.core.data.local.entity.SyncEventEntity
import com.agency.app.core.data.local.entity.UserEntity
import com.agency.app.core.domain.model.Money

/**
 * قاعدة البيانات المحلية — المصدر الوحيد للحقيقة (Single Source of Truth).
 *
 * كل ما تعرضه الواجهة يخرج من هنا عبر [kotlinx.coroutines.flow.Flow]،
 * ولا تقرأ الواجهة من الشبكة أبداً. الأحداث القادمة من WebSocket تُكتب
 * هنا أولاً ثم تتوزّع تلقائياً على كل الشاشات المشتركة في الاستعلام.
 *
 * [exportSchema] معطّل عمداً: المخطط يُدار عبر ترحيلات صريحة عند الحاجة،
 * ولا نريد ملفات schema مولّدة داخل مصدر التطبيق.
 */
@Database(
    entities = [
        UserEntity::class,
        BuyerEntity::class,
        ProductEntity::class,
        OrderEntity::class,
        OrderItemEntity::class,
        OrderTimelineEntity::class,
        InvoiceEntity::class,
        InvoiceItemEntity::class,
        ReceiptEntity::class,
        PermissionRequestEntity::class,
        SyncEventEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao

    abstract fun buyerDao(): BuyerDao

    abstract fun productDao(): ProductDao

    abstract fun orderDao(): OrderDao

    abstract fun invoiceDao(): InvoiceDao

    abstract fun receiptDao(): ReceiptDao

    abstract fun permissionDao(): PermissionDao

    abstract fun syncEventDao(): SyncEventDao

    companion object {
        /** اسم ملف قاعدة البيانات على الجهاز. */
        const val NAME: String = "agency.db"

        /**
         * بيانات أولية للتشغيل الحقيقي: مدير النظام الوحيد.
         *
         * كلمة المرور لا تُخزَّن هنا كنص؛ هذه بصمة SHA-256 للمفتاح الافتراضي
         * التي يولّدها [com.agency.app.core.data.auth.AuthManager.hash]،
         * ويُلزَم المدير بتغييرها عند أول دخول عبر شاشة الإعداد.
         */
        val SEED_ADMIN_USERNAME: String = "admin"

        /** قيمة افتتاحية لذمّة المشتري قبل أي فاتورة. */
        val INITIAL_BALANCE: Money = Money.ZERO
    }
}
