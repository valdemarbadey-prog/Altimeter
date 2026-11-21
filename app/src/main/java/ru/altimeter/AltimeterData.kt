package ru.altimeter

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.math.pow

data class Reading(val groundHpa: Double, val altitudeM: Double, val seaLevelHpa: Double)

object AltimeterData {
    private const val PREFS = "altimeter_settings"
    private const val KEY_BASE_PRESSURE = "base_pressure_hpa"
    private const val KEY_BASE_ALTITUDE = "base_altitude_m"
    private const val ISA_LAPSE_RATE = 0.0065
    private const val ISA_SEA_LEVEL_TEMPERATURE = 288.15
    private const val ISA_EXPONENT = 5.25588
    private const val ISA_HEIGHT_SCALE = ISA_SEA_LEVEL_TEMPERATURE / ISA_LAPSE_RATE

    fun hasCalibration(context: Context): Boolean {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return p.contains(KEY_BASE_PRESSURE) && p.contains(KEY_BASE_ALTITUDE)
    }

    fun calibrate(context: Context, pressureHpa: Double, knownAltitudeM: Double) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_BASE_PRESSURE, pressureHpa.toString())
            .putString(KEY_BASE_ALTITUDE, knownAltitudeM.toString()).apply()
    }

    fun adjustCalibration(context: Context, deltaM: Double) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!hasCalibration(context)) return
        val current = prefs.getString(KEY_BASE_ALTITUDE, "0")?.toDoubleOrNull() ?: return
        prefs.edit().putString(KEY_BASE_ALTITUDE, (current + deltaM).toString()).apply()
    }

    fun getReading(context: Context, pressureHpa: Double): Reading? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!hasCalibration(context)) return null
        val basePressure = prefs.getString(KEY_BASE_PRESSURE, null)?.toDoubleOrNull()\n            ?: prefs.getFloat(KEY_BASE_PRESSURE, 0f).toDouble()
        val baseAltitude = prefs.getString(KEY_BASE_ALTITUDE, "0")?.toDoubleOrNull() ?: return null
        if (basePressure <= 0.0 || pressureHpa <= 0.0) return null
        val altitude = baseAltitude + ISA_HEIGHT_SCALE * (1.0 - (pressureHpa / basePressure).pow(1.0 / ISA_EXPONENT))
        val factor = 1.0 - (ISA_LAPSE_RATE * altitude / ISA_SEA_LEVEL_TEMPERATURE)
        val seaLevel = if (factor > 0.0) pressureHpa / factor.pow(ISA_EXPONENT) else Double.NaN
        return Reading(pressureHpa, altitude, seaLevel)
    }

    suspend fun readPressure(context: Context, timeoutMs: Long = 1800L): Double? {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = manager.getDefaultSensor(Sensor.TYPE_PRESSURE) ?: return null
        return withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { continuation ->
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        val value = event.values.firstOrNull()
                        if (value != null && value > 0f && continuation.isActive) {
                            manager.unregisterListener(this)
                            continuation.resume(value.toDouble())
                        }
                    }
                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
                }
                continuation.invokeOnCancellation { manager.unregisterListener(listener) }
                manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
            }
        }
    }
}
