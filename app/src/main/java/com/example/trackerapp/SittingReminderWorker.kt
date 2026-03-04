package com.example.trackerapp

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class SittingReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prefs = TrackerPrefs(applicationContext)

        val status = prefs.getStatus()
        if (status != "Sitting") return Result.success()

        val now = System.currentTimeMillis()
        val sittingStart = prefs.getSittingStart()
        if (sittingStart == 0L) return Result.success()

        val minutesSitting = (now - sittingStart) / 60000L

        // 2 hours = 120 minutes
        if (minutesSitting >= 120) {
            val lastReminder = prefs.getLastReminder()

            // prevent spam: only remind once every 2 hours
            val minutesSinceLastReminder = (now - lastReminder) / 60000L
            if (lastReminder == 0L || minutesSinceLastReminder >= 120) {
                // Send notification using your existing function style
                NotificationHelper.sendWalkNotification(applicationContext)
                prefs.setLastReminder(now)
            }
        }

        return Result.success()
    }
}
