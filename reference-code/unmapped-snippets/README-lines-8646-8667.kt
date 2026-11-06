@Test
fun `test sync conflict resolution`() = runTest {
    val cashier1 = TestSession.login("cashier_01", "pass1")
    val cashier2 = TestSession.login("cashier_02", "pass2")

    // كلا المتحصلين يستلمان من نفس المشتري في نفس الوقت
    val receipt1 = cashier1.createReceiptOffline("buyer_001", 20000.0)
    val receipt2 = cashier2.createReceiptOffline("buyer_001", 30000.0)

    // إعادة الاتصال
    cashier1.reconnect()
    cashier2.reconnect()

    // التحقق: لا يوجد خصم مزدوج
    val buyerSession = TestSession.login("buyer@client.com", "Buyer@2026&")
    val debt = buyerSession.getBuyerDebt("buyer_001")
    
    // الذمة الصحيحة = الذمة الأصلية - (سند1 + سند2)
    assertEquals(50000.0, debt)
}
