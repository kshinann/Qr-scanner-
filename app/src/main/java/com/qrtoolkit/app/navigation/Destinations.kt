package com.qrtoolkit.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.ui.graphics.vector.ImageVector

enum class Destination(val route: String, val label: String, val icon: ImageVector) {
    SCAN("scan", "Scan", Icons.Default.QrCodeScanner),
    GENERATE("generate", "Generate", Icons.Default.QrCode),
    HISTORY("history", "History", Icons.Default.History),
}
