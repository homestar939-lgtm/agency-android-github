app/
├── src/main/
│   ├── java/com/agency/app/
│   │   ├── core/                          # النواة الأساسية
│   │   │   ├── di/                        # Dependency Injection
│   │   │   │   ├── AppModule.kt
│   │   │   │   ├── NetworkModule.kt
│   │   │   │   └── DatabaseModule.kt
│   │   │   ├── network/                   # الشبكة والمزامنة
│   │   │   │   ├── WebSocketManager.kt
│   │   │   │   ├── SyncManager.kt
│   │   │   │   └── ApiClient.kt
│   │   │   ├── database/                  # قاعدة البيانات المحلية
│   │   │   │   ├── AppDatabase.kt
│   │   │   │   ├── dao/
│   │   │   │   └── entity/
│   │   │   ├── security/                  # الأمان والصلاحيات
│   │   │   │   ├── AuthManager.kt
│   │   │   │   ├── PermissionChecker.kt
│   │   │   │   └── BiometricAuth.kt
│   │   │   └── ui/                        # مكونات UI مشتركة
│   │   │       ├── theme/
│   │   │       │   ├── Color.kt
│   │   │       │   ├── Typography.kt
│   │   │       │   └── Theme.kt
│   │   │       ├── components/
│   │   │       │   ├── AppButton.kt
│   │   │       │   ├── AppCard.kt
│   │   │       │   ├── AppTextField.kt
│   │   │       │   ├── BottomSheet.kt
│   │   │       │   └── LoadingShimmer.kt
│   │   │       └── navigation/
│   │   │           ├── NavGraph.kt
│   │   │           └── Screen.kt
│   │   │
│   │   ├── feature_admin/                 # ميزة المدير
│   │   │   ├── dashboard/                 # A1 - لوحة القيادة
│   │   │   │   ├── DashboardScreen.kt
│   │   │   │   ├── DashboardViewModel.kt
│   │   │   │   └── DashboardState.kt
│   │   │   ├── audit_log/                 # A2 - سجل التدقيق
│   │   │   ├── notifications/             # A3 - الإشعارات
│   │   │   ├── reports/                   # A4 - التقارير
│   │   │   ├── orders/                    # B1-B4 - العمليات
│   │   │   ├── finance/                   # C1-C5 - المالية
│   │   │   ├── inventory/                 # D1-D5 - المخزون
│   │   │   └── settings/                  # E1-E8 - الإدارة
│   │   │
│   │   ├── feature_warehouse/             # ميزة المخزن
│   │   │   ├── orders/                    # W1-W5 - الطلبات
│   │   │   │   ├── OrdersScreen.kt
│   │   │   │   ├── OrderDetailScreen.kt
│   │   │   │   ├── PreparationScreen.kt
│   │   │   │   ├── InvoiceCreatorScreen.kt
│   │   │   │   └── DispatchScreen.kt
│   │   │   ├── products/                  # W6-W10 - المنتجات
│   │   │   ├── invoices/                  # W11-W13 - الفواتير
│   │   │   └── profile/                   # W14-W15 - حسابي
│   │   │
│   │   ├── feature_buyer/                 # ميزة المشتري
│   │   ├── feature_cashier/               # ميزة المتحصل
│   │   ├── feature_accountant/            # ميزة المحاسب
│   │   │
│   │   └── MainActivity.kt
│   │
│   └── res/
│       ├── drawable/                      # الأيقونات والصور
│       ├── values/                        # الألوان والنصوص
│       └── font/                          # الخطوط (Tajawal, Inter)
