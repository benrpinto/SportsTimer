package com.quadtime.timer.constants
//IDs
const val TimeoutNotification: Int = 1
const val FlagNotification: Int = 2
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
const val defScoreInc: Int = 10
const val defTimeoutLength: Int = 1
const val defYellow1Length: Int = 1
const val defYellow2Length: Int = 2
const val defAudioVol: Int = 100
const val defVibeOn: Boolean = true
const val defConfirmReset: Boolean = true