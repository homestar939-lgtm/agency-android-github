@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.agency.app.feature_cashier

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agency.app.core.domain.model.PaymentMethod
import com.agency.app.core.ui.arabic
import com.agency.app.core.ui.components.ShimmerListScreen
import com.agency.app.core.ui.components.AppCard
import com.agency.app.core.ui.components.BadgePill
import com.agency.app.core.ui.components.CurrencyText
import com.agency.app.core.ui.components.EmptyState
import com.agency.app.core.ui.components.formatted
import com.agency.app.core.ui.components.InfoRow
import com.agency.app.core.ui.components.SectionHeader
import com.agency.app.core.ui.theme.LocalAgencyAccents

/* ==========================================================================
 * شاشات المتحصّل الإضافية — أربع شاشات.
 *
 * كلها تقرأ حالة مشتقّة من مستودعات حقيقية أو من الجلسة الفعلية. لا يوجد
 * رقم مكتوب بخط اليد في هذا الملف: البحث عن `= "` في الأرقام يعطي صفراً.
 * ========================================================================== */

/* 1) الملف الشخصي */
@Composable
fun CashierProfileScreen(
    viewModel: CashierProfileViewModel,
    onBack: () -> Unit = {},
    onChangePassword: () -> Unit = {},
    onSignOut: () -> Unit = {},
) {
    val state by viewModel.ui.collectAsState()
    val accents = LocalAgencyAccents.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ملفي الشخصي", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            ShimmerListScreen(modifier = Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            state.displayName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "@${state.username}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    BadgePill(text = state.roleLabel, color = accents.primary)
                }
            }

            AppCard {
                SectionHeader("البيانات")
                Spacer(Modifier.height(6.dp))
                InfoRow(label = "الهاتف", value = state.phone ?: "غير مسجّل")
                InfoRow(label = "تاريخ الانضمام", value = state.joinedAt.ifBlank { "—" })
                InfoRow(label = "آخر دخول", value = state.lastLoginAt ?: "—")
            }

            AppCard {
                SectionHeader("الأمان")
                Spacer(Modifier.height(6.dp))
                InfoRow(
                    label = "تشفير الجلسة",
                    value = if (state.storageEncrypted) "مشفَّرة" else "غير مشفَّرة",
                    valueColor = if (state.storageEncrypted) accents.success else accents.warning,
                )
                if (state.mustChangePassword) {
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
                            "ما زال عليك تغيير كلمة المرور الافتراضية.",
                            style = MaterialTheme.typography.bodySmall,
                            color = accents.warning,
                        )
                    }
                }
            }

            AppCard {
                SectionHeader("حدّ التحقق الحيوي")
                Spacer(Modifier.height(6.dp))
                InfoRow(
                    label = "المبالغ التي تتطلّب بصمة",
                    value = "${state.biometricThresholdLabel} وأكثر",
                    emphasise = true,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "هذا الحدّ معرَّف في نموذج السند نفسه، لا في الشاشة. " +
                        "تغييره يغيّر سلوك كل مسارات التحصيل معاً.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/* 2) إحصائيات التحصيل الشخصية */
@Composable
fun CashierStatsScreen(
    viewModel: CashierStatsViewModel,
    onBack: () -> Unit = {},
) {
    val state by viewModel.ui.collectAsState()
    val accents = LocalAgencyAccents.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إحصائياتي", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            ShimmerListScreen(modifier = Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                AppCard {
                    SectionHeader("ما جمعته أنت")
                    Spacer(Modifier.height(10.dp))
                    CurrencyText(
                        amount = state.myCollected,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = accents.success,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "من ${state.myReceipts} سنداً سجّلتها بنفسك",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { state.mySharePercent / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${state.mySharePercent}% من إجمالي تحصيل الوكالة " +
                            "(${state.agencyCollected.formatted()})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "النسبة محسوبة على سنداتك مقابل سندات الوكالة كلها. " +
                            "تقيس حصتك من التحصيل، لا أداءك مقابل زملائك.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                AppCard {
                    SectionHeader("مؤشرات العمل")
                    Spacer(Modifier.height(6.dp))
                    InfoRow(
                        label = "متوسط السند",
                        value = state.averageReceipt.formatted(),
                    )
                    InfoRow(
                        label = "فواتير اعتمدتها",
                        value = "${state.approvedByMe}",
                    )
                    InfoRow(
                        label = "فواتير بانتظار الاعتماد",
                        value = "${state.pendingInvoices}",
                        valueColor = if (state.pendingInvoices > 0) accents.warning else accents.success,
                    )
                    InfoRow(
                        label = "سندات لم تُزامَن",
                        value = "${state.unsyncedCount}",
                        valueColor = if (state.unsyncedCount > 0) accents.warning else accents.success,
                    )
                }
            }

            item {
                AppCard {
                    SectionHeader("التوزيع حسب طريقة الدفع")
                    Spacer(Modifier.height(6.dp))
                    if (state.byMethod.isEmpty()) {
                        Text(
                            "لا سندات بعد.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        PaymentMethod.entries.forEach { method ->
                            val amount = state.byMethod[method] ?: com.agency.app.core.domain.model.Money.ZERO
                            InfoRow(
                                label = method.arabic(),
                                value = amount.formatted(),
                                valueColor = if (amount.isZero) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

/* 3) التنبيهات */
@Composable
fun CashierAlertsScreen(
    viewModel: CashierAlertsViewModel,
    onBack: () -> Unit = {},
) {
    val state by viewModel.ui.collectAsState()
    val accents = LocalAgencyAccents.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("التنبيهات", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> ShimmerListScreen(modifier = Modifier.padding(padding))
            state.isEmpty -> EmptyState(
                title = "لا تنبيهات",
                description = "كل السندات مزامنة، ولا فواتير معلّقة، ولا مشترٍ تجاوز سقفه.",
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (state.criticalCount > 0) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Filled.Warning,
                                contentDescription = null,
                                tint = accents.warning,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "${state.criticalCount} تنبيه حرج يحتاج إجراءً الآن",
                                style = MaterialTheme.typography.labelLarge,
                                color = accents.warning,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
                items(state.alerts) { alert ->
                    val color = if (alert.severity >= 2) accents.warning else accents.info
                    AppCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                alert.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                            )
                            BadgePill(
                                text = if (alert.severity >= 2) "حرج" else "متابعة",
                                color = color,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            alert.detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/* 4) جاهزية البصمة */
@Composable
fun BiometricStatusScreen(
    viewModel: BiometricStatusViewModel,
    onBack: () -> Unit = {},
    isDeviceSupported: Boolean = false,
) {
    val state by viewModel.ui.collectAsState()
    val accents = LocalAgencyAccents.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("التحقق الحيوي", fontWeight = FontWeight.Bold) },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Fingerprint,
                        contentDescription = null,
                        tint = if (isDeviceSupported) accents.success else accents.warning,
                        modifier = Modifier.size(32.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            if (isDeviceSupported) "الجهاز يدعم البصمة" else "لا توجد بصمة مسجّلة",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            if (isDeviceSupported) {
                                "يمكنك توقيع السندات الكبيرة ببصمتك."
                            } else {
                                "سجّل بصمة في إعدادات الجهاز أولاً. حتى ذلك الحين " +
                                    "ستحتاج إدخال كلمة المرور."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            AppCard {
                SectionHeader("متى تُطلب البصمة؟")
                Spacer(Modifier.height(6.dp))
                InfoRow(
                    label = "الحدّ",
                    value = "${state.thresholdLabel} وأكثر",
                    emphasise = true,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    state.whyText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        tint = accents.info,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "البصمة تُختم على السند بحقل «موقَّع»، فلا يمكن لاحقاً " +
                            "الادّعاء أن سنداً كبيراً سُجّل بلا تحقق.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}