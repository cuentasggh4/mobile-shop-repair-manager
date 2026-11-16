package com.example.data.model

import com.example.data.entity.JobCardEntity

data class CustomerKhataSummary(
    val customerKey: String,
    val customerName: String,
    val customerPhone: String,
    val totalJobsCount: Int,
    val totalBilled: Double,
    val totalPaid: Double,
    val totalPendingDue: Double,
    val unpaidJobsCount: Int,
    val lastVisitAt: Long,
    val jobs: List<JobCardEntity>
)

fun normalizeKhataKey(name: String, phone: String): String {
    val digits = phone.filter { it.isDigit() }
    return if (digits.length >= 7) {
        "P:${digits.takeLast(10)}"
    } else {
        "N:${name.trim().lowercase()}"
    }
}

fun buildCustomerKhataList(jobs: List<JobCardEntity>): List<CustomerKhataSummary> {
    if (jobs.isEmpty()) return emptyList()
    return jobs
        .groupBy { normalizeKhataKey(it.customerName, it.customerPhone) }
        .map { (key, customerJobs) ->
            val sortedJobs = customerJobs.sortedByDescending { it.createdAt }
            val latest = sortedJobs.first()
            val validJobs = sortedJobs.filter { it.stage != RepairStage.CANCELLED }

            var totalBilled = 0.0
            var totalDue = 0.0
            var unpaidCount = 0

            for (job in validJobs) {
                val effectiveTotal = if (job.finalCost > 0.0) job.finalCost else job.estimatedCost
                val netBill = (effectiveTotal - job.discount).coerceAtLeast(0.0)
                totalBilled += netBill
                val due = job.balanceDue
                if (!job.isPaid && due > 0.01) {
                    totalDue += due
                    unpaidCount++
                }
            }
            val totalPaid = (totalBilled - totalDue).coerceAtLeast(0.0)

            CustomerKhataSummary(
                customerKey = key,
                customerName = latest.customerName.ifBlank { "Customer" },
                customerPhone = sortedJobs.firstNotNullOfOrNull {
                    it.customerPhone.trim().takeIf { p -> p.isNotEmpty() }
                } ?: "",
                totalJobsCount = sortedJobs.size,
                totalBilled = totalBilled,
                totalPaid = totalPaid,
                totalPendingDue = totalDue,
                unpaidJobsCount = unpaidCount,
                lastVisitAt = latest.createdAt,
                jobs = sortedJobs
            )
        }
        .sortedWith(
            compareByDescending<CustomerKhataSummary> { it.totalPendingDue > 0.01 }
                .thenByDescending { it.totalPendingDue }
                .thenByDescending { it.lastVisitAt }
        )
}
