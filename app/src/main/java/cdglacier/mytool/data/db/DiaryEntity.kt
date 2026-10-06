package cdglacier.mytool.data.db

import androidx.room.Entity

@Entity(tableName = "diaries", primaryKeys = ["date", "position"])
data class DiaryEntity(
    val date: String,
    val position: Int,
    val timestamp: String,
    val content: String,
)
