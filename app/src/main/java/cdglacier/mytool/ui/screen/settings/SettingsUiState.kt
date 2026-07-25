package cdglacier.mytool.ui.screen.settings

import android.net.Uri
import cdglacier.mytool.data.repository.AiAvailability

data class SettingsUiState(
    val vaultUri: Uri? = null,
    val journalDirUri: Uri? = null,
    val filenameFormat: String = "",
    val pagesDirUri: Uri? = null,
    val calendarPermissionGranted: Boolean = false,
    val fineLocationGranted: Boolean = false,
    val backgroundLocationGranted: Boolean = false,
    val aiAvailability: AiAvailability = AiAvailability.UNKNOWN,
    val healthConnectAvailable: Boolean = false,
    val healthPermissionsGranted: Boolean = false,
)
