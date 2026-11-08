package com.agency.app.feature_buyer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agency.app.core.data.auth.AuthManager
import com.agency.app.core.data.auth.SessionState
import com.agency.app.core.domain.model.Product
import com.agency.app.core.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

/**
 * كتالوج المشتري.
 *
 * البحث فوري: تُمرَّر العبارة إلى [ProductRepository.search] مباشرةً،
 * وهي استعلام Room على `name` و`sku`، فلا نسخة ثانية من البيانات في الذاكرة
 * ولا تصفية على مستوى الواجهة.
 */
data class CatalogUiState(
    val products: List<Product> = emptyList(),
    val query: String = "",
    val cartCounts: Map<String, Int> = emptyMap(),
    val isLoading: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CatalogViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val cartStore: CartStore,
    authManager: AuthManager,
) : ViewModel() {

    private val _query = MutableStateFlow("")

    val query: StateFlow<String> = _query.asStateFlow()

    val session: StateFlow<SessionState> = authManager.session

    private val products = _query.flatMapLatest { text ->
        if (text.isBlank()) {
            productRepository.observeActiveProducts()
        } else {
            productRepository.search(text.trim())
        }
    }

    val ui: StateFlow<CatalogUiState> = combine(
        products,
        _query,
        cartStore.lines,
    ) { rows, text, cartLines ->
        CatalogUiState(
            products = rows.filter { it.isActive },
            query = text,
            cartCounts = cartLines.associate { it.productId to it.quantity },
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CatalogUiState())

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun addToCart(product: Product) {
        cartStore.add(product, quantity = product.minOrderQuantity.coerceAtLeast(1))
    }

    fun setCartQuantity(product: Product, quantity: Int) {
        cartStore.setQuantity(product.id, quantity)
    }
}