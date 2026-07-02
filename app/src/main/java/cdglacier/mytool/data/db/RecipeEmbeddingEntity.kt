package cdglacier.mytool.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recipe_embeddings")
data class RecipeEmbeddingEntity(
    @PrimaryKey val url: String,
    val embedding: ByteArray,
    val model: String,
    val generatedAt: Long,
)
