package cdglacier.mytool.data.repository

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

interface JournalRepository {
    suspend fun exists(journalDirUri: String, date: LocalDate, filenameFormat: String): Boolean
    suspend fun readContent(journalDirUri: String, date: LocalDate, filenameFormat: String): String?
    suspend fun writeContent(
        journalDirUri: String,
        date: LocalDate,
        filenameFormat: String,
        content: String,
    ): Result<Unit>
    suspend fun copy(
        journalDirUri: String,
        sourceDate: LocalDate,
        targetDate: LocalDate,
        filenameFormat: String,
    ): Result<Unit>

    suspend fun getLineCounts(
        journalDirUri: String,
        dates: List<LocalDate>,
        filenameFormat: String,
    ): Map<LocalDate, Int>
}

@Singleton
class JournalRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : JournalRepository {

    private fun filenameOf(date: LocalDate, format: String): String =
        "${date.format(DateTimeFormatter.ofPattern(format))}.md"

    override suspend fun exists(
        journalDirUri: String,
        date: LocalDate,
        filenameFormat: String,
    ): Boolean = withContext(Dispatchers.IO) {
        val dir = DocumentFile.fromTreeUri(context, Uri.parse(journalDirUri)) ?: return@withContext false
        val file = dir.findFile(filenameOf(date, filenameFormat)) ?: return@withContext false
        val content = context.contentResolver.openInputStream(file.uri)
            ?.use { it.bufferedReader().readText() }
            ?: return@withContext false
        content.isNotBlank()
    }

    override suspend fun readContent(
        journalDirUri: String,
        date: LocalDate,
        filenameFormat: String,
    ): String? = withContext(Dispatchers.IO) {
        val dir = DocumentFile.fromTreeUri(context, Uri.parse(journalDirUri)) ?: return@withContext null
        val file = dir.findFile(filenameOf(date, filenameFormat)) ?: return@withContext null
        context.contentResolver.openInputStream(file.uri)
            ?.use { it.bufferedReader().readText() }
    }

    override suspend fun writeContent(
        journalDirUri: String,
        date: LocalDate,
        filenameFormat: String,
        content: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val dir = DocumentFile.fromTreeUri(context, Uri.parse(journalDirUri))
                ?: error("フォルダを開けません")
            val name = filenameOf(date, filenameFormat)
            val file = dir.findFile(name)
                ?: dir.createFile("text/markdown", name)
                ?: error("ファイルを作成できません: $name")
            context.contentResolver.openOutputStream(file.uri, "wt")
                ?.use { it.write(content.toByteArray()) }
                ?: error("書き込めません")
        }
    }

    override suspend fun copy(
        journalDirUri: String,
        sourceDate: LocalDate,
        targetDate: LocalDate,
        filenameFormat: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val srcName = filenameOf(sourceDate, filenameFormat)
            val dstName = filenameOf(targetDate, filenameFormat)

            val dir = DocumentFile.fromTreeUri(context, Uri.parse(journalDirUri))
                ?: error("フォルダを開けません")

            val srcFile = dir.findFile(srcName)
                ?: error("コピー元ファイルが見つかりません: $srcName")

            val srcBytes = context.contentResolver.openInputStream(srcFile.uri)
                ?.use { it.readBytes() }
                ?: error("コピー元ファイルを読み込めません")

            val dstFile = dir.findFile(dstName)
                ?: dir.createFile("text/markdown", dstName)
                ?: error("コピー先ファイルを作成できません")

            context.contentResolver.openOutputStream(dstFile.uri, "wt")
                ?.use { it.write(srcBytes) }
                ?: error("コピー先ファイルに書き込めません")
        }
    }

    override suspend fun getLineCounts(
        journalDirUri: String,
        dates: List<LocalDate>,
        filenameFormat: String,
    ): Map<LocalDate, Int> = withContext(Dispatchers.IO) {
        val formatter = DateTimeFormatter.ofPattern(filenameFormat)
        val dir = DocumentFile.fromTreeUri(context, Uri.parse(journalDirUri)) ?: return@withContext emptyMap()

        val filesByName = dir.listFiles().associateBy { it.name }

        dates.associateWith { date ->
            val filename = "${date.format(formatter)}.md"
            val file = filesByName[filename] ?: return@associateWith 0
            runCatching {
                context.contentResolver.openInputStream(file.uri)
                    ?.bufferedReader()
                    ?.use { reader -> reader.lines().count().toInt() }
                    ?: 0
            }.getOrDefault(0)
        }
    }
}
