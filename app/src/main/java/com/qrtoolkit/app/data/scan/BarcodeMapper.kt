package com.qrtoolkit.app.data.scan

import com.google.mlkit.vision.barcode.common.Barcode
import com.qrtoolkit.app.data.model.ScanContentKind
import com.qrtoolkit.app.data.model.ScanDetails

/** Converts an ML Kit [Barcode] into this app's own, decoupled [ScanDetails]/[ScanContentKind]. */
object BarcodeMapper {

    fun toScanDetails(barcode: Barcode): ScanDetails = when (barcode.valueType) {
        Barcode.TYPE_URL -> ScanDetails.Url(barcode.url?.url ?: barcode.rawValue.orEmpty())

        Barcode.TYPE_WIFI -> {
            val wifi = barcode.wifi
            ScanDetails.Wifi(
                ssid = wifi?.ssid.orEmpty(),
                password = wifi?.password,
                encryptionType = when (wifi?.encryptionType) {
                    Barcode.WiFi.TYPE_OPEN -> "Open"
                    Barcode.WiFi.TYPE_WPA -> "WPA/WPA2"
                    Barcode.WiFi.TYPE_WEP -> "WEP"
                    else -> null
                },
            )
        }

        Barcode.TYPE_CONTACT_INFO -> {
            val contact = barcode.contactInfo
            ScanDetails.Contact(
                name = contact?.name?.formattedName,
                organization = contact?.organization,
                phones = contact?.phones?.mapNotNull { it.number } ?: emptyList(),
                emails = contact?.emails?.mapNotNull { it.address } ?: emptyList(),
            )
        }

        Barcode.TYPE_EMAIL -> {
            val email = barcode.email
            ScanDetails.Email(address = email?.address, subject = email?.subject, body = email?.body)
        }

        Barcode.TYPE_SMS -> {
            val sms = barcode.sms
            ScanDetails.Sms(phoneNumber = sms?.phoneNumber, message = sms?.message)
        }

        Barcode.TYPE_PHONE -> ScanDetails.Phone(number = barcode.phone?.number)

        Barcode.TYPE_GEO -> {
            val geo = barcode.geoPoint
            ScanDetails.Geo(latitude = geo?.lat ?: 0.0, longitude = geo?.lng ?: 0.0)
        }

        Barcode.TYPE_CALENDAR_EVENT -> {
            val event = barcode.calendarEvent
            ScanDetails.CalendarEvent(summary = event?.summary, location = event?.location)
        }

        Barcode.TYPE_PRODUCT, Barcode.TYPE_ISBN -> ScanDetails.Product(code = barcode.rawValue.orEmpty())

        else -> ScanDetails.PlainText(text = barcode.rawValue.orEmpty())
    }

    fun toScanContentKind(details: ScanDetails): ScanContentKind = when (details) {
        is ScanDetails.Url -> ScanContentKind.URL
        is ScanDetails.Wifi -> ScanContentKind.WIFI
        is ScanDetails.Contact -> ScanContentKind.CONTACT
        is ScanDetails.Email -> ScanContentKind.EMAIL
        is ScanDetails.Sms -> ScanContentKind.SMS
        is ScanDetails.Phone -> ScanContentKind.PHONE
        is ScanDetails.Geo -> ScanContentKind.GEO
        is ScanDetails.CalendarEvent -> ScanContentKind.CALENDAR_EVENT
        is ScanDetails.Product -> ScanContentKind.PRODUCT
        is ScanDetails.PlainText -> ScanContentKind.TEXT
    }

    fun formatLabel(format: Int): String = when (format) {
        Barcode.FORMAT_QR_CODE -> "QR code"
        Barcode.FORMAT_CODE_128 -> "Code 128"
        Barcode.FORMAT_CODE_39 -> "Code 39"
        Barcode.FORMAT_CODE_93 -> "Code 93"
        Barcode.FORMAT_CODABAR -> "Codabar"
        Barcode.FORMAT_EAN_13 -> "EAN-13"
        Barcode.FORMAT_EAN_8 -> "EAN-8"
        Barcode.FORMAT_ITF -> "ITF"
        Barcode.FORMAT_UPC_A -> "UPC-A"
        Barcode.FORMAT_UPC_E -> "UPC-E"
        Barcode.FORMAT_PDF417 -> "PDF417"
        Barcode.FORMAT_AZTEC -> "Aztec"
        Barcode.FORMAT_DATA_MATRIX -> "Data Matrix"
        else -> "Barcode"
    }
}
