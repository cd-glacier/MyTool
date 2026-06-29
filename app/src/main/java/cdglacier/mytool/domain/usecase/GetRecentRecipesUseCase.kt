package cdglacier.mytool.domain.usecase

import cdglacier.mytool.data.repository.JournalRepository
import cdglacier.mytool.domain.model.RecipesByDate
import javax.inject.Inject

class GetRecentRecipesUseCase @Inject constructor(
    private val journalRepository: JournalRepository,
) {
    suspend operator fun invoke(
        journalDirUri: String,
        filenameFormat: String,
    ): List<RecipesByDate> {
        val dates = journalRepository.listJournalDates(journalDirUri, filenameFormat)
            .sortedDescending()
        return dates.mapNotNull { date ->
            val content = journalRepository.readContent(journalDirUri, date, filenameFormat)
                ?: return@mapNotNull null
            val recipes = RecipeParser.parse(content)
            if (recipes.isEmpty()) null else RecipesByDate(date, recipes)
        }
    }
}
