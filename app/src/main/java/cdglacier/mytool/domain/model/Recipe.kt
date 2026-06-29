package cdglacier.mytool.domain.model

import java.time.LocalDate

data class Recipe(
    val title: String,
    val url: String,
)

data class RecipesByDate(
    val date: LocalDate,
    val recipes: List<Recipe>,
)
