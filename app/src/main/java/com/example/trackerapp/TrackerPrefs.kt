package com.example.trackerapp

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.time.LocalDate

private val Context.dataStore by preferencesDataStore(name = "tracker_prefs")

object TrackerPrefsKeys {
    val STATUS = stringPreferencesKey("status")
    val SITTING_START_MS = longPreferencesKey("sitting_start_ms")
    val LAST_REMINDER_MS = longPreferencesKey("last_reminder_ms")
    val LIMIT_MINUTES = intPreferencesKey("limit_minutes")

    val LAST_STEPS = intPreferencesKey("last_steps")
    val LAST_STEP_CHANGE_MS = longPreferencesKey("last_step_change_ms")
    val LAST_UPDATE_DATE = stringPreferencesKey("last_update_date")

    val BOOT_BASELINE_STEPS = floatPreferencesKey("boot_baseline_steps")
}

class TrackerPrefs(private val context: Context) {

    suspend fun setStatus(status: String) {
        context.dataStore.edit { it[TrackerPrefsKeys.STATUS] = status }
    }

    suspend fun getStatus(): String {
        return context.dataStore.data.first()[TrackerPrefsKeys.STATUS] ?: "Sitting"
    }

    suspend fun setSittingStart(ms: Long) {
        context.dataStore.edit { it[TrackerPrefsKeys.SITTING_START_MS] = ms }
    }

    suspend fun getSittingStart(): Long {
        return context.dataStore.data.first()[TrackerPrefsKeys.SITTING_START_MS] ?: 0L
    }

    suspend fun setLastReminder(ms: Long) {
        context.dataStore.edit { it[TrackerPrefsKeys.LAST_REMINDER_MS] = ms }
    }

    suspend fun getLastReminder(): Long {
        return context.dataStore.data.first()[TrackerPrefsKeys.LAST_REMINDER_MS] ?: 0L
    }

    suspend fun setLimitMinutes(minutes: Int) {
        context.dataStore.edit { it[TrackerPrefsKeys.LIMIT_MINUTES] = minutes.coerceAtLeast(1) }
    }

    suspend fun getLimitMinutes(): Int {
        return context.dataStore.data.first()[TrackerPrefsKeys.LIMIT_MINUTES] ?: 120
    }

    suspend fun setLastSteps(steps: Int) {
        val today = LocalDate.now().toString()
        context.dataStore.edit { prefs ->
            prefs[TrackerPrefsKeys.LAST_STEPS] = steps.coerceAtLeast(0)
            prefs[TrackerPrefsKeys.LAST_UPDATE_DATE] = today
        }
    }

    suspend fun getLastSteps(): Int {
        val prefs = context.dataStore.data.first()
        val savedDate = prefs[TrackerPrefsKeys.LAST_UPDATE_DATE]
        val today = LocalDate.now().toString()

        // Reset to 0 if the date has changed
        if (savedDate != null && savedDate != today) {
            return 0
        }
        return prefs[TrackerPrefsKeys.LAST_STEPS] ?: 0
    }

    suspend fun setLastStepChangeMs(ms: Long) {
        context.dataStore.edit { it[TrackerPrefsKeys.LAST_STEP_CHANGE_MS] = ms }
    }

    suspend fun getLastStepChangeMs(): Long {
        return context.dataStore.data.first()[TrackerPrefsKeys.LAST_STEP_CHANGE_MS] ?: 0L
    }

    suspend fun setBootBaselineSteps(value: Float) {
        context.dataStore.edit { it[TrackerPrefsKeys.BOOT_BASELINE_STEPS] = value }
    }

    suspend fun getBootBaselineSteps(): Float? {
        return context.dataStore.data.first()[TrackerPrefsKeys.BOOT_BASELINE_STEPS]
    }

    suspend fun clearSittingSession() {
        context.dataStore.edit { prefs ->
            prefs[TrackerPrefsKeys.SITTING_START_MS] = 0L
            prefs[TrackerPrefsKeys.LAST_REMINDER_MS] = 0L
        }
    }

    suspend fun resetLastReminder() {
        context.dataStore.edit { it[TrackerPrefsKeys.LAST_REMINDER_MS] = 0L }
    }
}
