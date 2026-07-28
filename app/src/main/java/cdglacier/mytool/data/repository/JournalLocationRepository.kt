package cdglacier.mytool.data.repository

import cdglacier.mytool.data.db.JournalLocationDao
import cdglacier.mytool.data.db.JournalLocationEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

interface JournalLocationRepository {
    fun observeByDate(date: LocalDate): Flow<List<JournalLocationEntity>>
    suspend fun getByDate(date: LocalDate): List<JournalLocationEntity>
    suspend fun replaceForDate(date: LocalDate, items: List<JournalLocationEntity>)
}

@Singleton
class JournalLocationRepositoryImpl @Inject constructor(
    private val dao: JournalLocationDao,
) : JournalLocationRepository {

    override fun observeByDate(date: LocalDate): Flow<List<JournalLocationEntity>> =
        dao.observeByDate(date.toString())

    override suspend fun getByDate(date: LocalDate): List<JournalLocationEntity> =
        dao.getByDate(date.toString())

    override suspend fun replaceForDate(date: LocalDate, items: List<JournalLocationEntity>) {
        dao.replaceForDate(date.toString(), items)
    }
}
