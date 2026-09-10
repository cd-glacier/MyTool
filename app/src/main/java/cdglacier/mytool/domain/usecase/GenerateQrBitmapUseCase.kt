package cdglacier.mytool.domain.usecase

import android.graphics.Bitmap
import android.graphics.Color
import cdglacier.mytool.domain.model.QrEcLevel
import cdglacier.mytool.domain.model.QrEntry
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GenerateQrBitmapUseCase @Inject constructor() {
    suspend operator fun invoke(entry: QrEntry, sizePx: Int = 768): Result<Bitmap> =
        withContext(Dispatchers.Default) {
            runCatching {
                val ec = when (entry.ecLevel) {
                    QrEcLevel.L -> ErrorCorrectionLevel.L
                    QrEcLevel.M -> ErrorCorrectionLevel.M
                    QrEcLevel.Q -> ErrorCorrectionLevel.Q
                    QrEcLevel.H -> ErrorCorrectionLevel.H
                }
                val hints = mapOf(
                    EncodeHintType.ERROR_CORRECTION to ec,
                    EncodeHintType.MARGIN to 1,
                    EncodeHintType.CHARACTER_SET to "UTF-8",
                )
                val matrix = QRCodeWriter().encode(entry.content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
                val bmp = Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
                for (x in 0 until matrix.width) {
                    for (y in 0 until matrix.height) {
                        bmp.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
                    }
                }
                bmp
            }
        }
}
