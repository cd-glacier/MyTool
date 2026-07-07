package cdglacier.mytool.domain.usecase

import cdglacier.mytool.data.repository.HouseholdRepository
import cdglacier.mytool.data.repository.ObsidianRepository
import cdglacier.mytool.domain.model.HouseholdPoint
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GetHouseholdPointsUseCase @Inject constructor(
    private val householdRepository: HouseholdRepository,
    private val obsidianRepository: ObsidianRepository,
) {
    suspend operator fun invoke(): List<HouseholdPoint> {
        val pagesUri = obsidianRepository.pagesDirUri.first()?.toString() ?: return emptyList()
        return householdRepository.getHouseholdPoints(pagesUri)
    }
}
