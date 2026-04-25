package com.wellpaid.ui.goals

import com.wellpaid.core.model.goal.GoalUpdateDto
import org.junit.Assert.assertNull
import org.junit.Test

class GoalUpdateDtoTest {
    @Test
    fun `allows null current cents for edit flow`() {
        val dto = GoalUpdateDto(
            title = "Meta",
            targetCents = 10000,
            currentCents = null,
            isActive = true,
        )

        assertNull(dto.currentCents)
    }
}
