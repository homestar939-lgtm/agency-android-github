package com.agency.app.feature_buyer

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ProductionQuantityLimits
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agency.app.core.domain.model.Product
import com.agency.app.core.ui.components.AppCard
import com.agency.app.core.ui.components.AppTextField
import com.agency.app.core.ui.components.BadgePill
import com.agency.app.core.ui.components.EmptyState
import com.agency.app.core.ui.components.LoadingShimmer
import com.agency.app.core.ui.components.formatted
import com.agency.app.core.ui.theme.LocalAgencyAccents

/**
 * كتالوج المنتجات للمشتري.
 *
 * مفاتيح الإظهار ([com.agency.app.core.domain.model.ProductVisibility]) تُحترم
 * حرفياً: إن كان `showPrice` معطّلاً لا يظهر السعر إطلاقاً، وليس مخفياً بصرياً فقط.
 */
@Composable
fun BuyerCatalogScreen(
    viewModel: CatalogViewModel,
    onOpenProduct: (String) -> Unit,
    onOpenCart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.ui.collectAsStateWithLifecycle()
    val accents = LocalAgencyAccents.current
    val totalCartItems = state.cartCounts.values.sum()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            if (totalCartItems > 0) {
                ExtendedFloatingActionButton(
                    onClick = onOpenCart,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(Icons.Filled.ShoppingCart, contentDescription = null) },
                    text = { Text("السلة ($totalCartItems)") },
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(12.dp))
            AppTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                label = "ابحث باسم المنتج أو الرمز",
                leadingIcon = Icons.Filled.Search,
            )
            Spacer(Modifier.height(12.dp))

            when {
                state.isLoading -> LoadingShimmer(lines = 6)
                state.products.isEmpty() -> EmptyState(
                    title = if (state.query.isBlank()) "لا توجد منتجات متاحة" else "لا نتائج للبحث",
                    description = if (state.query.isBlank()) {
                        "لم يسجّل المخزن أي منتج نشط بعد."
                    } else {
                        "جرّب كلمة أخرى أو امسح حقل البحث."
                    },
                    icon = Icons.Filled.ProductionQuantityLimits,
                )
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 168.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 96.dp),
                ) {
                    items(state.products, key = { it.id }) { product ->
                        ProductCard(
                            product = product,
                            cartQuantity = state.cartCounts[product.id] ?: 0,
                            onOpen = { onOpenProduct(product.id) },
                            onAdd = { viewModel.addToCart(product) },
                            onDecrease = { viewModel.setCartQuantity(product, (state.cartCounts[product.id] ?: 0) - 1) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductCard(
    product: Product,
    cartQuantity: Int,
    onOpen: () -> Unit,
    onAdd: () -> Unit,
    onDecrease: () -> Unit,
) {
    val accents = LocalAgencyAccents.current
    AppCard(onClick = onOpen) {
        Text(
            text = product.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (product.visibility.showUnit) {
                Text(
                    text = "الوحدة: ${product.unit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        when {
            product.isOutOfStock -> BadgePill(text = "غير متوفر", color = MaterialTheme.colorScheme.error)
            product.isLowStock && product.visibility.showStock ->
                BadgePill(text = "كمية محدودة", color = accents.warning)
            else -> Unit
        }
        if (product.visibility.showPrice) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = product.price.formatted(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        if (product.visibility.showMinOrder && product.minOrderQuantity > 1) {
            Text(
                text = "أقل كمية: ${product.minOrderQuantity}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
        ) {
            if (cartQuantity > 0) {
                IconButton(onClick = onDecrease, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Remove, contentDescription = "تقليل", modifier = Modifier.size(18.dp))
                }
                Text(
                    text = cartQuantity.toString(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            IconButton(onClick = onAdd, enabled = !product.isOutOfStock) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "أضف للسلة",
                    tint = if (product.isOutOfStock) {
                        MaterialTheme.colorScheme.outline
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
            }
        }
    }
}
