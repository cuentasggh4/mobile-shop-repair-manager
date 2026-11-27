package com.example.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.database.AppDatabase
import com.example.data.entity.CashMovementEntity
import com.example.data.entity.InventoryPartEntity
import com.example.data.entity.SaleEntity
import com.example.data.entity.SupplierEntity
import com.example.data.repository.CartLine
import com.example.data.repository.NotEnoughStockException
import com.example.util.LanguageManager
import com.example.data.repository.PeriodSummary
import com.example.data.repository.ReportPeriod
import com.example.data.repository.StoreRepository
import com.example.data.repository.buildPeriodSummary
import com.example.data.repository.periodRange
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * State and actions for the store module: inventory, suppliers, point of sale,
 * cash register and reports. Navigation still lives in [ShopViewModel].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StoreViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StoreRepository(AppDatabase.getInstance(application))

    // One-shot message shown as a Toast by the screens.
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    fun consumeMessage() {
        _message.value = null
    }

    // --- Inventory & suppliers ---
    val parts: StateFlow<List<InventoryPartEntity>> = repository.parts
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val suppliers: StateFlow<List<SupplierEntity>> = repository.suppliers
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _partQuery = MutableStateFlow("")
    val partQuery: StateFlow<String> = _partQuery.asStateFlow()
    private val _onlyLowStock = MutableStateFlow(false)
    val onlyLowStock: StateFlow<Boolean> = _onlyLowStock.asStateFlow()

    fun onPartQueryChanged(q: String) {
        _partQuery.value = q
    }

    fun onOnlyLowStockChanged(v: Boolean) {
        _onlyLowStock.value = v
    }

    val filteredParts: StateFlow<List<InventoryPartEntity>> = combine(parts, _partQuery, _onlyLowStock) { list, q, low ->
        var result = list
        if (low) result = result.filter { it.isLowStock }
        if (q.isNotBlank()) {
            val needle = q.trim().lowercase()
            result = result.filter {
                it.partName.lowercase().contains(needle) ||
                    it.category.lowercase().contains(needle) ||
                    it.compatibleModels.lowercase().contains(needle) ||
                    it.barcode.lowercase().contains(needle) ||
                    it.locationRack.lowercase().contains(needle)
            }
        }
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockCount: StateFlow<Int> = parts
        .map { list -> list.count { it.isLowStock } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun savePart(part: InventoryPartEntity) = launchSafely {
        repository.savePart(part)
    }

    fun deletePart(part: InventoryPartEntity) = launchSafely {
        repository.deletePart(part)
    }

    fun saveSupplier(supplier: SupplierEntity) = launchSafely {
        repository.saveSupplier(supplier)
    }

    fun deleteSupplier(supplier: SupplierEntity) = launchSafely {
        repository.deleteSupplier(supplier)
    }

    fun receiveStock(partId: Long, supplier: SupplierEntity?, quantity: Int, unitCost: Double) = launchSafely {
        repository.receiveStock(partId, supplier, quantity, unitCost)
    }

    // --- Point of sale ---
    var cart by mutableStateOf(listOf<CartLine>())
        private set
    var cartDiscountText by mutableStateOf("")
    var cartPaymentMode by mutableStateOf("Cash")
    var cartCustomerName by mutableStateOf("")
    var posQuery by mutableStateOf("")

    val cartSubtotal: Double get() = cart.sumOf { it.lineTotal }
    val cartDiscount: Double
        get() = (cartDiscountText.replace(',', '.').toDoubleOrNull() ?: 0.0).coerceIn(0.0, cartSubtotal)
    val cartTotal: Double get() = cartSubtotal - cartDiscount

    /** Scanned barcode: adds the matching part to the cart, or searches for it when nothing matches. */
    fun scanBarcode(code: String) {
        val match = parts.value.firstOrNull { it.barcode.isNotBlank() && it.barcode.equals(code.trim(), ignoreCase = true) }
        if (match != null) {
            addPartToCart(match)
            posQuery = ""
        } else {
            posQuery = code.trim()
        }
    }

    fun addPartToCart(part: InventoryPartEntity) {
        val existing = cart.find { it.partId == part.id }
        if (existing == null) {
            if (part.quantityInStock <= 0) {
                _message.value = LanguageManager.getString(getApplication<Application>(), R.string.msg_out_of_stock, part.partName)
                return
            }
            cart = cart + CartLine(
                partId = part.id,
                name = part.partName,
                unitPrice = part.sellingPrice,
                unitCost = part.purchaseCost,
                quantity = 1,
                maxStock = part.quantityInStock
            )
        } else {
            changeCartQuantity(existing, 1)
        }
    }

    fun addCustomItemToCart(name: String, price: Double) {
        val cleanName = name.trim()
        if (cleanName.isEmpty() || price <= 0.0) return
        val existing = cart.find { it.partId == 0L && it.name == cleanName && it.unitPrice == price }
        if (existing != null) {
            changeCartQuantity(existing, 1)
        } else {
            cart = cart + CartLine(partId = 0L, name = cleanName, unitPrice = price, unitCost = 0.0, quantity = 1)
        }
    }

    fun changeCartQuantity(line: CartLine, delta: Int) {
        val newQty = line.quantity + delta
        if (newQty <= 0) {
            removeFromCart(line)
            return
        }
        if (newQty > line.maxStock) {
            _message.value = LanguageManager.getString(getApplication<Application>(), R.string.msg_only_in_stock, line.maxStock, line.name)
            return
        }
        cart = cart.map { if (it === line) it.copy(quantity = newQty) else it }
    }

    fun removeFromCart(line: CartLine) {
        cart = cart.filter { it !== line }
    }

    fun clearCart() {
        cart = emptyList()
        cartDiscountText = ""
        cartPaymentMode = "Cash"
        cartCustomerName = ""
    }

    fun checkout(onDone: (Double) -> Unit) {
        val lines = cart
        if (lines.isEmpty()) return
        val total = cartTotal
        val discount = cartDiscount
        val mode = cartPaymentMode
        val customer = cartCustomerName
        launchSafely {
            repository.checkout(lines, discount, mode, customer)
            clearCart()
            onDone(total)
        }
    }

    fun voidSale(sale: SaleEntity) = launchSafely {
        repository.voidSale(sale.id)
    }

    // --- Cash register & reports ---
    val todaySummary: StateFlow<PeriodSummary> = summaryFlow(ReportPeriod.TODAY)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PeriodSummary())

    val todaySales: StateFlow<List<SaleEntity>> = periodRange(ReportPeriod.TODAY).let { (from, to) ->
        repository.salesBetween(from, to)
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    val todayMovements: StateFlow<List<CashMovementEntity>> = periodRange(ReportPeriod.TODAY).let { (from, to) ->
        repository.movementsBetween(from, to)
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    private val _reportPeriod = MutableStateFlow(ReportPeriod.TODAY)
    val reportPeriod: StateFlow<ReportPeriod> = _reportPeriod.asStateFlow()
    fun onReportPeriodChanged(p: ReportPeriod) {
        _reportPeriod.value = p
    }

    val periodSummary: StateFlow<PeriodSummary> = _reportPeriod
        .flatMapLatest { summaryFlow(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PeriodSummary())

    fun addMovement(type: String, amount: Double, note: String) = launchSafely {
        repository.addMovement(type, amount, note)
    }

    fun deleteMovement(movement: CashMovementEntity) = launchSafely {
        repository.deleteMovement(movement)
    }

    // --- Parts used in repairs ---
    fun usePartOnJob(jobId: Long, partId: Long, quantity: Int) = launchSafely {
        repository.usePartOnJob(jobId, partId, quantity)
    }

    fun returnPartFromJob(jobId: Long, partId: Long) = launchSafely {
        repository.returnPartFromJob(jobId, partId)
    }

    private fun summaryFlow(period: ReportPeriod): Flow<PeriodSummary> = periodRange(period).let { (from, to) ->
        combine(
            repository.salesBetween(from, to),
            repository.saleItemsBetween(from, to),
            repository.purchasesBetween(from, to),
            repository.movementsBetween(from, to),
            repository.allJobs
        ) { sales, items, purchases, movements, jobs ->
            buildPeriodSummary(
                from = from,
                to = to,
                sales = sales,
                saleItems = items,
                purchases = purchases,
                movements = movements,
                jobs = jobs,
                partsById = parts.value.associateBy { it.id }
            )
        }.catch { emit(PeriodSummary()) }
    }

    private fun launchSafely(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                val app = getApplication<Application>()
                _message.value = when {
                    e is NotEnoughStockException && e.itemName != null ->
                        LanguageManager.getString(app, R.string.msg_not_enough_stock, e.itemName)
                    else -> e.message ?: LanguageManager.getString(app, R.string.msg_generic_error)
                }
            }
        }
    }
}
