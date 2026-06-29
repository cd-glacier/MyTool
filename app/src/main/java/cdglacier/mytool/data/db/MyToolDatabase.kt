package cdglacier.mytool.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [LocationRecordEntity::class, RecipeEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class MyToolDatabase : RoomDatabase() {
    abstract fun locationRecordDao(): LocationRecordDao
    abstract fun recipeDao(): RecipeDao
}
