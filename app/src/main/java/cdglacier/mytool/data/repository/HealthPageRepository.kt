package cdglacier.mytool.data.repository

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import cdglacier.mytool.domain.model.DailyHealth
import cdglacier.mytool.domain.model.HealthBook
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
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
    @ApplicationContext private val context: Context,
    private val obsidianRepository: ObsidianRepository,
) : HealthPageRepository {

    companion object {
        const val HEALTH_FILENAME = "Health.md"
    }

    override fun observeBook(): Flow<HealthBook> = flow {
        obsidianRepository.pagesDirUri.collect { emit(load()) }
    }.flowOn(Dispatchers.IO)

    override suspend fun load(): HealthBook = withContext(Dispatchers.IO) {
        val file = resolveFile(create = false) ?: return@withContext HealthBook()
        val text = context.contentResolver.openInputStream(file.uri)
            ?.use { it.bufferedReader().readText() }
            ?: return@withContext HealthBook()
        HealthMarkdown.parse(text)
    }

    override suspend fun save(book: HealthBook): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val file = resolveFile(create = true)
                ?: error("Health.md を作成できません。PAGES_DIR を設定してください。")
            val text = HealthMarkdown.serialize(book)
            context.contentResolver.openOutputStream(file.uri, "wt")
                ?.use { it.write(text.toByteArray()) }
                ?: error("Health.md に書き込めません")
        }
    }

    override suspend fun upsertDay(day: DailyHealth): Result<Unit> {
        val current = load()
        return save(current.with(day))
    }

    private suspend fun resolveFile(create: Boolean): DocumentFile? {
        val pagesDirUri = obsidianRepository.pagesDirUri.first() ?: return null
        val pagesDir = DocumentFile.fromTreeUri(context, pagesDirUri) ?: return null
        return pagesDir.findFile(HEALTH_FILENAME)
            ?: (if (create) pagesDir.createFile("text/markdown", HEALTH_FILENAME) else null)
    }
}
