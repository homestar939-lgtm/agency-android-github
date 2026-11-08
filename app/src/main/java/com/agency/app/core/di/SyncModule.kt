package com.agency.app.core.di

import com.agency.app.core.data.local.dao.BuyerDao
import com.agency.app.core.data.local.dao.InvoiceDao
import com.agency.app.core.data.local.dao.OrderDao
import com.agency.app.core.data.local.dao.ProductDao
import com.agency.app.core.data.local.dao.ReceiptDao
import com.agency.app.core.data.local.dao.SyncEventDao
import com.agency.app.core.data.sync.BackoffPolicy
import com.agency.app.core.data.sync.SyncManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * وحدة المزامنة الحيّة.
 *
 * [SyncManager] لم يكن موصولاً بأي وحدة من قبل — كان كوداً مكتوباً لا يُستعمل.
 * وهذا بالضبط نوع العطب الذي لا يكشفه بناء ناجح: الواجهة تبنى، والمزامنة
 * صامتة. الربط هنا هو ما يجعل قناة WebSocket تُغذّي Room فعلاً.
 */
@Module
@InstallIn(SingletonComponent::class)
object SyncModule {

    @Provides
    @Singleton
    fun provideSyncManager(
        syncEventDao: SyncEventDao,
        productDao: ProductDao,
        buyerDao: BuyerDao,
        orderDao: OrderDao,
        invoiceDao: InvoiceDao,
        receiptDao: ReceiptDao
    ): SyncManager = SyncManager(
        syncEventDao = syncEventDao,
        productDao = productDao,
        buyerDao = buyerDao,
        orderDao = orderDao,
        invoiceDao = invoiceDao,
        receiptDao = receiptDao
    )

    /**
     * سياسة إعادة الاتصال.
     *
     * تُقدَّم من DI بدل إنشائها داخل العميل كي يستطيع الاختبار حقن سياسة
     * بلا تذبذب وبمهلة قصيرة، فتُقاس المضاعفة بدقة بلا انتظار حقيقي.
     */
    @Provides
    @Singleton
    fun provideBackoffPolicy(): BackoffPolicy = BackoffPolicy.production()
}