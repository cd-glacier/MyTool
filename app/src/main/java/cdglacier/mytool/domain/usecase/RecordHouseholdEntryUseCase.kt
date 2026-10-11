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
        val body = HouseholdJournalParser.buildSectionBody(merged)
        val updated = JournalSectionWriter.write(current, JournalSection.HOUSEHOLD, body)

        journalRepository.writeContent(journalDirUri, date, format, updated).getOrThrow()
    }
}
