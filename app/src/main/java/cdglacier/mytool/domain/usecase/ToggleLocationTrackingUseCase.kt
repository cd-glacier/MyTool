package cdglacier.mytool.domain.usecase

import cdglacier.mytool.tracking.LocationTrackingManager
import javax.inject.Inject

class ToggleLocationTrackingUseCase @Inject constructor(
    private val manager: LocationTrackingManager,
) {
    suspend operator fun invoke(enabled: Boolean) {
        if (enabled) manager.start() else manager.stop()
    }
}
