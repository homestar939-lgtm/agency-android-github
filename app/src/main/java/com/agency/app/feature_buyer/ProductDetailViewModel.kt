package com.agency.app.feature_buyer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agency.app.core.domain.model.Product
import com.agency.app.core.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * تفاصيل منتج واحد.
 *
 * [SavedStateHandle] يحمل `productId` القادم من المسار الآمن نوعياً،
 * فالشاشة لا تعرف إلا المنتج الذي طُلب فتحه.
 */
data class ProductDetailUiState(
    val product: Product? = null,
    val cartQuantity: Int = 0,
    val isLoading: Boolean = true,
)

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    productRepository: ProductRepository,
    private val cartStore: CartStore,
) : ViewModel() {

    private val productId: String = savedStateHandle.get<String>("productId").orEmpty()

    val ui: StateFlow<ProductDetailUiState> = combine(
        productRepository.observeProduct(productId),
        cartStore.lines,
    ) { product, lines ->
        ProductDetailUiState(
            product = product,
            cartQuantity = lines.firstOrNull { it.productId == productId }?.quantity ?: 0,
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProductDetailUiState())

    fun addToCart(quantity: Int) {
        val product = ui.value.product ?: return
        cartStore.add(product, quantity)
    }

    fun setQuantity(quantity: Int) {
        cartStore.setQuantity(productId, quantity)
    }
}