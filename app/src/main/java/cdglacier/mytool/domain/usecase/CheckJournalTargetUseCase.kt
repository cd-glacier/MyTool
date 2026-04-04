package cdglacier.mytool.domain.usecase

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class CheckJournalTargetUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend operator fun invoke(
        journalDirUri: Uri,
        targetDate: LocalDate,
        filenameFormat: String,
    ): Boolean {
        val formatter = DateTimeFormatter.ofPattern(filenameFormat)
        val dstName = "${targetDate.format(formatter)}.md"
        val dir = DocumentFile.fromTreeUri(context, journalDirUri) ?: return false
        val dstFile = dir.findFile(dstName) ?: return false
        val content = context.contentResolver.openInputStream(dstFile.uri)
            ?.use { it.bufferedReader().readText() }
            ?: return false
        return content.isNotBlank()
    }
}
