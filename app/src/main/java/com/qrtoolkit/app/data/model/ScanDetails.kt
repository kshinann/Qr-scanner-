package com.qrtoolkit.app.data.model

/**
 * The structured fields ML Kit extracted from a just-decoded barcode, used only to build the
 * result screen's smart-action buttons. Deliberately not [kotlinx.serialization.Serializable] --
 * it only ever exists for the barcode currently on screen, never persisted to history.
 */
sealed class ScanDetails {
    data class Url(val url: String) : ScanDetails()
    data class Wifi(val ssid: String, val password: String?, val encryptionType: String?) : ScanDetails()
    data class Contact(
        val name: String?,
        val organization: String?,
        val phones: List<String>,
        val emails: List<String>,
    ) : ScanDetails()
    data class Email(val address: String?, val subject: String?, val body: String?) : ScanDetails()
    data class Sms(val phoneNumber: String?, val message: String?) : ScanDetails()
    data class Phone(val number: String?) : ScanDetails()
    data class Geo(val latitude: Double, val longitude: Double) : ScanDetails()
    data class CalendarEvent(val summary: String?, val location: String?) : ScanDetails()
    data class Product(val code: String) : ScanDetails()
    data class PlainText(val text: String) : ScanDetails()
}
