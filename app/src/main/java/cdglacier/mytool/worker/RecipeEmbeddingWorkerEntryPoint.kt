package cdglacier.mytool.worker

import cdglacier.mytool.data.repository.RecipeEmbeddingRepository
import cdglacier.mytool.data.repository.RecipeRepository
import cdglacier.mytool.domain.usecase.GenerateRecipeEmbeddingUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface RecipeEmbeddingWorkerEntryPoint {
    fun recipeRepository(): RecipeRepository
    fun recipeEmbeddingRepository(): RecipeEmbeddingRepository
    fun generateRecipeEmbeddingUseCase(): GenerateRecipeEmbeddingUseCase
}
