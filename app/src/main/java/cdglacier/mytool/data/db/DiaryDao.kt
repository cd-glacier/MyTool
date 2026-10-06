package cdglacier.mytool.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface DiaryDao {

    @Query("SELECT * FROM diaries ORDER BY date DESC, position ASC")
    fun observeAll(): Flow<List<DiaryEntity>>

    @Query("SELECT * FROM diaries WHERE date = :date ORDER BY position ASC")
    suspend fun getByDate(date: String): List<DiaryEntity>

    @Query("DELETE FROM diaries WHERE date = :date")
    suspend fun deleteByDate(date: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<DiaryEntity>)

    @Transaction
    suspend fun replaceForDate(date: String, items: List<DiaryEntity>) {
        deleteByDate(date)
        if (items.isNotEmpty()) insertAll(items)
    }
}
