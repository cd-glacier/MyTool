package cdglacier.mytool.domain.model

enum class Assignee(val key: String) {
    HUSBAND("husband"),
    WIFE("wife");

    companion object {
        fun fromKey(key: String): Assignee? =
            entries.firstOrNull { it.key.equals(key.trim(), ignoreCase = true) }
    }
}

data class HouseholdPoint(
    val name: String,
    val points: Int,
)

data class HouseholdEntry(
    val name: String,
    val assignee: Assignee,
    val count: Int,
    val adjustment: Int,
)

data class HouseholdSummary(
    val husbandTotal: Int,
    val wifeTotal: Int,
    val perAssignee: Map<Assignee, List<Breakdown>>,
) {
    data class Breakdown(
        val name: String,
        val count: Int,
        val adjustment: Int,
        val effectivePoints: Int,
    )
}
