@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.agency.app.feature_admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.agency.app.core.ui.arabic
import com.agency.app.core.ui.components.AppButton
import com.agency.app.core.ui.components.AppCard
import com.agency.app.core.ui.components.AppTextField
import com.agency.app.core.ui.components.BadgePill
import com.agency.app.core.ui.components.EmptyState
import com.agency.app.core.ui.components.InfoRow
import com.agency.app.core.ui.components.LoadingShimmer
import com.agency.app.core.ui.components.SectionHeader
import com.agency.app.core.ui.theme.LocalAgencyAccents
import com.agency.app.core.util.formattedDateTime

/* ==========================================================================
 * شاشات المدير الإضافية — خمس شاشات.
 *
 * القاعدة الحاكمة في هذا الملف: كل شاشة تعرض ما يملكه الكود فعلاً، وتقول
 * بصوت مسموع ما لا تملكه بعد. لا زر «حفظ» لا يحفظ، ولا «نسخ احتياطي» لا
 * ينسخ. الرقم المعروض يجب أن يكون مشتقاً من مستودع حقيقي، والنص يجب أن
 * يطابق السلوك — وإلا صارت الواجهة وعداً لم يُنفَّذ.
 * ========================================================================== */

/* --------------------------------------------------------------------------
 * 1) مصفوفة الصلاحيات
 *
 * لا تُكتب يدوياً. تُشتق من خصائص [UserRole] نفسها، وهي الخصائص التي
 * يقرأها منطق التطبيق عند كل قرار. لو كُتبت الجدول بخط اليد لاحتمل أن
 * تقول الصلاحية في الشاشة شيئاً ويقرر الكود شيئاً آخر — وهو أسوأ من غياب
 * الشاشة، لأنه يمنح ثقة كاذبة.
 * ------------------------------------------------------------------------ */
