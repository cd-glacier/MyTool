package cdglacier.mytool.domain.model

import java.time.LocalDateTime

data class QrEntry(
    val title: String,
    val content: String,
    val ecLevel: QrEcLevel,
    val mode: String,
    val version: Int?,
    val createdAt: LocalDateTime,
)

enum class QrEcLevel { L, M, Q, H }
