# نواقص تنفيذية ظاهرة في الملف المرجعي

هذه القائمة تقتصر على العناصر التي تظهر كاستدعاءات أو أنواع مطلوبة في المقاطع البرمجية ولا يظهر تعريفها ضمن المقاطع المستخرجة. لا يتم إنشاء بدائل لها هنا حتى لا يتم استبدال منطق غير موجود في المصدر بافتراضات.

## عناصر أساسية مستدعاة وغير معرفة ضمن ملفات المرجع المستخرجة

- `AuthManager` وواجهة `currentUser` المستخدمة في `MainActivity` المرجعي.
- `SyncManager` والعمليات المستخدمة من شاشات وViewModels متعددة.
- المكونات المشتركة `AppButton` و`AppCard` و`AppTextField`.
- `AgencyTypography` و`AdminDarkColorScheme` و`WarehouseDarkColorScheme` المشار إليها في `Theme.kt`.
- `DashboardScreen` و`OrderDetailScreen` المستدعيتان في `NavGraph.kt`.
- `WarehouseBottomNavigation`.
- `PreparationViewModel` و`ProductEditorViewModel` و`LoginViewModel` و`CartViewModel` و`ProductCatalogViewModel` و`ProfileViewModel` و`InvoicesListViewModel` و`RequestPermissionViewModel` و`ReceiptViewerViewModel` و`CollectionStatsViewModel`.

## ملاحظات من النص المرجعي نفسه

- يحتوي الثيم على تعليق `// ... باقي الأدوار`، كما يحتوي NavGraph على شاشات معلّقة/غير موصولة.
- بعض تدفقات البيانات معروضة كتعليقات توضيحية، وليست عمليات repository مكتملة.
- ملفات اختبارات التكامل تشير إلى أدوات وبيانات اختبار لا يقدم المرجع تعريفاتها الكاملة.
- إعداد البناء المرجعي يضع Kotlin Compose plugin 2.0.0 مع `kotlinCompilerExtensionVersion = "1.5.8"`. إعداد الحزمة النشطة يستخدم Compose compiler plugin المتوافق مع Kotlin 2.0، مع إبقاء كتلة البناء الأصلية دون تعديل في `reference-code/legacy-build-config/app.build.gradle.kts`.

## نتيجة ذلك

لا يمكن وصف ملفات المصدر المرجعي وحدها بأنها تطبيق كامل قابل للبناء دون استكمال العناصر أعلاه والتحقق من توقيعاتها ومنطقها. الحزمة الحالية لا تحذفها ولا تستبدلها، وتوفر مشروع Android تمهيدياً وخط CI حتى يمكن استكمال الربط لاحقاً على أساس المصدر الأصلي.
