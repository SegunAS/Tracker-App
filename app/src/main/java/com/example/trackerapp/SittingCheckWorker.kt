package com.example.trackerapp

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlin.math.max

class SittingCheckWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = TrackerPrefs(applicationContext)
        val now = System.currentTimeMillis()

        // Read status
        val status = prefs.getStatus()

        // If user isn't sitting, make sure sitting session isn't left hanging
        if (status != "Sitting") {
            // optional cleanup (prevents stale sessions)
            prefs.clearSittingSession()
            return Result.success()
        }

        val sittingStart = prefs.getSittingStart()
        if (sittingStart <= 0L) return Result.success()

        val minutesSitting = max(0, ((now - sittingStart) / 60000L).toInt())

        // User-selected limit (default 120)
        val limitMinutes = prefs.getLimitMinutes().coerceAtLeast(1)

        if (minutesSitting < limitMinutes) return Result.success()

        val lastReminder = prefs.getLastReminder()
        val minutesSinceLast = max(0, ((now - lastReminder) / 60000L).toInt())

        // Cooldown = limitMinutes (simple + prevents spam)
        val cooldownMinutes = limitMinutes

        if (lastReminder == 0L || minutesSinceLast >= cooldownMinutes) {
            NotificationHelper.sendWalkNotification(applicationContext)
            prefs.setLastReminder(now)
        }

        return Result.success()
    }
}