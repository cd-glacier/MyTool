package cdglacier.mytool.data.repository

import kotlinx.serialization.Serializable

@Serializable
data class CalendarEvent(
    val id: Long,
    val title: String,
    val dtStart: Long,
    val dtEnd: Long,
    val isAllDay: Boolean,
    val calendarDisplayName: String,
    val calendarColor: Int,
)
