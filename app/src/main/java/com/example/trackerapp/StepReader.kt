package com.example.trackerapp

import android.content.Context

object StepReader {
    /**
     * Return current steps count.
     * If your StepCounterManager already stores steps somewhere, we read it here.
     *
     * For now: return -1 so you can paste your StepCounterManager and I wire it properly.
     */
    fun getCurrentSteps(context: Context): Int {
        // TODO: connect to your real step source
        return -1
    }
}