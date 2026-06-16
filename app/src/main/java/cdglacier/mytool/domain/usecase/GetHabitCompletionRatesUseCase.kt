package cdglacier.mytool.domain.usecase

import cdglacier.mytool.data.repository.JournalRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * 各日の習慣達成率を返す。値は 0.0〜1.0、該当する習慣が無い日は null。
 */
class GetHabitCompletionRatesUseCase @Inject constructor(
    private val journalRepository: JournalRepository,
) {
    suspend operator fun invoke(
        journalDirUri: String,
        filenameFormat: String,
        dates: List<LocalDate>,
    ): Map<LocalDate, Float?> {
        return dates.associateWith { date ->
            val content = journalRepository.readContent(journalDirUri, date, filenameFormat)
                ?: return@associateWith null
            val applicable = HabitParser.parse(content).filter { it.appliesTo(date.dayOfWeek) }
            if (applicable.isEmpty()) return@associateWith null
            applicable.count { it.isCompleted }.toFloat() / applicable.size
        }
    }
}
