package com.rahmanilab.lingodo.data.preferences.model

enum class SchedulerType(val label: String) {
    FSRS("FSRS"),
    SM2("SM-2");

    companion object {
        fun fromName(name: String?): SchedulerType =
            entries.firstOrNull { it.name == name } ?: FSRS
    }
}
