package cdglacier.mytool.domain.usecase

import cdglacier.mytool.data.repository.HouseholdRepository
import cdglacier.mytool.data.repository.ObsidianRepository
import cdglacier.mytool.domain.model.HouseholdPoint
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class SaveHouseholdPointsUseCase @Inject constructor(
    private val householdRepository: HouseholdRepository,
    private val obsidianRepository: ObsidianRepository,
) {
    suspend operator fun invoke(points: List<HouseholdPoint>): Result<Unit> {
        val pagesUri = obsidianRepository.pagesDirUri.first()?.toString()
            ?: return Result.failure(IllegalStateException("pages フォルダが未設定です"))
        return householdRepository.saveHouseholdPoints(pagesUri, points)
    }
}
