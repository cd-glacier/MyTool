package cdglacier.mytool.data.repository

import android.content.Context
import cdglacier.mytool.worker.AutoCopyJournalWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface AutoCopyJournalScheduler {
    fun enable()
    fun disable()
}

@Singleton
class AutoCopyJournalSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : AutoCopyJournalScheduler {
    override fun enable() {
        AutoCopyJournalWorker.schedule(context)
        AutoCopyJournalWorker.runOnce(context)
    }

    override fun disable() {
        AutoCopyJournalWorker.cancel(context)
    }
}
