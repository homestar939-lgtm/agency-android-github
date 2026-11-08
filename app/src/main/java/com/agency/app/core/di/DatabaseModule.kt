package com.agency.app.core.di

import android.content.Context
import androidx.room.Room
import com.agency.app.core.data.local.AppDatabase
import com.agency.app.core.data.local.dao.BuyerDao
import com.agency.app.core.data.local.dao.InvoiceDao
import com.agency.app.core.data.local.dao.OrderDao
import com.agency.app.core.data.local.dao.PermissionDao
import com.agency.app.core.data.local.dao.ProductDao
import com.agency.app.core.data.local.dao.ReceiptDao
import com.agency.app.core.data.local.dao.SyncEventDao
import com.agency.app.core.data.local.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * وحدة قاعدة البيانات.
 *
 * لا نستخدم `fallbackToDestructiveMigration` لأن مسح قاعدة وكالة يعني
 * فقدان ذمم مالية حقيقية. أي تغيير في المخطط يستوجب ترحيلاً صريحاً.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .build()

    @Provides
    fun provideUserDao(db: AppDatabase): UserDao = db.userDao()

    @Provides
    fun provideBuyerDao(db: AppDatabase): BuyerDao = db.buyerDao()

    @Provides
    fun provideProductDao(db: AppDatabase): ProductDao = db.productDao()

    @Provides
    fun provideOrderDao(db: AppDatabase): OrderDao = db.orderDao()

    @Provides
    fun provideInvoiceDao(db: AppDatabase): InvoiceDao = db.invoiceDao()

    @Provides
    fun provideReceiptDao(db: AppDatabase): ReceiptDao = db.receiptDao()

    @Provides
    fun providePermissionDao(db: AppDatabase): PermissionDao = db.permissionDao()

    @Provides
    fun provideSyncEventDao(db: AppDatabase): SyncEventDao = db.syncEventDao()
}