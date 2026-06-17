package cdglacier.mytool.domain.model

import java.time.DayOfWeek

sealed interface HabitFrequency {
    data object Daily : HabitFrequency
    data class Weekly(val dayOfWeek: DayOfWeek) : HabitFrequency
}

data class Habit(
    val name: String,
    val frequency: HabitFrequency,
    val isCompleted: Boolean,
) {
    fun appliesTo(dayOfWeek: DayOfWeek): Boolean = when (frequency) {
        is HabitFrequency.Daily -> true
        is HabitFrequency.Weekly -> frequency.dayOfWeek == dayOfWeek
    }
}
