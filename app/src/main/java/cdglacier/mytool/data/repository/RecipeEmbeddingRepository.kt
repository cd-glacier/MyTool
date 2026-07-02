package cdglacier.mytool.data.repository

import cdglacier.mytool.data.db.RecipeEmbeddingDao
import cdglacier.mytool.data.db.RecipeEmbeddingEntity
import cdglacier.mytool.data.embedder.RecipeTextEmbedder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class EmbeddingProgress(
    val processed: Int = 0,
    val total: Int = 0,
) {
    val isRunning: Boolean get() = total > 0 && processed < total
}

interface RecipeEmbeddingRepository {
    val progress: StateFlow<EmbeddingProgress>
    val modelVersion: String
    suspend fun purgeOtherModels()
    suspend fun getMissingUrls(allUrls: List<String>): List<String>
    suspend fun save(url: String, embedding: FloatArray)
    suspend fun getAll(): List<Pair<String, FloatArray>>
    fun updateProgress(processed: Int, total: Int)
    fun clearProgress()
}

@Singleton
class RecipeEmbeddingRepositoryImpl @Inject constructor(
    private val dao: RecipeEmbeddingDao,
    private val embedder: RecipeTextEmbedder,
) : RecipeEmbeddingRepository {

    private val _progress = MutableStateFlow(EmbeddingProgress())
    override val progress: StateFlow<EmbeddingProgress> = _progress.asStateFlow()

    override val modelVersion: String = embedder.modelVersion

    override suspend fun purgeOtherModels() {
        dao.deleteOtherModels(modelVersion)
    }

    override suspend fun getMissingUrls(allUrls: List<String>): List<String> {
        val existing = dao.getUrlsForModel(modelVersion).toSet()
        return allUrls.filter { it !in existing }
    }

    override suspend fun save(url: String, embedding: FloatArray) {
        dao.insert(
            RecipeEmbeddingEntity(
                url = url,
                embedding = RecipeTextEmbedder.floatArrayToBytes(embedding),
                model = modelVersion,
                generatedAt = System.currentTimeMillis(),
            )
        )
    }

    override suspend fun getAll(): List<Pair<String, FloatArray>> =
        dao.getAllForModel(modelVersion).map { entity ->
            entity.url to RecipeTextEmbedder.bytesToFloatArray(entity.embedding)
        }

    override fun updateProgress(processed: Int, total: Int) {
        _progress.value = EmbeddingProgress(processed, total)
    }

    override fun clearProgress() {
        _progress.value = EmbeddingProgress()
    }
}
