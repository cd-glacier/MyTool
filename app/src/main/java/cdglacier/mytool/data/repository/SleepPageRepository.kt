package cdglacier.mytool.data.repository

import cdglacier.mytool.domain.model.SleepBook
import cdglacier.mytool.domain.model.SleepDay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import javax.inject.Inject
import javax.inject.Singleton

interface SleepPageRepository {
    fun observeBook(): Flow<SleepBook>
    suspend fun load(): SleepBook
    suspend fun save(book: SleepBook): Result<Unit>
    suspend fun upsertDay(day: SleepDay): Result<Unit>
}

@Singleton
class SleepPageRepositoryImpl @Inject constructor(
    private val store: PagesFileStore,
) : SleepPageRepository {

    companion object {
        const val SLEEP_FILENAME = "SleepStages.md"
    }

    private val refreshTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override fun observeBook(): Flow<SleepBook> = flow {
        merge(store.observeChanges(), refreshTrigger.asSharedFlow()).collect { emit(load()) }
    }

    override suspend fun load(): SleepBook {
        val text = store.readText(SLEEP_FILENAME) ?: return SleepBook()
        return SleepMarkdown.parse(text)
    }

    override suspend fun save(book: SleepBook): Result<Unit> {
        val result = store.writeText(SLEEP_FILENAME, SleepMarkdown.serialize(book))
        if (result.isSuccess) refreshTrigger.tryEmit(Unit)
        return result
    }

    override suspend fun upsertDay(day: SleepDay): Result<Unit> =
        save(load().with(day))
}
