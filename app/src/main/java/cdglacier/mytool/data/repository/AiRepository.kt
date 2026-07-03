package cdglacier.mytool.data.repository

import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

enum class AiAvailability {
    UNKNOWN,
    AVAILABLE,
    DOWNLOADING,
    DOWNLOADABLE,
    UNAVAILABLE,
}

interface AiRepository {
    val availability: Flow<AiAvailability>
    suspend fun refreshAvailability()
    suspend fun downloadModel()
    suspend fun generate(prompt: String): String?
}

@Singleton
class AiRepositoryImpl @Inject constructor() : AiRepository {
    private val mutex = Mutex()
    private var cachedModel: GenerativeModel? = null

    private val _availability = MutableStateFlow(AiAvailability.UNKNOWN)
    override val availability: StateFlow<AiAvailability> = _availability.asStateFlow()

    private suspend fun getModel(): GenerativeModel = mutex.withLock {
        cachedModel ?: Generation.getClient().also { cachedModel = it }
    }

    override suspend fun refreshAvailability() {
        val status = runCatching { getModel().checkStatus() }.getOrNull()
        _availability.value = when (status) {
            FeatureStatus.AVAILABLE -> AiAvailability.AVAILABLE
            FeatureStatus.DOWNLOADABLE -> AiAvailability.DOWNLOADABLE
            FeatureStatus.DOWNLOADING -> AiAvailability.DOWNLOADING
            FeatureStatus.UNAVAILABLE -> AiAvailability.UNAVAILABLE
            else -> AiAvailability.UNAVAILABLE
        }
    }

    override suspend fun downloadModel() {
        refreshAvailability()
        if (_availability.value != AiAvailability.DOWNLOADABLE) return
        runCatching {
            _availability.value = AiAvailability.DOWNLOADING
            getModel().download().collect { _: DownloadStatus -> }
        }
        refreshAvailability()
    }

    override suspend fun generate(prompt: String): String? {
        refreshAvailability()
        if (_availability.value != AiAvailability.AVAILABLE) return null
        return runCatching {
            val request = generateContentRequest(TextPart(prompt)) {
                temperature = 0.2f
                candidateCount = 1
            }
            getModel().generateContent(request).candidates.firstOrNull()?.text
        }.getOrNull()
    }
}
