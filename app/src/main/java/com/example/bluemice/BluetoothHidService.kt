package com.example.bluemice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothHidDeviceAppQosSettings
import android.os.Build
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat

class BluetoothHidService : Service() {

    companion object {
        private const val TAG = "BluetoothHidService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "hid_service_channel"

        const val ACTION_CONNECT = "com.example.bluemice.CONNECT"
        const val ACTION_DISCONNECT = "com.example.bluemice.DISCONNECT"
        const val EXTRA_DEVICE_ADDRESS = "device_address"
        const val EXTRA_DEVICE_NAME = "device_name"

        const val ACTION_SEND_REPORT = "SEND_REPORT"
        const val EXTRA_REPORT_ID = "report_id"
        const val EXTRA_REPORT = "report"

        const val BROADCAST_CONNECTION = "com.example.bluemice.CONNECTION_STATE"
        const val EXTRA_CONNECTED = "connected"
        const val EXTRA_CONN_DEVICE_NAME = "conn_device_name"
        const val EXTRA_CONN_DEVICE_ADDR = "conn_device_addr"

        const val BROADCAST_ERROR = "com.example.bluemice.ERROR"
        const val EXTRA_ERROR_MSG = "error_msg"
    }

    private var bluetoothHidDevice: BluetoothHidDevice? = null
    private var connectedDevice: BluetoothDevice? = null
    private var isRegistered = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private val connectTimeoutRunnable = Runnable {
        if (connectedDevice == null) {
            broadcastConnection(false, null, null)
            broadcastError("Connection timed out.\n\nCheck on your PC:\n1. Open Windows Bluetooth settings\n2. Find your phone under Paired Devices\n3. Click 'Connect' from the PC side and accept")
        }
    }

