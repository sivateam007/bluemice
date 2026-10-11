package com.example.bluemice.ui

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.bluemice.BleHidManager
import com.example.bluemice.HidReportBuilder
import com.example.bluemice.SettingsManager
import com.example.bluemice.model.KeyboardLanguageCatalog
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
fun ScannerScreen(bleManager: BleHidManager, settingsManager: SettingsManager? = null) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var scannedText by remember { mutableStateOf("") }
    var lastScanned by remember { mutableStateOf("") }
    var sendEnter by remember { mutableStateOf(settingsManager?.scannerSendEnter ?: false) }
    var continuous by remember { mutableStateOf(settingsManager?.scannerContinuous ?: false) }
    var skipDuplicate by remember { mutableStateOf(settingsManager?.scannerSkipDuplicate ?: false) }
    val language = remember { KeyboardLanguageCatalog.byLocale(settingsManager?.selectedLanguage ?: "en") }
    val KC = HidReportBuilder.KeyCodes
    var hasCamera = remember { ctx.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) }
    var permissionGranted = remember {
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }
    var previewViewRef = remember { mutableStateOf<PreviewView?>(null) }

    fun typeScanned() {
        val text = scannedText
        if (text.isEmpty()) return
        text.forEach { c -> sendChar(bleManager, c, language) }
        if (sendEnter) bleManager.sendKeyPress(0, KC.KEY_ENTER)
        lastScanned = text
        if (continuous) scannedText = ""
    }

    LaunchedEffect(scannedText) {
        if (scannedText.isNotEmpty() && continuous) typeScanned()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
            .padding(12.dp)
    ) {
        Text("Code Scanner", color = Color(0xFFEEEEEE), fontSize = 16.sp,
            fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))

        if (!hasCamera) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No camera available", color = Color(0xFF888888), fontSize = 14.sp)
            }
        } else if (!permissionGranted) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Camera permission required", color = Color(0xFFCCCCCC), fontSize = 14.sp)
                    Button(
                        onClick = {
                            ctx.startActivity(
                                android.content.Intent(
                                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                ).apply { data = android.net.Uri.fromParts("package", ctx.packageName, null) }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF)),
                        modifier = Modifier.padding(top = 12.dp)
                    ) { Text("Open Settings") }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF000000), RoundedCornerShape(12.dp))
            ) {
                AndroidView(
                    factory = { context ->
                        val pv = PreviewView(context)
                        previewViewRef.value = pv
                        pv
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (scannedText.isEmpty()) "Point camera at a barcode or QR code"
                           else scannedText,
                    color = if (scannedText.isEmpty()) Color(0xFF888888) else Color(0xFF4CAF50),
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }

            if (scannedText.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { typeScanned() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF)),
                        modifier = Modifier.weight(1f)
                    ) { Text("Type to PC") }

                    Button(
                        onClick = { scannedText = "" },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A2A2A)),
                        modifier = Modifier.weight(1f)
                    ) { Text("Clear") }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                ToggleRow("Send Enter after typing", sendEnter) {
                    sendEnter = it; settingsManager?.scannerSendEnter = it
                }
                ToggleRow("Continuous scanning", continuous) {
                    continuous = it; settingsManager?.scannerContinuous = it
                }
                ToggleRow("Skip duplicate codes", skipDuplicate) {
                    skipDuplicate = it; settingsManager?.scannerSkipDuplicate = it
                }
            }
        }
    }

    // Camera analysis setup
    if (permissionGranted && hasCamera) {
        val barcodeOptions = remember {
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_QR_CODE or Barcode.FORMAT_EAN_13 or
                        Barcode.FORMAT_EAN_8 or Barcode.FORMAT_CODE_39 or
                        Barcode.FORMAT_CODE_128 or Barcode.FORMAT_DATA_MATRIX or
                        Barcode.FORMAT_UPC_A or Barcode.FORMAT_UPC_E
                ).build()
        }
        val scanner = remember { BarcodeScanning.getClient(barcodeOptions) }
        val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

        DisposableEffect(previewViewRef.value) {
            val pv = previewViewRef.value ?: return@DisposableEffect onDispose {}

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            val listener = Runnable {
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build()
                    preview.surfaceProvider = pv.surfaceProvider
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setTargetResolution(Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                    imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null && scannedText.isEmpty()) {
                            val inputImage = InputImage.fromMediaImage(
                                mediaImage, imageProxy.imageInfo.rotationDegrees
                            )
                            scanner.process(inputImage)
                                .addOnSuccessListener { barcodes ->
                                    for (barcode in barcodes) {
                                        val raw = barcode.rawValue ?: continue
                                        val dup = skipDuplicate && raw == lastScanned
                                        if (scannedText.isEmpty() && !dup) {
                                            scannedText = raw
                                        }
                                        break
                                    }
                                }
                                .addOnCompleteListener { imageProxy.close() }
                        } else {
                            imageProxy.close()
                        }
                    }
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA,
                        preview, imageAnalysis
                    )
                } catch (_: Exception) { }
            }
            cameraProviderFuture.addListener(listener, ContextCompat.getMainExecutor(ctx))

            onDispose {
                try {
                    analysisExecutor.shutdown()
                    scanner.close()
                } catch (_: Exception) { }
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChanged: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color(0xFFCCCCCC), fontSize = 13.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChanged,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF7C4DFF),
                checkedTrackColor = Color(0xFF7C4DFF).copy(alpha = 0.4f)
            )
        )
    }
}
