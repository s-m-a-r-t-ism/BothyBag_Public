package com.bothybag.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BothyTest {

    private fun bothy(
        type: String = "MBA",
        status: String = "open",
        access: String? = null,
        fire: String? = null,
        fireplace: String? = null,
        elevationM: Int? = null,
    ) = Bothy(
        id = "test_001",
        name = "Test Bothy",
        latitude = 57.0,
        longitude = -5.0,
        type = type,
        status = status,
        access = access,
        fire = fire,
        fireplace = fireplace,
        elevationM = elevationM,
    )

    @Test
    fun `open bothy is open and not closed`() {
        val b = bothy(status = "open")
        assertTrue(b.isOpen)
        assertFalse(b.isClosed)
    }

    @Test
    fun `closed and permanently closed both count as closed`() {
        assertTrue(bothy(status = "closed").isClosed)
        assertTrue(bothy(status = "permanently_closed").isClosed)
        assertTrue(bothy(status = "Closed").isClosed)
        assertFalse(bothy(status = "permanently_closed").isOpen)
    }

    @Test
    fun `seasonal bothy is neither open nor closed`() {
        val b = bothy(status = "seasonal")
        assertFalse(b.isOpen)
        assertFalse(b.isClosed)
    }

    @Test
    fun `type decides whether a bothy is MBA`() {
        assertTrue(bothy(type = "MBA").isMba)
        assertFalse(bothy(type = "Non-MBA").isMba)
    }

    @Test
    fun `private, customers and no access are private`() {
        assertTrue(bothy(access = "private").isPrivate)
        assertTrue(bothy(access = "customers").isPrivate)
        assertTrue(bothy(access = "no").isPrivate)
        assertTrue(bothy(access = "Private").isPrivate)
    }

    @Test
    fun `permissive, public and unknown access are not private`() {
        assertFalse(bothy(access = "permissive").isPrivate)
        assertFalse(bothy(access = "yes").isPrivate)
        assertFalse(bothy(access = null).isPrivate)
    }

    @Test
    fun `access text is readable and absent when unknown`() {
        assertEquals("Paying customers only", bothy(access = "customers").accessInfo)
        assertEquals("Private, no public access", bothy(access = "private").accessInfo)
        assertEquals("something else", bothy(access = "something else").accessInfo)
        assertNull(bothy(access = null).accessInfo)
        assertNull(bothy(access = " ").accessInfo)
    }

    @Test
    fun `fire info prefers the MBA field and translates OSM values`() {
        assertEquals("Open fire", bothy(fire = "Open fire", fireplace = "no").fireInfo)
        assertEquals("Fireplace available", bothy(fireplace = "yes").fireInfo)
        assertEquals("No fireplace", bothy(fireplace = "no").fireInfo)
        assertEquals("stove", bothy(fireplace = "stove").fireInfo)
        assertNull(bothy().fireInfo)
    }

    @Test
    fun `elevation converts to feet`() {
        assertEquals(3280, bothy(elevationM = 1000).elevationFt)
        assertNull(bothy().elevationFt)
    }
}
