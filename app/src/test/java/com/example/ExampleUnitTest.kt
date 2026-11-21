package com.example

import com.example.data.database.Converters
import com.example.data.entity.JobCardEntity
import com.example.data.model.ConditionStatus
import com.example.data.model.IntakeChecklist
import com.example.data.model.RepairStage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testJobCardBalanceCalculation() {
        val job = JobCardEntity(
            jobNumber = "JOB-1001",
            customerName = "Marcus Vance",
            customerPhone = "+1 555-829-1029",
            deviceBrand = "Apple",
            deviceModel = "iPhone 13 Pro",
            issueDescription = "Broken screen",
            estimatedCost = 150.0,
            advancePaid = 50.0,
            discount = 10.0,
            finalCost = 140.0,
            isPaid = false
        )
        // Net due: finalCost (140) - discount (10) - advance (50) = 80.0
        assertEquals(80.0, job.balanceDue, 0.001)

        val paidJob = job.copy(isPaid = true)
        assertEquals(0.0, paidJob.balanceDue, 0.001)
    }

    @Test
    fun testIntakeChecklistSerialization() {
        val checklist = IntakeChecklist(
            screenTouch = ConditionStatus.DEFECTIVE,
            displayGlass = ConditionStatus.DEFECTIVE,
            frontCamera = ConditionStatus.WORKING,
            chargingPort = ConditionStatus.WORKING
        )

        val serialized = Converters.serializeChecklist(checklist)
        val deserialized = Converters.deserializeChecklist(serialized)

        assertEquals(ConditionStatus.DEFECTIVE, deserialized.screenTouch)
        assertEquals(ConditionStatus.DEFECTIVE, deserialized.displayGlass)
        assertEquals(ConditionStatus.WORKING, deserialized.frontCamera)
        assertEquals(ConditionStatus.WORKING, deserialized.chargingPort)
        assertEquals(ConditionStatus.UNTESTED, deserialized.housingCondition)
    }
}
