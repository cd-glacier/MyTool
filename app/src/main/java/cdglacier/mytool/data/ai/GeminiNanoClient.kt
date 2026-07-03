package cdglacier.mytool.data.ai

import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
class GeminiNanoClient @Inject constructor() {
    private val mutex = Mutex()
    private var cachedModel: GenerativeModel? = null

    private val _availability = MutableStateFlow(GeminiNanoAvailability.UNKNOWN)
    val availability: StateFlow<GeminiNanoAvailability> = _availability.asStateFlow()

    private suspend fun getModel(): GenerativeModel = mutex.withLock {
        cachedModel ?: Generation.getClient().also { cachedModel = it }
    }

    suspend fun refreshAvailability(): GeminiNanoAvailability {
        val status = runCatching { getModel().checkStatus() }.getOrNull()
        val mapped = when (status) {
            FeatureStatus.AVAILABLE -> GeminiNanoAvailability.AVAILABLE
            FeatureStatus.DOWNLOADABLE -> GeminiNanoAvailability.DOWNLOADABLE
            FeatureStatus.DOWNLOADING -> GeminiNanoAvailability.DOWNLOADING
            FeatureStatus.UNAVAILABLE -> GeminiNanoAvailability.UNAVAILABLE
            else -> GeminiNanoAvailability.UNAVAILABLE
        }
        _availability.value = mapped
        return mapped
    }

    suspend fun ensureDownloaded(): Boolean {
        val status = refreshAvailability()
        if (status == GeminiNanoAvailability.AVAILABLE) return true
        if (status != GeminiNanoAvailability.DOWNLOADABLE) return false
        return runCatching {
            _availability.value = GeminiNanoAvailability.DOWNLOADING
            getModel().download().collect { _: DownloadStatus -> }
            refreshAvailability() == GeminiNanoAvailability.AVAILABLE
        }.getOrDefault(false)
    }

    suspend fun generate(prompt: String): String? {
        if (refreshAvailability() != GeminiNanoAvailability.AVAILABLE) return null
        return runCatching {
            val model = getModel()
            val request = generateContentRequest(TextPart(prompt)) {
                temperature = 0.2f
                candidateCount = 1
            }
            model.generateContent(request).candidates.firstOrNull()?.text
        }.getOrNull()
    }
}
