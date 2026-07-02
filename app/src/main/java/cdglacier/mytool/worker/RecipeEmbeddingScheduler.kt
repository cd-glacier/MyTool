package cdglacier.mytool.worker

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface RecipeEmbeddingScheduler {
    fun schedule()
}

@Singleton
class RecipeEmbeddingSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : RecipeEmbeddingScheduler {
    override fun schedule() {
        RecipeEmbeddingWorker.schedule(context)
    }
}
