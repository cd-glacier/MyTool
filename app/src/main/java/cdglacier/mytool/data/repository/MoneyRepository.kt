package cdglacier.mytool.data.repository

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import cdglacier.mytool.domain.model.MoneyBook
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface MoneyRepository {
    /** vault と pages_dir の設定変化に応じて再ロードされる MoneyBook の Flow */
    fun observeBook(): Flow<MoneyBook>
    suspend fun load(): MoneyBook
    suspend fun save(book: MoneyBook): Result<Unit>
}

@Singleton
class MoneyRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val obsidianRepository: ObsidianRepository,
) : MoneyRepository {

    companion object {
        const val MONEY_FILENAME = "money.md"
    }

    override fun observeBook(): Flow<MoneyBook> = flow {
        combine(obsidianRepository.vaultUri, obsidianRepository.pagesDir) { v, d -> v to d }
            .collect { emit(load()) }
    }.flowOn(Dispatchers.IO)

    override suspend fun load(): MoneyBook = withContext(Dispatchers.IO) {
        val file = resolveFile(create = false) ?: return@withContext MoneyBook()
        val text = context.contentResolver.openInputStream(file.uri)
            ?.use { it.bufferedReader().readText() }
            ?: return@withContext MoneyBook()
        MoneyMarkdown.parse(text)
    }

    override suspend fun save(book: MoneyBook): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val file = resolveFile(create = true) ?: error("money.md を作成できません。Vault と pages_dir を設定してください。")
            val text = MoneyMarkdown.serialize(book)
            context.contentResolver.openOutputStream(file.uri, "wt")
                ?.use { it.write(text.toByteArray()) }
                ?: error("money.md に書き込めません")
        }
    }

    private suspend fun resolveFile(create: Boolean): DocumentFile? {
        val vaultUri = obsidianRepository.vaultUri.first() ?: return null
        val pagesDirName = obsidianRepository.pagesDir.first()
        val vault = DocumentFile.fromTreeUri(context, vaultUri) ?: return null
        val pagesDir: DocumentFile = vault.findFile(pagesDirName)
            ?: (if (create) vault.createDirectory(pagesDirName) else null)
            ?: return null
        return pagesDir.findFile(MONEY_FILENAME)
            ?: (if (create) pagesDir.createFile("text/markdown", MONEY_FILENAME) else null)
    }
}
