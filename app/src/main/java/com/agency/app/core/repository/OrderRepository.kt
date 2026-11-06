package com.agency.app.core.repository

import com.agency.app.core.database.dao.OrderDao
import com.agency.app.core.database.entity.OrderEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderRepository @Inject constructor(
    private val orderDao: OrderDao
) {
    fun getAllOrders(): Flow<List<OrderEntity>> = orderDao.getAllOrders()
    
    fun getOrdersByStatus(status: String): Flow<List<OrderEntity>> = orderDao.getOrdersByStatus(status)

    suspend fun saveOrder(order: OrderEntity) {
        orderDao.insertOrder(order)
    }

    suspend fun updateOrderStatus(orderId: String, newStatus: String) {
        // منطق التحديث الحقيقي
    }
}
