package cdglacier.mytool.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DailySummaryDao {

    @Query("SELECT * FROM daily_summaries")
    fun observeAll(): Flow<List<DailySummaryEntity>>

    @Query("SELECT date FROM daily_summaries WHERE date IN (:dates)")
    suspend fun getExistingDates(dates: List<String>): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<DailySummaryEntity>)
}
