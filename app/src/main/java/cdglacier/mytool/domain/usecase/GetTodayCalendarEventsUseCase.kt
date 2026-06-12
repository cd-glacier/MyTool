package cdglacier.mytool.domain.usecase

import cdglacier.mytool.data.repository.GoogleCalendarRepository
import cdglacier.mytool.domain.model.CalendarEvent
import java.time.LocalDate
import javax.inject.Inject

class GetTodayCalendarEventsUseCase @Inject constructor(
    private val calendarRepository: GoogleCalendarRepository,
) {
    suspend operator fun invoke(): List<CalendarEvent> =
        calendarRepository.getEventsForDate(LocalDate.now())
}
