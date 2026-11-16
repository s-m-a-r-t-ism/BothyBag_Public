package com.bothybag.app.data

import android.content.Context

class NotesRepository(context: Context) {

    private val prefs = context.getSharedPreferences("bothy_notes", Context.MODE_PRIVATE)

    fun getNote(bothyId: String): String {
        return prefs.getString("note_$bothyId", "") ?: ""
    }

    fun saveNote(bothyId: String, note: String) {
        prefs.edit().putString("note_$bothyId", note).apply()
    }

    /** Returns map of bothyId -> note, leaving out empty notes */
    fun getAllNotes(): Map<String, String> {
        val notes = mutableMapOf<String, String>()
        for ((key, value) in prefs.all) {
            if (key.startsWith(KEY_PREFIX) && value is String && value.isNotBlank()) {
                notes[key.removePrefix(KEY_PREFIX)] = value
            }
        }
        return notes
    }

    /** Saves several notes at once, replacing the existing note for the same bothy */
    fun saveNotes(notes: Map<String, String>) {
        if (notes.isEmpty()) return
        val editor = prefs.edit()
        for ((bothyId, note) in notes) {
            editor.putString("$KEY_PREFIX$bothyId", note)
        }
        editor.apply()
    }

    companion object {
        private const val KEY_PREFIX = "note_"
    }
}
