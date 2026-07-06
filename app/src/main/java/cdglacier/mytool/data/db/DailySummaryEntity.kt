package cdglacier.mytool.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_summaries")
data class DailySummaryEntity(
    @PrimaryKey val date: String,
    val habitRate: Float?,
    val distanceMeters: Double,
    val householdHusbandPoints: Int,
)
