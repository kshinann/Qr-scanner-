package com.qrtoolkit.app.ui.generate

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.qrtoolkit.app.data.generate.QrContentBuilder
import com.qrtoolkit.app.data.model.QrContentType
import com.qrtoolkit.app.data.model.QrDotStyle
import com.qrtoolkit.app.data.model.WifiQrSecurity

/** Compose "state holder" for the Generate screen's whole form -- not persisted across process death, which is an acceptable trade-off for a generator form (nothing here is data worth restoring). */
class GenerateFormState {
    var contentType by mutableStateOf(QrContentType.TEXT)

    var text by mutableStateOf("")

    var wifiSsid by mutableStateOf("")
    var wifiPassword by mutableStateOf("")
    var wifiSecurity by mutableStateOf(WifiQrSecurity.WPA)
    var wifiHidden by mutableStateOf(false)

    var contactName by mutableStateOf("")
    var contactOrg by mutableStateOf("")
    var contactPhone by mutableStateOf("")
    var contactEmail by mutableStateOf("")

    var emailAddress by mutableStateOf("")
    var emailSubject by mutableStateOf("")
    var emailBody by mutableStateOf("")

    var smsNumber by mutableStateOf("")
    var smsMessage by mutableStateOf("")

    var phoneNumber by mutableStateOf("")

    var geoLat by mutableStateOf("")
    var geoLng by mutableStateOf("")

    var eventSummary by mutableStateOf("")
    var eventLocation by mutableStateOf("")
    var eventStart by mutableStateOf("")
    var eventEnd by mutableStateOf("")

    var dotStyle by mutableStateOf(QrDotStyle.SQUARE)
    var foregroundColor by mutableStateOf(Color.Black)
    var logo by mutableStateOf<Bitmap?>(null)

    fun buildContent(): String = when (contentType) {
        QrContentType.TEXT -> QrContentBuilder.text(text)
        QrContentType.URL -> QrContentBuilder.url(text)
        QrContentType.WIFI -> QrContentBuilder.wifi(wifiSsid, wifiPassword, wifiSecurity, wifiHidden)
        QrContentType.CONTACT -> QrContentBuilder.contact(contactName, contactOrg, contactPhone, contactEmail)
        QrContentType.EMAIL -> QrContentBuilder.email(emailAddress, emailSubject, emailBody)
        QrContentType.SMS -> QrContentBuilder.sms(smsNumber, smsMessage)
        QrContentType.PHONE -> QrContentBuilder.phone(phoneNumber)
        QrContentType.GEO -> QrContentBuilder.geo(geoLat, geoLng)
        QrContentType.EVENT -> QrContentBuilder.event(eventSummary, eventLocation, eventStart, eventEnd)
    }
}
