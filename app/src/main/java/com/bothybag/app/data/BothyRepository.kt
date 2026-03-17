package com.bothybag.app.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class BothyRepository(private val context: Context) {

    private var bothies: List<Bothy> = emptyList()

    fun loadBothies(): List<Bothy> {
        if (bothies.isNotEmpty()) return bothies

        val json = context.assets
            .open("bothies.json")
            .bufferedReader()
            .use { it.readText() }

        val type = object : TypeToken<List<Bothy>>() {}.type
        bothies = Gson().fromJson(json, type)
        return bothies
    }

    fun getBothyById(id: String): Bothy? {
        if (bothies.isEmpty()) loadBothies()
        return bothies.find { it.id == id }
    }

    fun getMbaBothies(): List<Bothy> = loadBothies().filter { it.isMba }

    fun getNonMbaBothies(): List<Bothy> = loadBothies().filter { !it.isMba }

    val mbaBothyCount: Int get() = loadBothies().count { it.isMba }
}
