package cdglacier.mytool.domain.usecase

import cdglacier.mytool.data.repository.JournalRepository
import java.time.Duration
import java.time.LocalDate
import javax.inject.Inject

class WriteHealthSectionToJournalUseCase @Inject constructor(
    private val journalRepository: JournalRepository,
) {
    suspend operator fun invoke(
        journalDirUri: String,
        date: LocalDate,
        filenameFormat: String,
        health: HealthData,
    ): Result<Unit> = runCatching {
        if (!health.hasAny) error("挿入できる Health データがありません")
        val section = buildSection(health)
        val current = journalRepository.readContent(journalDirUri, date, filenameFormat).orEmpty()
        val updated = replaceOrAppendSection(current, section)
        journalRepository.writeContent(journalDirUri, date, filenameFormat, updated).getOrThrow()
    }

    private fun buildSection(health: HealthData): String = buildString {
        appendLine(SECTION_HEADING)
        appendLine()
        health.sleep?.let { appendLine("- 睡眠: ${formatSleep(it)}") }
        health.steps?.takeIf { it > 0 }?.let { appendLine("- 歩数: $it steps") }
    }

    private fun formatSleep(duration: Duration): String {
        val hours = duration.toHours()
        val minutes = duration.toMinutes() % 60
        return "${hours}h${minutes}m"
    }

    private fun replaceOrAppendSection(content: String, section: String): String {
        val lines = content.lines()
        val startIdx = lines.indexOfFirst { it.trimEnd() == SECTION_HEADING }
        if (startIdx < 0) {
            val sep = if (content.isEmpty() || content.endsWith("\n\n")) "" else if (content.endsWith("\n")) "\n" else "\n\n"
            return content + sep + section
        }
        val endIdx = (startIdx + 1 until lines.size)
            .firstOrNull { lines[it].startsWith("# ") }
            ?: lines.size
        val before = lines.subList(0, startIdx).joinToString("\n")
        val after = lines.subList(endIdx, lines.size).joinToString("\n")
        return buildString {
            if (before.isNotEmpty()) {
                append(before)
                if (!before.endsWith("\n")) append("\n")
            }
            append(section)
            if (after.isNotEmpty()) {
                if (!section.endsWith("\n")) append("\n")
                append(after)
            }
        }
    }

    companion object {
        private const val SECTION_HEADING = "# Health"
    }
}
