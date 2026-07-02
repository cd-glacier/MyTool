package cdglacier.mytool.domain.usecase

import cdglacier.mytool.data.embedder.RecipeTextEmbedder
import cdglacier.mytool.data.repository.RecipeEmbeddingRepository
import javax.inject.Inject

class SearchRecipesUseCase @Inject constructor(
    private val embedder: RecipeTextEmbedder,
    private val embeddingRepository: RecipeEmbeddingRepository,
) {
    suspend operator fun invoke(query: String, topN: Int = 20): List<String> {
        if (query.isBlank()) return emptyList()
        val queryVector = runCatching { embedder.embed(query) }.getOrNull() ?: return emptyList()
        val all = embeddingRepository.getAll()
        if (all.isEmpty()) return emptyList()
        return all
            .map { (url, vec) -> url to RecipeTextEmbedder.cosineSimilarity(queryVector, vec) }
            .sortedByDescending { it.second }
            .take(topN)
            .map { it.first }
    }
}
