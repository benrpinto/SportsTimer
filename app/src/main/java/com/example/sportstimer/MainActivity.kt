package com.example.sportstimer

import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.ComponentActivity
import java.util.*

private const val MillisecondsPerMinute :Long = 60000
private const val SEEKER_FLOOR = MillisecondsPerMinute * 20

class YellowCard(inputId:Int, inputDur:Long, isRunning:Boolean, inputContext: MainActivity){
    private val cardRow : TableRow = TableRow(inputContext)
    private val id : TextView = TextView(inputContext)
    private val cardChronometer : Chronometer = Chronometer(inputContext)
    private var cardPause : Long = SystemClock.elapsedRealtime()
    private val cardClear : Button = Button(inputContext)
    var isTrash : Boolean = false

    init {
        print("yellow card being initialised\n")
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
        //android:layout_width="match_parent"
        //android:layout_height="wrap_content"
        //android:orientation="vertical"

        //id text
        id.layoutParams = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT,
            1.0f
        )
        id.textSize = 30.toFloat()
        id.textAlignment = View.TEXT_ALIGNMENT_CENTER
        id.text = inputId.toString()

        //android:id="@+id/card1ID"
        //android:layout_width="wrap_content"
        //android:layout_height="wrap_content"
        //android:textSize="30sp"
        //android:textAlignment="center"
        //android:layout_weight="1"
        //android:text= "1"

        //chronometer content
        cardChronometer.layoutParams = TableRow.LayoutParams(
            TableRow.LayoutParams.MATCH_PARENT,
            TableRow.LayoutParams.WRAP_CONTENT,
            1.0f
        )
        cardChronometer.isCountDown = true
        cardChronometer.textSize = 30.toFloat()
        //android:id="@+id/cardCountdown1"
        //android:layout_width="fill_parent"
        //android:layout_height="wrap_content"
        //android:countDown="true"
        //android:layout_weight="1"
        //android:textSize="30sp"
        cardChronometer.base = SystemClock.elapsedRealtime() + inputDur
        if (isRunning) {
            cardChronometer.start()
        }
        cardChronometer.setOnChronometerTickListener {
            if (cardChronometer.base < SystemClock.elapsedRealtime()){
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
        //android:id="@+id/clearCard1"
        //android:layout_width="wrap_content"
        //android:layout_height="wrap_content"
        //android:layout_weight="1"
        //android:text="@string/clear_card_timer"
        cardClear.setOnClickListener {
            clearOut()
        }

        print("yellow card finished initialising\n")
    }

    private fun clearOut() {
        print("yellow card being cleared out\n")
        if(!isTrash) {
            cardChronometer.stop()
            (cardRow.parent as ViewGroup).removeView(cardRow)
            isTrash = true
        }
    }

    fun pauseTimer(){
        print("yellow card being paused\n")
        if(!isTrash) {
            cardPause = SystemClock.elapsedRealtime()
            cardChronometer.stop()
        }
    }
    fun resumeTimer(){
        print("yellow card being resumed\n")
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

        val scoreLeftText = findViewById<TextView>(R.id.scoreLeft)
        val scoreRightText = findViewById<TextView>(R.id.scoreRight)
        var scoreLeft = 0
        var scoreRight = 0
        var numCards = 0

        var isRunning = false
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
                flagChronometer.base = mainChronometer.base + SEEKER_FLOOR
                mainChronometer.start()
                flagChronometer.start()
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
            mainChronometer.base = SystemClock.elapsedRealtime()
            mainChronometer.stop()
            flagChronometer.base = SystemClock.elapsedRealtime()
            flagChronometer.base += SEEKER_FLOOR
            flagChronometer.stop()
            pauseTime = mainChronometer.base
            for(a in yellowCards){
                //timer gets cleared, but the yellowCard remains in the vector
                a.clearTimer()
            }
            yellowCards.clear()
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

            timeoutChronometer.setOnChronometerTickListener {
                if (timeoutChronometer.base < SystemClock.elapsedRealtime()) {
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
            yellowCards.add(YellowCard(numCards, MillisecondsPerMinute,isRunning,this))
        }

        button2Min.setOnClickListener{
            numCards++
            yellowCards.add(YellowCard(numCards, 2*MillisecondsPerMinute,isRunning,this))
        }
   }
}