package cdglacier.mytool.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first

class RecipeEmbeddingWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            RecipeEmbeddingWorkerEntryPoint::class.java,
        )
        val recipeRepository = entryPoint.recipeRepository()
        val embeddingRepository = entryPoint.recipeEmbeddingRepository()
        val generateUseCase = entryPoint.generateRecipeEmbeddingUseCase()

        return runCatching {
            embeddingRepository.purgeOtherModels()
            val sections = recipeRepository.sections.first()
            val allItems = sections
                .flatMap { it.items }
                .distinctBy { it.url }
            val urlToTitle = allItems.associate { it.url to it.title }
            val missing = embeddingRepository.getMissingUrls(urlToTitle.keys.toList())

            if (missing.isEmpty()) {
                embeddingRepository.clearProgress()
                return@runCatching Result.success()
            }

            embeddingRepository.updateProgress(0, missing.size)
            for ((index, url) in missing.withIndex()) {
                val title = urlToTitle[url].orEmpty()
                generateUseCase(url, title)
                embeddingRepository.updateProgress(index + 1, missing.size)
            }
            embeddingRepository.clearProgress()
            Result.success()
        }.getOrElse {
            embeddingRepository.clearProgress()
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "RecipeEmbedding"

        fun schedule(context: Context) {
            val request = OneTimeWorkRequestBuilder<RecipeEmbeddingWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request,
            )
        }
    }
}
