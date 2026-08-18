package com.qrtoolkit.app.data.model

/** What kind of payload the Generate screen's form should build. */
enum class QrContentType(val label: String) {
    TEXT("Plain text"),
    URL("Website URL"),
    WIFI("Wi-Fi network"),
    CONTACT("Contact card"),
    EMAIL("Email"),
    SMS("SMS"),
    PHONE("Phone number"),
    GEO("Location"),
    EVENT("Calendar event"),
}

/** Security token written into a `WIFI:` payload; matches the format Android/iOS scanners expect. */
enum class WifiQrSecurity(val label: String, val token: String) {
    WPA("WPA/WPA2", "WPA"),
    WEP("WEP", "WEP"),
    NONE("Open (no password)", "nopass"),
}

enum class QrDotStyle(val label: String) {
    SQUARE("Classic squares"),
    ROUNDED("Rounded dots"),
}

data class QrStyleOptions(
    val foregroundColorArgb: Int = 0xFF000000.toInt(),
    val backgroundColorArgb: Int = 0xFFFFFFFF.toInt(),
    val dotStyle: QrDotStyle = QrDotStyle.SQUARE,
) {
    companion object {
        val DEFAULT = QrStyleOptions()
    }
}
