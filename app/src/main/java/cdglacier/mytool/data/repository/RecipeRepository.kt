package cdglacier.mytool.data.repository

import cdglacier.mytool.data.db.RecipeDao
import cdglacier.mytool.data.db.RecipeEntity
import cdglacier.mytool.domain.model.Recipe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

interface RecipeRepository {
    val sections: Flow<List<RecipeSection>>
    suspend fun getByDate(date: LocalDate): List<RecipeEntity>
    suspend fun replaceForDate(date: LocalDate, recipes: List<Recipe>)
}

data class RecipeSection(
    val date: LocalDate,
    val items: List<RecipeItem>,
)

data class RecipeItem(
    val title: String,
    val url: String,
)

@Singleton
class RecipeRepositoryImpl @Inject constructor(
    private val dao: RecipeDao,
) : RecipeRepository {

    override val sections: Flow<List<RecipeSection>> =
        dao.observeAll().map { entities ->
            entities.groupBy { it.date }
                .mapNotNull { (dateStr, items) ->
                    val date = runCatching { LocalDate.parse(dateStr) }.getOrNull() ?: return@mapNotNull null
                    RecipeSection(
                        date = date,
                        items = items.sortedBy { it.position }
                            .map { RecipeItem(title = it.title, url = it.url) },
                    )
                }
                .sortedByDescending { it.date }
        }

    override suspend fun getByDate(date: LocalDate): List<RecipeEntity> =
        dao.getByDate(date.toString())

    override suspend fun replaceForDate(date: LocalDate, recipes: List<Recipe>) {
        val entities = recipes.mapIndexed { index, recipe ->
            RecipeEntity(
                date = date.toString(),
                url = recipe.url,
                title = recipe.title,
                position = index,
            )
        }
        dao.replaceForDate(date.toString(), entities)
    }
}
