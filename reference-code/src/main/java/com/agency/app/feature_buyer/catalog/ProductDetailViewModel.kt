// feature_buyer/catalog/ProductDetailViewModel.kt
package com.agency.app.feature_buyer.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agency.app.core.database.entity.Product
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductDetailUiState(
    val isLoading: Boolean = true,
    val product: Product? = null,
    val error: String? = null
)

@HiltViewModel
class ProductDetailViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    fun loadProduct(productId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // تحميل المنتج من قاعدة البيانات
                val product = Product(
                    id = productId,
                    name = "منتج تجريبي",
                    categoryId = "cat1",
                    price = 1500.0,
                    unit = "كغ",
                    stock = 50,
                    minOrderQuantity = 2,
                    expiryDate = null,
                    originCountry = "السعودية",
                    imageUrl = null,
                    description = "وصف تفصيلي للمنتج يوضح مميزاته واستخداماته وجودته العالية.",
                    barcode = "1234567890",
                    showPriceToBuyer = true,
                    showUnitToBuyer = true,
                    showExpiryToBuyer = true,
                    showMinOrderToBuyer = true
                )
                _uiState.update { it.copy(isLoading = false, product = product) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
