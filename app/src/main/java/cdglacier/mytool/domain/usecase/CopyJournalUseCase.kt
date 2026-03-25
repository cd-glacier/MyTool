package cdglacier.mytool.domain.usecase

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class CopyJournalUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend operator fun invoke(
        journalDirUri: Uri,
        sourceDate: LocalDate,
        targetDate: LocalDate,
        filenameFormat: String,
    ): Result<Unit> = runCatching {
        val formatter = DateTimeFormatter.ofPattern(filenameFormat)
        val srcName = "${sourceDate.format(formatter)}.md"
        val dstName = "${targetDate.format(formatter)}.md"

        val dir = DocumentFile.fromTreeUri(context, journalDirUri)
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
