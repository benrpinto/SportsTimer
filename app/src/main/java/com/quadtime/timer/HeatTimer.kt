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

    init {
        val myPref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(inputContext)
        timerDuration =
            try {
                (myPref.getString(inputContext.getString(R.string.heat_length_key), "$defHeatLength")?.toInt()
                    ?: defHeatLength) * MillisecondsPerMinute
            }catch(e: NumberFormatException){
                defHeatLength* MillisecondsPerMinute
            }
        //whenever timerBase is set from an external source, it does not include timerDuration
        timerBase += timerDuration

        //settings for the heat timer expiration pop-up
        alertDialog.apply{
            setTitle(inputContext.getString(R.string.heat_title))
            setMessage(inputContext.getString(R.string.heat_message))
            setPositiveButton(inputContext.getString(R.string.heat_positive)){ _, _->}
        }
    }

    //This function is used to recreate the heat timer data from saved data
    fun restoreValues(inputContext: MainActivity, inputTimerPause: Long, inputTimerBase: Long){
        timerBase = inputTimerBase
        timerPause = inputTimerPause

        val myPref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(inputContext)
        timerDuration =
            try {
                (myPref.getString(inputContext.getString(R.string.heat_length_key), "$defHeatLength")?.toInt()
                    ?: defHeatLength) * MillisecondsPerMinute
            }catch(e: NumberFormatException){
                defHeatLength* MillisecondsPerMinute
            }
        //whenever timerBase is set from an external source, it does not include timerDuration
        timerBase += timerDuration
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

    // this function is not currently needed, but included in case we need it for debugging
    // or for future features/fixes
    @Suppress("unused")
    fun getBase(): Long{
        return timerBase
    }

    // Timer duration can change, and it's easier to let the heat timer handle it
    // timer duration is added to the base in the initialisation function of this class
    fun getSaveBase(): Long{
        return timerBase - timerDuration
    }
}