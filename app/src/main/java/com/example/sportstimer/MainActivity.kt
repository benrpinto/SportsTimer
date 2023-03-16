package com.example.sportstimer

import android.os.Bundle
import androidx.activity.ComponentActivity
import android.os.SystemClock
import android.widget.Chronometer
import android.widget.ImageButton
import android.widget.TextView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mainChronometer = findViewById<Chronometer>(R.id.chronometer)
        val scoreLeftText = findViewById<TextView>(R.id.scoreLeft)
        val scoreRightText = findViewById<TextView>(R.id.scoreRight)

        //buttons
        val buttonPlayPause = findViewById<ImageButton>(R.id.playPauseButton)
        val buttonReset = findViewById<ImageButton>(R.id.resetButton)
        val buttonUpLeft = findViewById<ImageButton>(R.id.scoreUpLeft)
        val buttonUpRight = findViewById<ImageButton>(R.id.scoreUpRight)
        val buttonDownLeft = findViewById<ImageButton>(R.id.scoreDownLeft)
        val buttonDownRight = findViewById<ImageButton>(R.id.scoreDownRight)

        var isRunning = false
        var scoreLeft = 0
        var scoreRight = 0
        var pauseTime = SystemClock.elapsedRealtime()
        //button listeners
        buttonPlayPause.setOnClickListener(){
            if(isRunning){
                pauseTime = SystemClock.elapsedRealtime()
                mainChronometer.stop()
                buttonPlayPause.setImageResource(R.drawable.button_play)
            }else {
                mainChronometer.base += SystemClock.elapsedRealtime() - pauseTime
                mainChronometer.start()
                buttonPlayPause.setImageResource(R.drawable.button_pause)
            }
            isRunning = !isRunning
        }

        buttonReset.setOnClickListener(){
            mainChronometer.base = SystemClock.elapsedRealtime()
        }

        buttonUpLeft.setOnClickListener(){
            scoreLeft += 10
            scoreLeftText.text = scoreLeft.toString()
        }
        buttonUpRight.setOnClickListener(){
            scoreRight += 10
            scoreRightText.text = scoreRight.toString()
        }
        buttonDownLeft.setOnClickListener(){
            scoreLeft -= 10
            scoreLeftText.text = scoreLeft.toString()
        }
        buttonDownRight.setOnClickListener(){
            scoreRight -= 10
            scoreRightText.text = scoreRight.toString()
        }
    }
}