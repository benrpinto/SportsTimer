package com.quadtime.timer

import android.content.SharedPreferences
import android.os.SystemClock
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceManager
import com.quadtime.timer.constants.HeatNotification
import com.quadtime.timer.constants.MillisecondsPerMinute
import com.quadtime.timer.constants.defHeatLength

class HeatTimer(inputAlert: Alert, inputContext: MainActivity){
    private var timerBase: Long = SystemClock.elapsedRealtime()
    private var timerPause: Long = SystemClock.elapsedRealtime()
    private val siren: Alert = inputAlert
    private val notificationText: String = inputContext.getString(R.string.notification_heat_desc)
    private var timerDuration: Long
    private val alertDialog = AlertDialog.Builder(inputContext)
    private val myContext: MainActivity = inputContext

    init {
        val myPref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(inputContext)
        timerDuration =
            try {
                (myPref.getString(inputContext.getString(R.string.heat_length_key), "$defHeatLength")?.toInt()
                    ?: defHeatLength) * MillisecondsPerMinute
            }catch(e: NumberFormatException){
                defHeatLength* MillisecondsPerMinute
            }
        timerBase += timerDuration

        //settings for the heat timer expiration pop-up
        alertDialog.apply{
            setTitle(inputContext.getString(R.string.heat_title))
            setMessage(inputContext.getString(R.string.heat_message))
            setPositiveButton(inputContext.getString(R.string.heat_positive)){ _, _->}
        }
    }

    //This function is to catch situations where the timer duration changes after
    //the Heat Timer has already been initialised
    fun checkDuration(){
        val myPref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(myContext)
        val newTimerDuration =
            try {
                (myPref.getString(myContext.getString(R.string.heat_length_key), "$defHeatLength")?.toInt()
                    ?: defHeatLength) * MillisecondsPerMinute
            }catch(e: NumberFormatException){
                defHeatLength* MillisecondsPerMinute
            }
        if (timerDuration != newTimerDuration){
            timerBase += newTimerDuration - timerDuration
            timerDuration = newTimerDuration
        }
    }

    //This function is used to recreate the heat timer data from saved data
    fun restoreValues(inputTimerPause: Long, inputTimerBase: Long){
        checkDuration()
        timerBase = inputTimerBase
        timerPause = inputTimerPause
    }

    fun tickListener() {
        if ((timerDuration != 0L) && (timerBase < SystemClock.elapsedRealtime())){
            siren.ping(HeatNotification, notificationText)
            // ElapsedRealTime() used instead of timerBase because timer can go off "late"
            // e.g. By shortening the timer length to a length of time that has already expired
            // We want the next ping to be judged off when the timer actually goes off.
            timerBase = SystemClock.elapsedRealtime() + timerDuration
            if(ActivityChecker.isActivityVisible) {
                alertDialog.create().show()
            }
        }
    }

    fun pauseTimer(){
        timerPause = SystemClock.elapsedRealtime()
    }

    fun resumeTimer(){
        timerBase += SystemClock.elapsedRealtime() - timerPause
    }

    fun getPause(): Long{
        return timerPause
    }

    fun getBase(): Long{
        return timerBase
    }
}