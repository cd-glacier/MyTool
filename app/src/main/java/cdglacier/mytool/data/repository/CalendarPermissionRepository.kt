package cdglacier.mytool.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface CalendarPermissionRepository {
    val calendarGranted: StateFlow<Boolean>
    fun refresh()
}

@Singleton
class CalendarPermissionRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : CalendarPermissionRepository {

    private val _calendar = MutableStateFlow(check())
    override val calendarGranted: StateFlow<Boolean> = _calendar.asStateFlow()

    override fun refresh() {
        _calendar.value = check()
    }

    private fun check(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED
}
