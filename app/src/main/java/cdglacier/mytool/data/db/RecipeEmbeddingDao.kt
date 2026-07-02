package cdglacier.mytool.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RecipeEmbeddingDao {

    @Query("SELECT * FROM recipe_embeddings WHERE model = :model")
    suspend fun getAllForModel(model: String): List<RecipeEmbeddingEntity>

    @Query("SELECT url FROM recipe_embeddings WHERE model = :model")
    suspend fun getUrlsForModel(model: String): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: RecipeEmbeddingEntity)

    @Query("DELETE FROM recipe_embeddings WHERE model != :model")
    suspend fun deleteOtherModels(model: String)
}
