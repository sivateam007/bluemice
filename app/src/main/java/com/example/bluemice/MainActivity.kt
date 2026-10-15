package com.example.bluemice

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PresentToAll
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tablet
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.example.bluemice.model.ConnectionState
import com.example.bluemice.model.ConnectionStatus
import com.example.bluemice.ui.AccentCyan
import com.example.bluemice.ui.Dimens
import com.example.bluemice.ui.GamepadScreen
import com.example.bluemice.ui.KeyboardScreen
import com.example.bluemice.ui.MouseScreen
import com.example.bluemice.ui.MultimediaScreen
import com.example.bluemice.ui.PresenterScreen
import com.example.bluemice.ui.ScannerScreen
import com.example.bluemice.ui.ScreenPanel
import com.example.bluemice.ui.screenBackground
import com.example.bluemice.ui.theme.*
import androidx.compose.material3.MaterialTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var connectionManager: ConnectionManager
    private lateinit var bleManager: BleHidManager
    private lateinit var layoutManager: LayoutManager
    private lateinit var settingsManager: SettingsManager
    private var serviceStarted = false

    private val darkMode = mutableStateOf(true)
    private val themeColor = mutableStateOf("default")
    private val outlineTheme = mutableStateOf(false)

    private val connectionState = mutableStateOf(ConnectionState())
    private val pairedDevices = mutableStateOf<List<BluetoothDevice>>(emptyList())
    private val showAllDevices = mutableStateOf(false)
    private val isAdvertising = mutableStateOf(false)
    // Whether the Bluetooth runtime permissions are still missing. The app used to
    // request them silently at launch and then carry on if they were denied, so
    // advertising simply never started with no explanation anywhere in the UI.
    private val bluetoothPermissionsMissing = mutableStateOf(true)

    private val connectionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BleHidService.BROADCAST_CONNECTION -> {
                    val connected = intent.getBooleanExtra(BleHidService.EXTRA_CONNECTED, false)
                    val name = intent.getStringExtra(BleHidService.EXTRA_CONN_DEVICE_NAME)
                    val addr = intent.getStringExtra(BleHidService.EXTRA_CONN_DEVICE_ADDR)
                    if (connected && name != null && addr != null) {
                        connectionManager.onConnected(name, addr)
                    } else {
                        connectionManager.onDisconnected()
                    }
                }
                BleHidService.BROADCAST_ADVERTISING -> {
                    // Trust the service, not our optimistic local flag - otherwise the
                    // header can claim we're advertising while nothing is on air.
                    isAdvertising.value = intent.getBooleanExtra(BleHidService.EXTRA_ADVERTISING, false)
                }
                BleHidService.BROADCAST_ERROR -> {
                    val msg = intent.getStringExtra(BleHidService.EXTRA_ERROR_MSG) ?: "Unknown error"
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        connectionManager = ConnectionManager(this)
        bleManager = BleHidManager(this)
        layoutManager = LayoutManager(this)
        settingsManager = SettingsManager(this)

        darkMode.value = settingsManager.darkMode
        themeColor.value = settingsManager.themeColor
        outlineTheme.value = settingsManager.outlineTheme

        connectionManager.onStateChanged = { state -> connectionState.value = state }

        registerReceiver(connectionReceiver,
            IntentFilter().apply {
                addAction(BleHidService.BROADCAST_CONNECTION)
                addAction(BleHidService.BROADCAST_ADVERTISING)
                addAction(BleHidService.BROADCAST_ERROR)
            },
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) RECEIVER_NOT_EXPORTED else 0
        )

        checkAndRequestPermissions()
        showAllDevices.value = connectionManager.isShowAllDevices()

        setContent {
            BluemiceTheme(
                darkTheme = darkMode.value,
                themeColor = themeColor.value,
                outlineTheme = outlineTheme.value
            ) {
                MainScreen(
                    connectionState = connectionState.value,
                    pairedDevices = pairedDevices.value,
                    showAllDevices = showAllDevices.value,
                    isAdvertising = isAdvertising.value,
                    bleManager = bleManager,
                    layoutManager = layoutManager,
                    settingsManager = settingsManager,
                    darkMode = darkMode.value,
                    themeColor = themeColor.value,
                    outlineTheme = outlineTheme.value,
                    onDarkModeChange = { darkMode.value = it; settingsManager.darkMode = it },
                    onThemeColorChange = { themeColor.value = it; settingsManager.themeColor = it },
                    onOutlineThemeChange = { outlineTheme.value = it; settingsManager.outlineTheme = it },
                    onRefreshDevices = { refreshPairedDevices() },
                    onPairNewDevice = { connectionManager.openBluetoothSettings() },
                    onToggleShowAll = { toggleShowAll() },
                    onToggleAdvertising = { toggleAdvertising() },
                    onDisconnectFromPc = { disconnectFromPc() },
                    onRestartAdvertising = { restartAdvertising() },
                    onEnableBluetooth = { requestBluetoothEnable() },
                    permissionsMissing = bluetoothPermissionsMissing.value,
                    onRequestPermissions = { checkAndRequestPermissions() },
                    onOpenAppSettings = { openAppSettings() }
                )
            }
        }
    }

    override fun onDestroy() {
        unregisterReceiver(connectionReceiver)
        bleManager.stopService()
        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return handleVolumeKey(keyCode, true) || super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        return handleVolumeKey(keyCode, false) || super.onKeyUp(keyCode, event)
    }

    private fun handleVolumeKey(keyCode: Int, isDown: Boolean): Boolean {
        val isConnected = connectionState.value.status == ConnectionStatus.Connected
        if (!isConnected) return false

        val action = when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> settingsManager.volUpButton
            KeyEvent.KEYCODE_VOLUME_DOWN -> settingsManager.volDownButton
            else -> return false
        }

        if (action == "system") return false

        val KC = HidReportBuilder.KeyCodes
        when (action) {
            "vol_up" -> {
                if (isDown) bleManager.sendConsumerPress(0xE9)
            }
            "vol_down" -> {
                if (isDown) bleManager.sendConsumerPress(0xEA)
            }
            "mouse_left" -> {
                if (isDown) bleManager.sendMouseClick(0x01) else bleManager.sendMouseClick(0x00)
            }
            "mouse_right" -> {
                if (isDown) bleManager.sendMouseClick(0x02) else bleManager.sendMouseClick(0x00)
            }
            "page_up" -> {
                if (isDown) bleManager.sendKeyPress(0, KC.KEY_PAGE_UP) else bleManager.sendKeyPress(0, 0.toByte())
            }
            "page_down" -> {
                if (isDown) bleManager.sendKeyPress(0, KC.KEY_PAGE_DOWN) else bleManager.sendKeyPress(0, 0.toByte())
            }
        }
        return true
    }

    private fun refreshPairedDevices() {
        val devices = if (showAllDevices.value) connectionManager.getPairedDevices()
                      else connectionManager.getDesktopDevices()
        pairedDevices.value = devices
        if (devices.isEmpty()) Toast.makeText(this, "No paired devices found.", Toast.LENGTH_SHORT).show()
    }

    private fun toggleShowAll() {
        showAllDevices.value = !showAllDevices.value
        connectionManager.setShowAllDevices(showAllDevices.value)
        refreshPairedDevices()
    }

    private fun toggleAdvertising() {
        if (isAdvertising.value) {
            bleManager.stopService()
            isAdvertising.value = false
            connectionManager.onDisconnected()
        } else {
            bleManager.startService()
            isAdvertising.value = true
        }
    }

    private fun restartAdvertising() {
        // Restart advertising only. Tearing the service down here would rebuild the
        // GATT server, regenerate the attribute handles the PC cached, and force a
        // re-pair - so we deliberately keep the server alive. ACTION_START already
        // performs a full stop+start of the advertiser itself.
        bleManager.startService()
    }

    private fun disconnectFromPc() {
        bleManager.disconnectDevice()
        connectionManager.onDisconnected()
    }

    /** Bluetooth runtime permissions the app cannot advertise without. */
    private fun bluetoothPermissionsMissing(): List<String> {
        val missing = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
                missing.add("Bluetooth connect")
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)
                missing.add("Bluetooth scan")
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED)
                missing.add("Bluetooth advertise")
        } else {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                missing.add("Location (used to find Bluetooth devices)")
        }
        return missing
    }

    private fun refreshBluetoothPermissionState() {
        bluetoothPermissionsMissing.value = bluetoothPermissionsMissing().isNotEmpty()
    }

    private fun openAppSettings() {
        startActivity(
            Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.fromParts("package", packageName, null)
            }
        )
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)
                permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED)
                permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
        } else {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            permissions.add(Manifest.permission.CAMERA)

        if (permissions.isNotEmpty()) {
            bluetoothPermissionsMissing.value = bluetoothPermissionsMissing().isNotEmpty()
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 100)
        } else {
            refreshBluetoothPermissionState()
            startHidService()
            refreshPairedDevices()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            refreshBluetoothPermissionState()
            startHidService()
            refreshPairedDevices()
        }
    }

    override fun onResume() {
        super.onResume()
        // Grants made in the system Settings screen are only visible here.
        refreshBluetoothPermissionState()
    }

    private fun requestBluetoothEnable() {
        startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
    }

    private fun startHidService() {
        if (serviceStarted) return
        val adapter = connectionManager.adapter
        if (adapter == null || !adapter.isEnabled) {
            // Deliberately do NOT latch serviceStarted here: this is reached from the
            // permission callback, so Bluetooth being switched on later (or a permission
            // granted after the fact) must still be able to start advertising.
            Toast.makeText(this, "Please enable Bluetooth", Toast.LENGTH_LONG).show()
            return
        }
        serviceStarted = true
        if (settingsManager.autoConnect && !isAdvertising.value) {
            // Optimistic: the service confirms (or corrects) this via BROADCAST_ADVERTISING.
            isAdvertising.value = true
            bleManager.startService()
        }
    }
}

