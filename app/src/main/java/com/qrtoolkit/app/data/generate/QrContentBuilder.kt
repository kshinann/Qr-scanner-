package com.qrtoolkit.app.data.generate

import com.qrtoolkit.app.data.model.WifiQrSecurity
import java.net.URLEncoder

/**
 * Builds the raw text payload to encode for each [com.qrtoolkit.app.data.model.QrContentType].
 * Pure string formatting, no Android dependency, so it's easy to unit test against the formats
 * scanners actually expect.
 */
object QrContentBuilder {

    fun text(value: String): String = value

    fun url(value: String): String {
        val trimmed = value.trim()
        return if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            trimmed
        } else {
            "https://$trimmed"
        }
    }

    fun wifi(ssid: String, password: String, security: WifiQrSecurity, hidden: Boolean): String {
        val passwordPart = if (security == WifiQrSecurity.NONE) "" else "P:${escapeWifiField(password)};"
        return "WIFI:T:${security.token};S:${escapeWifiField(ssid)};$passwordPart" +
            "H:${if (hidden) "true" else "false"};;"
    }

    fun contact(
        fullName: String,
        organization: String,
        phone: String,
        email: String,
    ): String = buildString {
        appendLine("BEGIN:VCARD")
        appendLine("VERSION:3.0")
        appendLine("FN:${escapeVCardField(fullName)}")
        if (organization.isNotBlank()) appendLine("ORG:${escapeVCardField(organization)}")
        if (phone.isNotBlank()) appendLine("TEL:${escapeVCardField(phone)}")
        if (email.isNotBlank()) appendLine("EMAIL:${escapeVCardField(email)}")
        append("END:VCARD")
    }

    fun email(address: String, subject: String, body: String): String {
        val query = listOfNotNull(
            subject.takeIf { it.isNotBlank() }?.let { "subject=${urlEncode(it)}" },
            body.takeIf { it.isNotBlank() }?.let { "body=${urlEncode(it)}" },
        ).joinToString("&")
        return "mailto:$address" + if (query.isNotEmpty()) "?$query" else ""
    }

    fun sms(phoneNumber: String, message: String): String = "smsto:$phoneNumber:$message"

    fun phone(number: String): String = "tel:$number"

    fun geo(latitude: String, longitude: String): String = "geo:$latitude,$longitude"

    fun event(summary: String, location: String, startIso: String, endIso: String): String = buildString {
        appendLine("BEGIN:VEVENT")
        appendLine("SUMMARY:${escapeVCardField(summary)}")
        if (location.isNotBlank()) appendLine("LOCATION:${escapeVCardField(location)}")
        if (startIso.isNotBlank()) appendLine("DTSTART:$startIso")
        if (endIso.isNotBlank()) appendLine("DTEND:$endIso")
        append("END:VEVENT")
    }

    private fun urlEncode(value: String): String = URLEncoder.encode(value, "UTF-8")

    /** Escaping per the de-facto `WIFI:` QR convention (ZXing's, which every scanner follows). */
    private fun escapeWifiField(value: String): String = value
        .replace("\\", "\\\\")
        .replace(";", "\\;")
        .replace(",", "\\,")
        .replace(":", "\\:")
        .replace("\"", "\\\"")

    private fun escapeVCardField(value: String): String = value
        .replace("\\", "\\\\")
        .replace(";", "\\;")
        .replace(",", "\\,")
        .replace("\n", "\\n")
}
