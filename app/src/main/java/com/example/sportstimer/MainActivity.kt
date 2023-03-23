package com.example.sportstimer

import android.media.MediaPlayer
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat.getColor
import java.util.*

private const val MillisecondsPerMinute :Long = 60000
private const val SEEKER_FLOOR = MillisecondsPerMinute * 20

class YellowCard(inputId:Int, inputAudio:MediaPlayer, inputDur:Long, isRunning:Boolean, inputContext: MainActivity){
    private val idNum :Int = inputId
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
        id.setTextColor(getColor(inputContext,R.color.black))

        //chronometer content
        cardChronometer.layoutParams = TableRow.LayoutParams(
            TableRow.LayoutParams.MATCH_PARENT,
            TableRow.LayoutParams.WRAP_CONTENT,
            1.0f
        )
        cardChronometer.isCountDown = true
        cardChronometer.textSize = 30.toFloat()
        cardChronometer.setTextColor(getColor(inputContext,R.color.black))
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

    constructor(
        inputId:Int,
        inputAudio:MediaPlayer,
        isRunning:Boolean,
        inputContext: MainActivity,
        inputCardPause:Long,
        cardBase:Long):this(inputId,inputAudio,cardBase-SystemClock.elapsedRealtime(),isRunning,inputContext){
            cardPause = inputCardPause
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

    fun getID():Int{
        return idNum
    }

    fun getPause():Long{
        return cardPause
    }

    fun getBase():Long{
        return cardChronometer.base
    }

}

class MainActivity : ComponentActivity() {

    //scores
    var scoreLeft:Int = 0
    var scoreRight:Int = 0

    //state trackers
    val yellowCards: Vector<YellowCard> = Vector(3,3)
    var numCards = 0
    var isRunning = false
    var flagRunning = true
    var isTimeout = false
    var pauseTime:Long = SystemClock.elapsedRealtime()

    //audio player
    var auxCord:MediaPlayer = MediaPlayer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //timers
        val mainChronometer:Chronometer = findViewById(R.id.chronometer)
        val flagChronometer:Chronometer = findViewById(R.id.flagCountdown)
        val timeoutChronometer:Chronometer = findViewById(R.id.timeoutCounter)

        if(savedInstanceState == null){
            mainChronometer.base = SystemClock.elapsedRealtime()
            flagChronometer.base = SystemClock.elapsedRealtime() + SEEKER_FLOOR
            //timeoutChronometer is hidden, doesn't need to be initialised here
            pauseTime = SystemClock.elapsedRealtime()
        }else{
            //Set main and flag chronometers
            isRunning = savedInstanceState.getBoolean("isRunning")
            mainChronometer.base = savedInstanceState.getLong("mainBase")
            flagChronometer.base = mainChronometer.base + SEEKER_FLOOR
            if(isRunning){
                mainChronometer.start()
                flagChronometer.start()
                val buttonPlayPause = findViewById<ImageButton>(R.id.playPauseButton)
                buttonPlayPause.setImageResource(R.drawable.button_pause)
            }else{
                pauseTime = savedInstanceState.getLong("pauseTime")
                mainChronometer.base += SystemClock.elapsedRealtime() - pauseTime
                flagChronometer.base = mainChronometer.base + SEEKER_FLOOR
                pauseTime = SystemClock.elapsedRealtime()
            }

            //Set yellow cards
            val activeCards = savedInstanceState.getInt("ActiveCards")
            for(a in 0 until activeCards){
                val inputId = savedInstanceState.getInt("YC-ID$a")
                val inputCardPause = savedInstanceState.getLong("YC-Pause$a")
                val cardBase = savedInstanceState.getLong("YC-Base$a")
                yellowCards.add(YellowCard(inputId,auxCord,isRunning,this,inputCardPause,cardBase))
            }
            numCards = savedInstanceState.getInt("numCards")

            //Set Timeout chronometer
            isTimeout = savedInstanceState.getBoolean("isTimeout")
            timeoutChronometer.base = savedInstanceState.getLong("timeoutBase")
            if(isTimeout){
                timeoutChronometer.start()
                val buttonTimeout = findViewById<Button>(R.id.timeout)
                val timeoutRow = findViewById<TableRow>(R.id.timeoutRow)
                buttonTimeout.text = getString(R.string.ClearTimeout)
                timeoutRow.visibility = View.VISIBLE
            }

            //Set Scores
            val scoreLeftText = findViewById<TextView>(R.id.scoreLeft)
            val scoreRightText = findViewById<TextView>(R.id.scoreRight)
            scoreLeft = savedInstanceState.getInt("scoreLeft")
            scoreRight = savedInstanceState.getInt("scoreRight")
            scoreLeftText.text = scoreLeft.toString()
            scoreRightText.text = scoreRight.toString()

        }

