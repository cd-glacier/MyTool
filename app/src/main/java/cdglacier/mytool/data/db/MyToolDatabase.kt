package cdglacier.mytool.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [LocationRecordEntity::class, RecipeEntity::class, RecipeEmbeddingEntity::class],
    version = 4,
    exportSchema = false,
)
abstract class MyToolDatabase : RoomDatabase() {
    abstract fun locationRecordDao(): LocationRecordDao
    abstract fun recipeDao(): RecipeDao
    abstract fun recipeEmbeddingDao(): RecipeEmbeddingDao
}
