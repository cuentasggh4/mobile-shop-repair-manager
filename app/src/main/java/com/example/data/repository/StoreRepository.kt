package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.database.AppDatabase
import com.example.data.entity.CashMovementEntity
import com.example.data.entity.InventoryPartEntity
import com.example.data.entity.JobCardEntity
import com.example.data.entity.PurchaseEntity
import com.example.data.entity.SaleEntity
import com.example.data.entity.SaleItemEntity
import com.example.data.entity.SupplierEntity
import com.example.data.model.RepairStage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Calendar

/** One line of the point-of-sale cart. partId == 0 means a free-form item (not tracked in stock). */
data class CartLine(
    val partId: Long,
    val name: String,
    val unitPrice: Double,
    val unitCost: Double,
    val quantity: Int,
    val maxStock: Int = Int.MAX_VALUE
) {
    val lineTotal: Double get() = unitPrice * quantity
}

enum class ReportPeriod { TODAY, LAST_7_DAYS, LAST_30_DAYS }

data class TopItem(val name: String, val quantity: Int, val revenue: Double)

data class PeriodSummary(
    val salesCount: Int = 0,
    val salesTotal: Double = 0.0,
    val salesCost: Double = 0.0,
    val repairsCount: Int = 0,
    val repairsTotal: Double = 0.0,
    val repairPartsCost: Double = 0.0,
    val purchasesTotal: Double = 0.0,
    val cashIn: Double = 0.0,
    val expenses: Double = 0.0,
    val byPaymentMode: Map<String, Double> = emptyMap(),
    val expectedCash: Double = 0.0,
    val profit: Double = 0.0,
    val topItems: List<TopItem> = emptyList()
) {
    val revenue: Double get() = salesTotal + repairsTotal
}

/** Start (inclusive) and end (exclusive) timestamps of a report period. */
fun periodRange(period: ReportPeriod, now: Long = System.currentTimeMillis()): Pair<Long, Long> {
    val cal = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val startOfToday = cal.timeInMillis
    val daysBack = when (period) {
        ReportPeriod.TODAY -> 0
        ReportPeriod.LAST_7_DAYS -> 6
        ReportPeriod.LAST_30_DAYS -> 29
    }
    cal.add(Calendar.DAY_OF_YEAR, -daysBack)
    val from = cal.timeInMillis
    cal.timeInMillis = startOfToday
    cal.add(Calendar.DAY_OF_YEAR, 1)
    return from to cal.timeInMillis
}

private fun isCash(mode: String) = mode.trim().equals("Cash", ignoreCase = true) || mode.isBlank()

/** Money a repair job brought in. Only jobs marked as paid count. */
fun repairRevenue(job: JobCardEntity): Double {
    val total = if (job.finalCost > 0.0) job.finalCost else job.estimatedCost
    return (total - job.discount).coerceAtLeast(0.0)
}

