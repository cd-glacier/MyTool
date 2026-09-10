package cdglacier.mytool.domain.usecase

import android.net.Uri
import cdglacier.mytool.data.repository.QrImageRepository
import cdglacier.mytool.domain.model.DecodedQr
import cdglacier.mytool.domain.model.QrEcLevel
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.ResultMetadataType
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import javax.inject.Inject

class DecodeQrImageUseCase @Inject constructor(
    private val qrImageRepository: QrImageRepository,
) {
    suspend operator fun invoke(imageUri: Uri): Result<DecodedQr> = withContext(Dispatchers.Default) {
        runCatching {
            val bitmap = qrImageRepository.loadBitmap(imageUri) ?: error("画像を読み込めません")

            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            val source = RGBLuminanceSource(bitmap.width, bitmap.height, pixels)
            val binary = BinaryBitmap(HybridBinarizer(source))

            val reader = MultiFormatReader().apply {
                setHints(mapOf(DecodeHintType.TRY_HARDER to true))
            }
            val result = reader.decode(binary)
            val metadata = result.resultMetadata ?: emptyMap<Any, Any>()
            val ec = (metadata[ResultMetadataType.ERROR_CORRECTION_LEVEL] as? ErrorCorrectionLevel)?.toDomain()
                ?: QrEcLevel.M
            val hasByteSegments = metadata[ResultMetadataType.BYTE_SEGMENTS] as? List<*> != null
            val mode = if (hasByteSegments) "BYTE" else result.barcodeFormat.name

            DecodedQr(
                content = result.text,
                ecLevel = ec,
                mode = mode,
                version = null,
                decodedAt = LocalDateTime.now(),
            )
        }
    }

    private fun ErrorCorrectionLevel.toDomain(): QrEcLevel = when (this) {
        ErrorCorrectionLevel.L -> QrEcLevel.L
        ErrorCorrectionLevel.M -> QrEcLevel.M
        ErrorCorrectionLevel.Q -> QrEcLevel.Q
        ErrorCorrectionLevel.H -> QrEcLevel.H
    }
}
