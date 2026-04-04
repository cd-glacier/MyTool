package cdglacier.mytool.domain.usecase

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
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
    ): Map<LocalDate, Int> {
        val formatter = DateTimeFormatter.ofPattern(filenameFormat)
        val dir = DocumentFile.fromTreeUri(context, journalDirUri) ?: return emptyMap()
        return dates.associateWith { date ->
            val filename = "${date.format(formatter)}.md"
            val file = dir.findFile(filename)
            if (file == null) {
                0
            } else {
                context.contentResolver.openInputStream(file.uri)
                    ?.bufferedReader()
                    ?.use { reader -> reader.lines().count().toInt() }
                    ?: 0
            }
        }
    }
}
