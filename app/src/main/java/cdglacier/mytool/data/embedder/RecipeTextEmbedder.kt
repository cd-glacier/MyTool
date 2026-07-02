package cdglacier.mytool.data.embedder

import android.content.Context
import com.google.mediapipe.tasks.core.BaseOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder.TextEmbedderOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class RecipeTextEmbedder @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    @Volatile
    private var embedder: TextEmbedder? = null

    val modelVersion: String = MODEL_VERSION

    private fun getOrCreate(): TextEmbedder {
        return embedder ?: synchronized(this) {
            embedder ?: TextEmbedder.createFromOptions(
                context,
                TextEmbedderOptions.builder()
                    .setBaseOptions(
                        BaseOptions.builder()
                            .setModelAssetPath(MODEL_ASSET_PATH)
                            .build(),
                    )
                    .setL2Normalize(true)
                    .build(),
            ).also { embedder = it }
        }
    }

    suspend fun embed(text: String): FloatArray = withContext(Dispatchers.Default) {
        val result = getOrCreate().embed(text)
        result.embeddingResult().embeddings().first().floatEmbedding()
    }

    companion object {
        private const val MODEL_ASSET_PATH = "universal_sentence_encoder.tflite"
        private const val MODEL_VERSION = "use-en-v1"

        fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
            if (a.size != b.size) return 0f
            var dot = 0f
            var na = 0f
            var nb = 0f
            for (i in a.indices) {
                dot += a[i] * b[i]
                na += a[i] * a[i]
                nb += b[i] * b[i]
            }
            val denom = sqrt(na) * sqrt(nb)
            return if (denom == 0f) 0f else dot / denom
        }

        fun floatArrayToBytes(v: FloatArray): ByteArray {
            val buf = ByteBuffer.allocate(v.size * 4).order(ByteOrder.LITTLE_ENDIAN)
            v.forEach { buf.putFloat(it) }
            return buf.array()
        }

        fun bytesToFloatArray(bytes: ByteArray): FloatArray {
            val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            return FloatArray(bytes.size / 4) { buf.float }
        }
    }
}