        auxCord.release()
        auxCord = MediaPlayer.create(this,R.raw.ping)
        setListeners()
   }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        val mainChronometer:Chronometer = findViewById(R.id.chronometer)
        val timeoutChronometer:Chronometer = findViewById(R.id.timeoutCounter)

        //yellow cards
        //clean up cards first
        for(a in yellowCards.indices.reversed()){
            if(yellowCards[a].isTrash){
                yellowCards.removeAt(a)
            }
        }
        outState.putInt("ActiveCards",yellowCards.size)
        for(a in 0 until yellowCards.size){
            outState.putInt("YC-ID$a",yellowCards[a].getID())
            outState.putLong("YC-Pause$a",yellowCards[a].getPause())
            outState.putLong("YC-Base$a",yellowCards[a].getBase())
        }

        outState.putLong("mainBase",mainChronometer.base)
        outState.putLong("timeoutBase",timeoutChronometer.base)
        outState.putInt("scoreLeft",scoreLeft)
        outState.putInt("scoreRight",scoreRight)
        outState.putInt("numCards",numCards)
        outState.putBoolean("isRunning",isRunning)
        outState.putBoolean("isTimeout",isTimeout)
        outState.putLong("pauseTime",pauseTime)
        auxCord.release()
    }

    private fun setListeners(){
        //timers
        val mainChronometer:Chronometer = findViewById(R.id.chronometer)
        val flagChronometer:Chronometer = findViewById(R.id.flagCountdown)
        val timeoutChronometer:Chronometer = findViewById(R.id.timeoutCounter)
        val timeoutRow = findViewById<TableRow>(R.id.timeoutRow)

        //scores
        val scoreLeftText = findViewById<TextView>(R.id.scoreLeft)
        val scoreRightText = findViewById<TextView>(R.id.scoreRight)

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
            mainChronometer.base = SystemClock.elapsedRealtime()
            pauseTime = mainChronometer.base

            if(isRunning){
                buttonPlayPause.setImageResource(R.drawable.button_play)
                isRunning = false
            }

            flagChronometer.stop()
            flagChronometer.base = mainChronometer.base
            flagChronometer.base += SEEKER_FLOOR
            flagRunning = true

            scoreLeft = 0
            scoreRight = 0
            scoreLeftText.text = scoreLeft.toString()
            scoreRightText.text = scoreRight.toString()

            if (isTimeout) {
                timeoutChronometer.stop()
                timeoutChronometer.base = SystemClock.elapsedRealtime()
                buttonTimeout.text = getString(R.string.timeout)
                isTimeout = false
                timeoutRow.visibility = View.GONE
            }

            for(a in yellowCards){
                a.clearTimer()
            }
            yellowCards.clear()
            numCards = 0
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
                timeoutRow.visibility = View.GONE
            } else {
                timeoutChronometer.base = SystemClock.elapsedRealtime() + MillisecondsPerMinute
                timeoutChronometer.start()
                buttonTimeout.text = getString(R.string.ClearTimeout)
                timeoutRow.visibility = View.VISIBLE
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
                timeoutRow.visibility = View.GONE
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