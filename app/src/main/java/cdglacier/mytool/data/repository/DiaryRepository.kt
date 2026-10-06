package cdglacier.mytool.data.repository

import cdglacier.mytool.data.db.DiaryDao
import cdglacier.mytool.data.db.DiaryEntity
import cdglacier.mytool.domain.model.Diary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

interface DiaryRepository {
    val sections: Flow<List<DiarySection>>
    suspend fun getByDate(date: LocalDate): List<DiaryEntity>
    suspend fun replaceForDate(date: LocalDate, diaries: List<Diary>)
}

data class DiarySection(
    val date: LocalDate,
    val items: List<DiaryItem>,
)

data class DiaryItem(
    val timestamp: String,
    val content: String,
)

@Singleton
class DiaryRepositoryImpl @Inject constructor(
    private val dao: DiaryDao,
) : DiaryRepository {

    override val sections: Flow<List<DiarySection>> =
        dao.observeAll().map { entities ->
            entities.groupBy { it.date }
                .mapNotNull { (dateStr, items) ->
                    val date = runCatching { LocalDate.parse(dateStr) }.getOrNull() ?: return@mapNotNull null
                    DiarySection(
                        date = date,
                        items = items.sortedBy { it.position }
                            .map { DiaryItem(timestamp = it.timestamp, content = it.content) },
                    )
                }
                .sortedByDescending { it.date }
        }

    override suspend fun getByDate(date: LocalDate): List<DiaryEntity> =
        dao.getByDate(date.toString())

    override suspend fun replaceForDate(date: LocalDate, diaries: List<Diary>) {
        val entities = diaries.mapIndexed { index, diary ->
            DiaryEntity(
                date = date.toString(),
                position = index,
                timestamp = diary.timestamp,
                content = diary.content,
            )
        }
        dao.replaceForDate(date.toString(), entities)
    }
}
