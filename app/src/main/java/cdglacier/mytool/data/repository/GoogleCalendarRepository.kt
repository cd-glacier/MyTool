package cdglacier.mytool.data.repository

import android.content.ContentUris
import android.content.Context
import android.provider.CalendarContract
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
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
        val nextDayStart = dateStart + TimeUnit.DAYS.toMillis(1) - 1L

        // Time range is encoded in the URI; Instances expands recurring events
        // and surfaces multi-day events overlapping the range.
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().let { b ->
            ContentUris.appendId(b, dateStart)
            ContentUris.appendId(b, nextDayStart)
            b.build()
        }

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
            CalendarContract.Instances.CALENDAR_COLOR,
            CalendarContract.Instances.EVENT_COLOR,
        )

        val statusFilter =
            "${CalendarContract.Instances.STATUS} != ${CalendarContract.Events.STATUS_CANCELED}"

        val (selection, selectionArgs) = if (calendarIds.isNullOrEmpty()) {
            statusFilter to null
        } else {
            val placeholders = calendarIds.joinToString(",") { "?" }
            "$statusFilter AND ${CalendarContract.Instances.CALENDAR_ID} IN ($placeholders)" to
                calendarIds.map { it.toString() }.toTypedArray()
        }

        val sortOrder = "${CalendarContract.Instances.BEGIN} ASC"

        val events = mutableListOf<CalendarEvent>()

        context.contentResolver.query(
            uri,
            projection,
            selection,
            selectionArgs,
            sortOrder,
        )?.use { cursor ->
            val eventIdIdx  = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
            val titleIdx    = cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
            val beginIdx    = cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
            val endIdx      = cursor.getColumnIndexOrThrow(CalendarContract.Instances.END)
            val allDayIdx   = cursor.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)
            val calNameIdx  = cursor.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_DISPLAY_NAME)
            val calColorIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_COLOR)
            val eventColorIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_COLOR)

            while (cursor.moveToNext()) {
                // EVENT_COLOR: 0 / null means "use the calendar's default color"
                val eventColor = if (cursor.isNull(eventColorIdx)) 0 else cursor.getInt(eventColorIdx)
                val effectiveColor = if (eventColor != 0) eventColor else cursor.getInt(calColorIdx)

                events += CalendarEvent(
                    id = cursor.getLong(eventIdIdx),
                    title = cursor.getString(titleIdx) ?: "",
                    dtStart = cursor.getLong(beginIdx),
                    dtEnd = cursor.getLong(endIdx),
                    isAllDay = cursor.getInt(allDayIdx) != 0,
                    calendarDisplayName = cursor.getString(calNameIdx) ?: "",
                    calendarColor = effectiveColor,
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
