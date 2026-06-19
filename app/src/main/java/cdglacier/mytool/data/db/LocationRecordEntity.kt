package cdglacier.mytool.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "location_records",
    indices = [Index("timestamp")],
)
data class LocationRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val batteryLevel: Int,
    val sameLocationCount: Int,
)
