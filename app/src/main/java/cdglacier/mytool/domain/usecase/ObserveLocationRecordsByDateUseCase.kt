package cdglacier.mytool.domain.usecase

import cdglacier.mytool.data.db.LocationRecordEntity
import cdglacier.mytool.data.repository.LocationRecordRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

class ObserveLocationRecordsByDateUseCase @Inject constructor(
    private val repository: LocationRecordRepository,
) {
    operator fun invoke(date: LocalDate): Flow<List<LocationRecordEntity>> =
        repository.observeByDate(date)
}
