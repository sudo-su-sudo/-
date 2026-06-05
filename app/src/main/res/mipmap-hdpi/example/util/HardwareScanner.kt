package com.example.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HardwareScanner(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val lightSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
    private var currentLightLux: Float = -1f

    init {
        registerSensors()
    }

    fun registerSensors() {
        lightSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun unregisterSensors() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_LIGHT) {
            currentLightLux = event.values[0]
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun getBatteryLevel(): Int {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    fun isBatteryCharging(): Boolean {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            batteryManager.isCharging
        } else {
            false
        }
    }

    fun getAvailableStorageGb(): Double {
        val stat = StatFs(Environment.getDataDirectory().path)
        val bytesAvailable = stat.blockSizeLong * stat.availableBlocksLong
        return bytesAvailable / (1024.0 * 1024.0 * 1024.0)
    }

    fun getTotalStorageGb(): Double {
        val stat = StatFs(Environment.getDataDirectory().path)
        val bytesTotal = stat.blockSizeLong * stat.blockCountLong
        return bytesTotal / (1024.0 * 1024.0 * 1024.0)
    }

    fun getWifiEnabledStatus(): String {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
            if (wifiManager.isWifiEnabled) {
                val info = wifiManager.connectionInfo
                val ssid = info?.ssid ?: "<Hidden>"
                "Enabled (SSID: $ssid, Signal Rssi: ${info?.rssi}dBm)"
            } else {
                "Disabled"
            }
        } catch (e: Exception) {
            "Active (Unavailable)"
        }
    }

    fun getScannerSummary(): String {
        val lightState = if (currentLightLux < 0) "Reading..." else "${currentLightLux}lx"
        val luxValue = currentLightLux
        val environmentLuminosity = when {
            luxValue < 0 -> "Determining ambiance..."
            luxValue < 5 -> "Nocturnal pitch (Dark-Theme adaptive context active)"
            luxValue < 40 -> "Quiet Room ambiance (Dim environment, focus mode suggestion)"
            luxValue < 900 -> "Active Studio (Ideal workplace light context)"
            else -> "Daylight glare (High luminosity setting active)"
        }
        val battery = getBatteryLevel()
        val charging = if (isBatteryCharging()) "Charging" else "Discharging"
        
        return """
            [NATIVE SYSTEM ENV DATA SNAPSHOT]
            - Local Device Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}
            - Battery Charge Status: $battery% Level ($charging)
            - Ambient Light Intensity: $lightState ($environmentLuminosity)
            - Free Partition Memory Space: ${String.format("%.2f", getAvailableStorageGb())} GB of ${String.format("%.2f", getTotalStorageGb())} GB Total
            - Local Network Wifi: ${getWifiEnabledStatus()}
        """.trimIndent()
    }
}
