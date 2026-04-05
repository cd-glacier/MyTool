package cdglacier.mytool.data.repository

import android.content.Context
import android.provider.CalendarContract
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

interface GoogleCalendarRepository {
    suspend fun getTodayEvents(): List<CalendarEvent>
}

@Singleton
class GoogleCalendarRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : GoogleCalendarRepository {

    override suspend fun getTodayEvents(): List<CalendarEvent> = withContext(Dispatchers.IO) {
        val tz = TimeZone.getDefault()
        val todayStart = Calendar.getInstance(tz).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val tomorrowStart = todayStart + TimeUnit.DAYS.toMillis(1)
        // Extend range back 1 day to catch all-day events whose UTC DTSTART falls on the previous day
        val queryStart = todayStart - TimeUnit.DAYS.toMillis(1)

        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.ALL_DAY,
            CalendarContract.Events.CALENDAR_DISPLAY_NAME,
            CalendarContract.Events.CALENDAR_COLOR,
        )
        val selection = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} < ?"
        val selectionArgs = arrayOf(queryStart.toString(), tomorrowStart.toString())
        val sortOrder = "${CalendarContract.Events.DTSTART} ASC"

        val today = LocalDate.now()
        val events = mutableListOf<CalendarEvent>()

        context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder,
        )?.use { cursor ->
            val idIdx      = cursor.getColumnIndexOrThrow(CalendarContract.Events._ID)
            val titleIdx   = cursor.getColumnIndexOrThrow(CalendarContract.Events.TITLE)
            val startIdx   = cursor.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)
            val endIdx     = cursor.getColumnIndexOrThrow(CalendarContract.Events.DTEND)
            val allDayIdx  = cursor.getColumnIndexOrThrow(CalendarContract.Events.ALL_DAY)
            val calNameIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.CALENDAR_DISPLAY_NAME)
            val calColorIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.CALENDAR_COLOR)

            while (cursor.moveToNext()) {
                val dtStart = cursor.getLong(startIdx)
                val isAllDay = cursor.getInt(allDayIdx) != 0

                val eventLocalDate = java.time.Instant.ofEpochMilli(dtStart)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                if (eventLocalDate != today) continue

                events += CalendarEvent(
                    id = cursor.getLong(idIdx),
                    title = cursor.getString(titleIdx) ?: "",
                    dtStart = dtStart,
                    dtEnd = cursor.getLong(endIdx),
                    isAllDay = isAllDay,
                    calendarDisplayName = cursor.getString(calNameIdx) ?: "",
                    calendarColor = cursor.getInt(calColorIdx),
                )
            }
        }
        events
    }
}
