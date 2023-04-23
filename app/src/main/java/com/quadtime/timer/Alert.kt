package com.quadtime.timer

import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.appcompat.app.AppCompatActivity
import java.util.concurrent.locks.ReentrantLock

private const val VibrationDuration : Long = 750

class Alert(inputContext:MainActivity, audioVol: Int, private var vibeOn: Boolean) {
    private var auxCord: MediaPlayer
    private var vib:Vibrator
    private val sharedLock = ReentrantLock()

    init {
        vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                inputContext.getSystemService(AppCompatActivity.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            inputContext.getSystemService(AppCompatActivity.VIBRATOR_SERVICE) as Vibrator
        }
        val alarmSound: Uri = RingtoneManager. getDefaultUri (RingtoneManager. TYPE_NOTIFICATION )
        auxCord = MediaPlayer.create(inputContext, alarmSound)
        val volPercent:Float = audioVol.toFloat()/100
        auxCord.setVolume(volPercent,volPercent)
    }

    fun ping(){
        if(ActivityChecker.isActivityVisible) {
            try {
                sharedLock.lock()

                if (auxCord.isPlaying) {
                    auxCord.stop()
                    auxCord.prepare()
                }
                auxCord.start()

            }finally{
                sharedLock.unlock()
            }

            if (vibeOn) {
                vibrate()
            }
        }
    }

    fun release(){
        auxCord.release()
    }

    //Touch Feedback has to be set for vibration to work
    //Settings->Sound and Vibration->Vibration and haptics->Touch feedback
    //(This is the same setting that provides vibration when you tap on the keyboard)
    private fun vibrate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vib.vibrate(VibrationEffect.createOneShot(VibrationDuration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vib.vibrate(VibrationDuration)
        }
    }
}