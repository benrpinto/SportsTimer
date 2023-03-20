package com.example.sportstimer

import android.media.MediaPlayer
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.ComponentActivity
import java.util.*

private const val MillisecondsPerMinute :Long = 60000
private const val SEEKER_FLOOR = MillisecondsPerMinute * 20

class YellowCard(inputId:Int, inputAudio:MediaPlayer, inputDur:Long, isRunning:Boolean, inputContext: MainActivity){
    private val cardRow : TableRow = TableRow(inputContext)
    private val id : TextView = TextView(inputContext)
    private val cardChronometer : Chronometer = Chronometer(inputContext)
    private var cardPause : Long = SystemClock.elapsedRealtime()
    private val cardClear : Button = Button(inputContext)
    private val audio : MediaPlayer = inputAudio
    var isTrash : Boolean = false

    init {
        val cardTable = inputContext.findViewById<TableLayout>(R.id.cardTable)
        cardTable.addView(cardRow)
        //put elements into the row
        cardRow.addView(id)
        cardRow.addView(cardChronometer)
        cardRow.addView(cardClear)
        //table row
        cardRow.layoutParams = TableLayout.LayoutParams(
            TableLayout.LayoutParams.MATCH_PARENT,
            TableLayout.LayoutParams.WRAP_CONTENT
        )
        cardRow.orientation = TableLayout.VERTICAL

        //id text
        id.layoutParams = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT,
            1.0f
        )
        id.textSize = 30.toFloat()
        id.textAlignment = View.TEXT_ALIGNMENT_CENTER
        id.text = inputId.toString()

        //chronometer content
        cardChronometer.layoutParams = TableRow.LayoutParams(
            TableRow.LayoutParams.MATCH_PARENT,
            TableRow.LayoutParams.WRAP_CONTENT,
            1.0f
        )
        cardChronometer.isCountDown = true
        cardChronometer.textSize = 30.toFloat()
        cardChronometer.base = SystemClock.elapsedRealtime() + inputDur
        if (isRunning) {
            cardChronometer.start()
        }
        cardChronometer.setOnChronometerTickListener {
            if (cardChronometer.base < SystemClock.elapsedRealtime()){
                audio.start()
                clearOut()
            }
        }

        //card clear button content
        cardClear.layoutParams = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT,
            1.0f
        )
        cardClear.text = inputContext.getString(R.string.clear_card_timer)
        cardClear.setOnClickListener {
            clearOut()
        }
    }

    private fun clearOut() {
        if(!isTrash) {
            cardChronometer.stop()
            (cardRow.parent as ViewGroup).removeView(cardRow)
            isTrash = true
        }
    }

    fun pauseTimer(){
        if(!isTrash) {
            cardPause = SystemClock.elapsedRealtime()
            cardChronometer.stop()
        }
    }
    fun resumeTimer(){
        if(!isTrash) {
            cardChronometer.base += SystemClock.elapsedRealtime() - cardPause
            cardChronometer.start()
        }
    }
    fun clearTimer(){
        clearOut()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //timers
        val mainChronometer = findViewById<Chronometer>(R.id.chronometer)
        val flagChronometer = findViewById<Chronometer>(R.id.flagCountdown)
        val timeoutChronometer = findViewById<Chronometer>(R.id.timeoutCounter)
        val yellowCards: Vector<YellowCard> = Vector(3,3)

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

        //scores
        val scoreLeftText = findViewById<TextView>(R.id.scoreLeft)
        val scoreRightText = findViewById<TextView>(R.id.scoreRight)
        var scoreLeft = 0
        var scoreRight = 0

        //audio player
        val auxCord = MediaPlayer.create(this,R.raw.ping)

        //state trackers
        var numCards = 0
        var isRunning = false
        var flagRunning = true
        var isTimeout = false
        var pauseTime = SystemClock.elapsedRealtime()

        flagChronometer.base = SystemClock.elapsedRealtime() + SEEKER_FLOOR

        //button listeners
        buttonPlayPause.setOnClickListener {
            if(isRunning) {
                pauseTime = SystemClock.elapsedRealtime()
                mainChronometer.stop()
                flagChronometer.stop()
                buttonPlayPause.setImageResource(R.drawable.button_play)
                for(a in yellowCards.indices.reversed()){
                    if(yellowCards[a].isTrash){
                        yellowCards.removeAt(a)
                    }else{
                        yellowCards[a].pauseTimer()
                    }
                }
            }else{
                mainChronometer.base += SystemClock.elapsedRealtime() - pauseTime
                mainChronometer.start()
                if(flagRunning) {
                    flagChronometer.base = mainChronometer.base + SEEKER_FLOOR
                    flagChronometer.start()
                }
                for(a in yellowCards.indices.reversed()){
                    if(yellowCards[a].isTrash){
                        yellowCards.removeAt(a)
                    }else{
                        yellowCards[a].resumeTimer()
                    }
                }
                buttonPlayPause.setImageResource(R.drawable.button_pause)
            }
            isRunning = !isRunning
        }

        buttonReset.setOnClickListener{
            mainChronometer.stop()
            flagChronometer.stop()
            mainChronometer.base = SystemClock.elapsedRealtime()
            flagChronometer.base = mainChronometer.base
            flagChronometer.base += SEEKER_FLOOR
            pauseTime = mainChronometer.base
            for(a in yellowCards){
                a.clearTimer()
            }
            yellowCards.clear()
            if(isRunning){
                buttonPlayPause.setImageResource(R.drawable.button_play)
                isRunning = false
            }
            flagRunning = true
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

        buttonUpLeft.setOnClickListener{
            scoreLeft += 10
            scoreLeftText.text = scoreLeft.toString()
        }
        buttonUpRight.setOnClickListener{
            scoreRight += 10
            scoreRightText.text = scoreRight.toString()
        }
        buttonDownLeft.setOnClickListener{
            scoreLeft -= 10
            scoreLeftText.text = scoreLeft.toString()
        }
        buttonDownRight.setOnClickListener{
            scoreRight -= 10
            scoreRightText.text = scoreRight.toString()
        }

        buttonTimeout.setOnClickListener {
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

        flagChronometer.setOnChronometerTickListener {
            if(flagChronometer.base < SystemClock.elapsedRealtime() && flagRunning && isRunning){
                auxCord.start()
                flagChronometer.stop()
                flagChronometer.base = SystemClock.elapsedRealtime()
                flagRunning = false
            }
        }

        timeoutChronometer.setOnChronometerTickListener {
            if (timeoutChronometer.base < SystemClock.elapsedRealtime()) {
                auxCord.start()
                timeoutChronometer.stop()
                timeoutChronometer.base = SystemClock.elapsedRealtime()
                buttonTimeout.text = getString(R.string.timeout)
                isTimeout = false
            }
        }

        buttonTimeoutMinus.setOnClickListener {
            if(isTimeout) {
                timeoutChronometer.base -= MillisecondsPerMinute
            }
        }
        buttonTimeoutPlus.setOnClickListener {
            if(isTimeout) {
                timeoutChronometer.base += MillisecondsPerMinute
            }
        }

        button1Min.setOnClickListener{
            numCards++
            yellowCards.add(YellowCard(numCards,auxCord, MillisecondsPerMinute,isRunning,this))
        }

        button2Min.setOnClickListener{
            numCards++
            yellowCards.add(YellowCard(numCards,auxCord, 2*MillisecondsPerMinute,isRunning,this))
        }
   }
}