@Composable
fun AdminPermissionMatrixScreen(
    viewModel: AdminPermissionMatrixViewModel,
    onBack: () -> Unit = {},
) {
    val state by viewModel.ui.collectAsState()
    val accents = LocalAgencyAccents.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("مصفوفة الصلاحيات", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding)) { LoadingShimmer() }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                AppCard {
                    SectionHeader("كيف تُقرأ هذه المصفوفة؟")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "الأعمدة مأخوذة من خصائص الدور في الكود نفسه، لا من جدول " +
                            "مكتوب بيد. إن تغيّرت صلاحية في المنطق، تغيّرت هنا تلقائياً.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(state.rows, key = { it.role.name }) { row ->
                AppCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            row.role.arabic(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        BadgePill(
                            text = if (row.usersCount == 0) "لا مستخدمين" else "${row.usersCount} مستخدم",
                            color = if (row.usersCount == 0) accents.warning else accents.success,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    InfoRow(
                        label = "كتابة على المالية",
                        value = if (row.canWriteFinancials) "مسموحة" else "ممنوعة",
                        valueColor = if (row.canWriteFinancials) accents.success else accents.warning,
                    )
                    InfoRow(
                        label = "القراءة فقط",
                        value = if (row.isReadOnly) "نعم، دائماً" else "لا",
                        valueColor = if (row.isReadOnly) accents.warning else accents.success,
                    )
                    if (row.isReadOnly) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "أي تعديل من هذا الدور يمر عبر طلب صلاحية موقَّع.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/* --------------------------------------------------------------------------
 * 2) إعدادات الوكالة
 *
 * الحقول تُحفظ في [com.agency.app.core.data.auth.SecureStore] وتُقرأ منه.
 * والشاشة تصرّح بأن العمولة المحفوظة هنا **لا تدخل الفوترة بعد** — النص
 * موجود في البطاقة الأخيرة. إخفاء ذلك كان سيجعل المدير يظن أنه غيّر
 * سلوكاً بينما غيّر قيمة معروضة.
 * ------------------------------------------------------------------------ */
@Composable
fun AgencySettingsScreen(
    viewModel: AgencySettingsViewModel,
    onBack: () -> Unit = {},
) {
    val state by viewModel.ui.collectAsState()
    val settings = state.settings
    val accents = LocalAgencyAccents.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إعدادات الوكالة", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppCard {
                SectionHeader("بيانات الوكالة")
                Spacer(Modifier.height(12.dp))
                AppTextField(
                    value = settings.agencyName,
                    onValueChange = { value -> viewModel.update { it.copy(agencyName = value) } },
                    label = "اسم الوكالة",
                )
                Spacer(Modifier.height(10.dp))
                AppTextField(
                    value = settings.phone,
                    onValueChange = { value -> viewModel.update { it.copy(phone = value) } },
                    label = "الهاتف",
                    keyboardType = KeyboardType.Phone,
                )
                Spacer(Modifier.height(10.dp))
                AppTextField(
                    value = settings.address,
                    onValueChange = { value -> viewModel.update { it.copy(address = value) } },
                    label = "العنوان",
                    singleLine = false,
                )
            }

            AppCard {
                SectionHeader("السياسات المالية")
                Spacer(Modifier.height(12.dp))
                AppTextField(
                    value = settings.defaultCommissionPercent.toString(),
                    onValueChange = { raw ->
                        raw.filter { it.isDigit() }.toIntOrNull()?.let { parsed ->
                            viewModel.update { it.copy(defaultCommissionPercent = parsed) }
                        }
                    },
                    label = "نسبة العمولة الافتراضية %",
                    keyboardType = KeyboardType.Number,
                    supportingText = "القيمة الحالية: ${settings.defaultCommissionPercent}%",
                )
                Spacer(Modifier.height(10.dp))
                AppTextField(
                    value = settings.creditWarningPercent.toString(),
                    onValueChange = { raw ->
                        raw.filter { it.isDigit() }.toIntOrNull()?.let { parsed ->
                            viewModel.update { it.copy(creditWarningPercent = parsed) }
                        }
                    },
                    label = "تحذير عند بلوغ % من سقف الدين",
                    keyboardType = KeyboardType.Number,
                    supportingText = "تنبيه للمتحصّل قبل تجاوز الحد",
                )
            }

            AppCard {
                SectionHeader("حالة التخزين")
                Spacer(Modifier.height(10.dp))
                InfoRow(
                    label = "تشفير المفاتيح",
                    value = if (state.storageEncrypted) "مشفَّر (AndroidKeyStore)" else "تخزين احتياطي غير مشفَّر",
                    valueColor = if (state.storageEncrypted) accents.success else accents.warning,
                )
                if (!state.storageEncrypted) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Warning,
                            contentDescription = null,
                            tint = accents.warning,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "الجهاز لا يوفّر مخزن مفاتيح؛ البيانات محفوظة في تخزين عادي.",
                            style = MaterialTheme.typography.bodySmall,
                            color = accents.warning,
                        )
                    }
                }
            }

            if (state.error != null) {
                Text(
                    state.error ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (state.notice != null) {
                Text(
                    state.notice ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = accents.success,
                )
            }

            AppButton(
                text = "حفظ الإعدادات",
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth(),
                loading = state.isSaving,
                enabled = settings.isValid,
            )

            AppCard(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                SectionHeader("ما لا تفعله هذه الشاشة بعد")
                Spacer(Modifier.height(8.dp))
                Text(
                    "نسبة العمولة تُحفظ وتُعرض، لكن منطق الفوترة يستقبلها كوسيط " +
                        "من شاشة الفاتورة. أي تغيير هنا لا يغيّر فاتورة قائمة ولا " +
                        "قادمة حتى تُربط بالمستودع. نكتبها صريحة كي لا يُبنى قرار " +
                        "مالي على قيمة لا أثر لها.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/* --------------------------------------------------------------------------
 * 3) إعدادات السند
 *
 * التبديلات تُحفظ فوراً في SecureStore — لا زر حفظ، لأن إعداداً بحجم
 * مفتاح واحد لا يستحق رحلة تأكيد. السند يُقرأ منه عند الطباعة.
 * ------------------------------------------------------------------------ */
@Composable
fun ReceiptSettingsScreen(
    viewModel: ReceiptSettingsViewModel,
    onBack: () -> Unit = {},
    biometricThresholdLabel: String = "1,000,000.00",
) {
    val state by viewModel.ui.collectAsState()
    val settings = state.settings

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إعدادات السند", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppCard {
                SectionHeader("الطباعة")
                Spacer(Modifier.height(4.dp))
                SettingSwitchRow(
                    title = "ختم الوكالة على السند",
                    description = "يُطبع اسم الوكالة وتاريخ الإصدار أسفل السند.",
                    checked = settings.showAgencyStamp,
                    onCheckedChange = { viewModel.toggleStamp() },
                )
                SettingSwitchRow(
                    title = "تحذير قبل طبع نسخة مكرّرة",
                    description = "يمنع طبع سندين لنفس العملية بالخطأ.",
                    checked = settings.duplicatePrintWarning,
                    onCheckedChange = { viewModel.toggleDuplicateWarning() },
                )
            }

            AppCard {
                SectionHeader("التحقق الحيوي")
                Spacer(Modifier.height(4.dp))
                SettingSwitchRow(
                    title = "طلب بصمة للمبالغ الكبيرة",
                    description = "المبالغ التي تبلغ $biometricThresholdLabel أو أكثر " +
                        "تتطلّب تحققاً حيوياً قبل الحفظ. الإعداد يخصّ العرض، " +
                        "والحد نفسه معرَّف في نموذج السند.",
                    checked = settings.requireBiometricOverThreshold,
                    onCheckedChange = { viewModel.toggleBiometric() },
                )
            }

            AppCard {
                SectionHeader("نص أسفل السند")
                Spacer(Modifier.height(12.dp))
                AppTextField(
                    value = settings.footerNote,
                    onValueChange = viewModel::onFooterChange,
                    label = "ملاحظة الشكر",
                    singleLine = false,
                )
            }

            if (state.notice != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = LocalAgencyAccents.current.success,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        state.notice ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalAgencyAccents.current.success,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = { onCheckedChange(it) })
    }
}

/* --------------------------------------------------------------------------
 * 4) سجل التدقيق
 *
 * مُشتق من آثار حقيقية: من أنشأ الفاتورة، من اعتمدها ومتى، حركات الطلبية
 * من خطّها الزمني، من جمع السند، ومن راجع طلب الصلاحية. لم أنشئ جدولاً
 * جديداً لأن شرط سجل التدقيق أن يكون غير قابل للتحرير من التطبيق، وأي
 * جدول في Room قابل للمسح من الشاشة — فيصير السجل قابلاً للتزييف.
 * ------------------------------------------------------------------------ */
@Composable
fun AuditLogScreen(
    viewModel: AuditLogViewModel,
    onBack: () -> Unit = {},
) {
    val state by viewModel.ui.collectAsState()
    val accents = LocalAgencyAccents.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("سجل التدقيق", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BadgePill(
                    text = "الكل",
                    color = if (state.category == null) accents.primary else accents.info,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.setCategory(null) },
                )
                AuditCategory.entries.forEach { category ->
                    BadgePill(
                        text = category.label,
                        color = if (state.category == category) accents.primary else accents.info,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.setCategory(category) },
                    )
                }
            }

            if (state.isLoading) {
                Box(Modifier.fillMaxSize()) { LoadingShimmer() }
                return@Column
            }
            if (state.isEmpty) {
                EmptyState(
                    title = "لا أحداث بعد",
                    description = "يظهر هنا كل إجراء مالي أو تشغيلي بمجرد تسجيله في النظام.",
                    actionLabel = null,
                )
                return@Column
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.visible) { event ->
                    AppCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            BadgePill(text = event.category.label, color = accents.info)
                            Text(
                                event.at.formattedDateTime(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            event.action,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(2.dp))
                        InfoRow(label = "الجهة", value = event.subject)
                        InfoRow(label = "المنفّذ", value = event.actor)
                        if (!event.detail.isNullOrBlank()) {
                            InfoRow(label = "التفصيل", value = event.detail)
                        }
                    }
                }
            }
        }
    }
}