private data class Tab(val title: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("Mouse", Icons.Default.Mouse),
    Tab("Keyboard", Icons.Default.Keyboard),
    Tab("Gamepad", Icons.Default.Gamepad),
    Tab("Media", Icons.Default.MusicNote),
    Tab("Presenter", Icons.Default.PresentToAll),
    Tab("Scanner", Icons.Default.QrCodeScanner)
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(
    connectionState: ConnectionState,
    pairedDevices: List<BluetoothDevice>,
    showAllDevices: Boolean,
    isAdvertising: Boolean,
    bleManager: BleHidManager,
    layoutManager: LayoutManager,
    settingsManager: SettingsManager,
    darkMode: Boolean,
    themeColor: String,
    outlineTheme: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    onThemeColorChange: (String) -> Unit,
    onOutlineThemeChange: (Boolean) -> Unit,
    onRefreshDevices: () -> Unit,
    onPairNewDevice: () -> Unit,
    onToggleShowAll: () -> Unit,
    onToggleAdvertising: () -> Unit,
    onRestartAdvertising: () -> Unit,
    onDisconnectFromPc: () -> Unit,
    onEnableBluetooth: () -> Unit,
    permissionsMissing: Boolean,
    onRequestPermissions: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var manualConnected by remember { mutableStateOf(false) }
    val isConnected = connectionState.status == ConnectionStatus.Connected || manualConnected
    val connectedName = connectionState.deviceName ?: "PC"
    val pagerState = rememberPagerState(pageCount = { tabs.size })

    var isFullScreen by remember { mutableStateOf(settingsManager.fullscreen) }
    var pointerSpeed by remember { mutableStateOf(settingsManager.pointerSpeed) }
    var scrollSpeed by remember { mutableStateOf(settingsManager.scrollSpeed) }
    var isLandscape by remember { mutableStateOf(settingsManager.landscape) }
    var keepScreenOn by remember { mutableStateOf(settingsManager.keepScreenOn) }
    var overLockScreen by remember { mutableStateOf(settingsManager.overLockScreen) }
    var brightness by remember { mutableStateOf(settingsManager.screenBrightness) }
    var hapticsEnabled by remember { mutableStateOf(settingsManager.hapticsEnabled) }
    var autoConnect by remember { mutableStateOf(settingsManager.autoConnect) }
    var pagerSwipeLocked by remember { mutableStateOf(settingsManager.pagerSwipeLocked) }
    var layoutsVersion by remember { mutableIntStateOf(0) }
    var importResult by remember { mutableStateOf<String?>(null) }
    var renameTarget by remember { mutableStateOf<Pair<String, String>?>(null) }
    var deleteTarget by remember { mutableStateOf<Pair<String, String>?>(null) }
    // Dismissing play mode. Kept separate from the fullscreen setting so the
    // chevron and the header's expand button are a symmetric in/out pair and
    // neither of them has to reach into the drawer to work.
    var gamepadPlayHidden by remember { mutableStateOf(false) }
    // Read the real device orientation rather than the "force landscape"
    // preference - play mode must follow how the phone is actually held.
    val actualOrientation = LocalConfiguration.current.orientation
    // `isConnected` is part of the condition, not an oversight: the connection
    // and advertising screens live behind the same pager, and with no top bar
    // they would have no hamburger and no way out.
    val gamepadPlayAvailable =
        isFullScreen && actualOrientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE &&
            selectedTab == 2 && isConnected
    val gamepadPlayMode = gamepadPlayAvailable && !gamepadPlayHidden
    // Leaving the conditions clears the dismissal, so returning to landscape +
    // Fullscreen + the gamepad starts you back in play mode instead of stuck
    // on a dismissal you made three tabs ago.
    LaunchedEffect(gamepadPlayAvailable) {
        if (!gamepadPlayAvailable) gamepadPlayHidden = false
    }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) runCatching {
            context.contentResolver.openOutputStream(uri)?.use { os -> os.write(layoutManager.exportAllLayouts().toByteArray()) }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runCatching {
            val json = context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } ?: return@runCatching
            val n = layoutManager.importAllLayouts(json)
            layoutsVersion++
            importResult = if (n > 0) "Imported $n layout${if (n == 1) "" else "s"}"
                           else "Import failed - not a valid BlueMice file"
        }
    }

    val view = LocalView.current

    LaunchedEffect(selectedTab) {
        pagerState.animateScrollToPage(selectedTab)
    }

    // Keep the drawer highlight in sync when the user swipes between tabs
    // (previously one-way, so the drawer went stale after a swipe).
    LaunchedEffect(pagerState.currentPage) {
        if (selectedTab != pagerState.currentPage) selectedTab = pagerState.currentPage
    }

    // Import feedback (was completely silent before).
    LaunchedEffect(importResult) {
        importResult?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
            importResult = null
        }
    }

    // Restore persisted landscape orientation on launch.
    LaunchedEffect(Unit) {
        if (isLandscape) {
            (view.context as? android.app.Activity)?.requestedOrientation =
                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }
    LaunchedEffect(isFullScreen) {
        val window = (view.context as? android.app.Activity)?.window ?: return@LaunchedEffect
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val ctrl = window.insetsController
            if (ctrl != null) {
                if (isFullScreen) {
                    ctrl.hide(android.view.WindowInsets.Type.systemBars())
                    ctrl.systemBarsBehavior = android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                } else {
                    ctrl.show(android.view.WindowInsets.Type.systemBars())
                }
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = if (isFullScreen) {
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                    android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            } else {
                android.view.View.SYSTEM_UI_FLAG_VISIBLE
            }
        }
    }
    LaunchedEffect(keepScreenOn) {
        val window = (view.context as? android.app.Activity)?.window ?: return@LaunchedEffect
        if (keepScreenOn) {
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        settingsManager.keepScreenOn = keepScreenOn
    }
    LaunchedEffect(overLockScreen) {
        val activity = view.context as? android.app.Activity ?: return@LaunchedEffect
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            activity.setShowWhenLocked(overLockScreen)
        } else {
            val window = activity.window
            if (overLockScreen) {
                window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
            } else {
                window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
            }
        }
        settingsManager.overLockScreen = overLockScreen
    }
    LaunchedEffect(brightness) {
        val window = (view.context as? android.app.Activity)?.window ?: return@LaunchedEffect
        window.attributes = window.attributes.apply { screenBrightness = brightness }
        settingsManager.screenBrightness = brightness
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !pagerSwipeLocked && !gamepadPlayMode,
        scrimColor = Overlay,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(280.dp),
                drawerContainerColor = SurfaceDark
            ) {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Gradient header
                    Box(
                        Modifier.fillMaxWidth().background(
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                listOf(NavyPrimary, BlueSecondary)
                            )
                        ).padding(20.dp).statusBarsPadding()
                    ) {
                        Column {
                            Text("BlueMice", fontSize = 26.sp, fontWeight = FontWeight.Bold,
                                color = Color.White)
                            Text("BLE HID Controller", fontSize = 11.sp, color = TextPrimary.copy(alpha = 0.85f))
                            Spacer(Modifier.height(10.dp))
                            if (isConnected) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(SuccessGreen))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Connected to $connectedName", fontSize = 12.sp,
                                        color = TextPrimary.copy(alpha = 0.85f))
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(GitRed))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Disconnected", fontSize = 12.sp,
                                        color = TextPrimary.copy(alpha = 0.65f))
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    Text("NAVIGATION", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))

                    tabs.forEachIndexed { index, tab ->
                        val isSelected = selectedTab == index
                        Box(
                            Modifier.fillMaxWidth().padding(horizontal = 8.dp).clip(RoundedCornerShape(10.dp))
                                .background(
                                    // Themed tint, not a hardcoded purple — the old
                                    // 0xFF2A1A4A clashed with every non-purple accent.
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                    else Color.Transparent
                                )
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    selectedTab = index
                                    scope.launch { drawerState.close() }
                                }
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(tab.icon, null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else TextSecondary,
                                        modifier = Modifier.size(20.dp))
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(tab.title,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else TextPrimary,
                                    fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
                                if (isSelected) {
                                    Spacer(Modifier.weight(1f))
                                    Box(Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.primary))
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    androidx.compose.material3.HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 16.dp))

                    // Settings section
                    Text("SETTINGS", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))

                    Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(CardSurface).padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Full screen toggle
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("Full Screen", color = TextPrimary, fontSize = 13.sp,
                                    modifier = Modifier.weight(1f))
                                Switch(checked = isFullScreen, onCheckedChange = { isFullScreen = it; settingsManager.fullscreen = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer))
                            }

                            // Pointer speed
                            Column {
                                Text("Pointer Speed: ${"%.1f".format(pointerSpeed)}x",
                                    color = TextPrimary, fontSize = 13.sp)
                                androidx.compose.material3.Slider(
                                    value = pointerSpeed, onValueChange = { pointerSpeed = it.coerceIn(0.5f, 5f); settingsManager.pointerSpeed = pointerSpeed },
                                    valueRange = 0.5f..5f, steps = 8,
                                    colors = androidx.compose.material3.SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }

                            // Scroll speed
                            Column {
                                Text("Scroll Speed: ${"%.1f".format(scrollSpeed)}x",
                                    color = TextPrimary, fontSize = 13.sp)
                                androidx.compose.material3.Slider(
                                    value = scrollSpeed, onValueChange = { scrollSpeed = it.coerceIn(0.5f, 5f); settingsManager.scrollSpeed = scrollSpeed },
                                    valueRange = 0.5f..5f, steps = 8,
                                    colors = androidx.compose.material3.SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }

                            // Orientation toggle
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("Landscape Mode", color = TextPrimary, fontSize = 13.sp,
                                    modifier = Modifier.weight(1f))
                                Switch(checked = isLandscape, onCheckedChange = { v ->
                                    isLandscape = v
                                    settingsManager.landscape = v
                                    val act = view.context as? android.app.Activity
                                    if (v) {
                                        act?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                    } else {
                                        act?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                    }
                                },
                                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer))
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // Display settings
                    Text("DISPLAY", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))

                    Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(CardSurface).padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Theme color
                            Column {
                                Text("Theme Color", color = TextPrimary, fontSize = 13.sp)
                                Spacer(Modifier.height(4.dp))
                                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("default","red","purple","indigo","blue","green","yellow","orange","brown","grey","blue_grey","dynamic").forEach { c ->
                                        val chipColor = if (c == "dynamic") MaterialTheme.colorScheme.primary else accentColorFor(c)
                                        Box(Modifier.size(28.dp).clip(CircleShape)
                                            .border(BorderStroke(if (themeColor == c) 2.dp else 1.dp, if (themeColor == c) Color.White else DividerColor), CircleShape)
                                            .background(chipColor)
                                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                                onThemeColorChange(c)
                                            })
                                    }
                                }
                            }

                            // Dark mode
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("Dark Mode", color = TextPrimary, fontSize = 13.sp,
                                    modifier = Modifier.weight(1f))
                                Switch(checked = darkMode, onCheckedChange = onDarkModeChange,
                                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer))
                            }

                            // Tron style
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("Tron Style", color = TextPrimary, fontSize = 13.sp,
                                    modifier = Modifier.weight(1f))
                                Switch(checked = outlineTheme, onCheckedChange = onOutlineThemeChange,
                                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer))
                            }

                            // Brightness
                            Column {
                                Text("Brightness: ${"%.0f".format(brightness * 100)}%",
                                    color = TextPrimary, fontSize = 13.sp)
                                androidx.compose.material3.Slider(
                                    value = brightness, onValueChange = { brightness = it.coerceIn(0.1f, 1f) },
                                    valueRange = 0.1f..1f,
                                    colors = androidx.compose.material3.SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }

                            // Haptics
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("Haptic Feedback", color = TextPrimary, fontSize = 13.sp,
                                    modifier = Modifier.weight(1f))
                                Switch(checked = hapticsEnabled, onCheckedChange = {
                                    hapticsEnabled = it; settingsManager.hapticsEnabled = it
                                },
                                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer))
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // Screen settings
                    Text("SCREEN", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))

                    Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(CardSurface).padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("Keep Screen On", color = TextPrimary, fontSize = 13.sp,
                                    modifier = Modifier.weight(1f))
                                Switch(checked = keepScreenOn, onCheckedChange = { keepScreenOn = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer))
                            }
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("Show Over Lock Screen", color = TextPrimary, fontSize = 13.sp,
                                    modifier = Modifier.weight(1f))
                                Switch(checked = overLockScreen, onCheckedChange = { overLockScreen = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer))
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // Connection settings
                    Text("CONNECTION", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))

                    Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(CardSurface).padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(Modifier.fillMaxWidth()) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text("Auto-Connect", color = TextPrimary, fontSize = 13.sp,
                                        modifier = Modifier.weight(1f))
                                    Switch(checked = autoConnect, onCheckedChange = { autoConnect = it; settingsManager.autoConnect = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary,
                                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer))
                                }
                                Text("Start advertising on launch", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    androidx.compose.material3.HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 16.dp))

                    // My Layouts - grouped by mode
                    val modeTabIndex = remember {
                        mapOf("mouse" to 0, "keyboard" to 1, "gamepad" to 2, "media" to 3, "presenter" to 4)
                    }
                    val allCustomLayouts = remember(layoutsVersion) {
                        layoutManager.run {
                            listOf("mouse","keyboard","gamepad","media","presenter")
                                .flatMap { mode -> getCustomLayouts(mode).map { l -> Triple(mode, l.name, l) } }
                        }
                    }
                    val duplicateLayout: (String, String) -> Unit = { m, name ->
                        var copyName = "$name (copy)"
                        var i = 2
                        while (layoutManager.getAllLayouts(m).any { it.name == copyName }) {
                            copyName = "$name (copy $i)"; i++
                        }
                        layoutManager.duplicateLayout(m, name, copyName)
                        layoutsVersion++
                    }
                    // Always show the section so Import is reachable for new users,
                    // and export a fixed "all-mode" filename (not whichever layout
                    // happened to be first).
                    run {
                        Text("MY LAYOUTS", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                        Row(horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(80.dp).padding(4.dp).clickable { exportLauncher.launch("bluemice-all-layouts.json") }) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                    Text("Export All", color = MaterialTheme.colorScheme.primary, fontSize = 9.sp)
                                }
                            }
                            Box(modifier = Modifier.size(80.dp).padding(4.dp).clickable { importLauncher.launch(arrayOf("application/json")) }) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                    Text("Import All", color = MaterialTheme.colorScheme.primary, fontSize = 9.sp)
                                }
                            }
                        }
                        if (allCustomLayouts.isEmpty()) {
                            Text("No custom layouts yet. Tap Import, or use +Key in any screen's edit mode.",
                                color = TextSecondary, fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp))
                        }
                        Spacer(Modifier.height(4.dp))
                        allCustomLayouts.forEach { (mode, _, layout) ->
                            Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                                Row(
                                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                        .background(CardSurface)
                                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                            layoutManager.setCurrentLayoutName(mode, layout.name)
                                            selectedTab = modeTabIndex[mode] ?: 0
                                            scope.launch { drawerState.close() }
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center) {
                                        Icon(modeIcon(mode), null, tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(layout.name, color = TextPrimary, fontSize = 13.sp)
                                        Text(mode.replaceFirstChar { it.uppercase() }, color = TextSecondary, fontSize = 9.sp)
                                    }
                                    Spacer(Modifier.weight(1f))
                                    IconButton(onClick = { renameTarget = mode to layout.name },
                                        modifier = Modifier.size(30.dp)) {
                                        Icon(Icons.Default.Edit, "Rename", tint = TextSecondary, modifier = Modifier.size(15.dp))
                                    }
                                    IconButton(onClick = { duplicateLayout(mode, layout.name) },
                                        modifier = Modifier.size(30.dp)) {
                                        Icon(Icons.Default.ContentCopy, "Duplicate", tint = TextSecondary, modifier = Modifier.size(15.dp))
                                    }
                                    IconButton(onClick = { deleteTarget = mode to layout.name },
                                        modifier = Modifier.size(30.dp)) {
                                        Icon(Icons.Default.Delete, "Delete", tint = GitRed, modifier = Modifier.size(15.dp))
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        androidx.compose.material3.HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 16.dp))
                    }

                    if (isConnected) {
                        Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF3A1A1A))
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                        manualConnected = false
                                        onDisconnectFromPc()
                                        scope.launch { drawerState.close() }
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                                    .background(GitRed.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.BluetoothDisabled, null, tint = GitRed,
                                        modifier = Modifier.size(20.dp))
                                }
                                Spacer(Modifier.width(10.dp))
                                Text("Disconnect", color = GitRed, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.weight(1f))
                                Icon(Icons.Default.BluetoothDisabled, null, tint = GitRed.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Spacer(Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(bottom = 16.dp).fillMaxWidth()) {
                        Text("Developed by Siva", fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        Text("v1.0", fontSize = 10.sp, color = TextSecondary)
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                // Play mode drops the activity bar as well. Without this the pad
                // would still sit under the hamburger, mode chevrons and the
                // disconnect button - which on a landscape phone is most of the
                // viewport gone before a single key is drawn.
                if (!gamepadPlayMode) {
                Column(Modifier.statusBarsPadding()) {
                    if (isConnected) {
                        Surface(color = NavyPrimary, shadowElevation = 8.dp) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, "Menu", tint = TextSecondary, modifier = Modifier.size(22.dp))
                                }
                                // Mode on top, connection under it - the pager can hold six
                                // screens, so the old single line never told you which.
                                // Chevrons give one-tap paging; tapping the name opens the
                                // drawer. Previously the hamburger and a hidden swipe were
                                // the only way to reach another screen.
                                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { selectedTab-- },
                                        enabled = selectedTab > 0,
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(Icons.Default.NavigateBefore, "Previous mode",
                                            tint = TextSecondary, modifier = Modifier.size(24.dp))
                                    }
                                    Column(
                                        Modifier.weight(1f)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) { scope.launch { drawerState.open() } }
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                tabs[selectedTab].title,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextPrimary
                                            )
                                            Icon(Icons.Default.ArrowDropDown, null,
                                                tint = TextSecondary, modifier = Modifier.size(18.dp))
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(Modifier.size(7.dp).clip(RoundedCornerShape(4.dp)).background(SuccessGreen))
                                            Spacer(Modifier.width(6.dp))
                                            Text(connectedName, fontSize = 11.sp, color = TextSecondary, maxLines = 1)
                                        }
                                    }
                                    IconButton(
                                        onClick = { selectedTab++ },
                                        enabled = selectedTab < tabs.size - 1,
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(Icons.Default.NavigateNext, "Next mode",
                                            tint = TextSecondary, modifier = Modifier.size(24.dp))
                                    }
                                }
                                IconButton(onClick = {
                                    pagerSwipeLocked = !pagerSwipeLocked
                                    settingsManager.pagerSwipeLocked = pagerSwipeLocked
                                }) {
                                    Icon(
                                        if (pagerSwipeLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                        if (pagerSwipeLocked) "Unlock swipe (next/previous layout)" else "Lock swipe (next/previous layout)",
                                        tint = if (pagerSwipeLocked) MaterialTheme.colorScheme.primary else TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Box(
                                    Modifier.clip(RoundedCornerShape(8.dp)).background(GitRed.copy(alpha = 0.15f))
                                        .clickable { manualConnected = false; onDisconnectFromPc() }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Disconnect", fontSize = 12.sp, color = GitRed, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                    if (!isConnected) {
                        Surface(color = NavyPrimary, shadowElevation = 4.dp) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 10.dp, bottom = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, "Menu", tint = TextSecondary, modifier = Modifier.size(22.dp))
                                }
                                if (isAdvertising) {
                                    Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(Periwinkle))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Advertising as BlueMice", fontSize = 12.sp, color = Periwinkle, fontWeight = FontWeight.Medium)
                                    Spacer(Modifier.weight(1f))
                                    Box(
                                        Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFFFFB74D).copy(alpha = 0.15f))
                                            .clickable { onRestartAdvertising() }
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text("Restart", fontSize = 12.sp, color = Color(0xFFFFB74D), fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(TextSecondary.copy(alpha = 0.5f)))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Not connected", fontSize = 12.sp, color = TextSecondary)
                                    Spacer(Modifier.weight(1f))
                                    Box(
                                        Modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.primary)
                                            .clickable { onToggleAdvertising() }
                                            .padding(horizontal = 18.dp, vertical = 8.dp)
                                    ) {
                                        Text("Connect", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
                }
            }
        ) { padding ->
            // Same backdrop as the six control screens, so the app opens on the
            // same visual language instead of a plain flat background.
            Box(
                modifier = Modifier.fillMaxSize()
                    // Play mode forgoes the Scaffold's inset entirely - with the
                    // system bars hidden it would still leave a dead strip above
                    // the pad, which is exactly what play mode exists to remove.
                    .then(if (gamepadPlayMode) Modifier else Modifier.padding(padding))
                    .screenBackground()
            ) {
                if (!isConnected && !isAdvertising) {
                    ConnectionScreen(
                        pairedDevices = pairedDevices,
                        showAllDevices = showAllDevices,
                        isAdvertising = isAdvertising,
                        onRefreshDevices = onRefreshDevices,
                        onPairNewDevice = onPairNewDevice,
                        onToggleShowAll = onToggleShowAll,
                        onToggleAdvertising = onToggleAdvertising,
                        onManualConnect = { manualConnected = true },
                        permissionsMissing = permissionsMissing,
                        onRequestPermissions = onRequestPermissions,
                        onOpenAppSettings = onOpenAppSettings
                    )
                } else if (!isConnected && isAdvertising) {
                    AdvertisingScreen(
                        onManualConnect = { manualConnected = true },
                        onRestartAdvertising = onRestartAdvertising,
                        onStopAdvertising = onToggleAdvertising
                    )
                } else {
                    HorizontalPager(
                        state = pagerState,
                        userScrollEnabled = !pagerSwipeLocked && !gamepadPlayMode,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        when (page) {
                            0 -> MouseScreen(
                                bleManager = bleManager,
                                pointerSpeed = pointerSpeed,
                                scrollSpeed = scrollSpeed,
                                onScrollSpeedChange = { scrollSpeed = it; settingsManager.scrollSpeed = it },
                                onPointerSpeedChange = { pointerSpeed = it; settingsManager.pointerSpeed = it },
                                layoutManager = layoutManager,
                                settingsManager = settingsManager
                            )
                            1 -> KeyboardScreen(
                                bleManager = bleManager,
                                layoutManager = layoutManager,
                                settingsManager = settingsManager
                            )
                            2 -> GamepadScreen(
                                bleManager = bleManager,
                                layoutManager = layoutManager,
                                settingsManager = settingsManager,
                                playMode = gamepadPlayMode,
                                playAvailable = gamepadPlayAvailable,
                                // Toggles in both directions: the chevron uses it to
                                // step out, the header's expand button to step back in.
                                onTogglePlayMode = { gamepadPlayHidden = !gamepadPlayHidden }
                            )
                            3 -> MultimediaScreen(
                                    bleManager = bleManager,
                                    layoutManager = layoutManager,
                                    settingsManager = settingsManager
                                )
                            4 -> PresenterScreen(
                                bleManager = bleManager,
                                layoutManager = layoutManager
                            )
                            5 -> ScannerScreen(
                                bleManager = bleManager,
                                settingsManager = settingsManager
                            )
                        }
                    }
                }
            }
        }
    }

    renameTarget?.let { (mode, name) ->
        var newName by remember(name) { mutableStateOf(name) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename layout") },
            text = {
                Column {
                    Text("Give '$name' a new name.", color = TextSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = newName, onValueChange = { newName = it },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = DividerColor))
                }
            },
            confirmButton = {
                TextButton(enabled = newName.isNotBlank() && newName.trim() != name, onClick = {
                    if (layoutManager.renameLayout(mode, name, newName.trim())) {
                        layoutsVersion++; renameTarget = null
                    }
                }) { Text("Rename", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("Cancel", color = TextSecondary) } }
        )
    }

    deleteTarget?.let { (mode, name) ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete layout?") },
            text = { Text("'$name' will be permanently removed.", color = TextSecondary, fontSize = 12.sp) },
            confirmButton = {
                TextButton(onClick = {
                    // Mode-scoped delete also repoints the current-layout pointer
                    // to that mode's real default (no more dangling "Default").
                    layoutManager.deleteCustomLayout(mode, name)
                    layoutsVersion++
                    deleteTarget = null
                }) { Text("Delete", color = GitRed) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel", color = TextSecondary) } }
        )
    }
}

