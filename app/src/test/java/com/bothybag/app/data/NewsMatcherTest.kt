package com.bothybag.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NewsMatcherTest {

    private fun bothy(id: String, name: String, type: String = "MBA") =
        Bothy(id = id, name = name, latitude = 57.0, longitude = -5.0, type = type)

    private val bothies = listOf(
        bothy("mba_001", "A'Chuil Bothy"),
        bothy("mba_022", "Corrour Bothy"),
        bothy("mba_023", "Craig Bothy"),
        bothy("mba_048", "Green Bothy"),
        bothy("mba_049", "Greensykes Bothy"),
        bothy("mba_074", "Oban Bothy"),
        bothy("mba_085", "Shenavall Bothy"),
        bothy("osm_001", "Red Bothy", type = "Non-MBA"),
        bothy("osm_092", "Bothy", type = "Non-MBA"),
    )

    private fun matchedId(title: String): String? {
        val item = NewsItem(title = title, link = "https://example.org", pubDate = "", description = "")
        return NewsMatcher.match(listOf(item), bothies).single().matchedBothyId
    }

    @Test
    fun `matches a bothy named in the title`() {
        assertEquals("mba_022", matchedId("Work party required at Corrour"))
        assertEquals("mba_049", matchedId("Greensykes roof repairs"))
        assertEquals("mba_023", matchedId("Craig: roof work complete"))
    }

    @Test
    fun `sets the matched name as well as the id`() {
        val item = NewsItem("Corrour update", "l", "", "")
        assertEquals("Corrour Bothy", NewsMatcher.match(listOf(item), bothies).single().matchedBothyName)
    }

    @Test
    fun `ignores apostrophe style and possessives`() {
        assertEquals("mba_001", matchedId("A’Chuil bothy update"))
        assertEquals("mba_001", matchedId("A'Chuil bothy update"))
        assertEquals("mba_085", matchedId("Shenavall’s new stove"))
    }

    @Test
    fun `the word bothy alone matches nothing`() {
        assertNull(matchedId("Bothy restored after storm damage"))
    }

    @Test
    fun `does not match inside a longer word`() {
        assertNull(matchedId("Volunteers required for work party"))
        assertNull(matchedId("Craigmore estate news"))
    }

    @Test
    fun `non MBA bothies are never matched`() {
        assertNull(matchedId("Red Bothy reopens"))
    }

    @Test
    fun `everyday names need the full bothy name`() {
        assertNull(matchedId("Green energy grant for the MBA"))
        assertEquals("mba_048", matchedId("Green Bothy to be demolished"))
        assertNull(matchedId("Meeting in Oban next month"))
        assertEquals("mba_074", matchedId("Oban Bothy closed during stalking"))
    }

    @Test
    fun `longer names win over shorter ones`() {
        // "Greensykes" must not be mistaken for "Green Bothy"
        assertEquals("mba_049", matchedId("Green Bothy and Greensykes Bothy work parties"))
    }

    @Test
    fun `unrelated titles match nothing`() {
        assertNull(matchedId("Annual General Meeting 2026"))
    }
}
