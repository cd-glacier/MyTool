package cdglacier.mytool.domain.usecase

import cdglacier.mytool.data.repository.JournalRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * 今日の習慣達成率を返す。該当する習慣が無ければ null。
 */
class GetTodayHabitCompletionRateUseCase @Inject constructor(
    private val journalRepository: JournalRepository,
) {
    suspend operator fun invoke(
        journalDirUri: String,
        filenameFormat: String,
        date: LocalDate,
    ): Float? {
        val content = journalRepository.readContent(journalDirUri, date, filenameFormat)
            ?: return null
        val applicable = HabitParser.parse(content).filter { it.appliesTo(date.dayOfWeek) }
        if (applicable.isEmpty()) return null
        return applicable.count { it.isCompleted }.toFloat() / applicable.size
    }
}