@Composable
private fun AdvertisingScreen(
    onManualConnect: () -> Unit,
    onRestartAdvertising: () -> Unit,
    onStopAdvertising: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Branded hero instead of a bare 64dp icon floating on the background.
        Box(
            Modifier.size(92.dp)
                .clip(CircleShape)
                .background(AccentCyan.copy(alpha = 0.10f))
                .border(1.dp, AccentCyan.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Bluetooth, null,
                modifier = Modifier.size(44.dp),
                tint = AccentCyan
            )
        }
        Spacer(Modifier.height(18.dp))
        Text("BlueMice", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(SuccessGreen))
            Spacer(Modifier.width(7.dp))
            Text("Advertising as BlueMice", fontSize = 13.sp, color = TextSecondary)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Open Bluetooth settings on your PC and pair with\n\"BlueMice\" to start controlling it.",
            fontSize = 13.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 19.sp
        )
        Spacer(Modifier.height(26.dp))

        ScreenPanel(Modifier.fillMaxWidth()) {
            Column(
                Modifier.fillMaxWidth().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Primary path first - this is the one almost everyone needs.
                Button(
                    onClick = onManualConnect,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Dimens.ControlRadius),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentCyan,
                        contentColor = Color(0xFF03222C)
                    )
                ) {
                    Icon(Icons.Default.BluetoothConnected, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Already connected? Show controls",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                OutlinedButton(
                    onClick = onRestartAdvertising,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Dimens.ControlRadius),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Restart advertising", fontSize = 13.sp, color = TextSecondary)
                }
                OutlinedButton(
                    onClick = onStopAdvertising,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Dimens.ControlRadius),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Text("Stop advertising", fontSize = 13.sp, color = TextSecondary)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "PC won't reconnect? Use Restart advertising.",
            fontSize = 11.sp,
            color = TextSecondary.copy(alpha = 0.75f)
        )
    }
}

