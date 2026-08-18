@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.qrtoolkit.app.ui.scan

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.vision.barcode.common.Barcode
import com.qrtoolkit.app.data.model.ScanDetails
import com.qrtoolkit.app.data.scan.BarcodeAnalyzer
import com.qrtoolkit.app.data.scan.SmartActionResolver
import com.qrtoolkit.app.di.appContainer
import com.qrtoolkit.app.ui.components.CameraPermissionGate

@Composable
fun ScanScreen() {
    val context = LocalContext.current
    val container = remember { context.appContainer() }
    val viewModel: ScanViewModel = viewModel(factory = ScanViewModel.factory(container))

    val currentResult by viewModel.currentResult.collectAsState()
    val message by viewModel.message.collectAsState()
    val productLookup by viewModel.productLookup.collectAsState()

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.scanFromImage(context, it) }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    CameraPermissionGate {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text("Scan") },
                    actions = {
                        IconButton(onClick = {
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        }) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = "Scan from image")
                        }
                    },
                )
            },
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                CameraPreview(
                    onBarcodeDetected = { viewModel.onBarcodeDetected(it) },
                    modifier = Modifier.fillMaxSize(),
                )
                ScanFrameOverlay(modifier = Modifier.fillMaxSize())
            }
        }
    }

    currentResult?.let { result ->
        ModalBottomSheet(onDismissRequest = { viewModel.dismissResult() }) {
            ScanResultContent(
                result = result,
                productLookup = productLookup,
                onLookupProduct = { viewModel.lookupProduct(it) },
                onDismiss = { viewModel.dismissResult() },
            )
        }
    }
}

@Composable
private fun CameraPreview(
    onBarcodeDetected: (Barcode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    AndroidView(factory = { previewView }, modifier = modifier)

    DisposableEffect(lifecycleOwner) {
        // Guards the async listener below: if this composable leaves composition (e.g. the
        // user switches to another bottom-nav tab) before the future resolves, the callback
        // must not go on to bind a camera nobody will ever unbind.
        var disposed = false
        var boundCameraProvider: ProcessCameraProvider? = null

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener(
            {
                if (!disposed) {
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also {
                                it.setAnalyzer(ContextCompat.getMainExecutor(context), BarcodeAnalyzer(onBarcodeDetected))
                            }
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            analysis,
                        )
                        boundCameraProvider = cameraProvider
                    } catch (e: Exception) {
                        // Camera bind can fail if the device has no back camera, or it's
                        // already claimed elsewhere; nothing actionable beyond leaving the
                        // preview blank.
                    }
                }
            },
            ContextCompat.getMainExecutor(context),
        )

        onDispose {
            disposed = true
            // NavHost keeps this destination's lifecycle owner around (STOPPED, not DESTROYED)
            // when switching bottom-nav tabs, so bindToLifecycle alone won't release the camera
            // -- without this, the camera and barcode analysis would keep running in the
            // background after leaving the Scan tab.
            boundCameraProvider?.unbindAll()
        }
    }
}

@Composable
private fun ScanFrameOverlay(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(240.dp)
                .border(width = 3.dp, color = Color.White, shape = RoundedCornerShape(16.dp)),
        )
    }
}

@Composable
private fun ScanResultContent(
    result: ScanResultUi,
    productLookup: ProductLookupUiState,
    onLookupProduct: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
            .navigationBarsPadding(),
    ) {
        Text(result.kind.label, style = MaterialTheme.typography.titleLarge)
        Text(
            result.formatLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.height(12.dp))

        ScanDetailsBody(result.details, productLookup, onLookupProduct)

        Spacer(Modifier.height(16.dp))

        SmartActionResolver.resolve(result.details).forEach { action ->
            Button(
                onClick = {
                    try {
                        context.startActivity(action.intent)
                    } catch (e: ActivityNotFoundException) {
                        // No app on this device can handle this specific action.
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            ) {
                Text(action.label)
            }
        }

        OutlinedButton(
            onClick = { clipboard.setText(AnnotatedString(result.rawValue)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        ) {
            Text("Copy raw content")
        }

        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text("Scan again")
        }
    }
}

@Composable
private fun ScanDetailsBody(
    details: ScanDetails,
    productLookup: ProductLookupUiState,
    onLookupProduct: (String) -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    when (details) {
        is ScanDetails.Url -> Text(details.url, style = MaterialTheme.typography.bodyLarge)

        is ScanDetails.Wifi -> Column {
            Text("SSID: ${details.ssid}", style = MaterialTheme.typography.bodyLarge)
            details.encryptionType?.let { Text("Security: $it", style = MaterialTheme.typography.bodyMedium) }
            details.password?.takeIf { it.isNotBlank() }?.let { password ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Password: $password", style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { clipboard.setText(AnnotatedString(password)) }) { Text("Copy") }
                }
            }
        }

        is ScanDetails.Contact -> Column {
            details.name?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
            details.organization?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            details.phones.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
            details.emails.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }

        is ScanDetails.Email -> Column {
            Text(details.address.orEmpty(), style = MaterialTheme.typography.bodyLarge)
            details.subject?.let { Text("Subject: $it", style = MaterialTheme.typography.bodyMedium) }
        }

        is ScanDetails.Sms -> Column {
            Text(details.phoneNumber.orEmpty(), style = MaterialTheme.typography.bodyLarge)
            details.message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }

        is ScanDetails.Phone -> Text(details.number.orEmpty(), style = MaterialTheme.typography.bodyLarge)

        is ScanDetails.Geo -> Text(
            "${details.latitude}, ${details.longitude}",
            style = MaterialTheme.typography.bodyLarge,
        )

        is ScanDetails.CalendarEvent -> Column {
            Text(details.summary.orEmpty(), style = MaterialTheme.typography.bodyLarge)
            details.location?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }

        is ScanDetails.Product -> Column {
            Text("Barcode: ${details.code}", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            when (productLookup) {
                is ProductLookupUiState.Idle -> Button(onClick = { onLookupProduct(details.code) }) {
                    Text("Look up product")
                }
                is ProductLookupUiState.Loading -> CircularProgressIndicator(modifier = Modifier.size(24.dp))
                is ProductLookupUiState.Found -> Column {
                    productLookup.info.name?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
                    productLookup.info.brand?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                }
                is ProductLookupUiState.NotFound -> Text(
                    "No product info found for this barcode.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        is ScanDetails.PlainText -> Text(details.text, style = MaterialTheme.typography.bodyLarge)
    }
}
