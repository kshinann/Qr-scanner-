@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.qrtoolkit.app.ui.generate

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.qrtoolkit.app.data.generate.QrEncoder
import com.qrtoolkit.app.data.generate.QrExportUtils
import com.qrtoolkit.app.data.model.QrContentType
import com.qrtoolkit.app.data.model.QrDotStyle
import com.qrtoolkit.app.data.model.QrStyleOptions
import com.qrtoolkit.app.data.model.WifiQrSecurity
import com.qrtoolkit.app.ui.components.SectionCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val PRESET_COLORS = listOf(
    Color.Black,
    Color(0xFF4B3FD4),
    Color(0xFF0B6E4F),
    Color(0xFFB3261E),
    Color(0xFF6A1B9A),
    Color(0xFF01579B),
)

@Composable
fun GenerateScreen() {
    val form = remember { GenerateFormState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val logoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val bitmap = runCatching {
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            }.getOrNull()
            form.logo = bitmap
        }
    }

    val content = form.buildContent()
    val qrBitmap: Bitmap? = remember(content, form.dotStyle, form.foregroundColor, form.logo) {
        if (content.isBlank()) {
            null
        } else {
            runCatching {
                QrEncoder.encode(
                    content = content,
                    sizePx = 512,
                    style = QrStyleOptions(
                        foregroundColorArgb = form.foregroundColor.toArgb(),
                        backgroundColorArgb = Color.White.toArgb(),
                        dotStyle = form.dotStyle,
                    ),
                    logo = form.logo,
                )
            }.getOrNull()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { TopAppBar(title = { Text("Generate") }) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { ContentTypeSelector(form) }
            item { ContentTypeForm(form) }
            item {
                StyleOptionsCard(
                    form = form,
                    onPickLogo = {
                        logoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                )
            }
            item {
                PreviewCard(
                    qrBitmap = qrBitmap,
                    onSave = {
                        if (qrBitmap != null) {
                            scope.launch {
                                val uri = withContext(Dispatchers.IO) {
                                    QrExportUtils.saveToGallery(context, qrBitmap, "qr_${System.currentTimeMillis()}")
                                }
                                snackbarHostState.showSnackbar(if (uri != null) "Saved to gallery" else "Couldn't save image")
                            }
                        }
                    },
                    onShare = {
                        if (qrBitmap != null) {
                            scope.launch {
                                val uri = withContext(Dispatchers.IO) {
                                    QrExportUtils.shareableUri(context, qrBitmap, "qr_${System.currentTimeMillis()}")
                                }
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/png"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share QR code"))
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun ContentTypeSelector(form: GenerateFormState) {
    SectionCard(title = "Content type") {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(QrContentType.entries.toList()) { type ->
                FilterChip(
                    selected = form.contentType == type,
                    onClick = { form.contentType = type },
                    label = { Text(type.label) },
                )
            }
        }
    }
}

@Composable
private fun ContentTypeForm(form: GenerateFormState) {
    SectionCard(title = "Details") {
        when (form.contentType) {
            QrContentType.TEXT, QrContentType.URL -> {
                OutlinedTextField(
                    value = form.text,
                    onValueChange = { form.text = it },
                    label = { Text(if (form.contentType == QrContentType.URL) "Website URL" else "Text") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            QrContentType.WIFI -> Column {
                OutlinedTextField(
                    value = form.wifiSsid,
                    onValueChange = { form.wifiSsid = it },
                    label = { Text("Network name (SSID)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WifiQrSecurity.entries.forEach { security ->
                        FilterChip(
                            selected = form.wifiSecurity == security,
                            onClick = { form.wifiSecurity = security },
                            label = { Text(security.label) },
                        )
                    }
                }
                if (form.wifiSecurity != WifiQrSecurity.NONE) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = form.wifiPassword,
                        onValueChange = { form.wifiPassword = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = form.wifiHidden, onCheckedChange = { form.wifiHidden = it })
                    Text("Hidden network")
                }
            }

            QrContentType.CONTACT -> Column {
                OutlinedTextField(form.contactName, { form.contactName = it }, label = { Text("Full name") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(form.contactOrg, { form.contactOrg = it }, label = { Text("Organization") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(form.contactPhone, { form.contactPhone = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(form.contactEmail, { form.contactEmail = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
            }

            QrContentType.EMAIL -> Column {
                OutlinedTextField(form.emailAddress, { form.emailAddress = it }, label = { Text("To") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(form.emailSubject, { form.emailSubject = it }, label = { Text("Subject") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(form.emailBody, { form.emailBody = it }, label = { Text("Body") }, modifier = Modifier.fillMaxWidth())
            }

            QrContentType.SMS -> Column {
                OutlinedTextField(form.smsNumber, { form.smsNumber = it }, label = { Text("Phone number") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(form.smsMessage, { form.smsMessage = it }, label = { Text("Message") }, modifier = Modifier.fillMaxWidth())
            }

            QrContentType.PHONE -> {
                OutlinedTextField(
                    value = form.phoneNumber,
                    onValueChange = { form.phoneNumber = it },
                    label = { Text("Phone number") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            QrContentType.GEO -> Column {
                OutlinedTextField(form.geoLat, { form.geoLat = it }, label = { Text("Latitude") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(form.geoLng, { form.geoLng = it }, label = { Text("Longitude") }, modifier = Modifier.fillMaxWidth())
            }

            QrContentType.EVENT -> Column {
                OutlinedTextField(form.eventSummary, { form.eventSummary = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(form.eventLocation, { form.eventLocation = it }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    form.eventStart,
                    { form.eventStart = it },
                    label = { Text("Start (yyyyMMddTHHmmss)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    form.eventEnd,
                    { form.eventEnd = it },
                    label = { Text("End (yyyyMMddTHHmmss)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun StyleOptionsCard(form: GenerateFormState, onPickLogo: () -> Unit) {
    SectionCard(title = "Style") {
        Text("Color", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PRESET_COLORS.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (form.foregroundColor == color) 3.dp else 1.dp,
                            color = if (form.foregroundColor == color) MaterialTheme.colorScheme.primary else Color.Gray,
                            shape = CircleShape,
                        )
                        .clickable { form.foregroundColor = color },
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Module shape", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QrDotStyle.entries.forEach { style ->
                FilterChip(
                    selected = form.dotStyle == style,
                    onClick = { form.dotStyle = style },
                    label = { Text(style.label) },
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Center logo (optional)", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onPickLogo) { Text(if (form.logo == null) "Choose image" else "Change image") }
            if (form.logo != null) {
                OutlinedButton(onClick = { form.logo = null }) { Text("Remove") }
            }
        }
    }
}

@Composable
private fun PreviewCard(qrBitmap: Bitmap?, onSave: () -> Unit, onShare: () -> Unit) {
    SectionCard(title = "Preview") {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (qrBitmap != null) {
                Image(bitmap = qrBitmap.asImageBitmap(), contentDescription = "Generated QR code")
            } else {
                Text("Fill in the details above to generate a code.", style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (qrBitmap != null) {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSave) { Text("Save to gallery") }
                OutlinedButton(onClick = onShare) { Text("Share") }
            }
        }
    }
}
