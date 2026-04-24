package com.bothybag.app.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate

class VisitedRepository(context: Context) {

    private val prefs = context.getSharedPreferences("visited_bothies", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val key = "visited_map"

    /** Returns map of bothyId -> date string (yyyy-MM-dd) */
    fun getVisitedMap(): Map<String, String> {
        val json = prefs.getString(key, null) ?: return emptyMap()
        val type = object : TypeToken<Map<String, String>>() {}.type
        return try {
            gson.fromJson<Map<String, String>>(json, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun isVisited(bothyId: String): Boolean = getVisitedMap().containsKey(bothyId)

    fun getVisitedDate(bothyId: String): String? = getVisitedMap()[bothyId]

    fun markVisited(bothyId: String) {
        val map = getVisitedMap().toMutableMap()
        map[bothyId] = LocalDate.now().toString()
        save(map)
    }

    fun markVisitedOnDate(bothyId: String, date: LocalDate) {
        val map = getVisitedMap().toMutableMap()
        map[bothyId] = date.toString()
        save(map)
    }

    fun markUnvisited(bothyId: String) {
        val map = getVisitedMap().toMutableMap()
        map.remove(bothyId)
        save(map)
    }

    fun toggleVisited(bothyId: String): Boolean {
        return if (isVisited(bothyId)) {
            markUnvisited(bothyId)
            false
        } else {
            markVisited(bothyId)
            true
        }
    }

    /** Count of visited MBA bothies */
    fun visitedMbaCount(bothyRepo: BothyRepository): Int {
        val visited = getVisitedMap().keys
        return bothyRepo.loadBothies().count { it.isMba && it.id in visited }
    }

    /** Total visited count */
    fun visitedTotalCount(): Int = getVisitedMap().size

    private fun save(map: Map<String, String>) {
        prefs.edit().putString(key, gson.toJson(map)).apply()
    }
}
