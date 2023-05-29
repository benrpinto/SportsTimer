package com.quadtime.timer

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.concurrent.locks.ReentrantLock

private const val VibrationDuration: Long = 750

class Alert(inputContext: MainActivity, audioVol: Int, private var vibeOn: Boolean) {
    private var auxCord: MediaPlayer
    private var vib: Vibrator
    private val sharedLock = ReentrantLock()
    private val myContext = inputContext
    private val pendingIntent: PendingIntent

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel.
            val name = inputContext.getString(R.string.notification_channel_name)
            val descriptionText = inputContext.getString(R.string.notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val mChannel = NotificationChannel(inputContext.getString(R.string.notification_channel_id), name, importance)
            mChannel.description = descriptionText
            // Register the channel with the system. You can't change the importance
            // or other notification behaviors after this.
            val notificationManager = inputContext.getSystemService(AppCompatActivity.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mChannel)
        }

        val intent = Intent(inputContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        pendingIntent = PendingIntent.getActivity(inputContext, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                inputContext.getSystemService(AppCompatActivity.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            inputContext.getSystemService(AppCompatActivity.VIBRATOR_SERVICE) as Vibrator
        }
        val alarmSound: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        auxCord = try {
            MediaPlayer.create(inputContext, alarmSound)
        }catch(e: NullPointerException){
            MediaPlayer.create(inputContext, R.raw.ping)
        }
        val volPercent: Float = audioVol.toFloat()/100
        auxCord.setVolume(volPercent,volPercent)
    }

    fun ping(notificationID:Int, notificationText:String){
        //If activity is in the foreground, then sound the alert and vibrate
        //Otherwise send a notification
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
        }else{
            if (ActivityCompat.checkSelfPermission(
                    myContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val builder = NotificationCompat.Builder(myContext, myContext.getString(R.string.notification_channel_id))
                    .setSmallIcon(R.drawable.timer)
                    .setContentTitle(myContext.getString(R.string.notification_timer_title))
                    .setContentText(notificationText)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)

                with(NotificationManagerCompat.from(myContext)) {
                    // notificationId is a unique int for each notification that you must define
                    notify(notificationID, builder.build())
                }
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