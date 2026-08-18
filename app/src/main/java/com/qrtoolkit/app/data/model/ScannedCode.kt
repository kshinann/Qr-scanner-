package com.qrtoolkit.app.data.model

import kotlinx.serialization.Serializable

/** What general category a decoded barcode's content falls into. */
enum class ScanContentKind(val label: String) {
    URL("Website"),
    WIFI("Wi-Fi network"),
    CONTACT("Contact"),
    EMAIL("Email"),
    PHONE("Phone number"),
    SMS("SMS"),
    GEO("Location"),
    CALENDAR_EVENT("Calendar event"),
    PRODUCT("Product barcode"),
    TEXT("Text"),
}

/** A persisted history entry. Deliberately flat/simple -- see [com.qrtoolkit.app.data.model.ScanDetails] for the richer, non-persisted structure used to build smart-action buttons right after a scan. */
@Serializable
data class ScannedCode(
    val rawValue: String,
    val kind: ScanContentKind,
    val barcodeFormatLabel: String,
    val timestampEpochMillis: Long,
)
