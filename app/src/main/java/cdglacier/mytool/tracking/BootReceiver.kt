package cdglacier.mytool.tracking

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import cdglacier.mytool.data.repository.TrackingStateRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Deps {
        fun trackingStateRepository(): TrackingStateRepository
        fun trackingManager(): LocationTrackingManager
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val deps = EntryPointAccessors.fromApplication(context.applicationContext, Deps::class.java)
        val pending = goAsync()
        scope.launch {
            try {
                val stateRepo = deps.trackingStateRepository()
                if (stateRepo.trackingEnabled.first()) {
                    deps.trackingManager().start(stateRepo.mode.first())
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
