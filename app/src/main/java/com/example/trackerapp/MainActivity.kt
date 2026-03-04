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
    private val stepsTodayState = mutableStateOf(0)

    // Auto-detection (while app is open)
    private var lastSteps: Int? = null
    private var lastStepChangeMs: Long = 0L

    // Permission gate
    private var hasActivityPermission: Boolean = false

    private val requestNotifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    private val requestActivityPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasActivityPermission = granted
            if (granted) {
                // Start immediately if we're in foreground
                stepManager?.start()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = TrackerPrefs(this)

        NotificationHelper.createNotificationChannel(this)

        // Create StepCounterManager ONCE (before permission callbacks might try to start it)
        stepManager = StepCounterManager(this) { steps ->
            stepsTodayState.value = steps
            val now = System.currentTimeMillis()

            val prev = lastSteps
            if (prev == null) {
                lastSteps = steps
                lastStepChangeMs = now
                return@StepCounterManager
            }

            val delta = steps - prev

            if (delta > 0) {
                lastSteps = steps
                lastStepChangeMs = now

                lifecycleScope.launch {
                    prefs.setStatus("Walking")
                    prefs.clearSittingSession()
                }
            } else {
                val minutesNoSteps = ((now - lastStepChangeMs) / 60000L).toInt()
                val sittingDetectMinutes = 2

                if (minutesNoSteps >= sittingDetectMinutes) {
                    lifecycleScope.launch {
                        val currentStatus = prefs.getStatus()
                        if (currentStatus != "Sitting") {
                            prefs.setStatus("Sitting")
                            prefs.setSittingStart(now)
                            prefs.resetLastReminder()
                        } else {
                            if (prefs.getSittingStart() == 0L) {
                                prefs.setSittingStart(now)
                            }
                        }
                    }
                }
            }
        }

        // Permissions
        ensureNotificationPermission()
        ensureActivityRecognitionPermission()

        scheduleSittingWorker()
        enableEdgeToEdge()

        setContent {
            TrackerAppTheme {

                // OPTION B: Month + selected day owned here
                val initialMonth = remember { YearMonth.of(2026, 2) }
                var currentMonth by remember { mutableStateOf(initialMonth) }
                var selectedDate by remember { mutableStateOf(initialMonth.atDay(1)) }

                val monthLabel = remember(currentMonth) {
                    currentMonth.format(
                        DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
                    )
                }

                var limitMinutes by remember { mutableIntStateOf(120) }
                var status by remember { mutableStateOf("Sitting") }
                var sittingStart by remember { mutableLongStateOf(0L) }

                LaunchedEffect(Unit) {
                    limitMinutes = prefs.getLimitMinutes()
                    status = prefs.getStatus()
                    sittingStart = prefs.getSittingStart()
                }

                LaunchedEffect(Unit) {
                    while (isActive) {
                        delay(5_000)
                        status = prefs.getStatus()
                        sittingStart = prefs.getSittingStart()
                        limitMinutes = prefs.getLimitMinutes()
                    }
                }

                val isToday = selectedDate == LocalDate.now()
                val nowMs = System.currentTimeMillis()

                val minutesSitting =
                    if (isToday && status == "Sitting" && sittingStart > 0L) {
                        ((nowMs - sittingStart) / 60000L).toInt().coerceAtLeast(0)
                    } else 0

                val inactiveLabel = run {
                    val h = minutesSitting / 60
                    val m = minutesSitting % 60
                    when {
                        h <= 0 -> "${m}m"
                        m == 0 -> "${h}h"
                        else -> "${h}h ${m}m"
                    }
                }

                val progressToLimit =
                    if (limitMinutes <= 0) 0f
                    else (minutesSitting.toFloat() / limitMinutes.toFloat()).coerceIn(0f, 1f)

                HomeScreen(
                    name = "Oluwasegun",

                    currentMonth = currentMonth,
                    selectedDate = selectedDate,
                    onPrevMonth = {
                        currentMonth = currentMonth.minusMonths(1)
                        selectedDate = currentMonth.atDay(1)
                    },
                    onNextMonth = {
                        currentMonth = currentMonth.plusMonths(1)
                        selectedDate = currentMonth.atDay(1)
                    },
                    onDateSelected = { date ->
                        selectedDate = date
                        currentMonth = YearMonth.from(date)
                    },

                    monthLabel = monthLabel,

                    stepsToday = if (isToday) stepsTodayState.value else 0,
                    goalSteps = 10_000,

                    status = if (isToday) status else "—",
                    inactiveLabel = if (isToday) inactiveLabel else "—",
                    progressToLimit = if (isToday) progressToLimit else 0f,

                    limitMinutes = limitMinutes,
                    onLimitMinutesChange = { newMinutes ->
                        limitMinutes = newMinutes
                        lifecycleScope.launch { prefs.setLimitMinutes(newMinutes) }
                    },

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
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                requestNotifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun ensureActivityRecognitionPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED

            hasActivityPermission = granted

            if (!granted) {
                requestActivityPermission.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        } else {
            hasActivityPermission = true
        }
    }

    private fun scheduleSittingWorker() {
        val request =
            PeriodicWorkRequestBuilder<SittingCheckWorker>(15, TimeUnit.MINUTES).build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "sitting_check_worker",
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}