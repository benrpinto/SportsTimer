package com.quadtime.timer.constants

import kotlin.math.absoluteValue

//IDs
const val TimeoutNotification: Int = 1
const val FlagNotification: Int = 2
const val HeatNotification: Int = 3
const val YCNotification: Int = 10

//Ratios
const val MillisecondsPerUpdate: Long = 40
const val MillisecondsPerTenth: Long = 100
const val MillisecondsPerSecond: Long = 1000
const val SecondsPerMinute: Long = 60
const val MillisecondsPerMinute: Long = SecondsPerMinute* MillisecondsPerSecond
const val MinutesPerHour: Long = 60
const val MillisecondsPerHour: Long = MillisecondsPerMinute* MinutesPerHour

//Default settings
const val defFlagLength: Int = 20
const val defHeatLength: Int = 0
const val defScoreInc: Int = 10
const val defTimeoutLength: Int = 1
const val defYellow1Length: Int = 1
const val defYellow2Length: Int = 2
const val defAudioVol: Int = 100
const val defVibeOn: Boolean = true
const val defConfirmReset: Boolean = true

 fun timeFormatter(milliTime: Long, inclMilli: Boolean): String{
    val toReturn = StringBuilder()
    val myMilli = milliTime.absoluteValue
    val milli: Long = myMilli.mod(MillisecondsPerSecond)/ MillisecondsPerTenth
    val sec: Long = (myMilli/ MillisecondsPerSecond).mod(SecondsPerMinute)
    val min: Long = (myMilli/ MillisecondsPerMinute).mod(MinutesPerHour)

    if(milliTime < 0){
        toReturn.append("-")
    }

    if(myMilli > MillisecondsPerHour){
        val hour: Long = (myMilli/ MillisecondsPerHour)
        toReturn.append(hour)
        toReturn.append(":")
    }

    toReturn.append(min.toString().padStart(2,'0'))
    toReturn.append(":")

    toReturn.append(sec.toString().padStart(2,'0'))
    if(inclMilli) {
        toReturn.append(".")
        toReturn.append(milli)
    }
    return toReturn.toString()
}