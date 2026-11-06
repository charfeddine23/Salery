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

    companion object {
        // Shake threshold in G-force (1G = earth gravity)
        private const val SHAKE_THRESHOLD_GRAVITY = 2.4f
        private const val SHAKE_COOLDOWN_MS = 1200L
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
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val gX = x / SensorManager.GRAVITY_EARTH
        val gY = y / SensorManager.GRAVITY_EARTH
        val gZ = z / SensorManager.GRAVITY_EARTH

        val gForce = sqrt((gX * gX + gY * gY + gZ * gZ).toDouble()).toFloat()

        if (gForce > SHAKE_THRESHOLD_GRAVITY) {
            val now = System.currentTimeMillis()
            if (now - lastShakeTimestamp > SHAKE_COOLDOWN_MS) {
                lastShakeTimestamp = now
                onShake()
            }
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
