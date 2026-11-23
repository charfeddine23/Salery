package com.example.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import kotlin.math.sqrt

/**
 * Shake detector using the device's accelerometer.
 * When the user shakes the device with sufficient force, onShake is triggered.
 */
class ShakeDetector(
    private val onShake: () -> Unit
) : SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var lastShakeTimestamp: Long = 0L
    private var lastPeakTimestamp: Long = 0L
    private var shakePeakCount: Int = 0

    companion object {
        // Require a stronger deliberate shake (~3.2G) and 2 distinct peaks to avoid accidental triggers
        internal const val SHAKE_THRESHOLD_GRAVITY = 3.2f
        internal const val SHAKE_PEAK_WINDOW_MS = 750L
        internal const val SHAKE_MIN_PEAK_GAP_MS = 120L
        internal const val SHAKE_REQUIRED_PEAKS = 2
        internal const val SHAKE_COOLDOWN_MS = 1600L
    }

    fun start(context: Context) {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        accelerometer?.let { sensor ->
            sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
        sensorManager = null
        accelerometer = null
        shakePeakCount = 0
        lastPeakTimestamp = 0L
    }

    internal fun processAccelerationSample(
        x: Float,
        y: Float,
        z: Float,
        timestampMs: Long = System.currentTimeMillis()
    ): Boolean {
        val gX = x / SensorManager.GRAVITY_EARTH
        val gY = y / SensorManager.GRAVITY_EARTH
        val gZ = z / SensorManager.GRAVITY_EARTH

        val gForce = sqrt((gX * gX + gY * gY + gZ * gZ).toDouble()).toFloat()

        if (timestampMs - lastPeakTimestamp > SHAKE_PEAK_WINDOW_MS) {
            shakePeakCount = 0
        }

        if (gForce > SHAKE_THRESHOLD_GRAVITY) {
            if (timestampMs - lastShakeTimestamp <= SHAKE_COOLDOWN_MS) {
                return false
            }
            if (shakePeakCount == 0 || (timestampMs - lastPeakTimestamp >= SHAKE_MIN_PEAK_GAP_MS)) {
                shakePeakCount += 1
                lastPeakTimestamp = timestampMs
                if (shakePeakCount >= SHAKE_REQUIRED_PEAKS) {
                    shakePeakCount = 0
                    lastShakeTimestamp = timestampMs
                    return true
                }
            }
        }
        return false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return

        val triggered = processAccelerationSample(
            x = event.values[0],
            y = event.values[1],
            z = event.values[2],
            timestampMs = System.currentTimeMillis()
        )
        if (triggered) {
            onShake()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }
}

/**
 * Composable lifecycle helper that registers and automatically unregisters
 * the ShakeDetector with the local Android Context.
 */
@Composable
fun RememberShakeDetector(enabled: Boolean = true, onShake: () -> Unit) {
    val context = LocalContext.current

    DisposableEffect(context, enabled) {
        if (!enabled) return@DisposableEffect onDispose {}

        val detector = ShakeDetector(onShake)
        detector.start(context)

        onDispose {
            detector.stop()
        }
    }
}
