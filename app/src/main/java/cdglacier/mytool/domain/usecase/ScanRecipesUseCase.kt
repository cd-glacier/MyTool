package cdglacier.mytool.domain.usecase

import cdglacier.mytool.data.db.RecipeEntity
import cdglacier.mytool.data.repository.JournalRepository
import cdglacier.mytool.data.repository.RecipeRepository
import javax.inject.Inject

class ScanRecipesUseCase @Inject constructor(
    private val journalRepository: JournalRepository,
    private val recipeRepository: RecipeRepository,
) {
    /**
     * journal を新しい日付順に走査し、Recipe セクションを発見するたびに DB に保存する。
     * 個別ファイル読み取りの失敗は無視する。
     */
    suspend operator fun invoke(journalDirUri: String, filenameFormat: String) {
        val dates = runCatching {
            journalRepository.listJournalDates(journalDirUri, filenameFormat)
        }.getOrDefault(emptyList()).sortedDescending()

        for (date in dates) {
            runCatching {
                val content = journalRepository.readContent(journalDirUri, date, filenameFormat)
                    ?: return@runCatching
                val parsed = RecipeParser.parse(content)
                val existing = recipeRepository.getByDate(date)
                if (!sameContent(existing, parsed)) {
                    recipeRepository.replaceForDate(date, parsed)
                }
            }
        }
    }

    private fun sameContent(existing: List<RecipeEntity>, parsed: List<cdglacier.mytool.domain.model.Recipe>): Boolean {
        if (existing.size != parsed.size) return false
        val sortedExisting = existing.sortedBy { it.position }
        return sortedExisting.zip(parsed).all { (e, p) -> e.title == p.title && e.url == p.url }
    }
}
