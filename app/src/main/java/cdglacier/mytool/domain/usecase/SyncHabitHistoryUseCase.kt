package cdglacier.mytool.domain.usecase

import cdglacier.mytool.data.repository.HabitHistoryRepository
import cdglacier.mytool.data.repository.JournalRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * 過去N日分のJournalを読み、習慣達成率に集計してキャッシュへ保存する。
 */
class SyncHabitHistoryUseCase @Inject constructor(
    private val journalRepository: JournalRepository,
    private val habitHistoryRepository: HabitHistoryRepository,
) {
    suspend operator fun invoke(
        journalDirUri: String,
        filenameFormat: String,
        dates: List<LocalDate>,
    ): Result<Int> = runCatching {
        val rates = mutableMapOf<LocalDate, Float>()
        for (date in dates) {
            val content = journalRepository.readContent(journalDirUri, date, filenameFormat)
                ?: continue
            val applicable = HabitParser.parse(content).filter { it.appliesTo(date.dayOfWeek) }
            if (applicable.isEmpty()) continue
            rates[date] = applicable.count { it.isCompleted }.toFloat() / applicable.size
        }
        habitHistoryRepository.save(rates, System.currentTimeMillis())
        rates.size
    }
}
