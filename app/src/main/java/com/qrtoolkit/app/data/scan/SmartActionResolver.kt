package com.qrtoolkit.app.data.scan

import android.app.SearchManager
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.Settings
import com.qrtoolkit.app.data.model.ScanDetails

data class SmartAction(val label: String, val intent: Intent)

/**
 * Builds the "smart action" buttons for a decoded barcode's [ScanDetails]. Every action here
 * hands off to another app via a plain [Intent] rather than performing the action itself, so
 * none of them need any extra runtime permission beyond CAMERA (e.g. `ACTION_DIAL` opens the
 * dialer pre-filled rather than calling directly, `ACTION_INSERT` on contacts/calendar opens
 * those apps' own add-entry UI rather than writing to their providers itself).
 */
object SmartActionResolver {

    fun resolve(details: ScanDetails): List<SmartAction> = when (details) {
        is ScanDetails.Url -> listOf(
            SmartAction("Open in browser", Intent(Intent.ACTION_VIEW, Uri.parse(details.url))),
        )

        is ScanDetails.Wifi -> listOf(
            // Programmatic connect (WifiNetworkSuggestion) is deliberately not attempted here --
            // behavior is inconsistent enough across OEM Wi-Fi stacks that showing the credentials
            // for a manual connect is the more honest, reliable default.
            SmartAction("Open Wi-Fi settings", Intent(Settings.ACTION_WIFI_SETTINGS)),
        )

        is ScanDetails.Contact -> listOf(
            SmartAction(
                "Add to contacts",
                Intent(Intent.ACTION_INSERT_OR_EDIT).apply {
                    type = ContactsContract.Contacts.CONTENT_ITEM_TYPE
                    details.name?.let { putExtra(ContactsContract.Intents.Insert.NAME, it) }
                    details.organization?.let { putExtra(ContactsContract.Intents.Insert.COMPANY, it) }
                    details.phones.firstOrNull()?.let { putExtra(ContactsContract.Intents.Insert.PHONE, it) }
                    details.emails.firstOrNull()?.let { putExtra(ContactsContract.Intents.Insert.EMAIL, it) }
                },
            ),
        )

        is ScanDetails.Email -> listOf(
            SmartAction(
                "Send email",
                Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${details.address.orEmpty()}")).apply {
                    details.subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
                    details.body?.let { putExtra(Intent.EXTRA_TEXT, it) }
                },
            ),
        )

        is ScanDetails.Sms -> listOf(
            SmartAction(
                "Send SMS",
                Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${details.phoneNumber.orEmpty()}")).apply {
                    details.message?.let { putExtra("sms_body", it) }
                },
            ),
        )

        is ScanDetails.Phone -> listOf(
            SmartAction("Call", Intent(Intent.ACTION_DIAL, Uri.parse("tel:${details.number.orEmpty()}"))),
        )

        is ScanDetails.Geo -> listOf(
            SmartAction(
                "Open in maps",
                Intent(Intent.ACTION_VIEW, Uri.parse("geo:${details.latitude},${details.longitude}")),
            ),
        )

        is ScanDetails.CalendarEvent -> listOf(
            SmartAction(
                "Add to calendar",
                Intent(Intent.ACTION_INSERT).apply {
                    data = CalendarContract.Events.CONTENT_URI
                    putExtra(CalendarContract.Events.TITLE, details.summary.orEmpty())
                    details.location?.let { putExtra(CalendarContract.Events.EVENT_LOCATION, it) }
                },
            ),
        )

        is ScanDetails.PlainText -> listOf(
            SmartAction(
                "Search the web",
                Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, details.text),
            ),
        )

        is ScanDetails.Product -> emptyList()
    }
}
