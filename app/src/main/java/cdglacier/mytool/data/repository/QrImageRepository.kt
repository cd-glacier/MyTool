package cdglacier.mytool.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface QrImageRepository {
    suspend fun loadBitmap(uri: Uri): Bitmap?
}

@Singleton
class QrImageRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : QrImageRepository {
    override suspend fun loadBitmap(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
    }
}
