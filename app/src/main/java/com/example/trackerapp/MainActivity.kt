package com.example.trackerapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.trackerapp.ui.theme.TrackerAppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private lateinit var prefs: TrackerPrefs

    private var stepManager: StepCounterManager? = null
    private val stepsTodayState = mutableIntStateOf(0)

    private var lastSteps: Int? = null
    private var lastStepChangeMs: Long = 0L
    private var hasActivityPermission: Boolean = false

    private val requestNotifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private val requestActivityPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasActivityPermission = granted
            if (granted) stepManager?.start()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = TrackerPrefs(this)
        NotificationHelper.createNotificationChannel(this)

        stepManager = StepCounterManager(this) { steps ->
            stepsTodayState.intValue = steps
            val now = System.currentTimeMillis()

            val prev = lastSteps
            if (prev == null) {
                lastSteps = steps
                lastStepChangeMs = now
                return@StepCounterManager
            }

            if (steps > prev) {
                lastSteps = steps
                lastStepChangeMs = now
                lifecycleScope.launch {
                    prefs.setStatus("Walking")
                    prefs.clearSittingSession()
                }
            } else if (steps == prev) {
                val minutesNoSteps = ((now - lastStepChangeMs) / 60000L).toInt()
                if (minutesNoSteps >= 2) {
                    lifecycleScope.launch {
                        if (prefs.getStatus() != "Sitting") {
                            prefs.setStatus("Sitting")
                            prefs.setSittingStart(now)
                            prefs.resetLastReminder()
                        }
                    }
                }
            }
        }

        ensureNotificationPermission()
        ensureActivityRecognitionPermission()
        scheduleSittingWorker()
        enableEdgeToEdge()

        setContent {
            TrackerAppTheme {
                val initialMonth = remember { YearMonth.now() }
                var currentMonth by remember { mutableStateOf(initialMonth) }
                var selectedDate by remember { mutableStateOf(LocalDate.now()) }

                val monthLabel = remember(currentMonth) {
                    currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
                }

                var limitMinutes by remember { mutableIntStateOf(120) }
                var status by remember { mutableStateOf("Sitting") }
                var sittingStart by remember { mutableLongStateOf(0L) }

                // LOAD DATA ON STARTUP
                LaunchedEffect(Unit) {
                    stepsTodayState.intValue = prefs.getLastSteps()
                    limitMinutes = prefs.getLimitMinutes()
                    status = prefs.getStatus()
                    sittingStart = prefs.getSittingStart()
                }

                // REFRESH PERIODICALLY
                LaunchedEffect(Unit) {
                    while (isActive) {
                        delay(5_000)
                        status = prefs.getStatus()
                        sittingStart = prefs.getSittingStart()
                    }
                }

                val isToday = selectedDate == LocalDate.now()
                val minutesSitting = if (isToday && status == "Sitting" && sittingStart > 0L) {
                    ((System.currentTimeMillis() - sittingStart) / 60000L).toInt().coerceAtLeast(0)
                } else 0

                HomeScreen(
                    name = "Oluwasegun",
                    currentMonth = currentMonth,
                    selectedDate = selectedDate,
                    onPrevMonth = { currentMonth = currentMonth.minusMonths(1) },
                    onNextMonth = { currentMonth = currentMonth.plusMonths(1) },
                    onDateSelected = { selectedDate = it },
                    monthLabel = monthLabel,
                    stepsToday = if (isToday) stepsTodayState.intValue else 0,
                    status = if (isToday) status else "—",
                    limitMinutes = limitMinutes,
                    onLimitMinutesChange = { lifecycleScope.launch { prefs.setLimitMinutes(it); limitMinutes = it } },
                    onStandUpNow = {
                        lifecycleScope.launch {
                            prefs.setStatus("Walking")
                            prefs.clearSittingSession()
                        }
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasActivityPermission) stepManager?.start()
    }

    override fun onPause() {
        super.onPause()
        stepManager?.stop()
    }

    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun ensureActivityRecognitionPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            hasActivityPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
            if (!hasActivityPermission) requestActivityPermission.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        } else {
            hasActivityPermission = true
        }
    }

    private fun scheduleSittingWorker() {
        val request = PeriodicWorkRequestBuilder<SittingCheckWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("sitting_check_worker", ExistingPeriodicWorkPolicy.UPDATE, request)
    }
}