fun buildPeriodSummary(
    from: Long,
    to: Long,
    sales: List<SaleEntity>,
    saleItems: List<SaleItemEntity>,
    purchases: List<PurchaseEntity>,
    movements: List<CashMovementEntity>,
    jobs: List<JobCardEntity>,
    partsById: Map<Long, InventoryPartEntity>
): PeriodSummary {
    val paidJobs = jobs.filter { job ->
        val paidAt = job.deliveredAt ?: job.updatedAt
        job.isPaid && job.stage != RepairStage.CANCELLED && paidAt >= from && paidAt < to
    }

    val byMode = linkedMapOf<String, Double>()
    sales.forEach { s ->
        val key = s.paymentMode.ifBlank { "Cash" }
        byMode[key] = (byMode[key] ?: 0.0) + s.total
    }
    paidJobs.forEach { j ->
        val key = j.paymentMode.ifBlank { "Cash" }
        byMode[key] = (byMode[key] ?: 0.0) + repairRevenue(j)
    }

    val salesTotal = sales.sumOf { it.total }
    val salesCost = sales.sumOf { it.costTotal }
    val repairsTotal = paidJobs.sumOf { repairRevenue(it) }
    val repairPartsCost = paidJobs.sumOf { job ->
        job.linkedPartIdsCsv.split(",")
            .mapNotNull { it.trim().toLongOrNull() }
            .sumOf { id -> partsById[id]?.purchaseCost ?: 0.0 }
    }
    val cashIn = movements.filter { it.type == "IN" }.sumOf { it.amount }
    val expenses = movements.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val cashSales = sales.filter { isCash(it.paymentMode) }.sumOf { it.total }
    val cashRepairs = paidJobs.filter { isCash(it.paymentMode) }.sumOf { repairRevenue(it) }

    val top = saleItems
        .groupBy { it.name }
        .map { (name, items) ->
            TopItem(
                name = name,
                quantity = items.sumOf { it.quantity },
                revenue = items.sumOf { it.unitPrice * it.quantity }
            )
        }
        .sortedByDescending { it.revenue }
        .take(5)

    return PeriodSummary(
        salesCount = sales.size,
        salesTotal = salesTotal,
        salesCost = salesCost,
        repairsCount = paidJobs.size,
        repairsTotal = repairsTotal,
        repairPartsCost = repairPartsCost,
        purchasesTotal = purchases.sumOf { it.unitCost * it.quantity },
        cashIn = cashIn,
        expenses = expenses,
        byPaymentMode = byMode,
        expectedCash = cashSales + cashRepairs + cashIn - expenses,
        profit = (salesTotal - salesCost) + (repairsTotal - repairPartsCost) - expenses,
        topItems = top
    )
}

class StoreRepository(private val db: AppDatabase) {

    private val store = db.storeDao()
    private val inventory = db.inventoryDao()
    private val jobs = db.jobCardDao()

    val parts: Flow<List<InventoryPartEntity>> = inventory.getAllParts()
    val suppliers: Flow<List<SupplierEntity>> = store.getAllSuppliers()
    val allJobs: Flow<List<JobCardEntity>> = jobs.getAllJobs()

    fun salesBetween(from: Long, to: Long) = store.getSalesBetween(from, to)
    fun saleItemsBetween(from: Long, to: Long) = store.getSaleItemsBetween(from, to)
    fun purchasesBetween(from: Long, to: Long) = store.getPurchasesBetween(from, to)
    fun movementsBetween(from: Long, to: Long) = store.getMovementsBetween(from, to)

    // --- Inventory ---
    suspend fun savePart(part: InventoryPartEntity): Long = withContext(Dispatchers.IO) {
        if (part.id == 0L) {
            inventory.insertPart(part.copy(updatedAt = System.currentTimeMillis()))
        } else {
            inventory.updatePart(part.copy(updatedAt = System.currentTimeMillis()))
            part.id
        }
    }

    suspend fun deletePart(part: InventoryPartEntity) = withContext(Dispatchers.IO) {
        inventory.deletePart(part)
    }

    // --- Suppliers ---
    suspend fun saveSupplier(supplier: SupplierEntity) = withContext(Dispatchers.IO) {
        if (supplier.id == 0L) store.insertSupplier(supplier) else store.updateSupplier(supplier)
    }

    suspend fun deleteSupplier(supplier: SupplierEntity) = withContext(Dispatchers.IO) {
        store.deleteSupplier(supplier)
    }

    // --- Purchases (receiving stock) ---
    suspend fun receiveStock(
        partId: Long,
        supplier: SupplierEntity?,
        quantity: Int,
        unitCost: Double
    ) = withContext(Dispatchers.IO) {
        require(quantity > 0) { "Quantity must be greater than zero" }
        db.withTransaction {
            val part = store.getPart(partId) ?: throw IllegalStateException("Part not found")
            val now = System.currentTimeMillis()
            store.addStock(partId, quantity, now)
            store.setPurchaseInfo(partId, unitCost, supplier?.id ?: part.supplierId, now)
            store.insertPurchase(
                PurchaseEntity(
                    supplierId = supplier?.id ?: 0L,
                    supplierName = supplier?.name ?: "",
                    partId = partId,
                    partName = part.partName,
                    quantity = quantity,
                    unitCost = unitCost,
                    createdAt = now
                )
            )
        }
    }

