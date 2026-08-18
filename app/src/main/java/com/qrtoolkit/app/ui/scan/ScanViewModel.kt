package com.qrtoolkit.app.ui.scan

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.qrtoolkit.app.data.lookup.ProductInfo
import com.qrtoolkit.app.data.lookup.ProductLookupService
import com.qrtoolkit.app.data.model.ScanContentKind
import com.qrtoolkit.app.data.model.ScanDetails
import com.qrtoolkit.app.data.model.ScannedCode
import com.qrtoolkit.app.data.scan.BarcodeMapper
import com.qrtoolkit.app.data.scan.ScanHistoryStore
import com.qrtoolkit.app.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class ScanResultUi(
    val rawValue: String,
    val details: ScanDetails,
    val kind: ScanContentKind,
    val formatLabel: String,
)

sealed class ProductLookupUiState {
    object Idle : ProductLookupUiState()
    object Loading : ProductLookupUiState()
    data class Found(val info: ProductInfo) : ProductLookupUiState()
    object NotFound : ProductLookupUiState()
}

class ScanViewModel(
    private val historyStore: ScanHistoryStore,
    private val productLookupService: ProductLookupService,
) : ViewModel() {

    private val galleryScanner = BarcodeScanning.getClient()

    private val _currentResult = MutableStateFlow<ScanResultUi?>(null)
    val currentResult: StateFlow<ScanResultUi?> = _currentResult

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val _productLookup = MutableStateFlow<ProductLookupUiState>(ProductLookupUiState.Idle)
    val productLookup: StateFlow<ProductLookupUiState> = _productLookup

    /** Called from the camera analyzer for every frame that contains a barcode. */
    fun onBarcodeDetected(barcode: Barcode) {
        if (_currentResult.value != null) return // already showing a result; ignore further frames
        recordResult(barcode)
    }

    fun scanFromImage(context: Context, uri: Uri) {
        if (_currentResult.value != null) return
        viewModelScope.launch {
            try {
                val image = InputImage.fromFilePath(context, uri)
                val barcode = galleryScanner.process(image).await().firstOrNull()
                if (barcode == null) {
                    _message.value = "No QR code or barcode found in that image."
                } else {
                    recordResult(barcode)
                }
            } catch (e: Exception) {
                _message.value = "Couldn't read that image."
            }
        }
    }

    private fun recordResult(barcode: Barcode) {
        val details = BarcodeMapper.toScanDetails(barcode)
        val kind = BarcodeMapper.toScanContentKind(details)
        val formatLabel = BarcodeMapper.formatLabel(barcode.format)
        val rawValue = barcode.rawValue.orEmpty()
        _currentResult.value = ScanResultUi(rawValue, details, kind, formatLabel)
        _productLookup.value = ProductLookupUiState.Idle
        viewModelScope.launch {
            historyStore.add(ScannedCode(rawValue, kind, formatLabel, System.currentTimeMillis()))
        }
    }

    fun dismissResult() {
        _currentResult.value = null
        _productLookup.value = ProductLookupUiState.Idle
    }

    fun clearMessage() {
        _message.value = null
    }

    fun lookupProduct(code: String) {
        _productLookup.value = ProductLookupUiState.Loading
        viewModelScope.launch {
            val info = productLookupService.lookup(code)
            _productLookup.value = if (info != null) ProductLookupUiState.Found(info) else ProductLookupUiState.NotFound
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { ScanViewModel(container.scanHistoryStore, container.productLookupService) }
        }
    }
}