@Composable
private fun ConnectionScreen(
    pairedDevices: List<BluetoothDevice>,
    showAllDevices: Boolean,
    isAdvertising: Boolean,
    onRefreshDevices: () -> Unit,
    onPairNewDevice: () -> Unit,
    onToggleShowAll: () -> Unit,
    onToggleAdvertising: () -> Unit,
    onManualConnect: () -> Unit,
    permissionsMissing: Boolean = false,
    onRequestPermissions: () -> Unit = {},
    onOpenAppSettings: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(12.dp))
        // Same accent-bar lockup as ScreenHeader on the control screens.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.width(3.dp).height(36.dp)
                    .clip(RoundedCornerShape(2.dp)).background(AccentCyan)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    "BlueMice Controller",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    "BLE HID mouse, keyboard, gamepad & tools",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        // Denied permissions used to fail silently: the service never started and
        // nothing in the UI ever said why.
        if (permissionsMissing) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = GitRed.copy(alpha = 0.12f)),
                border = BorderStroke(1.dp, GitRed.copy(alpha = 0.55f)),
                shape = RoundedCornerShape(Dimens.ControlRadius)
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = GitRed, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Bluetooth permissions needed",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "BlueMice advertises itself as a Bluetooth device so your PC can " +
                            "pair with it. Allow the permissions to continue.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onRequestPermissions,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(Dimens.ControlRadius),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentCyan,
                                contentColor = Color(0xFF03222C)
                            )
                        ) { Text("Grant access", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                        OutlinedButton(
                            onClick = onOpenAppSettings,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(Dimens.ControlRadius),
                            border = BorderStroke(1.dp, CardBorder)
                        ) { Text("App settings", color = TextSecondary, fontSize = 13.sp) }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isAdvertising) MaterialTheme.colorScheme.primaryContainer
                                else CardSurface
            ),
            border = BorderStroke(1.dp, CardBorder),
            shape = RoundedCornerShape(Dimens.PanelRadius)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = if (isAdvertising) Icons.Default.Bluetooth
                                  else Icons.Default.BluetoothDisabled,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = if (isAdvertising) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (isAdvertising) "Advertising as BlueMice"
                           else "Tap Start to advertise",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                if (isAdvertising) {
                    Text(
                        text = "Your PC should now find 'BlueMice' as a new Bluetooth device.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onManualConnect, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.BluetoothConnected, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Already Connected? Show Controls")
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onToggleAdvertising,
                    modifier = Modifier.fillMaxWidth(),
                    colors = if (isAdvertising) ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                             ) else ButtonDefaults.buttonColors()
                ) { Text(if (isAdvertising) "Stop Advertising" else "Start Advertising") }
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(onClick = onRefreshDevices, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp)); Text("Refresh")
            }
            FilledTonalButton(onClick = onPairNewDevice, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Bluetooth, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp)); Text("Pair Device")
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Show all device types", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Switch(checked = showAllDevices, onCheckedChange = { onToggleShowAll() })
        }

        if (pairedDevices.isEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = if (showAllDevices) "No paired devices found."
                       else "No desktop devices found.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text("Pair from your PC or phone Bluetooth settings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center)
        }

        // Weighted so the permission banner above can't push the list off screen.
        LazyColumn(Modifier.weight(1f)) {
            items(pairedDevices) { device ->
                DeviceCard(device = device)
            }
        }
    }
}

@Composable
private fun DeviceCard(device: BluetoothDevice) {
    val category = ConnectionManager.getDeviceMajorCategory(device)
    val isDesktop = ConnectionManager.isDesktopDevice(device)
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = if (isDesktop) BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                 else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = deviceTypeIcon(category),
                    contentDescription = category,
                    tint = if (isDesktop) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(device.name ?: "Unknown Device",
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(device.address, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    Text(category, style = MaterialTheme.typography.labelSmall,
                        color = if (isDesktop) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private fun deviceTypeIcon(category: String): ImageVector {
    return when (category) {
        "Computer" -> Icons.Default.Laptop
        "Audio/Video" -> Icons.Default.MusicNote
        "Phone" -> Icons.Default.Tablet
        else -> Icons.Default.Bluetooth
    }
}

/** Correct icon per layout mode (previously every row showed a keyboard icon). */
private fun modeIcon(mode: String): ImageVector = when (mode) {
    "mouse" -> Icons.Default.Mouse
    "gamepad" -> Icons.Default.Gamepad
    "media" -> Icons.Default.MusicNote
    "presenter" -> Icons.Default.PresentToAll
    else -> Icons.Default.Keyboard
}
