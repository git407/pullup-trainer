package com.pulluptrainer

data class WorkoutDay(
    val dayNumber: Int,
    val sets: List<Int>
) {
    fun getSetsString(): String {
        return if (sets.size == 1) {
            sets[0].toString()
        } else {
            sets.joinToString(" - ")
        }
    }
}

data class WorkoutLevel(
    val levelNumber: Int,
    val days: List<WorkoutDay>
)
