package com.example.bluemice

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.provider.Settings
import com.example.bluemice.model.ConnectionState
import com.example.bluemice.model.ConnectionStatus

class ConnectionManager(private val context: Context) {
    companion object {
        private const val PREFS_NAME = "bluemice_prefs"
        private const val KEY_LAST_DEVICE_ADDRESS = "last_device_address"
        private const val KEY_LAST_DEVICE_NAME = "last_device_name"
        private const val KEY_SHOW_ALL_DEVICES = "show_all_devices"

        fun getDeviceMajorCategory(device: BluetoothDevice): String {
            return when (device.bluetoothClass?.majorDeviceClass) {
                BluetoothClass.Device.Major.COMPUTER -> "Computer"
                BluetoothClass.Device.Major.AUDIO_VIDEO -> "Audio/Video"
                BluetoothClass.Device.Major.PHONE -> "Phone"
                BluetoothClass.Device.Major.PERIPHERAL -> "Peripheral"
                BluetoothClass.Device.Major.WEARABLE -> "Wearable"
                BluetoothClass.Device.Major.IMAGING -> "Imaging"
                BluetoothClass.Device.Major.TOY -> "Toy"
                BluetoothClass.Device.Major.HEALTH -> "Health"
                else -> "Other"
            }
        }

        fun isDesktopDevice(device: BluetoothDevice): Boolean {
            return device.bluetoothClass?.majorDeviceClass == BluetoothClass.Device.Major.COMPUTER
        }
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val bluetoothManager: BluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    val adapter: BluetoothAdapter? = bluetoothManager.adapter

    var onStateChanged: ((ConnectionState) -> Unit)? = null

    private var _state = ConnectionState()
    val state: ConnectionState get() = _state

    fun getPairedDevices(): List<BluetoothDevice> {
        return adapter?.bondedDevices?.toList() ?: emptyList()
    }

    fun getDesktopDevices(): List<BluetoothDevice> {
        return getPairedDevices().filter { isDesktopDevice(it) }
    }

    fun isShowAllDevices(): Boolean = prefs.getBoolean(KEY_SHOW_ALL_DEVICES, false)

    fun setShowAllDevices(showAll: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_ALL_DEVICES, showAll).apply()
    }

    fun getLastDeviceAddress(): String? {
        return prefs.getString(KEY_LAST_DEVICE_ADDRESS, null)
    }

    fun saveLastDevice(name: String, address: String) {
        prefs.edit().apply {
            putString(KEY_LAST_DEVICE_ADDRESS, address)
            putString(KEY_LAST_DEVICE_NAME, name)
            apply()
        }
    }

    fun openBluetoothSettings() {
        context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun onConnected(name: String, address: String) {
        saveLastDevice(name, address)
        updateState(
            ConnectionState(
                status = ConnectionStatus.Connected,
                deviceName = name,
                deviceAddress = address
            )
        )
    }

    fun onDisconnected() {
        updateState(ConnectionState(status = ConnectionStatus.Disconnected))
    }

    private fun updateState(newState: ConnectionState) {
        _state = newState
        onStateChanged?.invoke(newState)
    }
}
