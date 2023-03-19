package com.example.sportstimer

import android.os.Bundle
import androidx.activity.ComponentActivity
import android.os.SystemClock
import android.widget.Button
import android.widget.Chronometer
import android.widget.ImageButton
import android.widget.TextView

private const val MillisecondsPerMinute :Long = 60000
private const val SEEKER_FLOOR = MillisecondsPerMinute * 20
private const val NUM_CARDS = 4

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //timers
        val mainChronometer = findViewById<Chronometer>(R.id.chronometer)
        val flagChronometer = findViewById<Chronometer>(R.id.flagCountdown)
        val timeoutChronometer = findViewById<Chronometer>(R.id.timeoutCounter)
        val cardChronometer = arrayOf(
            findViewById<Chronometer>(R.id.cardCountdown1),
            findViewById<Chronometer>(R.id.cardCountdown2),
            findViewById<Chronometer>(R.id.cardCountdown3),
            findViewById<Chronometer>(R.id.cardCountdown4)
        )

        //buttons
        val buttonPlayPause = findViewById<ImageButton>(R.id.playPauseButton)
        val buttonReset = findViewById<ImageButton>(R.id.resetButton)
        val buttonUpLeft = findViewById<ImageButton>(R.id.scoreUpLeft)
        val buttonUpRight = findViewById<ImageButton>(R.id.scoreUpRight)
        val buttonDownLeft = findViewById<ImageButton>(R.id.scoreDownLeft)
        val buttonDownRight = findViewById<ImageButton>(R.id.scoreDownRight)
        val buttonTimeout = findViewById<Button>(R.id.timeout)
        val buttonTimeoutMinus = findViewById<Button>(R.id.minus1)
        val buttonTimeoutPlus = findViewById<Button>(R.id.plus1)
        val button1Min = findViewById<Button>(R.id.yellow1)
        val button2Min = findViewById<Button>(R.id.yellow2)
        val cardClear = arrayOf(
            findViewById<Button>(R.id.clearCard1),
            findViewById<Button>(R.id.clearCard2),
            findViewById<Button>(R.id.clearCard3),
            findViewById<Button>(R.id.clearCard4)
        )

        val scoreLeftText = findViewById<TextView>(R.id.scoreLeft)
        val scoreRightText = findViewById<TextView>(R.id.scoreRight)
        var scoreLeft = 0
        var scoreRight = 0

        var isRunning = false
        var isTimeout = false
        var pauseTime = SystemClock.elapsedRealtime()
        val cardRunner = longArrayOf(0,0,0,0)
        val cardPause = longArrayOf(0,0,0,0)

        flagChronometer.base = SystemClock.elapsedRealtime() + SEEKER_FLOOR

        //button listeners
        buttonPlayPause.setOnClickListener(){
            if(isRunning){
                pauseTime = SystemClock.elapsedRealtime()
                mainChronometer.stop()
                flagChronometer.stop()
                buttonPlayPause.setImageResource(R.drawable.button_play)
                for(a in 0..NUM_CARDS-1){
                    cardPause[a] = SystemClock.elapsedRealtime()
                    cardChronometer[a].stop()
                }
            }else {
                mainChronometer.base += SystemClock.elapsedRealtime() - pauseTime
                flagChronometer.base = mainChronometer.base + SEEKER_FLOOR
                mainChronometer.start()
                flagChronometer.start()
                for(a in 0..NUM_CARDS-1){
                    if(cardRunner[a] > 0 ) {
                        cardChronometer[a].base += SystemClock.elapsedRealtime() - cardPause[a]
                        cardChronometer[a].start()
                    }
                }
                buttonPlayPause.setImageResource(R.drawable.button_pause)
            }
            isRunning = !isRunning
        }

        buttonReset.setOnClickListener(){
            mainChronometer.base = SystemClock.elapsedRealtime()
            mainChronometer.stop()
            flagChronometer.base = SystemClock.elapsedRealtime()
            flagChronometer.base += SEEKER_FLOOR
            flagChronometer.stop()
            pauseTime = mainChronometer.base
            for(a in 0..NUM_CARDS-1){
                cardChronometer[a].stop()
                cardChronometer[a].base = SystemClock.elapsedRealtime()
                cardPause[a] = SystemClock.elapsedRealtime()
                cardRunner[a] = 0
            }
            if(isRunning){
                buttonPlayPause.setImageResource(R.drawable.button_play)
                isRunning = false
            }
            scoreLeft = 0
            scoreRight = 0
            scoreLeftText.text = scoreLeft.toString()
            scoreRightText.text = scoreRight.toString()
            if (isTimeout) {
                timeoutChronometer.base = SystemClock.elapsedRealtime()
                timeoutChronometer.stop()
                buttonTimeout.text = getString(R.string.timeout)
                isTimeout = false
            }
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

        buttonTimeout.setOnClickListener() {
            if (isTimeout) {
                timeoutChronometer.base = SystemClock.elapsedRealtime()
                timeoutChronometer.stop()
                buttonTimeout.text = getString(R.string.timeout)
            } else {
                timeoutChronometer.base = SystemClock.elapsedRealtime() + MillisecondsPerMinute
                timeoutChronometer.start()
                buttonTimeout.text = getString(R.string.ClearTimeout)
            }
            isTimeout = !isTimeout
        }

            timeoutChronometer.setOnChronometerTickListener {
                if (timeoutChronometer.base < SystemClock.elapsedRealtime()) {
                    timeoutChronometer.stop()
                    timeoutChronometer.base = SystemClock.elapsedRealtime()
                    buttonTimeout.text = getString(R.string.timeout)
                    isTimeout = false
                }
            }

        buttonTimeoutMinus.setOnClickListener() {
            if(isTimeout) {
                timeoutChronometer.base -= MillisecondsPerMinute
            }
        }
        buttonTimeoutPlus.setOnClickListener() {
            if(isTimeout) {
                timeoutChronometer.base += MillisecondsPerMinute
            }
        }

        button1Min.setOnClickListener(){
            var a = 0
            //search for the first available timer
            while (a <= NUM_CARDS -1){
                if(cardRunner[a] <= 0 ) {
                    cardChronometer[a].base = SystemClock.elapsedRealtime() + MillisecondsPerMinute
                    if (isRunning) {
                        cardChronometer[a].start()
                    } else {
                        cardPause[a] = SystemClock.elapsedRealtime()
                    }
                    cardRunner[a] = MillisecondsPerMinute
                    a = NUM_CARDS //exit the loop
                }else{
                    a++ //increment the counter
                }
            }        }

        button2Min.setOnClickListener(){
            var a = 0
            while (a <= NUM_CARDS -1){
                if(cardRunner[a] <= 0 ) {
                    cardChronometer[a].base = SystemClock.elapsedRealtime() + 2 * MillisecondsPerMinute
                    if (isRunning) {
                        cardChronometer[a].start()
                    } else {
                        cardPause[a] = SystemClock.elapsedRealtime()
                    }
                    cardRunner[a] = 2 * MillisecondsPerMinute
                    a = NUM_CARDS //exit the loop
                }else{
                    a++ //increment the counter
                }
            }
        }

        for (a in 0..NUM_CARDS-1){
            cardClear[a].setOnClickListener(){
                cardChronometer[a].stop()
                cardChronometer[a].base = SystemClock.elapsedRealtime()
                cardPause[a] = SystemClock.elapsedRealtime()
                cardRunner[a] = 0
            }
            cardChronometer[a].setOnChronometerTickListener {
                if (cardChronometer[a].base < SystemClock.elapsedRealtime()){
                    cardChronometer[a].stop()
                    cardChronometer[a].base = SystemClock.elapsedRealtime()
                    cardPause[a] = SystemClock.elapsedRealtime()
                    cardRunner[a] = 0
                }
            }
        }
    }
}