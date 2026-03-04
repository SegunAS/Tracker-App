package com.example.trackerapp

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore(name = "tracker_prefs")

object TrackerPrefsKeys {
    // Core status tracking
    val STATUS = stringPreferencesKey("status") // "Sitting" or "Walking"
    val SITTING_START_MS = longPreferencesKey("sitting_start_ms")
    val LAST_REMINDER_MS = longPreferencesKey("last_reminder_ms")
    val LIMIT_MINUTES = intPreferencesKey("limit_minutes")

    // Step tracking (for background logic)
    val LAST_STEPS = intPreferencesKey("last_steps")
    val LAST_STEP_CHANGE_MS = longPreferencesKey("last_step_change_ms")

    // Baseline for TYPE_STEP_COUNTER (since boot)
    val BOOT_BASELINE_STEPS = floatPreferencesKey("boot_baseline_steps")
}

class TrackerPrefs(private val context: Context) {

    // -----------------------------
    // STATUS
    // -----------------------------
    suspend fun setStatus(status: String) {
        context.dataStore.edit { prefs ->
            prefs[TrackerPrefsKeys.STATUS] = status
        }
    }

    suspend fun getStatus(): String {
        val prefs = context.dataStore.data.first()
        return prefs[TrackerPrefsKeys.STATUS] ?: "Sitting"
    }

    // -----------------------------
    // SITTING START
    // -----------------------------
    suspend fun setSittingStart(ms: Long) {
        context.dataStore.edit { prefs ->
            prefs[TrackerPrefsKeys.SITTING_START_MS] = ms
        }
    }

    suspend fun getSittingStart(): Long {
        val prefs = context.dataStore.data.first()
        return prefs[TrackerPrefsKeys.SITTING_START_MS] ?: 0L
    }

    // -----------------------------
    // LAST REMINDER
    // -----------------------------
    suspend fun setLastReminder(ms: Long) {
        context.dataStore.edit { prefs ->
            prefs[TrackerPrefsKeys.LAST_REMINDER_MS] = ms
        }
    }

    suspend fun getLastReminder(): Long {
        val prefs = context.dataStore.data.first()
        return prefs[TrackerPrefsKeys.LAST_REMINDER_MS] ?: 0L
    }

    // -----------------------------
    // INACTIVITY LIMIT
    // -----------------------------
    suspend fun setLimitMinutes(minutes: Int) {
        context.dataStore.edit { prefs ->
            prefs[TrackerPrefsKeys.LIMIT_MINUTES] = minutes.coerceAtLeast(1)
        }
    }

    suspend fun getLimitMinutes(): Int {
        val prefs = context.dataStore.data.first()
        return prefs[TrackerPrefsKeys.LIMIT_MINUTES] ?: 120
    }

    // -----------------------------
    // STEP SNAPSHOT (worker / background)
    // -----------------------------
    suspend fun setLastSteps(steps: Int) {
        context.dataStore.edit { prefs ->
            prefs[TrackerPrefsKeys.LAST_STEPS] = steps.coerceAtLeast(0)
        }
    }

    suspend fun getLastSteps(): Int {
        val prefs = context.dataStore.data.first()
        return prefs[TrackerPrefsKeys.LAST_STEPS] ?: -1
    }

    suspend fun setLastStepChangeMs(ms: Long) {
        context.dataStore.edit { prefs ->
            prefs[TrackerPrefsKeys.LAST_STEP_CHANGE_MS] = ms
        }
    }

    suspend fun getLastStepChangeMs(): Long {
        val prefs = context.dataStore.data.first()
        return prefs[TrackerPrefsKeys.LAST_STEP_CHANGE_MS] ?: 0L
    }

    // -----------------------------
    // BOOT BASELINE (TYPE_STEP_COUNTER)
    // -----------------------------
    suspend fun setBootBaselineSteps(value: Float) {
        context.dataStore.edit { prefs ->
            prefs[TrackerPrefsKeys.BOOT_BASELINE_STEPS] = value
        }
    }

    suspend fun getBootBaselineSteps(): Float? {
        val prefs = context.dataStore.data.first()
        return prefs[TrackerPrefsKeys.BOOT_BASELINE_STEPS]
    }

    // -----------------------------
    // HELPERS
    // -----------------------------
    suspend fun clearSittingSession() {
        context.dataStore.edit { prefs ->
            prefs[TrackerPrefsKeys.SITTING_START_MS] = 0L
            prefs[TrackerPrefsKeys.LAST_REMINDER_MS] = 0L
        }
    }

    suspend fun resetLastReminder() {
        context.dataStore.edit { prefs ->
            prefs[TrackerPrefsKeys.LAST_REMINDER_MS] = 0L
        }
    }
}