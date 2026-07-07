package cdglacier.mytool.data.repository

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import cdglacier.mytool.domain.model.HouseholdPoint
import cdglacier.mytool.domain.usecase.HouseholdPointParser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface HouseholdRepository {
    suspend fun getHouseholdPoints(pagesDirUri: String): List<HouseholdPoint>
    suspend fun saveHouseholdPoints(pagesDirUri: String, points: List<HouseholdPoint>): Result<Unit>
}

@Singleton
class HouseholdRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : HouseholdRepository {

    override suspend fun getHouseholdPoints(pagesDirUri: String): List<HouseholdPoint> =
        withContext(Dispatchers.IO) {
            val dir = DocumentFile.fromTreeUri(context, Uri.parse(pagesDirUri))
                ?: return@withContext emptyList()
            val file = dir.findFile(FILENAME) ?: return@withContext emptyList()
            val content = context.contentResolver.openInputStream(file.uri)
                ?.use { it.bufferedReader().readText() } ?: return@withContext emptyList()
            HouseholdPointParser.parse(content)
        }

    override suspend fun saveHouseholdPoints(
        pagesDirUri: String,
        points: List<HouseholdPoint>,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val dir = DocumentFile.fromTreeUri(context, Uri.parse(pagesDirUri))
                ?: error("pages フォルダを開けません")
            val file = dir.findFile(FILENAME)
                ?: dir.createFile("text/markdown", FILENAME)
                ?: error("ファイルを作成できません: $FILENAME")
            val content = HouseholdPointParser.serialize(points)
            context.contentResolver.openOutputStream(file.uri, "wt")
                ?.use { it.write(content.toByteArray()) }
                ?: error("書き込めません")
        }
    }

    companion object {
        private const val FILENAME = "HouseholdPoint.md"
    }
}
