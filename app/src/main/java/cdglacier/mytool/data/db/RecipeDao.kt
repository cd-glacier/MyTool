package cdglacier.mytool.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {

    @Query("SELECT * FROM recipes ORDER BY date DESC, position ASC")
    fun observeAll(): Flow<List<RecipeEntity>>

    @Query("SELECT * FROM recipes WHERE date = :date ORDER BY position ASC")
    suspend fun getByDate(date: String): List<RecipeEntity>

    @Query("DELETE FROM recipes WHERE date = :date")
    suspend fun deleteByDate(date: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<RecipeEntity>)

    @Transaction
    suspend fun replaceForDate(date: String, items: List<RecipeEntity>) {
        deleteByDate(date)
        if (items.isNotEmpty()) insertAll(items)
    }

}
