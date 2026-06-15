package cdglacier.mytool

import android.app.Application
import cdglacier.mytool.data.repository.ObsidianRepository
import cdglacier.mytool.worker.AutoCopyJournalWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MyToolApplication : Application() {

    @Inject
    lateinit var obsidianRepository: ObsidianRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            if (obsidianRepository.autoCopyEnabled.first()) {
                AutoCopyJournalWorker.schedule(this@MyToolApplication)
            } else {
                AutoCopyJournalWorker.cancel(this@MyToolApplication)
            }
        }
    }
}
