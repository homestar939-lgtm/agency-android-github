package com.agency.app.feature_home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agency.app.core.domain.model.Money
import com.agency.app.core.domain.model.UserRole
import com.agency.app.core.ui.components.AppCard
import com.agency.app.core.ui.components.CurrencyText
import com.agency.app.core.ui.components.InfoRow
import com.agency.app.core.ui.components.SectionHeader
import com.agency.app.core.ui.navigation.Route
import com.agency.app.core.ui.theme.LocalAgencyAccents

/**
 * لوحة الدور.
 *
 * تعرض لمحة عن العمل اليومي حسب طبيعة الدور، ومداخل الشاشات المتاحة له فقط.
 * كل رقم يقرأ من [DashboardMetrics] المشتق من Flow قادم من Room.
 */
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    role: UserRole,
    userName: String,
    onSignOut: () -> Unit,
    onNavigate: (Route) -> Unit,
    modifier: Modifier = Modifier,
) {
    val metrics by viewModel.metrics.collectAsStateWithLifecycle()
    val accents = LocalAgencyAccents.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = greeting(role),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "$userName — ${roleLabel(role)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onSignOut) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "خروج",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }

        when (role) {
            UserRole.BUYER -> Unit
            else -> Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MetricTile(
                    modifier = Modifier.weight(1f),
                    label = "طلبيات جديدة",
                    value = metrics.ordersNew.toString(),
                    accent = accents.info,
                    icon = Icons.Filled.ReceiptLong,
                )
                MetricTile(
                    modifier = Modifier.weight(1f),
                    label = "قيد التحضير",
                    value = metrics.ordersPreparing.toString(),
                    accent = accents.warning,
                    icon = Icons.Filled.Inventory2,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MetricTile(
                modifier = Modifier.weight(1f),
                label = "فواتير معتمدة",
                value = metrics.invoicesApproved.toString(),
                accent = accents.success,
                icon = Icons.Filled.Assessment,
            )
            MetricTile(
                modifier = Modifier.weight(1f),
                label = "مخزون منخفض",
                value = metrics.productsLowStock.toString(),
                accent = accents.warning,
                icon = Icons.Filled.Inventory2,
            )
        }

        if (role == UserRole.ADMIN || role == UserRole.ACCOUNTANT || role == UserRole.CASHIER) {
            AppCard {
                SectionHeader(title = "المحصّلة المالية")
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "إجمالي الذمم القائمة",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    CurrencyText(
                        amount = Money(metrics.outstandingTotalCents),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(8.dp))
                InfoRow(
                    label = "مشترون تجاوزوا الحد الائتماني",
                    value = metrics.overLimitBuyers.toString(),
                    valueColor = if (metrics.overLimitBuyers > 0) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    emphasise = metrics.overLimitBuyers > 0,
                )
                InfoRow(
                    label = "فواتير بانتظار الاعتماد",
                    value = metrics.invoicesPending.toString(),
                )
                InfoRow(label = "مسودات غير معتمدة", value = metrics.invoicesDraft.toString())
                InfoRow(label = "إجمالي الطلبيات", value = metrics.ordersTotal.toString())
            }
        }

        AppCard {
            SectionHeader(title = "الشاشات المتاحة لك")
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                entriesFor(role).forEach { entry ->
                    EntryRow(
                        icon = entry.icon,
                        label = entry.label,
                        accent = accents.primary,
                        onClick = { onNavigate(entry.route) },
                    )
                }
            }
        }
    }
}

/** مدخل شاشة واحد، محدَّد بدوره. */
private data class DashboardEntry(
    val label: String,
    val route: Route,
    val icon: ImageVector,
)

/**
 * الشاشات المتاحة لكل دور.
 *
 * القائمة تعكس صلاحيات الدور فعلياً: المحاسب لا يرى إلا القراءة،
 * والمشتري لا يرى شاشات المخزن، والمخزن لا يرى المالية.
 */
private fun entriesFor(role: UserRole): List<DashboardEntry> = when (role) {
    UserRole.BUYER -> listOf(
        DashboardEntry("تصفّح المنتجات", Route.BuyerCatalog, Icons.Filled.AddShoppingCart),
        DashboardEntry("سلة الطلب", Route.BuyerCart, Icons.Filled.AddShoppingCart),
        DashboardEntry("طلبياتي", Route.BuyerOrders, Icons.Filled.ListAlt),
        DashboardEntry("كشف حسابي", Route.BuyerStatement, Icons.Filled.AccountBalanceWallet),
        DashboardEntry("فواتيري", Route.BuyerInvoices, Icons.Filled.ReceiptLong),
    )
    UserRole.WAREHOUSE -> listOf(
        DashboardEntry("طلبيات المخزن", Route.WarehouseOrders, Icons.Filled.Inventory2),
        DashboardEntry("إدارة المنتجات", Route.WarehouseProducts, Icons.Filled.Inventory2),
        DashboardEntry("مسح باركود", Route.WarehouseScan, Icons.Filled.QrCodeScanner),
    )
    UserRole.CASHIER -> listOf(
        DashboardEntry("تحصيل", Route.CashierCollect, Icons.Filled.PointOfSale),
        DashboardEntry("الفواتير", Route.CashierInvoices, Icons.Filled.ReceiptLong),
    )
    UserRole.ACCOUNTANT -> listOf(
        DashboardEntry("أرصدة المشترين", Route.AccountantBalances, Icons.Filled.AccountBalanceWallet),
        DashboardEntry("الفواتير", Route.CashierInvoices, Icons.Filled.ReceiptLong),
    )
    UserRole.ADMIN -> listOf(
        DashboardEntry("طلبيات المخزن", Route.WarehouseOrders, Icons.Filled.Inventory2),
        DashboardEntry("إدارة المنتجات", Route.WarehouseProducts, Icons.Filled.Inventory2),
        DashboardEntry("تحصيل", Route.CashierCollect, Icons.Filled.PointOfSale),
        DashboardEntry("الفواتير", Route.CashierInvoices, Icons.Filled.ReceiptLong),
        DashboardEntry("أرصدة المشترين", Route.AccountantBalances, Icons.Filled.AccountBalanceWallet),
        DashboardEntry("المستخدمون", Route.AdminUsers, Icons.Filled.Assessment),
        DashboardEntry("المشترون", Route.AdminBuyers, Icons.Filled.Assessment),
        DashboardEntry("مركز الموافقات", Route.AdminPermissions, Icons.Filled.Assessment),
        DashboardEntry("التقارير", Route.AdminReports, Icons.Filled.Assessment),
    )
}

@Composable
private fun EntryRow(
    icon: ImageVector,
    label: String,
    accent: Color,
    onClick: () -> Unit,
) {
    AppCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = accent)
            Spacer(Modifier.size(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/** بلاطة مؤشر واحدة. */
@Composable
private fun MetricTile(
    label: String,
    value: String,
    accent: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = accent,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun greeting(role: UserRole): String = when (role) {
    UserRole.ADMIN -> "لوحة الإدارة"
    UserRole.WAREHOUSE -> "لوحة المخزن"
    UserRole.CASHIER -> "لوحة الصندوق"
    UserRole.ACCOUNTANT -> "لوحة المحاسبة"
    UserRole.BUYER -> "لوحة المشتري"
}

private fun roleLabel(role: UserRole): String = when (role) {
    UserRole.ADMIN -> "مدير"
    UserRole.WAREHOUSE -> "موظف مخزن"
    UserRole.CASHIER -> "أمين صندوق"
    UserRole.ACCOUNTANT -> "محاسب"
    UserRole.BUYER -> "مشتري"
}