    private val callback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            Log.d(TAG, "onAppStatusChanged: registered=$registered, device=$pluggedDevice")
            isRegistered = registered
        }

        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            Log.d(TAG, "onConnectionStateChanged: state=$state, device=${device.address}")
            mainHandler.removeCallbacks(connectTimeoutRunnable)
            when (state) {
                BluetoothHidDevice.STATE_CONNECTED -> {
                    connectedDevice = device
                    broadcastConnection(true, device.name ?: "Unknown", device.address)
                }
                BluetoothHidDevice.STATE_DISCONNECTED -> {
                    connectedDevice = null
                    broadcastConnection(false, null, null)
                }
            }
        }

        override fun onGetReport(device: BluetoothDevice, type: Byte, id: Byte, bufferSize: Int) {
            Log.d(TAG, "onGetReport")
        }

        override fun onSetReport(device: BluetoothDevice, type: Byte, id: Byte, data: ByteArray) {
            Log.d(TAG, "onSetReport")
        }

        override fun onSetProtocol(device: BluetoothDevice, protocol: Byte) {
            Log.d(TAG, "onSetProtocol: protocol=$protocol")
        }

        override fun onInterruptData(device: BluetoothDevice, reportId: Byte, data: ByteArray) {
            Log.d(TAG, "onInterruptData")
        }

        fun onIntrData(device: BluetoothDevice, reportId: Int, data: ByteArray) {
            Log.d(TAG, "onIntrData")
        }

        override fun onVirtualCableUnplug(device: BluetoothDevice) {
            Log.d(TAG, "onVirtualCableUnplug")
            connectedDevice = null
            broadcastConnection(false, null, null)
        }
    }

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                bluetoothHidDevice = proxy as BluetoothHidDevice
                Log.d(TAG, "BluetoothHidDevice proxy obtained")
                registerApp()
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                bluetoothHidDevice = null
                isRegistered = false
                Log.d(TAG, "BluetoothHidDevice proxy disconnected")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter
        if (adapter != null && adapter.isEnabled) {
            adapter.getProfileProxy(this, profileListener, BluetoothProfile.HID_DEVICE)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            when (intent.action) {
                ACTION_CONNECT -> {
                    val addr = intent.getStringExtra(EXTRA_DEVICE_ADDRESS)
                    if (addr != null) connectToDevice(addr)
                }
                ACTION_DISCONNECT -> disconnectDevice()
                ACTION_SEND_REPORT -> {
                    val reportId = intent.getIntExtra(EXTRA_REPORT_ID, 0)
                    val report = intent.getByteArrayExtra(EXTRA_REPORT)
                    if (report != null) sendReport(reportId, report)
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        unregisterApp()
        try {
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bluetoothManager.adapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, bluetoothHidDevice)
        } catch (e: Exception) {
            Log.e(TAG, "Error closing profile proxy", e)
        }
        super.onDestroy()
    }

    private fun registerApp() {
        val hid = bluetoothHidDevice
        if (hid == null) {
            Log.e(TAG, "Cannot register: bluetoothHidDevice is null")
            broadcastError("HID proxy not available yet")
            return
        }
        try {
            val ok = if (Build.VERSION.SDK_INT >= 36) {
                registerAppSdk36(hid)
            } else {
                registerAppSdk35(hid)
            }
            if (ok) {
                isRegistered = true
                Log.d(TAG, "HID device registered")
            } else {
                Log.e(TAG, "HID registerApp returned false")
                broadcastError("HID registration rejected by Bluetooth stack")
            }
        } catch (e: Throwable) {
            isRegistered = false
            Log.e(TAG, "Failed to register HID device", e)
            broadcastError("HID registration failed: ${e.message}")
        }
    }

    private fun registerAppSdk36(hid: BluetoothHidDevice): Boolean {
        val sdp = BluetoothHidDeviceAppSdpSettings(
            "BlueMice Controller",
            "Bluetooth Mouse, Keyboard & Gamepad",
            "BlueMice",
            0,
            HidReportBuilder.reportDescriptor
        )
        return hid.registerApp(sdp, null, null,
            java.util.concurrent.Executors.newSingleThreadExecutor(), callback)
    }

    private fun registerAppSdk35(hid: BluetoothHidDevice): Boolean {
        val appClass = Class.forName("android.bluetooth.BluetoothHidDeviceApp")
        val app = appClass.getConstructor(
            String::class.java, String::class.java, String::class.java,
            java.lang.Byte.TYPE, ByteArray::class.java
        ).newInstance(
            "BlueMice Controller",
            "Bluetooth Mouse, Keyboard & Gamepad",
            "BlueMice",
            0.toByte(),
            HidReportBuilder.reportDescriptor
        )
        val method = hid.javaClass.getMethod("registerApp",
            appClass, BluetoothHidDeviceAppQosSettings::class.java,
            BluetoothHidDeviceAppQosSettings::class.java,
            java.util.concurrent.Executor::class.java, BluetoothHidDevice.Callback::class.java)
        val result = method.invoke(hid, app, null, null,
            java.util.concurrent.Executors.newSingleThreadExecutor(), callback)
        return result as? Boolean ?: false
    }

    private fun unregisterApp() {
        try {
            if (isRegistered) {
                bluetoothHidDevice?.unregisterApp()
                isRegistered = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister HID device", e)
        }
    }

    private fun connectToDevice(address: String) {
        try {
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val adapter = bluetoothManager.adapter
            val device = adapter?.getRemoteDevice(address)
            if (device == null) {
                broadcastConnection(false, null, null)
                broadcastError("Device $address not found. Re-pair from Bluetooth settings.")
                return
            }

            val hid = bluetoothHidDevice
            if (hid == null) {
                broadcastConnection(false, null, null)
                broadcastError("Bluetooth HID not ready. Restart app and ensure Bluetooth is on.")
                return
            }

            if (!isRegistered) {
                broadcastConnection(false, null, null)
                broadcastError("HID registration pending. Try again in a few seconds.")
                return
            }

            val result = hid.connect(device)
            Log.d(TAG, "connect: address=$address result=$result")
            if (result == true) {
                mainHandler.postDelayed(connectTimeoutRunnable, 20000)
            } else {
                broadcastConnection(false, null, null)
                broadcastError("Connection failed.\n\nEnsure PC Bluetooth is ON and the PC is discoverable in Bluetooth settings.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect", e)
            broadcastConnection(false, null, null)
            broadcastError("Connection error: ${e.message}")
        }
    }

    private fun disconnectDevice() {
        val device = connectedDevice ?: return
        try {
            bluetoothHidDevice?.disconnect(device)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to disconnect", e)
        }
        connectedDevice = null
        isRegistered = false
        broadcastConnection(false, null, null)
    }

    private fun sendReport(reportId: Int, report: ByteArray) {
        val device = connectedDevice ?: return
        try {
            bluetoothHidDevice?.sendReport(device, reportId, report)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send report", e)
        }
    }

    private fun broadcastConnection(connected: Boolean, name: String?, addr: String?) {
        val intent = Intent(BROADCAST_CONNECTION).apply {
            putExtra(EXTRA_CONNECTED, connected)
            putExtra(EXTRA_CONN_DEVICE_NAME, name)
            putExtra(EXTRA_CONN_DEVICE_ADDR, addr)
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
}
