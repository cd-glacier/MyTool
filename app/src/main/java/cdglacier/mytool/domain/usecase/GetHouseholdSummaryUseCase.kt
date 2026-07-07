package cdglacier.mytool.domain.usecase

import cdglacier.mytool.data.repository.JournalRepository
import cdglacier.mytool.data.repository.ObsidianRepository
import cdglacier.mytool.domain.model.Assignee
import cdglacier.mytool.domain.model.HouseholdSummary
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

class GetHouseholdSummaryUseCase @Inject constructor(
    private val journalRepository: JournalRepository,
    private val obsidianRepository: ObsidianRepository,
    private val getHouseholdPointsUseCase: GetHouseholdPointsUseCase,
) {
    suspend operator fun invoke(dateRange: ClosedRange<LocalDate>): HouseholdSummary {
        val journalDirUri = obsidianRepository.journalDirUri.first()?.toString()
            ?: return empty()
        val format = obsidianRepository.filenameFormat.first()
        val pointsMap = getHouseholdPointsUseCase().associate { it.name to it.points }

        val dates = generateSequence(dateRange.start) { d ->
            val next = d.plusDays(1)
            if (next > dateRange.endInclusive) null else next
        }.toList()

        val breakdowns = mutableMapOf<Assignee, MutableMap<String, HouseholdSummary.Breakdown>>()
        for (date in dates) {
            val content = journalRepository.readContent(journalDirUri, date, format).orEmpty()
            if (content.isEmpty()) continue
            for (entry in HouseholdJournalParser.parse(content)) {
                val base = pointsMap[entry.name] ?: 0
                val effective = base * entry.count + entry.adjustment
                val forAssignee = breakdowns.getOrPut(entry.assignee) { mutableMapOf() }
                val prev = forAssignee[entry.name]
                forAssignee[entry.name] = HouseholdSummary.Breakdown(
                    name = entry.name,
                    count = (prev?.count ?: 0) + entry.count,
                    adjustment = (prev?.adjustment ?: 0) + entry.adjustment,
                    effectivePoints = (prev?.effectivePoints ?: 0) + effective,
                )
            }
        }

        val perAssignee = breakdowns.mapValues { (_, m) ->
            m.values.sortedByDescending { it.effectivePoints }
        }
        val husbandTotal = perAssignee[Assignee.HUSBAND]?.sumOf { it.effectivePoints } ?: 0
        val wifeTotal = perAssignee[Assignee.WIFE]?.sumOf { it.effectivePoints } ?: 0
        return HouseholdSummary(husbandTotal, wifeTotal, perAssignee)
    }

    private fun empty() = HouseholdSummary(0, 0, emptyMap())
}
