package cdglacier.mytool.data.repository

import cdglacier.mytool.data.db.LocationRecordDao
import cdglacier.mytool.data.db.LocationRecordEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

interface LocationRecordRepository {
    fun observeByDate(date: LocalDate): Flow<List<LocationRecordEntity>>
    suspend fun getLatest(): LocationRecordEntity?
    suspend fun insert(record: LocationRecordEntity): Long
    suspend fun update(record: LocationRecordEntity)
}

@Singleton
class LocationRecordRepositoryImpl @Inject constructor(
    private val dao: LocationRecordDao,
) : LocationRecordRepository {

    override fun observeByDate(date: LocalDate): Flow<List<LocationRecordEntity>> {
        val zone = ZoneId.systemDefault()
        val from = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val to = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return dao.observeBetween(from, to)
    }

    override suspend fun getLatest(): LocationRecordEntity? = dao.getLatest()
    override suspend fun insert(record: LocationRecordEntity): Long = dao.insert(record)
    override suspend fun update(record: LocationRecordEntity) = dao.update(record)
}
