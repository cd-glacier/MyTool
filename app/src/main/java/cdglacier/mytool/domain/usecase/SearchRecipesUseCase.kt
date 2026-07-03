package cdglacier.mytool.domain.usecase

import cdglacier.mytool.data.repository.AiRepository
import cdglacier.mytool.data.repository.RecipeItem
import javax.inject.Inject

class SearchRecipesUseCase @Inject constructor(
    private val aiRepository: AiRepository,
) {
    suspend operator fun invoke(query: String, recipes: List<RecipeItem>, topN: Int = 20): List<RecipeItem> {
        if (query.isBlank() || recipes.isEmpty()) return emptyList()
        val prompt = buildPrompt(query, recipes)
        val response = aiRepository.generate(prompt) ?: return emptyList()
        val indices = parseIndices(response)
        return indices
            .mapNotNull { recipes.getOrNull(it - 1) }
            .distinct()
            .take(topN)
    }

    private fun buildPrompt(query: String, recipes: List<RecipeItem>): String = buildString {
        appendLine("あなたはレシピ検索アシスタントです。")
        appendLine("ユーザーのクエリに意味的に関連するレシピを、関連度が高い順に選んでください。")
        appendLine("該当するものが無ければ空行を返してください。")
        appendLine()
        appendLine("クエリ: $query")
        appendLine()
        appendLine("レシピ一覧:")
        recipes.forEachIndexed { index, item ->
            appendLine("${index + 1}. ${item.title}")
        }
        appendLine()
        appendLine("出力は関連度順のインデックス番号のみをカンマ区切りで1行で返してください。例: 3,1,5")
    }

    private fun parseIndices(response: String): List<Int> {
        val line = response.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.matches(Regex("""[\d,\s]+""")) }
            ?: response
        return line.split(",", " ", "、")
            .mapNotNull { it.trim().toIntOrNull() }
    }
}
