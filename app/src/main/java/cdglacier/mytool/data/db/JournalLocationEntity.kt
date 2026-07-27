package cdglacier.mytool.data.db

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "journal_locations",
    primaryKeys = ["date", "timestamp"],
    indices = [Index("date"), Index("timestamp")],
)
data class JournalLocationEntity(
    val date: String,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val batteryLevel: Int,
    val sameLocationCount: Int,
)
