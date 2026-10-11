package com.example.bluemice

import android.content.Context
import android.content.Intent
import android.util.Log

class HidDeviceManager(private val context: Context) {
    companion object {
        private const val TAG = "HidDeviceManager"
    }

    private var isActive = true

    fun sendMouseReport(buttons: Int, deltaX: Int, deltaY: Int, wheel: Int) {
        if (!isActive) return
        val report = HidReportBuilder.buildMouseReport(buttons, deltaX, deltaY, wheel)
        sendReport(HidReportBuilder.REPORT_ID_MOUSE.toInt(), report)
    }

    fun sendMouseClick(button: Int) {
        sendMouseReport(button, 0, 0, 0)
        sendMouseReport(0, 0, 0, 0)
    }

    fun sendKeyDown(modifiers: Int, key: Byte) {
        if (!isActive) return
        val report = HidReportBuilder.buildKeyboardReportSingleKey(modifiers, key)
        sendReport(HidReportBuilder.REPORT_ID_KEYBOARD.toInt(), report)
    }

    fun sendKeyUp() {
        if (!isActive) return
        val report = HidReportBuilder.buildEmptyKeyboardReport()
        sendReport(HidReportBuilder.REPORT_ID_KEYBOARD.toInt(), report)
    }

    fun sendKeyPress(modifiers: Int, key: Byte) {
        sendKeyDown(modifiers, key)
        try { Thread.sleep(10) } catch (_: InterruptedException) {}
        sendKeyUp()
    }

    fun sendGamepadReport(leftX: Int, leftY: Int, rightX: Int, rightY: Int, buttons: Int) {
        if (!isActive) return
        val report = HidReportBuilder.buildGamepadReport(leftX, leftY, rightX, rightY, buttons)
        sendReport(HidReportBuilder.REPORT_ID_GAMEPAD.toInt(), report)
    }

    fun sendEmptyGamepadReport() {
        if (!isActive) return
        val report = HidReportBuilder.buildEmptyGamepadReport()
        sendReport(HidReportBuilder.REPORT_ID_GAMEPAD.toInt(), report)
    }

    private fun sendReport(reportId: Int, report: ByteArray) {
        val intent = Intent(context, BluetoothHidService::class.java).apply {
            action = BluetoothHidService.ACTION_SEND_REPORT
            putExtra(BluetoothHidService.EXTRA_REPORT_ID, reportId)
            putExtra(BluetoothHidService.EXTRA_REPORT, report)
        }
        try {
            context.startService(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send report", e)
        }
    }

    fun cleanup() {
        isActive = false
    }
}
