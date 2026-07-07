package cdglacier.mytool.domain.usecase

import cdglacier.mytool.data.repository.JournalRepository
import cdglacier.mytool.data.repository.ObsidianRepository
import cdglacier.mytool.domain.model.HouseholdEntry
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

class RecordHouseholdEntryUseCase @Inject constructor(
    private val journalRepository: JournalRepository,
    private val obsidianRepository: ObsidianRepository,
) {
    suspend operator fun invoke(date: LocalDate, newEntry: HouseholdEntry): Result<Unit> = runCatching {
        val journalDirUri = obsidianRepository.journalDirUri.first()?.toString()
            ?: error("Journal フォルダが未設定です")
        val format = obsidianRepository.filenameFormat.first()

        val current = journalRepository.readContent(journalDirUri, date, format).orEmpty()
        val existing = HouseholdJournalParser.parse(current)
        val merged = existing + newEntry
        val section = HouseholdJournalParser.buildSection(merged)
        val updated = replaceOrAppendSection(current, section)

        journalRepository.writeContent(journalDirUri, date, format, updated).getOrThrow()
    }

    private fun replaceOrAppendSection(content: String, section: String): String {
        val lines = content.lines()
        val startIdx = lines.indexOfFirst { it.trimEnd() == HouseholdJournalParser.SECTION_HEADING }
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
}
