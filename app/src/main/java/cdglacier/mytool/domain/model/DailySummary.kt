package cdglacier.mytool.domain.model

import java.time.LocalDate

data class DailySummary(
    val date: LocalDate,
    val habitRate: Float?,
    val distanceMeters: Double,
    val householdHusbandPoints: Int,
)
