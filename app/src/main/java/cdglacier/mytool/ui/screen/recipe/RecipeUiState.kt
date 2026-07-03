package cdglacier.mytool.ui.screen.recipe

import cdglacier.mytool.data.ai.GeminiNanoAvailability
import cdglacier.mytool.ui.component.RecipeItemUiModel
import java.time.LocalDate

data class RecipeUiState(
    val isLoading: Boolean = false,
    val sections: List<RecipeSectionUiModel> = emptyList(),
    val addForm: AddRecipeFormUiModel = AddRecipeFormUiModel(),
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<RecipeItemUiModel> = emptyList(),
    val aiAvailability: GeminiNanoAvailability = GeminiNanoAvailability.UNKNOWN,
)

data class RecipeSectionUiModel(
    val date: LocalDate,
    val items: List<RecipeItemUiModel>,
)

data class AddRecipeFormUiModel(
    val url: String = "",
    val title: String? = null,
    val isFetching: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: String? = null,
)
