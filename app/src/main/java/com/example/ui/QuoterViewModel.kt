package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.database.AppDatabase
import com.example.data.entity.InventoryPartEntity
import com.example.data.entity.PriceItemEntity
import com.example.data.repository.StoreRepository
import com.example.util.LanguageManager
import com.example.util.PriceListPdfParser
import com.example.util.ScreenTariff
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.ceil

data class QuoteLine(val itemId: Long, val name: String, val cost: Double, val qty: Int, val category: String = "")

sealed interface ImportState {
    data object Idle : ImportState
    data object Working : ImportState
    data class Done(val count: Int, val skipped: Int) : ImportState
    data class Failed(val message: String) : ImportState
}

/** Customer price for a part: supplier cost plus margin, rounded up to the next 5. */
fun quotePrice(cost: Double, marginPercent: Double): Double {
    val raw = cost * (1.0 + marginPercent / 100.0)
    return ceil(raw / 5.0) * 5.0
}

/**
 * Price the customer pays for one unit. Screens follow the shop tariff table (with the chosen discount);
 * everything else is supplier cost plus the margin.
 */
fun priceOf(category: String, cost: Double, marginPercent: Double, discountPercent: Int, noDiscount: Boolean): Double {
    if (ScreenTariff.isScreen(category)) {
        ScreenTariff.price(cost, if (noDiscount) 0 else discountPercent)?.let { return it }
    }
    return quotePrice(cost, marginPercent)
}

class QuoterViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StoreRepository(AppDatabase.getInstance(application))
    private val prefs = application.getSharedPreferences("quoter_prefs", Context.MODE_PRIVATE)

    val allItems: StateFlow<List<PriceItemEntity>> = repository.priceItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val query = MutableStateFlow("")
    val category = MutableStateFlow<String?>(null)

    val categories: StateFlow<List<String>> = allItems.map { items ->
        items.groupingBy { it.category }.eachCount().entries
            .sortedByDescending { it.value }
            .take(14)
            .map { it.key }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val results: StateFlow<List<PriceItemEntity>> = combine(allItems, query, category) { items, q, cat ->
        val tokens = q.trim().lowercase().split(Regex("""\s+""")).filter { it.isNotEmpty() }
        items.asSequence()
            .filter { cat == null || it.category == cat }
            .filter { item ->
                val name = item.name.lowercase()
                tokens.all { name.contains(it) }
            }
            .take(150)
            .toList()
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val message = MutableStateFlow<String?>(null)

    val marginText = MutableStateFlow(prefs.getInt("margin_percent", 30).toString())
    val laborText = MutableStateFlow("")
    val customerName = MutableStateFlow("")
    val lines = MutableStateFlow<List<QuoteLine>>(emptyList())
    val discountPercent = MutableStateFlow(0)
    val marketplaceQuote = MutableStateFlow(false)

    fun marginPercent(): Double = marginText.value.replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0

    fun setMargin(text: String) {
        marginText.value = text
        text.toDoubleOrNull()?.let { prefs.edit().putInt("margin_percent", it.toInt()).apply() }
    }

    fun consumeMessage() {
        message.value = null
    }

    fun addLine(item: PriceItemEntity) {
        val current = lines.value
        lines.value = if (current.any { it.itemId == item.id }) {
            current.map { if (it.itemId == item.id) it.copy(qty = it.qty + 1) else it }
        } else {
            current + QuoteLine(item.id, item.name, item.cost, 1, item.category)
        }
    }

    fun changeQty(itemId: Long, delta: Int) {
        lines.value = lines.value.mapNotNull {
            if (it.itemId != itemId) it else (it.qty + delta).let { q -> if (q <= 0) null else it.copy(qty = q) }
        }
    }

    fun clearQuote() {
        lines.value = emptyList()
        laborText.value = ""
        customerName.value = ""
        discountPercent.value = 0
        marketplaceQuote.value = false
    }

    fun importPdf(uri: Uri, replaceExisting: Boolean) {
        if (importState.value == ImportState.Working) return
        importState.value = ImportState.Working
        viewModelScope.launch {
            val app = getApplication<Application>()
            try {
                val result = withContext(Dispatchers.Default) { PriceListPdfParser.parse(app, uri) }
                if (result.items.isEmpty()) {
                    importState.value = ImportState.Failed(LanguageManager.getString(app, R.string.q_import_nothing))
                    return@launch
                }
                val source = displayName(app, uri)
                val now = System.currentTimeMillis()
                repository.replacePriceList(
                    result.items.map {
                        PriceItemEntity(
                            name = it.name,
                            category = PriceListPdfParser.categoryOf(it.name),
                            cost = it.price,
                            source = source,
                            importedAt = now
                        )
                    },
                    replaceExisting
                )
                importState.value = ImportState.Done(result.items.size, result.skipped)
            } catch (e: Exception) {
                importState.value = ImportState.Failed(e.message ?: LanguageManager.getString(app, R.string.msg_generic_error))
            }
        }
    }

    fun clearCatalog() {
        viewModelScope.launch {
            repository.clearPriceList()
            lines.value = emptyList()
            importState.value = ImportState.Idle
        }
    }

    fun addToInventory(item: PriceItemEntity) {
        viewModelScope.launch {
            repository.savePart(
                InventoryPartEntity(
                    partName = item.name,
                    category = item.category,
                    compatibleModels = "",
                    quantityInStock = 0,
                    purchaseCost = item.cost,
                    sellingPrice = priceOf(item.category, item.cost, marginPercent(), 0, true),
                    locationRack = ""
                )
            )
            message.value = LanguageManager.getString(getApplication<Application>(), R.string.q_added_inventory, item.name)
        }
    }

    private fun displayName(app: Application, uri: Uri): String {
        return try {
            app.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            } ?: ""
        } catch (e: Exception) {
            ""
        }
    }
}
