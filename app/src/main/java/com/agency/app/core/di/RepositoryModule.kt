package com.agency.app.core.di

import com.agency.app.core.data.repository.BuyerRepositoryImpl
import com.agency.app.core.data.repository.InvoiceRepositoryImpl
import com.agency.app.core.data.repository.OrderRepositoryImpl
import com.agency.app.core.data.repository.PermissionRepositoryImpl
import com.agency.app.core.data.repository.ProductRepositoryImpl
import com.agency.app.core.data.repository.ReceiptRepositoryImpl
import com.agency.app.core.data.repository.UserRepositoryImpl
import com.agency.app.core.domain.repository.BuyerRepository
import com.agency.app.core.domain.repository.InvoiceRepository
import com.agency.app.core.domain.repository.OrderRepository
import com.agency.app.core.domain.repository.PermissionRepository
import com.agency.app.core.domain.repository.ProductRepository
import com.agency.app.core.domain.repository.ReceiptRepository
import com.agency.app.core.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * ربط العقود بالتنفيذات.
 *
 * الواجهة تعرف `UserRepository` فقط، ولا تعرف أن وراءه Room.
 * هذا هو الحد الذي يمنع تسرّب تفاصيل التخزين إلى الشاشات.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindBuyerRepository(impl: BuyerRepositoryImpl): BuyerRepository

    @Binds
    @Singleton
    abstract fun bindProductRepository(impl: ProductRepositoryImpl): ProductRepository

    @Binds
    @Singleton
    abstract fun bindOrderRepository(impl: OrderRepositoryImpl): OrderRepository

    @Binds
    @Singleton
    abstract fun bindInvoiceRepository(impl: InvoiceRepositoryImpl): InvoiceRepository

    @Binds
    @Singleton
    abstract fun bindReceiptRepository(impl: ReceiptRepositoryImpl): ReceiptRepository

    @Binds
    @Singleton
    abstract fun bindPermissionRepository(impl: PermissionRepositoryImpl): PermissionRepository
}