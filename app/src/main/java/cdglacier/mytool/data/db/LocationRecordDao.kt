package cdglacier.mytool.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationRecordDao {

    @Query("SELECT * FROM location_records WHERE timestamp BETWEEN :fromMillis AND :toMillis ORDER BY timestamp ASC")
    fun observeBetween(fromMillis: Long, toMillis: Long): Flow<List<LocationRecordEntity>>

    @Query("SELECT * FROM location_records ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(): LocationRecordEntity?

    @Query("SELECT * FROM location_records WHERE timestamp BETWEEN :fromMillis AND :toMillis ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestBetween(fromMillis: Long, toMillis: Long): LocationRecordEntity?

    @Insert
    suspend fun insert(record: LocationRecordEntity): Long

    @Update
    suspend fun update(record: LocationRecordEntity)
}
