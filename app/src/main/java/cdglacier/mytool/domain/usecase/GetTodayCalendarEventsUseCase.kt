package cdglacier.mytool.domain.usecase

import cdglacier.mytool.data.repository.CalendarEvent
import cdglacier.mytool.data.repository.GoogleCalendarRepository
import javax.inject.Inject

class GetTodayCalendarEventsUseCase @Inject constructor(
    private val calendarRepository: GoogleCalendarRepository,
) {
    suspend operator fun invoke(): List<CalendarEvent> =
        calendarRepository.getTodayEvents()
}
