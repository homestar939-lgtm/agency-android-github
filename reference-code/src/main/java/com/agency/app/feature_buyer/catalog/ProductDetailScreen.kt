// feature_buyer/catalog/ProductDetailScreen.kt
package com.agency.app.feature_buyer.catalog

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.agency.app.core.database.entity.Product
import com.agency.app.core.ui.components.AppButton
import com.agency.app.core.ui.components.AppCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: String,
    onBackClick: () -> Unit,
    onAddToCart: (Product, Int) -> Unit,
    viewModel: ProductDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHaptic.current
    val scrollState = rememberScrollState()
    var quantity by remember { mutableStateOf(1) }

    LaunchedEffect(productId) {
        viewModel.loadProduct(productId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = { /* مشاركة */ }) {
                        Icon(Icons.Filled.Share, "مشاركة")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        bottomBar = {
            if (uiState.product != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // السعر
                        Column {
                            Text(
                                "السعر",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "${uiState.product!!.price} د.ع",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // زر الإضافة للسلة
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onAddToCart(uiState.product!!, quantity)
                            },
                            modifier = Modifier.height(56.dp),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Icon(Icons.Filled.ShoppingCart, "إضافة")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "إضافة للسلة",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.product != null) {
            val product = uiState.product!!
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
            ) {
                // صورة المنتج — Hero Animation Target
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(
                                RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                            ),
                        contentScale = ContentScale.Crop
                    )
                }

                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // العنوان والتصنيف
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        
                        if (product.showUnitToBuyer) {
                            AssistChip(
                                onClick = { },
                                label = { Text(product.unit) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Scale,
                                        "الوحدة",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }

                    // شبكة المعلومات
                    val infoItems = buildList {
                        if (product.showExpiryToBuyer && product.expiryDate != null) {
                            add(
                                ProductInfoItem(
                                    icon = Icons.Outlined.Event,
                                    label = "تاريخ الانتهاء",
                                    value = product.expiryDate.toString()
                                )
                            )
                        }
                        if (product.showMinOrderToBuyer) {
                            add(
                                ProductInfoItem(
                                    icon = Icons.Outlined.Minimize,
                                    label = "أقل كمية",
                                    value = "${product.minOrderQuantity} ${product.unit}"
                                )
                            )
                        }
                        if (!product.originCountry.isNullOrBlank()) {
                            add(
                                ProductInfoItem(
                                    icon = Icons.Outlined.Public,
                                    label = "بلد الصنع",
                                    value = product.originCountry
                                )
                            )
                        }
                    }

                    if (infoItems.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(infoItems.size) { index ->
                                InfoChip(infoItems[index])
                            }
                        }
                    }

                    HorizontalDivider()

                    // الوصف
                    if (!product.description.isNullOrBlank()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "الوصف",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = product.description,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
                            )
                        }
                    }

                    HorizontalDivider()

                    // محدد الكمية
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "الكمية",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        if (quantity > product.minOrderQuantity) {
                                            quantity--
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    },
                                    enabled = quantity > product.minOrderQuantity,
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Icon(Icons.Filled.Remove, "تقليل")
                                }

                                Text(
                                    text = "$quantity ${product.unit}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.minWidth(80.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )

                                IconButton(
                                    onClick = {
                                        quantity++
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Icon(Icons.Filled.Add, "زيادة")
                                }
                            }

                            // المجموع الفرعي
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "المجموع",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "${product.price * quantity} د.ع",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

data class ProductInfoItem(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String,
    val value: String
)

@Composable
private fun InfoChip(item: ProductInfoItem) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                item.icon,
                item.label,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Column {
                Text(
                    item.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    item.value,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}
