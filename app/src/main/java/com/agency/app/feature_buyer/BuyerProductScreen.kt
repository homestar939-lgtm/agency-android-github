package com.agency.app.feature_buyer

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agency.app.core.domain.model.Product
import com.agency.app.core.ui.components.AppButton
import com.agency.app.core.ui.components.AppCard
import com.agency.app.core.ui.components.BadgePill
import com.agency.app.core.ui.components.EmptyState
import com.agency.app.core.ui.components.InfoRow
import com.agency.app.core.ui.components.SectionHeader
import com.agency.app.core.ui.components.formatted
import com.agency.app.core.ui.theme.LocalAgencyAccents
import com.agency.app.core.util.formattedDate

/**
 * شاشة تفاصيل المنتج.
 *
 * الكمية المقترحة تبدأ من `minOrderQuantity` لأن طلب أقل منها مرفوض
 * في طبقة البيانات، فلا نعرض زرّاً يقود إلى خطأ معروف مسبقاً.
 */
@Composable
fun BuyerProductScreen(
    viewModel: ProductDetailViewModel,
    onBack: () -> Unit,
    onOpenCart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.ui.collectAsStateWithLifecycle()
    val accents = LocalAgencyAccents.current
    val product = state.product

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        if (state.isLoading) {
            Spacer(Modifier.padding(innerPadding))
        } else if (product == null) {
            EmptyState(
                title = "المنتج غير موجود",
                description = "قد يكون المخزن قد أزال هذا المنتج أو أوقفه.",
                actionLabel = "رجوع",
                onAction = onBack,
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                AppButton(text = "رجوع", onClick = onBack, modifier = Modifier.fillMaxWidth())
                AppCard {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (product.isOutOfStock) {
                            BadgePill(text = "غير متوفر", color = MaterialTheme.colorScheme.error)
                        } else if (product.isLowStock) {
                            BadgePill(text = "كمية محدودة", color = accents.warning)
                        } else {
                            BadgePill(text = "متوفر", color = accents.success)
                        }
                    }
                    if (product.visibility.showPrice) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = product.price.formatted(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                AppCard {
                    SectionHeader(title = "بيانات الطلب")
                    if (product.visibility.showUnit) {
                        InfoRow(label = "وحدة البيع", value = product.unit)
                    }
                    if (product.visibility.showMinOrder) {
                        InfoRow(label = "أقل كمية للطلب", value = product.minOrderQuantity.toString())
                    }
                    if (product.visibility.showStock) {
                        InfoRow(label = "المتاح في المخزن", value = product.stockQuantity.toString())
                    }
                    if (product.visibility.showExpiry && product.expiryDate != null) {
                        InfoRow(label = "تاريخ الانتهاء", value = product.expiryDate.formattedDate())
                    }
                    InfoRow(label = "آخر تحديث", value = product.updatedAt.formattedDate())
                }

                if (!product.isOutOfStock) {
                    QuantitySelector(
                        product = product,
                        quantity = state.cartQuantity,
                        onIncrease = { viewModel.setQuantity(state.cartQuantity + 1) },
                        onDecrease = { viewModel.setQuantity(state.cartQuantity - 1) },
                    )
                }

                AppButton(
                    text = if (state.cartQuantity > 0) "السلة (${state.cartQuantity})" else "أضف إلى السلة",
                    onClick = {
                        if (state.cartQuantity > 0) {
                            onOpenCart()
                        } else {
                            viewModel.addToCart(product.minOrderQuantity.coerceAtLeast(1))
                        }
                    },
                    enabled = !product.isOutOfStock,
                    icon = Icons.Filled.ShoppingCart,
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun QuantitySelector(
    product: Product,
    quantity: Int,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
) {
    AppCard {
        SectionHeader(title = "الكمية")
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onDecrease, enabled = quantity > 0) {
                Icon(Icons.Filled.Remove, contentDescription = "تقليل", modifier = Modifier.size(22.dp))
            }
            Text(
                text = if (quantity == 0) product.minOrderQuantity.toString() else quantity.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = onIncrease, enabled = quantity < product.stockQuantity) {
                Icon(Icons.Filled.Add, contentDescription = "زيادة", modifier = Modifier.size(22.dp))
            }
        }
        if (quantity in 1 until product.minOrderQuantity) {
            Text(
                text = "أقل كمية مسموحة ${product.minOrderQuantity}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}