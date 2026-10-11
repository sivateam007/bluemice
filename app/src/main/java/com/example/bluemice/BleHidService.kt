package com.example.bluemice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import androidx.core.app.NotificationCompat
import java.util.UUID

/**
 * BLE HID *peripheral*: we advertise, the PC connects to us and we push reports.
 *
 * Reconnect design notes (these were the source of "pairs fine, never reconnects"):
 *  - The GATT server is deliberately kept alive across connections. Closing and
 *    reopening it regenerates the HID service's attribute handles, which the PC has
 *    cached from pairing - so its service discovery fails and the user is forced to
 *    re-pair. We only tear down on an explicit stop / Bluetooth-off / destroy.
 *  - Advertising is started from onServiceAdded(), i.e. only once the HID service is
 *    actually in the GATT database. Starting earlier lets a fast reconnecting PC
 *    discover an empty service table.
 *  - Advertising failures are retried with backoff instead of giving up after one toast.
 */
class BleHidService : Service() {

    companion object {
        private const val TAG = "BleHidService"
        private const val NOTIFICATION_ID = 1002
        private const val CHANNEL_ID = "ble_hid_channel"

        /** Delay before making sure advertising is back after a disconnect. */
        private const val RESTART_DELAY_MS = 2000L
        private const val DISCONNECT_ACTION_DELAY_MS = 1000L
        /** Bluetooth toggle churn needs a beat before the stack is ready again. */
        private const val BLUETOOTH_ON_DELAY_MS = 1500L

        /** 500ms, 1s, 2s, 4s, 8s */
        private const val MAX_ADVERTISE_RETRIES = 5
        private const val MAX_SETUP_RETRIES = 3

        const val ACTION_START = "com.example.bluemice.BLE_START"
        const val ACTION_STOP = "com.example.bluemice.BLE_STOP"
        const val ACTION_DISCONNECT = "com.example.bluemice.BLE_DISCONNECT"
        const val ACTION_SEND_REPORT = "com.example.bluemice.BLE_SEND_REPORT"
        const val EXTRA_REPORT_ID = "report_id"
        const val EXTRA_REPORT = "report"

        const val BROADCAST_CONNECTION = "com.example.bluemice.BLE_CONNECTION_STATE"
        const val EXTRA_CONNECTED = "connected"
        const val EXTRA_CONN_DEVICE_NAME = "conn_device_name"
        const val EXTRA_CONN_DEVICE_ADDR = "conn_device_addr"

        /** Real advertising state, so the UI never has to guess. */
        const val BROADCAST_ADVERTISING = "com.example.bluemice.BLE_ADVERTISING"
        const val EXTRA_ADVERTISING = "advertising"

        const val BROADCAST_ERROR = "com.example.bluemice.BLE_ERROR"
        const val EXTRA_ERROR_MSG = "error_msg"

        private val HID_SERVICE_UUID = UUID.fromString("00001812-0000-1000-8000-00805f9b34fb")
        private val REPORT_MAP_UUID = UUID.fromString("00002A4B-0000-1000-8000-00805f9b34fb")
        private val HID_INFO_UUID = UUID.fromString("00002A4A-0000-1000-8000-00805f9b34fb")
        private val HID_CONTROL_POINT_UUID = UUID.fromString("00002A4C-0000-1000-8000-00805f9b34fb")
        private val REPORT_UUID = UUID.fromString("00002A4D-0000-1000-8000-00805f9b34fb")
        private val PROTOCOL_MODE_UUID = UUID.fromString("00002A4E-0000-1000-8000-00805f9b34fb")
        private val REPORT_REF_DESCR_UUID = UUID.fromString("00002908-0000-1000-8000-00805f9b34fb")
        private val CLIENT_CHAR_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        private val HID_COUNTRY_CODE = 0x00.toByte()
    }

    private var gattServer: BluetoothGattServer? = null
    private var advertiser: BluetoothLeAdvertiser? = null
    private var connectedDevice: BluetoothDevice? = null
    private var reportCharacteristic: BluetoothGattCharacteristic? = null
    private var cccdEnabled = false

    /** True while the user wants this service running. */
    private var serviceActive = false
    /** True once the HID service is actually registered in the GATT database. */
    private var serviceReady = false
    private var isAdvertising = false

