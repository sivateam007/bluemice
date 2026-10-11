package com.example.bluemice

import android.content.Context
import android.content.Intent
import android.util.Log

class BleHidManager(private val context: Context) {
    companion object {
        private const val TAG = "BleHidManager"
    }

    fun sendMouseReport(buttons: Int, deltaX: Int, deltaY: Int, wheel: Int) {
        val report = HidReportBuilder.buildMouseReport(buttons, deltaX, deltaY, wheel)
        sendReport(HidReportBuilder.REPORT_ID_MOUSE, report)
    }

    fun sendMouseClick(button: Int) {
        sendMouseReport(button, 0, 0, 0)
        sendMouseReport(0, 0, 0, 0)
    }

    fun sendKeyDown(modifiers: Int, key: Byte) {
        val report = HidReportBuilder.buildKeyboardReportSingleKey(modifiers, key)
        sendReport(HidReportBuilder.REPORT_ID_KEYBOARD, report)
    }

    fun sendKeyUp() {
        val report = HidReportBuilder.buildEmptyKeyboardReport()
        sendReport(HidReportBuilder.REPORT_ID_KEYBOARD, report)
    }

    fun sendKeyPress(modifiers: Int, key: Byte) {
        sendKeyDown(modifiers, key)
        try { Thread.sleep(10) } catch (_: InterruptedException) {}
        sendKeyUp()
    }

    fun sendConsumerPress(code: Int) {
        val report = HidReportBuilder.buildConsumerReport(code)
        sendReport(HidReportBuilder.REPORT_ID_CONSUMER, report)
        try { Thread.sleep(10) } catch (_: InterruptedException) {}
        val empty = HidReportBuilder.buildEmptyConsumerReport()
        sendReport(HidReportBuilder.REPORT_ID_CONSUMER, empty)
    }

    fun sendGamepadReport(leftX: Int, leftY: Int, rightX: Int, rightY: Int, buttons: Int) {
        val report = HidReportBuilder.buildGamepadReport(leftX, leftY, rightX, rightY, buttons)
        sendReport(HidReportBuilder.REPORT_ID_GAMEPAD, report)
    }

    fun sendEmptyGamepadReport() {
        val report = HidReportBuilder.buildEmptyGamepadReport()
        sendReport(HidReportBuilder.REPORT_ID_GAMEPAD, report)
    }

    private fun sendReport(reportId: Byte, report: ByteArray) {
        val intent = Intent(context, BleHidService::class.java).apply {
            action = BleHidService.ACTION_SEND_REPORT
            putExtra(BleHidService.EXTRA_REPORT_ID, reportId)
            putExtra(BleHidService.EXTRA_REPORT, report)
        }
        try {
            context.startService(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send report", e)
        }
    }

    fun startService() {
        val intent = Intent(context, BleHidService::class.java).apply {
            action = BleHidService.ACTION_START
        }
        try {
            context.startForegroundService(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start BLE service", e)
        }
    }

    fun stopService() {
        val intent = Intent(context, BleHidService::class.java).apply {
            action = BleHidService.ACTION_STOP
        }
        try {
            context.startService(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop BLE service", e)
        }
    }

    fun disconnectDevice() {
        val intent = Intent(context, BleHidService::class.java).apply {
            action = BleHidService.ACTION_DISCONNECT
        }
        try {
            context.startService(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to disconnect BLE", e)
        }
    }

    fun cleanup() {
    }
}
