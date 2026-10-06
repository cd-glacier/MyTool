package cdglacier.mytool.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        LocationRecordEntity::class,
        RecipeEntity::class,
        DailySummaryEntity::class,
        JournalLocationEntity::class,
        DiaryEntity::class,
    ],
    version = 6,
    exportSchema = false,
)
abstract class MyToolDatabase : RoomDatabase() {
    abstract fun locationRecordDao(): LocationRecordDao
    abstract fun recipeDao(): RecipeDao
    abstract fun dailySummaryDao(): DailySummaryDao
    abstract fun journalLocationDao(): JournalLocationDao
    abstract fun diaryDao(): DiaryDao
}