/* --------------------------------------------------------------------------
 * 5) حالة النسخ الاحتياطي
 *
 * لا زر «نسخ احتياطي» لأني لا أملك تصديراً مشفَّراً حقيقياً. ما أملكه
 * فعلاً هو فحص جاهزية البيانات: عدد السجلات، وهل الذمم تطابق عملياتها،
 * وكم سنداً لم يُزامَن بعد (سيضيع لو تعطّب الجهاز).
 * فحص التناسق هو الأهم: اختلاف بين مجموع الذمم و(المفوتر − المدفوع) يعني
 * قاعدة تحمل رصيداً لا يطابق عملياته، وهي أخطر ما يمكن نسخه بلا إنذار.
 * ------------------------------------------------------------------------ */
@Composable
fun BackupStatusScreen(
    viewModel: BackupStatusViewModel,
    onBack: () -> Unit = {},
) {
    val state by viewModel.ui.collectAsState()
    val accents = LocalAgencyAccents.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("حالة النسخ الاحتياطي", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding)) { LoadingShimmer() }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                AppCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "الحالة العامة",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        BadgePill(
                            text = if (state.isHealthy) "سليمة" else "تحتاج مراجعة",
                            color = if (state.isHealthy) accents.success else accents.warning,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (state.isHealthy) {
                            "البيانات متماسكة وقابلة للنسخ. لا سندات معلّقة."
                        } else {
                            "في البند الأحمر أعلاه سبب يمنع اعتبار البيانات آمنة للنسخ."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                AppCard {
                    SectionHeader("فحص البيانات")
                    Spacer(Modifier.height(8.dp))
                    state.items.forEach { check ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (check.ok) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = if (check.ok) accents.success else accents.warning,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(check.label, style = MaterialTheme.typography.bodyMedium)
                            }
                            Text(
                                check.value,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (check.ok) MaterialTheme.colorScheme.onSurface else accents.warning,
                            )
                        }
                    }
                }
            }

            if (state.hasLossRisk) {
                item {
                    AppCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Warning,
                                contentDescription = null,
                                tint = accents.warning,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "خطر فقد",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = accents.warning,
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "${state.unsyncedReceipts} سنداً لم يُزامَن بعد. لو تعطّب " +
                                "الجهاز الآن، ستضيع هذه السندات ولا يمكن استعادتها من " +
                                "الخادم. أعِد المزامنة قبل أي نسخ.",
                            style = MaterialTheme.typography.bodySmall,
                            color = accents.warning,
                        )
                    }
                }
            }

            item {
                AppCard(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "لماذا لا يوجد زر نسخ؟",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "النسخة الآمنة تحتاج تصديراً مشفَّراً بمفتاح مستقل ومساحة " +
                            "خارج الجهاز. أي زر هنا اليوم سيكتب ملفاً غير مشفَّر ويسمّيه " +
                            "نسخة احتياطية. الشاشة تقيس الجاهزية وتصدُق في ذلك، وهي " +
                            "خطوة قبل التصدير لا بديل عنه.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
