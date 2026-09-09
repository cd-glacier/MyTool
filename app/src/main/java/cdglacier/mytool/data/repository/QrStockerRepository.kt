package cdglacier.mytool.data.repository

import cdglacier.mytool.domain.model.QrEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import javax.inject.Inject
import javax.inject.Singleton

interface QrStockerRepository {
    fun observeEntries(): Flow<List<QrEntry>>
    suspend fun load(): List<QrEntry>
    suspend fun upsert(entry: QrEntry): Result<Unit>
}

@Singleton
class QrStockerRepositoryImpl @Inject constructor(
    private val store: PagesFileStore,
) : QrStockerRepository {

    companion object {
        const val QR_FILENAME = "QR.md"
    }

    private val refreshTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override fun observeEntries(): Flow<List<QrEntry>> = flow {
        merge(store.observeChanges(), refreshTrigger.asSharedFlow()).collect { emit(load()) }
    }

    override suspend fun load(): List<QrEntry> {
        val text = store.readText(QR_FILENAME) ?: return emptyList()
        return QrMarkdown.parse(text)
    }

    override suspend fun upsert(entry: QrEntry): Result<Unit> {
        val current = load().filterNot { it.title == entry.title }
        val next = current + entry
        val result = store.writeText(QR_FILENAME, QrMarkdown.serialize(next))
        if (result.isSuccess) refreshTrigger.tryEmit(Unit)
        return result
    }
}
