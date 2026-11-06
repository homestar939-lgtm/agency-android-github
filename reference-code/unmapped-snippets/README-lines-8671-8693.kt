@Test
fun `test connection loss during operation`() = runTest {
    val warehouseSession = TestSession.login("warehouse@agency.com", "Store#2026!")
    
    // بدء التحضير
    warehouseSession.startPreparation("order_001")
    
    // قطع الاتصال في المنتصف
    warehouseSession.disconnect()
    
    // إكمال التحضير في وضع Offline
    warehouseSession.completePreparationOffline("order_001")
    
    // إعادة الاتصال
    warehouseSession.reconnect()
    delay(2000)
    
    // التحقق: الحالة متزامنة
    val buyerSession = TestSession.login("buyer@client.com", "Buyer@2026&")
    assertEquals(OrderStatus.PREPARED, buyerSession.getOrderStatus("order_001"))
}
