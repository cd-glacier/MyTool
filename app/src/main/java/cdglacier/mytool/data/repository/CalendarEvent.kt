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

@Serializable
data class CalendarAccount(
    val id: Long,
    val displayName: String,
    val accountName: String,
    val color: Int,
)
