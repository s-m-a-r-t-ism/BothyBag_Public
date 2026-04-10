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
}
