package cdglacier.mytool.domain.usecase

import cdglacier.mytool.data.repository.JournalRepository
import cdglacier.mytool.domain.model.Habit
import java.time.LocalDate
import javax.inject.Inject

class GetTodayHabitsUseCase @Inject constructor(
    private val journalRepository: JournalRepository,
) {
    suspend operator fun invoke(
        journalDirUri: String,
        filenameFormat: String,
        date: LocalDate,
    ): List<Habit> {
        val content = journalRepository.readContent(journalDirUri, date, filenameFormat)
            ?: return emptyList()
        return HabitParser.parse(content).filter { it.appliesTo(date.dayOfWeek) }
    }
}
