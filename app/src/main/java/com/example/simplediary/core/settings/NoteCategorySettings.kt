package com.example.simplediary.core.settings

import android.content.SharedPreferences

private const val PREF_NOTE_CATEGORIES = "note_categories"
private const val CATEGORIES_DELIMITER = "\n"

val DEFAULT_NOTE_CATEGORIES: List<String> = listOf("🙂", "🙁")

fun SharedPreferences.loadNoteCategories(): List<String> {
    val raw = getString(PREF_NOTE_CATEGORIES, null)
    val parsed = raw
        ?.split(CATEGORIES_DELIMITER)
        ?.map { it.trim() }
        ?.filter { it.isNotBlank() }
        .orEmpty()
    return if (parsed.isEmpty()) DEFAULT_NOTE_CATEGORIES else parsed.distinct()
}

fun SharedPreferences.saveNoteCategories(categories: List<String>) {
    val normalized = categories
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .ifEmpty { DEFAULT_NOTE_CATEGORIES }
    edit().putString(PREF_NOTE_CATEGORIES, normalized.joinToString(CATEGORIES_DELIMITER)).apply()
}
