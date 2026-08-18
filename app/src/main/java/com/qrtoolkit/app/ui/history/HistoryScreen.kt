@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.qrtoolkit.app.ui.history

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qrtoolkit.app.data.model.ScanContentKind
import com.qrtoolkit.app.data.model.ScannedCode
import com.qrtoolkit.app.di.appContainer
import com.qrtoolkit.app.ui.components.SectionCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val historyDateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())

@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    val container = remember { context.appContainer() }
    val viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.factory(container))
    val history by viewModel.history.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History (${history.size})") },
                actions = {
                    if (history.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clear() }) { Text("Clear") }
                    }
                },
            )
        },
    ) { padding ->
        if (history.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Nothing scanned yet.", style = MaterialTheme.typography.bodyLarge)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(history.reversed(), key = { it.timestampEpochMillis }) { entry ->
                HistoryRow(entry)
            }
        }
    }
}

@Composable
private fun HistoryRow(entry: ScannedCode) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    SectionCard(title = entry.kind.label) {
        Text(entry.rawValue, style = MaterialTheme.typography.bodyLarge, maxLines = 3)
        Text(
            "${entry.barcodeFormatLabel} · ${historyDateFormat.format(Date(entry.timestampEpochMillis))}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { clipboard.setText(AnnotatedString(entry.rawValue)) }) { Text("Copy") }
            if (entry.kind == ScanContentKind.URL) {
                TextButton(onClick = {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(entry.rawValue)))
                    } catch (e: ActivityNotFoundException) {
                        // No browser available to handle this.
                    }
                }) { Text("Open") }
            }
        }
    }
}
