package cdglacier.mytool.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalLocationDao {

    @Query("SELECT * FROM journal_locations WHERE date = :date ORDER BY timestamp ASC")
    fun observeByDate(date: String): Flow<List<JournalLocationEntity>>

    @Query("SELECT * FROM journal_locations WHERE date = :date ORDER BY timestamp ASC")
    suspend fun getByDate(date: String): List<JournalLocationEntity>

    @Query("DELETE FROM journal_locations WHERE date = :date")
    suspend fun deleteByDate(date: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<JournalLocationEntity>)

    @Transaction
    suspend fun replaceForDate(date: String, items: List<JournalLocationEntity>) {
        deleteByDate(date)
        if (items.isNotEmpty()) insertAll(items)
    }
}
