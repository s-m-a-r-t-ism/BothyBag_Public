package com.bothybag.app.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Checks the bundled bothies.json itself. The app loads this file with no
 * second chance: one bad entry can empty the whole map, so mistakes made while
 * editing or re-scraping the data should fail here instead of on a phone.
 */
class BothyDatasetTest {

    private val bothies: List<Bothy> by lazy {
        // Unit tests run from the app module folder; fall back to the repo root
        val file = listOf("src/main/assets/bothies.json", "app/src/main/assets/bothies.json")
            .map { File(it) }
            .first { it.exists() }
        val type = object : TypeToken<List<Bothy>>() {}.type
        Gson().fromJson<List<Bothy>>(file.readText(Charsets.UTF_8), type)
    }

    @Test
    fun `dataset loads and is not empty`() {
        assertTrue(bothies.size > 200)
    }

    @Test
    fun `ids are present and unique`() {
        assertTrue(bothies.all { it.id.isNotBlank() })
        val duplicates = bothies.groupBy { it.id }.filter { it.value.size > 1 }.keys
        assertEquals(emptySet<String>(), duplicates)
    }

    @Test
    fun `every bothy has a name`() {
        val unnamed = bothies.filter { it.name.isBlank() }.map { it.id }
        assertEquals(emptyList<String>(), unnamed)
    }

    @Test
    fun `coordinates are within Britain and Ireland`() {
        val outside = bothies.filter {
            it.latitude !in 49.5..61.5 || it.longitude !in -11.0..2.5
        }.map { it.id }
        assertEquals(emptyList<String>(), outside)
    }

    @Test
    fun `type is MBA or Non-MBA`() {
        val wrong = bothies.filter { it.type !in setOf("MBA", "Non-MBA") }.map { it.id }
        assertEquals(emptyList<String>(), wrong)
    }

    @Test
    fun `status is one the app understands`() {
        val known = setOf("open", "closed", "permanently_closed", "seasonal")
        val wrong = bothies.filter { it.status !in known }.map { "${it.id}: ${it.status}" }
        assertEquals(emptyList<String>(), wrong)
    }

    @Test
    fun `MBA bothies link to the MBA website`() {
        val wrong = bothies.filter {
            it.isMba && it.mbaUrl?.startsWith("https://www.mountainbothies.org.uk/") != true
        }.map { it.id }
        assertEquals(emptyList<String>(), wrong)
    }

    @Test
    fun `elevations are plausible`() {
        val wrong = bothies.filter { it.elevationM != null && it.elevationM !in 0..1350 }.map { it.id }
        assertEquals(emptyList<String>(), wrong)
    }

    @Test
    fun `descriptions carry no scraping leftovers`() {
        val wrong = bothies.filter {
            val text = it.description ?: ""
            "[email" in text || "protected]" in text || text != text.trim()
        }.map { it.id }
        assertEquals(emptyList<String>(), wrong)
    }
}
