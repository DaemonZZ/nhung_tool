package com.nhungtool.reconcore.ui

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WorkspaceAnalysisServiceTest {

    @Test
    fun `khong bao am kho khi mua xay ra truoc cac ngay ban`() {
        val events = WorkspaceAnalysisService.calculateNegativeInventoryTimeline(
            openingQty = 0.0,
            purchaseByDate = mapOf(
                LocalDate.of(2025, 3, 28) to 50.0,
                LocalDate.of(2025, 4, 23) to 26.0,
            ),
            purchaseAmtByDate = mapOf(
                LocalDate.of(2025, 3, 28) to 727_703.0,
                LocalDate.of(2025, 4, 23) to 378_405.0,
            ),
            salesQtyByDate = mapOf(
                LocalDate.of(2025, 4, 29) to 5.0,
                LocalDate.of(2025, 5, 30) to 50.0,
            ),
            salesAmtByDate = mapOf(
                LocalDate.of(2025, 4, 29) to 125_000.0,
                LocalDate.of(2025, 5, 30) to 1_000_000.0,
            ),
        )

        assertTrue(events.isEmpty())
    }

    @Test
    fun `bao dung ngay am kho khi ban xay ra truoc ngay mua bo sung`() {
        val events = WorkspaceAnalysisService.calculateNegativeInventoryTimeline(
            openingQty = 0.0,
            purchaseByDate = mapOf(LocalDate.of(2025, 4, 23) to 20.0),
            purchaseAmtByDate = emptyMap(),
            salesQtyByDate = mapOf(LocalDate.of(2025, 4, 20) to 12.0),
            salesAmtByDate = mapOf(LocalDate.of(2025, 4, 20) to 240_000.0),
        )

        assertEquals(1, events.size)
        assertEquals(LocalDate.of(2025, 4, 20), events.first().date)
        assertEquals(-12.0, events.first().runningQty)
    }
}
