package com.bothybag.app.data

import android.content.Context
import com.google.gson.Gson
import java.time.LocalDate

/**
 * Exports and imports the user's own data (visited bothies and personal notes)
 * as a small JSON file, so it can be moved to another phone or kept as a backup.
 */
class UserDataBackup(context: Context) {

    private val visitedRepo = VisitedRepository(context)
    private val notesRepo = NotesRepository(context)
    private val gson = Gson()

    /** All fields nullable: Gson leaves anything missing from the file as null. */
    private data class BackupFile(
        val app: String? = null,
        val version: Int? = null,
        val exported: String? = null,
        val visited: Map<String, String?>? = null,
        val notes: Map<String, String?>? = null,
    )

    data class ImportResult(val visitedCount: Int, val notesCount: Int)

    fun exportJson(): String {
        val backup = BackupFile(
            app = APP_NAME,
            version = FORMAT_VERSION,
            exported = LocalDate.now().toString(),
            visited = visitedRepo.getVisitedMap(),
            notes = notesRepo.getAllNotes(),
        )
        return gson.toJson(backup)
    }

    /**
     * Merges a backup into the current data: entries from the file are added, and
     * replace the existing entry for the same bothy. Nothing is removed.
     * Returns null when [json] is not a BothyBag backup.
     */
    fun importJson(json: String): ImportResult? {
        val backup: BackupFile = try {
            gson.fromJson(json, BackupFile::class.java) ?: return null
        } catch (e: Exception) {
            return null
        }

        if (backup.visited == null && backup.notes == null) return null

        // Keep only visits with a valid yyyy-MM-dd date
        val visited = mutableMapOf<String, String>()
        backup.visited?.forEach { (bothyId, date) ->
            if (bothyId.isNotBlank() && date != null && isValidDate(date)) {
                visited[bothyId] = date
            }
        }

        val notes = mutableMapOf<String, String>()
        backup.notes?.forEach { (bothyId, note) ->
            if (bothyId.isNotBlank() && !note.isNullOrBlank()) {
                notes[bothyId] = note
            }
        }

        visitedRepo.mergeVisited(visited)
        notesRepo.saveNotes(notes)

        return ImportResult(visitedCount = visited.size, notesCount = notes.size)
    }

    private fun isValidDate(date: String): Boolean {
        return try {
            LocalDate.parse(date)
            true
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        private const val APP_NAME = "BothyBag"
        private const val FORMAT_VERSION = 1
    }
}
