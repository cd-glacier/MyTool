package cdglacier.mytool.domain.usecase

import cdglacier.mytool.data.repository.JournalRepository
import cdglacier.mytool.data.repository.ObsidianRepository
import cdglacier.mytool.domain.model.Assignee
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

class GetHouseholdHusbandDailyPointsUseCase @Inject constructor(
    private val journalRepository: JournalRepository,
    private val obsidianRepository: ObsidianRepository,
    private val getHouseholdPointsUseCase: GetHouseholdPointsUseCase,
) {
    suspend operator fun invoke(dateRange: ClosedRange<LocalDate>): Map<LocalDate, Int> {
        val journalDirUri = obsidianRepository.journalDirUri.first()?.toString() ?: return emptyMap()
        val format = obsidianRepository.filenameFormat.first()
        val pointsMap = getHouseholdPointsUseCase().associate { it.name to it.points }

        val result = mutableMapOf<LocalDate, Int>()
        var date = dateRange.start
        while (date <= dateRange.endInclusive) {
            val content = journalRepository.readContent(journalDirUri, date, format).orEmpty()
            if (content.isNotEmpty()) {
                val total = HouseholdJournalParser.parse(content)
                    .filter { it.assignee == Assignee.HUSBAND }
                    .sumOf { (pointsMap[it.name] ?: 0) * it.count + it.adjustment }
                if (total != 0) result[date] = total
            }
            date = date.plusDays(1)
        }
        return result
    }
}
