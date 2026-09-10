package cdglacier.mytool.domain.model

import java.time.LocalDateTime

data class DecodedQr(
    val content: String,
    val ecLevel: QrEcLevel,
    val mode: String,
    val version: Int?,
    val decodedAt: LocalDateTime,
)
