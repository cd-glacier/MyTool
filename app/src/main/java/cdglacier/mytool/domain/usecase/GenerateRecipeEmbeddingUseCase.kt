package cdglacier.mytool.domain.usecase

import cdglacier.mytool.data.embedder.RecipeTextEmbedder
import cdglacier.mytool.data.repository.OgpRepository
import cdglacier.mytool.data.repository.RecipeEmbeddingRepository
import javax.inject.Inject

class GenerateRecipeEmbeddingUseCase @Inject constructor(
    private val ogpRepository: OgpRepository,
    private val embedder: RecipeTextEmbedder,
    private val embeddingRepository: RecipeEmbeddingRepository,
) {
    suspend operator fun invoke(url: String, fallbackTitle: String): Result<Unit> = runCatching {
        val ogp = runCatching { ogpRepository.fetch(url) }.getOrNull()
        val title = ogp?.title?.takeIf { it.isNotBlank() } ?: fallbackTitle
        val description = ogp?.description?.takeIf { it.isNotBlank() }
        val input = buildString {
            append(title)
            if (description != null) {
                append('\n')
                append(description)
            }
        }
        if (input.isBlank()) return@runCatching
        val vector = embedder.embed(input)
        embeddingRepository.save(url, vector)
    }
}
