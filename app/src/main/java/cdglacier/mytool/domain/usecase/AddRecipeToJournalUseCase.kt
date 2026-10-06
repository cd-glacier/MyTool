package cdglacier.mytool.domain.usecase

import cdglacier.mytool.data.repository.JournalRepository
import cdglacier.mytool.data.repository.RecipeRepository
import cdglacier.mytool.domain.model.Recipe
import java.time.LocalDate
import javax.inject.Inject

class AddRecipeToJournalUseCase @Inject constructor(
    private val journalRepository: JournalRepository,
    private val recipeRepository: RecipeRepository,
) {
    suspend operator fun invoke(
        journalDirUri: String,
        date: LocalDate,
        filenameFormat: String,
        title: String,
        url: String,
    ): Result<Unit> = runCatching {
        val original = journalRepository.readContent(journalDirUri, date, filenameFormat) ?: ""
        val item = "- [${title.trim()}]($url)"
        val updated = JournalSectionWriter.write(original, JournalSection.RECIPE, item)
        journalRepository.writeContent(journalDirUri, date, filenameFormat, updated).getOrThrow()
        val existing = recipeRepository.getByDate(date)
            .map { Recipe(title = it.title, url = it.url) }
        if (existing.none { it.url == url }) {
            recipeRepository.replaceForDate(date, existing + Recipe(title = title.trim(), url = url))
        }
    }
}
