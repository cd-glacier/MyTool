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
    suspend fun getEventsForDate(
        date: LocalDate,
        calendarIds: Set<Long>? = null,
    ): List<CalendarEvent>

    suspend fun getCalendars(): List<CalendarAccount>
}

@Singleton
class GoogleCalendarRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : GoogleCalendarRepository {

    override suspend fun getEventsForDate(
        date: LocalDate,
        calendarIds: Set<Long>?,
    ): List<CalendarEvent> = withContext(Dispatchers.IO) {
        if (calendarIds != null && calendarIds.isEmpty()) return@withContext emptyList()

        val tz = TimeZone.getDefault()
        val dateStart = Calendar.getInstance(tz).apply {
            set(Calendar.YEAR, date.year)
            set(Calendar.MONTH, date.monthValue - 1)
            set(Calendar.DAY_OF_MONTH, date.dayOfMonth)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val nextDayStart = dateStart + TimeUnit.DAYS.toMillis(1)
        // Extend range back 1 day to catch all-day events whose UTC DTSTART falls on the previous day
        val queryStart = dateStart - TimeUnit.DAYS.toMillis(1)

        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.ALL_DAY,
            CalendarContract.Events.CALENDAR_DISPLAY_NAME,
            CalendarContract.Events.CALENDAR_COLOR,
        )

        val baseSelection = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} < ?"
        val baseArgs = arrayOf(queryStart.toString(), nextDayStart.toString())

        val (selection, selectionArgs) = if (calendarIds.isNullOrEmpty()) {
            baseSelection to baseArgs
        } else {
            val placeholders = calendarIds.joinToString(",") { "?" }
            val sel = "$baseSelection AND ${CalendarContract.Events.CALENDAR_ID} IN ($placeholders)"
            val args = baseArgs + calendarIds.map { it.toString() }.toTypedArray()
            sel to args
        }

        val sortOrder = "${CalendarContract.Events.DTSTART} ASC"

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
                if (eventLocalDate != date) continue

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

    override suspend fun getCalendars(): List<CalendarAccount> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR,
        )
        val selection = "${CalendarContract.Calendars.VISIBLE} = 1"
        val sortOrder = "${CalendarContract.Calendars.ACCOUNT_NAME} ASC, ${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} ASC"

        val accounts = mutableListOf<CalendarAccount>()

        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder,
        )?.use { cursor ->
            val idIdx       = cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
            val nameIdx     = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
            val accountIdx  = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
            val colorIdx    = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_COLOR)

            while (cursor.moveToNext()) {
                accounts += CalendarAccount(
                    id = cursor.getLong(idIdx),
                    displayName = cursor.getString(nameIdx) ?: "",
                    accountName = cursor.getString(accountIdx) ?: "",
                    color = cursor.getInt(colorIdx),
                )
            }
        }
        accounts
    }
}
