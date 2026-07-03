package cdglacier.mytool.data.ai

import android.content.Context
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.request.GenerateContentRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

enum class GeminiNanoAvailability {
    UNKNOWN,
    AVAILABLE,
    DOWNLOADING,
    DOWNLOADABLE,
    UNAVAILABLE,
}

@Singleton
class GeminiNanoClient @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val mutex = Mutex()
    private var cachedModel: GenerativeModel? = null

    private val _availability = MutableStateFlow(GeminiNanoAvailability.UNKNOWN)
    val availability: StateFlow<GeminiNanoAvailability> = _availability.asStateFlow()

    private suspend fun getModel(): GenerativeModel = mutex.withLock {
        cachedModel ?: Generation.getClient(context).also { cachedModel = it }
    }

    suspend fun refreshAvailability(): GeminiNanoAvailability {
        val status = runCatching {
            val model = getModel()
            model.checkStatus().await()
        }.getOrNull()
        val mapped = when (status?.featureStatus) {
            FeatureStatus.AVAILABLE -> GeminiNanoAvailability.AVAILABLE
            FeatureStatus.DOWNLOADABLE -> GeminiNanoAvailability.DOWNLOADABLE
            FeatureStatus.DOWNLOADING -> GeminiNanoAvailability.DOWNLOADING
            FeatureStatus.UNAVAILABLE -> GeminiNanoAvailability.UNAVAILABLE
            null -> GeminiNanoAvailability.UNAVAILABLE
        }
        _availability.value = mapped
        return mapped
    }

    suspend fun ensureDownloaded(): Boolean {
        val status = refreshAvailability()
        if (status == GeminiNanoAvailability.AVAILABLE) return true
        if (status != GeminiNanoAvailability.DOWNLOADABLE) return false
        return runCatching {
            getModel().download().await()
            refreshAvailability() == GeminiNanoAvailability.AVAILABLE
        }.getOrDefault(false)
    }

    suspend fun generate(prompt: String): String? {
        if (refreshAvailability() != GeminiNanoAvailability.AVAILABLE) return null
        return runCatching {
            val model = getModel()
            val request = GenerateContentRequest.builder().setPrompt(prompt).build()
            model.generateContent(request).await().text
        }.getOrNull()
    }
}
