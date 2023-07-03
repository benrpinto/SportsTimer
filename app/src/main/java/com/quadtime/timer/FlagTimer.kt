package com.quadtime.timer

import android.content.SharedPreferences
import android.widget.TextView
import androidx.preference.PreferenceManager
import com.quadtime.timer.constants.FlagNotification
import com.quadtime.timer.constants.MillisecondsPerMinute
import com.quadtime.timer.constants.defFlagLength
import com.quadtime.timer.constants.timeFormatter
import kotlin.math.max

class FlagTimer(inputAlert: Alert, inputContext: MainActivity){
    private var timerBase: Long = System.currentTimeMillis()
    private val siren: Alert = inputAlert
    private val notificationText: String = inputContext.getString(R.string.notification_flag_desc)
    private var timerDuration: Long
    private val flagChronometer: TextView = inputContext.findViewById(R.id.flagCountdown)
    private var flagRunning = true

    init {
        val myPref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(inputContext)

        timerDuration =
            try {
                (myPref.getString(inputContext.getString(R.string.flag_length_key), "$defFlagLength")?.toInt()
                    ?: defFlagLength) * MillisecondsPerMinute
            }catch(e: NumberFormatException){
                defFlagLength* MillisecondsPerMinute
            }
        //whenever timerBase is set from an external source, it does not include timerDuration
        timerBase += timerDuration

    }

    fun updateFlagTimer(inputContext: MainActivity, inputTimerBase: Long, inputTime: Long){
        val myPref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(inputContext)
        timerDuration =
            try {
                (myPref.getString(inputContext.getString(R.string.flag_length_key), "$defFlagLength")?.toInt()
                    ?: defFlagLength) * MillisecondsPerMinute
            }catch(e: NumberFormatException){
                defFlagLength* MillisecondsPerMinute
            }
        //whenever timerBase is set from an external source, it does not include timerDuration
        sync(inputTimerBase, inputTime)
        flagRunning = timerBase - inputTime >= 0L
    }

    fun sync(inputTimerBase: Long, inputTime: Long){
        timerBase = inputTimerBase + timerDuration
        flagChronometer.text = timeFormatter(max(timerBase - inputTime,0L), true)
    }

    //This function is used to recreate the flag timer data from saved data
    fun restoreValues(inputContext: MainActivity, inputTimerBase: Long, inputTime: Long){

        val myPref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(inputContext)
        timerDuration =
            try {
                (myPref.getString(inputContext.getString(R.string.flag_length_key), "$defFlagLength")?.toInt()
                    ?: defFlagLength) * MillisecondsPerMinute
            }catch(e: NumberFormatException){
                defFlagLength* MillisecondsPerMinute
            }

        //whenever timerBase is set from an external source, it does not include timerDuration
        sync(inputTimerBase, inputTime)
        flagRunning = timerBase - inputTime >= 0L
    }

    fun tickListener() {
        flagChronometer.text = timeFormatter(max(timerBase - System.currentTimeMillis(),0), true)
        if ((timerDuration != 0L) && (flagRunning) && (timerBase < System.currentTimeMillis())){
            siren.ping(FlagNotification, notificationText)
            flagRunning = false
        }
    }
}