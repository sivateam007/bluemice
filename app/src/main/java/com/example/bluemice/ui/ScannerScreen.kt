package com.example.bluemice.ui

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.bluemice.BleHidManager
import com.example.bluemice.HidReportBuilder
import com.example.bluemice.SettingsManager
import com.example.bluemice.model.KeyboardLanguageCatalog
import com.example.bluemice.ui.theme.CardBorder
import com.example.bluemice.ui.theme.GitRed
import com.example.bluemice.ui.theme.SuccessGreen
import com.example.bluemice.ui.theme.TextPrimary
import com.example.bluemice.ui.theme.TextSecondary
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
    val hasCamera = remember { ctx.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) }
    // This used to be a plain `remember { ... }`, i.e. a non-reactive Boolean read
    // exactly once - so granting the camera permission (here or in Settings) left
    // the screen stuck on "Camera permission required" until the app restarted.
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var previewViewRef = remember { mutableStateOf<PreviewView?>(null) }

    // Ask in place. The old flow only offered "Open Settings", which is a dead end
    // for a permission this routine should just prompt for.
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> permissionGranted = granted }

    // Returning from the Settings screen has to re-read the grant; the pager keeps
    // this composition alive, so `remember` alone never picks it up.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionGranted = ContextCompat.checkSelfPermission(
                    ctx, Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
            .screenBackground()
            .padding(16.dp)
    ) {
        // Same lockup as the six control screens - this was the odd one out.
        ScreenHeader(
            title = "Scanner",
            subtitle = "Scan barcodes and QR codes straight to the PC"
        )

        Spacer(Modifier.height(12.dp))

        if (!hasCamera) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No camera available", color = TextSecondary, fontSize = 14.sp)
            }
        } else if (!permissionGranted) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                ScreenPanel(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            Modifier.size(56.dp)
                                .clip(RoundedCornerShape(Dimens.PanelRadius))
                                .background(GitRed.copy(alpha = 0.14f))
                                .border(1.dp, GitRed.copy(alpha = 0.40f), RoundedCornerShape(Dimens.PanelRadius)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Warning, null, tint = GitRed, modifier = Modifier.size(26.dp))
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(
                            "Camera access needed",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "The scanner reads barcodes with your phone camera. " +
                                "Results are typed straight to the PC and nothing is uploaded.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 17.sp
                        )
                        Spacer(Modifier.height(16.dp))
                        // Primary path: prompt right here instead of exiling the user
                        // to Android Settings with no context.
                        Button(
                            onClick = { cameraLauncher.launch(Manifest.permission.CAMERA) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(Dimens.ControlRadius),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentCyan,
                                contentColor = Color(0xFF03222C)
                            )
                        ) {
                            Text("Allow camera access", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                ctx.startActivity(
                                    android.content.Intent(
                                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                    ).apply { data = android.net.Uri.fromParts("package", ctx.packageName, null) }
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(Dimens.ControlRadius),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                        ) {
                            Icon(Icons.Default.Settings, null, Modifier.size(16.dp), tint = TextSecondary)
                            Spacer(Modifier.width(6.dp))
                            Text("Open app settings", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF000000), RoundedCornerShape(12.dp))
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
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
                    color = if (scannedText.isEmpty()) TextSecondary else SuccessGreen,
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
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentCyan,
                            contentColor = Color(0xFF03222C)
                        ),
                        shape = RoundedCornerShape(Dimens.ControlRadius),
                        modifier = Modifier.weight(1f)
                    ) { Text("Type to PC", fontWeight = FontWeight.SemiBold) }

                    Button(
                        onClick = { scannedText = "" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GitRed.copy(alpha = 0.20f),
                            contentColor = GitRed
                        ),
                        shape = RoundedCornerShape(Dimens.ControlRadius),
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
        Text(label, color = TextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChanged,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AccentCyan,
                checkedTrackColor = AccentCyan.copy(alpha = 0.35f)
            )
        )
    }
}