    private var advertiseRetryCount = 0
    private var setupRetryCount = 0
    private var restartRunnable: Runnable? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val btStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != BluetoothAdapter.ACTION_STATE_CHANGED) return
            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
            when (state) {
                BluetoothAdapter.STATE_OFF, BluetoothAdapter.STATE_TURNING_OFF -> {
                    // Stack is going away - the server must be rebuilt later, so this
                    // is the one disconnect path where a full teardown is correct.
                    cancelRestart()
                    teardownGatt()
                    connectedDevice = null
                    broadcastConnection(false, null, null)
                }
                BluetoothAdapter.STATE_ON -> {
                    if (serviceActive) {
                        mainHandler.postDelayed({ if (serviceActive) setupGattAndAdvertise() }, BLUETOOTH_ON_DELAY_MS)
                    }
                }
            }
        }
    }

    private val gattCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            Log.d(TAG, "onConnectionStateChange: status=$status newState=$newState device=${device.address}")
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                cancelRestart()
                connectedDevice = device
                broadcastConnection(true, device.name ?: "PC", device.address)
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                handleDisconnect(device)
            }
        }

        override fun onServiceAdded(status: Int, service: BluetoothGattService) {
            Log.d(TAG, "onServiceAdded: status=$status uuid=${service.uuid}")
            if (status == BluetoothGatt.GATT_SUCCESS && service.uuid == HID_SERVICE_UUID) {
                serviceReady = true
                setupRetryCount = 0
                // Only now is it safe to be discoverable - a PC that connects
                // immediately must not find an empty service table.
                mainHandler.post { if (serviceActive) startAdvertising() }
            } else {
                Log.e(TAG, "HID service registration failed, status=$status")
                broadcastError("HID service registration failed (status=$status)")
                scheduleSetupRetry()
            }
        }

        override fun onCharacteristicReadRequest(
            device: BluetoothDevice,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic
        ) {
            Log.d(TAG, "onCharacteristicReadRequest: uuid=${characteristic.uuid}")
            gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.value)
        }

        override fun onDescriptorReadRequest(
            device: BluetoothDevice,
            requestId: Int,
            offset: Int,
            descriptor: BluetoothGattDescriptor
        ) {
            Log.d(TAG, "onDescriptorReadRequest")
            gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, descriptor.value)
        }

        override fun onDescriptorWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            descriptor: BluetoothGattDescriptor,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            Log.d(TAG, "onDescriptorWriteRequest: uuid=${descriptor.uuid} value=${value.toHexString()}")
            if (descriptor.uuid == CLIENT_CHAR_CONFIG_UUID && value.size >= 2) {
                cccdEnabled = value[0] != 0x00.toByte()
                Log.d(TAG, "CCCD enabled=$cccdEnabled")
            }
            if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null)
            }
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            Log.d(TAG, "onCharacteristicWriteRequest: uuid=${characteristic.uuid} value=${value.toHexString()}")
            if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null)
            }
        }

        override fun onNotificationSent(device: BluetoothDevice, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Notification send failed: status=$status")
            }
        }
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Log.d(TAG, "Advertising started successfully")
            isAdvertising = true
            advertiseRetryCount = 0
            broadcastAdvertising(true)
        }

        override fun onStartFailure(errorCode: Int) {
            val msg = when (errorCode) {
                AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED -> "Already advertising"
                AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE -> "Advertise data too large"
                AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "Advertising not supported on this device"
                AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR -> "Internal advertising error"
                AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "Too many advertisers"
                else -> "Unknown advertising error: $errorCode"
            }

            // The stack already has us advertising under this same callback, so this
            // is a success, not a failure. Treating it as an error used to leave the
            // UI and the retry logic convinced we were offline while we were not.
            if (errorCode == AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED) {
                Log.w(TAG, "Advertising already started - treating as active")
                isAdvertising = true
                advertiseRetryCount = 0
                broadcastAdvertising(true)
                return
            }

            isAdvertising = false
            broadcastAdvertising(false)
            Log.e(TAG, "Advertising failed: errorCode=$errorCode ($msg)")
            scheduleAdvertiseRetry(msg)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        registerReceiver(btStateReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // START_STICKY can revive us with a null intent. Treating that as a fresh
        // start keeps advertising alive after the system kills the process;
        // previously it fell through every branch and the phone went deaf.
        val action = intent?.action ?: ACTION_START
        when (action) {
            ACTION_START -> {
                serviceActive = true
                advertiseRetryCount = 0
                setupRetryCount = 0
                // Force a fresh advertise cycle: ACTION_START doubles as
                // "restart advertising", which is what the Restart button is for.
                isAdvertising = false
                setupGattAndAdvertise()
            }
            ACTION_STOP -> {
                serviceActive = false
                cancelRestart()
                teardownGatt()
                connectedDevice = null
                broadcastConnection(false, null, null)
            }
            ACTION_DISCONNECT -> {
                connectedDevice?.let { dev ->
                    try {
                        gattServer?.cancelConnection(dev)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to cancel GATT connection", e)
                    }
                }
                connectedDevice = null
                resetSubscriptionState()
                broadcastConnection(false, null, null)
                // Guarantee we advertise again even if the stack never delivers
                // STATE_DISCONNECTED for a connection we cancelled ourselves.
                scheduleRestart(DISCONNECT_ACTION_DELAY_MS)
            }
            ACTION_SEND_REPORT -> {
                val reportId = intent?.getByteExtra(EXTRA_REPORT_ID, 0) ?: 0
                val report = intent?.getByteArrayExtra(EXTRA_REPORT)
                if (report != null) sendReport(reportId, report)
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        unregisterReceiver(btStateReceiver)
        cancelRestart()
        teardownGatt()
        mainHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    // ---------------------------------------------------------------- lifecycle

    private fun setupGattAndAdvertise() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter
        if (adapter == null || !adapter.isEnabled) {
            broadcastAdvertising(false)
            broadcastError("Bluetooth is not enabled")
            return
        }

        if (gattServer != null && serviceReady) {
            // Healthy server already up (ACTION_START after a running service, or a
            // Bluetooth re-entry). Rebuilding it here would invalidate the handles
            // the PC cached, which is exactly what forced the re-pair.
            startAdvertising()
            return
        }

        if (gattServer != null) {
            Log.d(TAG, "Replacing an incomplete GATT server")
            teardownGatt()
        }

        gattServer = bluetoothManager.openGattServer(this, gattCallback)
        if (gattServer == null) {
            broadcastAdvertising(false)
            broadcastError("Failed to create GATT server")
            scheduleSetupRetry()
            return
        }
        serviceReady = false

        val hidService = BluetoothGattService(HID_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)

        val protocolMode = BluetoothGattCharacteristic(
            PROTOCOL_MODE_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
            BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE
        ).apply { value = byteArrayOf(1) }
        hidService.addCharacteristic(protocolMode)

        val reportMap = BluetoothGattCharacteristic(
            REPORT_MAP_UUID, BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
        ).apply { value = HidReportBuilder.reportDescriptor }
        hidService.addCharacteristic(reportMap)

        val hidInfo = BluetoothGattCharacteristic(
            HID_INFO_UUID, BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
        ).apply { value = byteArrayOf(0x01, 0x11, 0x00, HID_COUNTRY_CODE) }
        hidService.addCharacteristic(hidInfo)

        val hidControlPoint = BluetoothGattCharacteristic(
            HID_CONTROL_POINT_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        hidService.addCharacteristic(hidControlPoint)

        addSingleReportCharacteristic(hidService)

        val ok = gattServer?.addService(hidService) ?: false
        if (!ok) {
            broadcastAdvertising(false)
            broadcastError("Failed to add HID service to GATT server")
            scheduleSetupRetry()
            return
        }

        gattServer?.getService(HID_SERVICE_UUID)?.let { svc ->
            for (char in svc.characteristics) {
                Log.d(TAG, "Service characteristic: uuid=${char.uuid}, properties=${char.properties}")
            }
        }
        // Advertising starts in onServiceAdded() once the service is registered.
    }

    private fun addSingleReportCharacteristic(service: BluetoothGattService) {
        val report = BluetoothGattCharacteristic(
            REPORT_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ or
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY or
                    BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
            BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE
        ).apply {
            value = ByteArray(0)
            addDescriptor(
                BluetoothGattDescriptor(
                    CLIENT_CHAR_CONFIG_UUID,
                    BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
                )
            )
            addDescriptor(
                BluetoothGattDescriptor(REPORT_REF_DESCR_UUID, BluetoothGattDescriptor.PERMISSION_READ)
                    .apply { value = byteArrayOf(0, 0x01) }
            )
        }
        service.addCharacteristic(report)
        reportCharacteristic = report
    }

    private fun startAdvertising() {
        if (!serviceActive || isAdvertising) return

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter
        if (adapter == null || !adapter.isEnabled) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(android.Manifest.permission.BLUETOOTH_ADVERTISE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                broadcastAdvertising(false)
                broadcastError("BLUETOOTH_ADVERTISE permission not granted")
                return
            }
        }

        try {
            val oldName = adapter.name
            if (oldName != "BlueMice") {
                adapter.name = "BlueMice"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Could not set adapter name", e)
        }

        // Drop any stale registration from a previous cycle before starting again.
        try {
            if (advertiser != null) advertiser?.stopAdvertising(advertiseCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop stale advertiser", e)
        }
        isAdvertising = false

        advertiser = adapter.bluetoothLeAdvertiser
        if (advertiser == null) {
            broadcastAdvertising(false)
            broadcastError("BLE advertising not available on this device")
            return
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(true)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .build()

        val scanResponse = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(HID_SERVICE_UUID))
            .build()

        try {
            advertiser?.startAdvertising(settings, data, scanResponse, advertiseCallback)
        } catch (e: Exception) {
            try {
                advertiser?.startAdvertising(settings, data, advertiseCallback)
            } catch (e2: Exception) {
                Log.e(TAG, "startAdvertising threw", e2)
                scheduleAdvertiseRetry(e2.message ?: "exception")
            }
        }
    }

    private fun stopAdvertising() {
        try {
            advertiser?.stopAdvertising(advertiseCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping advertising", e)
        }
        isAdvertising = false
    }

    private fun teardownGatt() {
        stopAdvertising()
        broadcastAdvertising(false)
        serviceReady = false
        reportCharacteristic = null
        resetSubscriptionState()
        try {
            gattServer?.clearServices()
            gattServer?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing GATT server", e)
        }
        gattServer = null
    }

    /** Per-connection bookkeeping that must never leak into the next connection. */
    private fun resetSubscriptionState() {
        cccdEnabled = false
        reportCharacteristic?.value = ByteArray(0)
    }

    private fun handleDisconnect(device: BluetoothDevice) {
        if (device == connectedDevice) {
            connectedDevice = null
            broadcastConnection(false, null, null)
        }
        resetSubscriptionState()
        scheduleRestart(RESTART_DELAY_MS)
    }

    private fun scheduleRestart(delayMs: Long) {
        if (!serviceActive) return
        cancelRestart()
        val r = Runnable {
            restartRunnable = null
            if (serviceActive && connectedDevice == null) restartAfterDisconnect()
        }
        restartRunnable = r
        mainHandler.postDelayed(r, delayMs)
    }

    private fun cancelRestart() {
        restartRunnable?.let { mainHandler.removeCallbacks(it) }
        restartRunnable = null
    }

    private fun restartAfterDisconnect() {
        if (gattServer != null && serviceReady) {
            // Keep the server (and its attribute handles) exactly as they are.
            startAdvertising()
        } else {
            setupGattAndAdvertise()
        }
    }

    private fun scheduleAdvertiseRetry(msg: String) {
        if (!serviceActive) return
        if (advertiseRetryCount >= MAX_ADVERTISE_RETRIES) {
            broadcastError("BLE advertising failed: $msg")
            return
        }
        val delay = 500L shl advertiseRetryCount.coerceAtMost(4)
        advertiseRetryCount++
        Log.w(TAG, "Retrying advertising in ${delay}ms (attempt $advertiseRetryCount)")
        mainHandler.postDelayed({ if (serviceActive && !isAdvertising) startAdvertising() }, delay)
    }

    private fun scheduleSetupRetry() {
        if (!serviceActive) return
        if (setupRetryCount >= MAX_SETUP_RETRIES) {
            setupRetryCount = 0
            broadcastError("Could not start the HID service. Toggle Bluetooth off and on, then tap Connect.")
            return
        }
        setupRetryCount++
        val delay = 1000L * setupRetryCount
        Log.w(TAG, "Retrying GATT setup in ${delay}ms (attempt $setupRetryCount)")
        mainHandler.postDelayed({ if (serviceActive) setupGattAndAdvertise() }, delay)
    }

    // ------------------------------------------------------------------ sending

    private fun sendReport(reportId: Byte, report: ByteArray) {
        val device = connectedDevice ?: return
        val characteristic = reportCharacteristic ?: return
        characteristic.value = report
        try {
            val ok = gattServer?.notifyCharacteristicChanged(device, characteristic, false) ?: false
            if (!ok) {
                Log.w(TAG, "notifyCharacteristicChanged failed (CCCD enabled=$cccdEnabled)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to notify characteristic", e)
        }
    }

    // --------------------------------------------------------------- broadcasts

    private fun broadcastConnection(connected: Boolean, name: String?, addr: String?) {
        val intent = Intent(BROADCAST_CONNECTION).apply {
            putExtra(EXTRA_CONNECTED, connected)
            putExtra(EXTRA_CONN_DEVICE_NAME, name)
            putExtra(EXTRA_CONN_DEVICE_ADDR, addr)
        }
        sendBroadcast(intent)
    }

    private fun broadcastAdvertising(advertising: Boolean) {
        val intent = Intent(BROADCAST_ADVERTISING).apply {
            putExtra(EXTRA_ADVERTISING, advertising)
        }
        sendBroadcast(intent)
    }

    private fun broadcastError(msg: String) {
        Log.e(TAG, "Error: $msg")
        val intent = Intent(BROADCAST_ERROR).apply {
            putExtra(EXTRA_ERROR_MSG, msg)
        }
        sendBroadcast(intent)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.channel_hid),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.perm_notification_reason)
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.service_label))
            .setContentText(getString(R.string.notif_hid_running))
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setOngoing(true)
            .build()
    }

    private fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }
}
