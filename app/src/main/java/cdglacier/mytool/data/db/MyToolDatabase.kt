package cdglacier.mytool.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        LocationRecordEntity::class,
        RecipeEntity::class,
        DailySummaryEntity::class,
        JournalLocationEntity::class,
    ],
    version = 5,
    exportSchema = false,
)
abstract class MyToolDatabase : RoomDatabase() {
    abstract fun locationRecordDao(): LocationRecordDao
    abstract fun recipeDao(): RecipeDao
    abstract fun dailySummaryDao(): DailySummaryDao
    abstract fun journalLocationDao(): JournalLocationDao
}
