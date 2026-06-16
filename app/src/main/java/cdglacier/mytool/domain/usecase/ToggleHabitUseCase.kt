package cdglacier.mytool.domain.usecase

import cdglacier.mytool.data.repository.JournalRepository
import cdglacier.mytool.domain.model.Habit
import java.time.LocalDate
import javax.inject.Inject

class ToggleHabitUseCase @Inject constructor(
    private val journalRepository: JournalRepository,
) {
    suspend operator fun invoke(
        journalDirUri: String,
        filenameFormat: String,
        date: LocalDate,
        habit: Habit,
    ): Result<Unit> {
        val content = journalRepository.readContent(journalDirUri, date, filenameFormat)
            ?: return Result.failure(IllegalStateException("Journalが見つかりません"))
        val updated = HabitParser.toggle(content, habit)
        if (updated == content) {
            return Result.failure(IllegalStateException("該当する習慣が見つかりません"))
        }
        return journalRepository.writeContent(journalDirUri, date, filenameFormat, updated)
    }
}
