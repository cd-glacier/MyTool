package cdglacier.mytool.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [LocationRecordEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class MyToolDatabase : RoomDatabase() {
    abstract fun locationRecordDao(): LocationRecordDao
}
