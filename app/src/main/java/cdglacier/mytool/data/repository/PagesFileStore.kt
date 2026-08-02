package cdglacier.mytool.data.repository

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PagesFileStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val obsidianRepository: ObsidianRepository,
) {
    fun observeChanges(): Flow<Unit> = flow {
        obsidianRepository.pagesDirUri.collect { emit(Unit) }
    }.flowOn(Dispatchers.IO)

    suspend fun readText(filename: String): String? = withContext(Dispatchers.IO) {
        val file = resolveFile(filename, create = false) ?: return@withContext null
        context.contentResolver.openInputStream(file.uri)
            ?.use { it.bufferedReader().readText() }
    }

    suspend fun writeText(filename: String, text: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val file = resolveFile(filename, create = true)
                ?: error("$filename を作成できません。PAGES_DIR を設定してください。")
            context.contentResolver.openOutputStream(file.uri, "wt")
                ?.use { it.write(text.toByteArray()) }
                ?: error("$filename に書き込めません")
        }
    }

    private suspend fun resolveFile(filename: String, create: Boolean): DocumentFile? {
        val pagesDirUri = obsidianRepository.pagesDirUri.first() ?: return null
        val pagesDir = DocumentFile.fromTreeUri(context, pagesDirUri) ?: return null
        return pagesDir.findFile(filename)
            ?: (if (create) pagesDir.createFile("text/markdown", filename) else null)
    }
}
