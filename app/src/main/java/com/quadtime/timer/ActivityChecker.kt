package com.quadtime.timer

import android.app.Application

class ActivityChecker : Application() {
    companion object {
        fun activityResumed() {
            isActivityVisible = true
        }

        fun activityPaused() {
            isActivityVisible = false
        }

        var isActivityVisible = false
            private set
    }
}