    // --- Point of sale ---
    suspend fun checkout(
        lines: List<CartLine>,
        discount: Double,
        paymentMode: String,
        customerName: String
    ): Long = withContext(Dispatchers.IO) {
        require(lines.isNotEmpty()) { "The cart is empty" }
        db.withTransaction {
            val now = System.currentTimeMillis()
            val subtotal = lines.sumOf { it.lineTotal }
            val appliedDiscount = discount.coerceIn(0.0, subtotal)
            lines.filter { it.partId > 0L }.forEach { line ->
                val updated = store.deductStockSafe(line.partId, line.quantity, now)
                if (updated == 0) throw NotEnoughStockException(line.name)
            }
            val saleId = store.insertSale(
                SaleEntity(
                    customerName = customerName.trim(),
                    subtotal = subtotal,
                    discount = appliedDiscount,
                    total = subtotal - appliedDiscount,
                    costTotal = lines.sumOf { it.unitCost * it.quantity },
                    paymentMode = paymentMode,
                    createdAt = now
                )
            )
            store.insertSaleItems(
                lines.map {
                    SaleItemEntity(
                        saleId = saleId,
                        partId = it.partId,
                        name = it.name,
                        quantity = it.quantity,
                        unitPrice = it.unitPrice,
                        unitCost = it.unitCost
                    )
                }
            )
            saleId
        }
    }

    /** Cancels a sale and returns its items to stock. */
    suspend fun voidSale(saleId: Long) = withContext(Dispatchers.IO) {
        db.withTransaction {
            val now = System.currentTimeMillis()
            store.getSaleItems(saleId).filter { it.partId > 0L }.forEach {
                store.addStock(it.partId, it.quantity, now)
            }
            store.deleteSaleItems(saleId)
            store.deleteSale(saleId)
        }
    }

    // --- Cash register ---
    suspend fun addMovement(type: String, amount: Double, note: String) = withContext(Dispatchers.IO) {
        store.insertMovement(CashMovementEntity(type = type, amount = amount, note = note.trim()))
    }

    suspend fun deleteMovement(movement: CashMovementEntity) = withContext(Dispatchers.IO) {
        store.deleteMovement(movement)
    }

    // --- Parts used in a repair ---
    /** Takes [quantity] units out of stock and records them on the job card. */
    suspend fun usePartOnJob(jobId: Long, partId: Long, quantity: Int) = withContext(Dispatchers.IO) {
        require(quantity > 0) { "Quantity must be greater than zero" }
        db.withTransaction {
            val job = jobs.getJobByIdSync(jobId) ?: throw IllegalStateException("Job not found")
            val now = System.currentTimeMillis()
            val updated = store.deductStockSafe(partId, quantity, now)
            if (updated == 0) throw NotEnoughStockException(null)
            val newIds = List(quantity) { partId.toString() }
            val merged = (job.linkedPartIdsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() } + newIds)
                .joinToString(",")
            jobs.updateJob(job.copy(linkedPartIdsCsv = merged, updatedAt = now))
        }
    }

    /** Puts one unit of [partId] back into stock and removes it from the job card. */
    suspend fun returnPartFromJob(jobId: Long, partId: Long) = withContext(Dispatchers.IO) {
        db.withTransaction {
            val job = jobs.getJobByIdSync(jobId) ?: return@withTransaction
            val ids = job.linkedPartIdsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
            if (!ids.remove(partId.toString())) return@withTransaction
            val now = System.currentTimeMillis()
            store.addStock(partId, 1, now)
            jobs.updateJob(job.copy(linkedPartIdsCsv = ids.joinToString(","), updatedAt = now))
        }
    }
}

/** Thrown when a stock deduction fails; the UI turns it into a translated message. */
class NotEnoughStockException(val itemName: String?) : IllegalStateException("Not enough stock")
