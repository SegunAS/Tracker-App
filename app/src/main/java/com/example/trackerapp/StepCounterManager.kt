package com.example.trackerapp

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

class StepCounterManager(
    context: Context,
    private val onStepsChanged: (Int) -> Unit
) : SensorEventListener {

    private val appContext = context.applicationContext
    private val sensorManager =
        appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    // Prefer STEP_COUNTER, fallback to STEP_DETECTOR
    private val stepCounterSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private val stepDetectorSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

    private var activeSensor: Sensor? = null

    private val prefs = TrackerPrefs(appContext)

    // One scope only
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // Counter-mode baseline
    private var baseline: Float? = null

    // Detector-mode counter
    private var detectorSteps: Int = -1

    // Cache to avoid UI spam
    private var cachedSteps: Int = -1

    // Prevent baseline being initialized many times in parallel
    private val baselineInitInProgress = AtomicBoolean(false)

    fun start() {
        // If permission is required and missing, sensor callbacks may never arrive on some devices.
        if (!hasActivityRecognitionPermission()) {
            // Still call UI once so you can see it’s “stuck at 0”
            onStepsChanged(max(0, cachedSteps))
            return
        }

        activeSensor = stepCounterSensor ?: stepDetectorSensor

        activeSensor?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val now = System.currentTimeMillis()
        val sensorType = event.sensor.type

        when (sensorType) {
            Sensor.TYPE_STEP_COUNTER -> {
                val totalStepsSinceBoot = event.values[0]

                // Initialize baseline safely (once)
                if (baseline == null) {
                    if (baselineInitInProgress.compareAndSet(false, true)) {
                        scope.launch {
                            initBaselineIfNeeded(totalStepsSinceBoot)
                            baselineInitInProgress.set(false)

                            val stepsNow = computeCounterSteps(totalStepsSinceBoot)
                            emitAndPersist(stepsNow, now)
                        }
                    }
                    return
                }

                val stepsNow = computeCounterSteps(totalStepsSinceBoot)
                scope.launch { emitAndPersist(stepsNow, now) }
            }

            Sensor.TYPE_STEP_DETECTOR -> {
                // value is typically 1.0 per step event
                val stepPulse = event.values.firstOrNull()?.toInt() ?: 1

                scope.launch {
                    if (detectorSteps < 0) {
                        // initialize from persisted snapshot (so it survives screen rotations etc)
                        detectorSteps = withContext(Dispatchers.IO) {
                            val saved = prefs.getLastSteps()
                            if (saved < 0) 0 else saved
                        }
                    }

                    detectorSteps += max(1, stepPulse)
                    emitAndPersist(detectorSteps, now)
                }
            }
        }
    }

    private suspend fun initBaselineIfNeeded(totalStepsSinceBoot: Float) {
        val saved = withContext(Dispatchers.IO) { prefs.getBootBaselineSteps() }

        // Reboot-safe baseline logic:
        // if boot counter reset (totalStepsSinceBoot becomes small), reset baseline.
        val usedBaseline = when {
            saved == null -> totalStepsSinceBoot
            totalStepsSinceBoot < saved -> totalStepsSinceBoot
            else -> saved
        }

        baseline = usedBaseline

        if (saved == null || totalStepsSinceBoot < saved) {
            withContext(Dispatchers.IO) { prefs.setBootBaselineSteps(usedBaseline) }
        }
    }

    private fun computeCounterSteps(totalStepsSinceBoot: Float): Int {
        val b = baseline ?: totalStepsSinceBoot
        return max(0, (totalStepsSinceBoot - b).toInt())
    }

    private suspend fun emitAndPersist(stepsNow: Int, now: Long) {
        if (stepsNow == cachedSteps) return
        cachedSteps = stepsNow

        // UI update (main)
        onStepsChanged(stepsNow)

        // Persist snapshot for workers / status detection
        withContext(Dispatchers.IO) {
            val prev = prefs.getLastSteps()
            if (prev < 0 || stepsNow > prev) {
                prefs.setLastStepChangeMs(now)
            }
            prefs.setLastSteps(stepsNow)
        }
    }

    private fun hasActivityRecognitionPermission(): Boolean {
        // Android 10+ needs ACTIVITY_RECOGNITION permission for step sensors on many devices.
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                appContext,
                android.Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}