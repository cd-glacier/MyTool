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

interface LocationPermissionRepository {
    val fineLocationGranted: StateFlow<Boolean>
    val backgroundLocationGranted: StateFlow<Boolean>
    fun refresh()
}

@Singleton
class LocationPermissionRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : LocationPermissionRepository {

    private val _fine = MutableStateFlow(check(Manifest.permission.ACCESS_FINE_LOCATION))
    private val _bg = MutableStateFlow(check(Manifest.permission.ACCESS_BACKGROUND_LOCATION))

    override val fineLocationGranted: StateFlow<Boolean> = _fine.asStateFlow()
    override val backgroundLocationGranted: StateFlow<Boolean> = _bg.asStateFlow()

    override fun refresh() {
        _fine.value = check(Manifest.permission.ACCESS_FINE_LOCATION)
        _bg.value = check(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    }

    private fun check(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
