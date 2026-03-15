package cdglacier.mytool.widget

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object JournalReader {
    fun readTodayJournal(
        context: Context,
        journalDirUri: Uri,
        filenameFormat: String,
        date: LocalDate = LocalDate.now()
    ): String? {
        val formatter = DateTimeFormatter.ofPattern(filenameFormat)
        val filename = "${date.format(formatter)}.md"
        return DocumentFile.fromTreeUri(context, journalDirUri)
            ?.findFile(filename)
            ?.let { context.contentResolver.openInputStream(it.uri)?.bufferedReader()?.use { r -> r.readText() } }
    }
}
