package com.example.trackerapp

import android.content.Context

object StepReader {
    /**
     * Reads the last persisted step count from TrackerPrefs.
     * This is useful for background tasks or when the StepCounterManager isn't active.
     */
    suspend fun getCurrentSteps(context: Context): Int {
        val prefs = TrackerPrefs(context)
        val savedSteps = prefs.getLastSteps()
        // Return 0 if no steps have been recorded yet (getLastSteps returns -1 if empty)
        return if (savedSteps < 0) 0 else savedSteps
    }
}
