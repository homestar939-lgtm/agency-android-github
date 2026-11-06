@Test
fun `test unauthorized access attempt`() {
    val buyerSession = TestSession.login("buyer@client.com", "Buyer@2026&")
    
    // محاولة المشتري الوصول لسجلات مالية
    val result = buyerSession.tryAccessFinancialRecords()
    
    // التحقق: العملية مرفوضة ومسجلة
    assertFalse(result.success)
    assertEquals("Unauthorized access", result.error)
    
    // التحقق: سجل التدقيق يحتوي على المحاولة
    val adminSession = TestSession.login("admin@agency.com", "Admin@2026#")
    assertTrue(adminSession.auditLogContains("unauthorized_access_attempt", "buyer_user"))
}
