package cdglacier.mytool.data.db

import androidx.room.Entity

@Entity(tableName = "recipes", primaryKeys = ["date", "url"])
data class RecipeEntity(
    val date: String,
    val url: String,
    val title: String,
    val position: Int,
    val ogpTitle: String? = null,
    val ogpImageUrl: String? = null,
)
