package cdglacier.mytool

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object JournalAppender {
    suspend fun append(
        context: Context,
        journalDirUri: Uri,
        filenameFormat: String,
        title: String,
        datetime: String,
        content: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val formatter = DateTimeFormatter.ofPattern(filenameFormat)
            val filename = "${LocalDate.now().format(formatter)}.md"

            val dir = DocumentFile.fromTreeUri(context, journalDirUri)
                ?: error("フォルダを開けません")

            val file = dir.findFile(filename)
                ?: dir.createFile("text/markdown", filename)
                ?: error("ファイルを作成できません: $filename")

            val block = "\n# $title [[Bookmark]]\n$datetime\n$content\n"

            context.contentResolver.openOutputStream(file.uri, "wa")
                ?.use { it.write(block.toByteArray()) }
                ?: error("ファイルに書き込めません")
        }
    }
}
