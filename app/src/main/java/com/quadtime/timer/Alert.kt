package com.quadtime.timer

import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.appcompat.app.AppCompatActivity

private const val VibrationDuration : Long = 750

class Alert(inputContext:MainActivity, audioOn:Boolean, private var vibeOn: Boolean) {
    private var auxCord: MediaPlayer
    private var vib:Vibrator

    init {
        vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                inputContext.getSystemService(AppCompatActivity.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            inputContext.getSystemService(AppCompatActivity.VIBRATOR_SERVICE) as Vibrator
        }

        auxCord = MediaPlayer.create(inputContext, R.raw.ping)
        if(!audioOn){
            auxCord.setVolume(0F,0F)
        }
    }

    fun ping(){
        if(ActivityChecker.isActivityVisible) {
            if (auxCord.isPlaying) {
                auxCord.stop()
                auxCord.prepare()
            }
            auxCord.start()
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