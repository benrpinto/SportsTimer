package com.quadtime.timer

import android.app.Application
import android.os.Bundle

class ActivityChecker : Application() {
    companion object {
        private var savedBundle = Bundle()

        fun activityResumed() {
            isActivityVisible = true
        }

        fun activityPaused() {
            isActivityVisible = false
        }

        fun getBundle():Bundle{
            return savedBundle
        }

        fun setBundle(inBundle:Bundle){
            savedBundle.clear()
            savedBundle.putAll(inBundle)

        }

        var isActivityVisible = false
            private set
    }
}