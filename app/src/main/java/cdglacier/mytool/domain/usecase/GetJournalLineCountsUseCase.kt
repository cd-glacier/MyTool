package cdglacier.mytool.domain.usecase

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class GetJournalLineCountsUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend operator fun invoke(
        journalDirUri: Uri,
        filenameFormat: String,
        dates: List<LocalDate>,
    ): Map<LocalDate, Int> = withContext(Dispatchers.IO) {
        val formatter = DateTimeFormatter.ofPattern(filenameFormat)
        val dir = DocumentFile.fromTreeUri(context, journalDirUri) ?: return@withContext emptyMap()

        // findFile() はディレクトリ全体を毎回スキャンするため、1回だけ listFiles() してキャッシュする
        val filesByName = dir.listFiles().associateBy { it.name }

        dates.associateWith { date ->
            val filename = "${date.format(formatter)}.md"
            val file = filesByName[filename]
            if (file == null) {
                0
            } else {
                runCatching {
                    context.contentResolver.openInputStream(file.uri)
                        ?.bufferedReader()
                        ?.use { reader -> reader.lines().count().toInt() }
                        ?: 0
                }.getOrDefault(0)
            }
        }
    }
}
