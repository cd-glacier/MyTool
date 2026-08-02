package cdglacier.mytool.data.repository

import cdglacier.mytool.domain.model.DailyHealth
import cdglacier.mytool.domain.model.HealthBook
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import javax.inject.Inject
import javax.inject.Singleton

interface HealthPageRepository {
    fun observeBook(): Flow<HealthBook>
    suspend fun load(): HealthBook
    suspend fun save(book: HealthBook): Result<Unit>
    suspend fun upsertDay(day: DailyHealth): Result<Unit>
}

@Singleton
class HealthPageRepositoryImpl @Inject constructor(
    private val store: PagesFileStore,
) : HealthPageRepository {

    companion object {
        const val HEALTH_FILENAME = "Health.md"
    }

    private val refreshTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override fun observeBook(): Flow<HealthBook> = flow {
        merge(store.observeChanges(), refreshTrigger.asSharedFlow()).collect { emit(load()) }
    }

    override suspend fun load(): HealthBook {
        val text = store.readText(HEALTH_FILENAME) ?: return HealthBook()
        return HealthMarkdown.parse(text)
    }

    override suspend fun save(book: HealthBook): Result<Unit> {
        val result = store.writeText(HEALTH_FILENAME, HealthMarkdown.serialize(book))
        if (result.isSuccess) refreshTrigger.tryEmit(Unit)
        return result
    }

    override suspend fun upsertDay(day: DailyHealth): Result<Unit> =
        save(load().with(day))
}
