package cdglacier.mytool.domain.usecase

import cdglacier.mytool.data.repository.JournalRepository
import java.time.LocalDate
import javax.inject.Inject

class CopyJournalUseCase @Inject constructor(
    private val journalRepository: JournalRepository,
    private val getYesterdayHealthDataUseCase: GetYesterdayHealthDataUseCase,
) {
    suspend operator fun invoke(
        journalDirUri: String,
        sourceDate: LocalDate,
        targetDate: LocalDate,
        filenameFormat: String,
    ): Result<Unit> = runCatching {
        val source = journalRepository.readContent(journalDirUri, sourceDate, filenameFormat)
            ?: error("コピー元ファイルが見つかりません")
        val healthData = runCatching { getYesterdayHealthDataUseCase(targetDate) }.getOrNull()
        val transformed = JournalTransformer.transform(source, healthData)
        journalRepository.writeContent(journalDirUri, targetDate, filenameFormat, transformed)
            .getOrThrow()
    }
}
