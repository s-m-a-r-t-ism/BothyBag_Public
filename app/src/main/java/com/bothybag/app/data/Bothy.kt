package com.bothybag.app.data

import com.google.gson.annotations.SerializedName

data class Bothy(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,

    @SerializedName("elevation_m")
    val elevationM: Int? = null,

    @SerializedName("os_grid_ref")
    val osGridRef: String? = null,

    val region: String? = null,
    val type: String = "Non-MBA",    // "MBA" or "Non-MBA"
    val status: String = "open",     // "open", "closed", "seasonal"
    val source: String? = null,

    // MBA-specific fields
    val fire: String? = null,
    val size: String? = null,
    val notes: String? = null,
    @SerializedName("mba_url")
    val mbaUrl: String? = null,

    // OSM-specific fields
    val fireplace: String? = null,
    val capacity: String? = null,
    val operator: String? = null,
    @SerializedName("osm_id")
    val osmId: String? = null,
    val access: String? = null,
    val description: String? = null,
) {
    val isMba: Boolean get() = type == "MBA"
    val isOpen: Boolean get() = status.lowercase() == "open"
    val isClosed: Boolean get() = status.lowercase() == "closed"

    val elevationFt: Int? get() = elevationM?.let { (it * 3.28084).toInt() }

    /** Combined fire info from MBA or OSM fields */
    val fireInfo: String?
        get() = fire ?: fireplace?.let {
            when (it.lowercase()) {
                "yes" -> "Fireplace available"
                "no" -> "No fireplace"
                else -> it
            }
        }

    /** Combined size/capacity info */
    val sizeInfo: String?
        get() = size ?: capacity?.let { "Capacity: $it" }

    /** Combined notes from MBA notes or OSM description */
    val combinedNotes: String?
        get() = notes ?: description
}
