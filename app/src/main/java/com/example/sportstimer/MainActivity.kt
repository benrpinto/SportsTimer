package com.example.sportstimer

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat.getColor
import java.util.*
import kotlin.math.absoluteValue

private const val MillisecondsPerSecond : Long = 1000
private const val SecondsPerMinute : Long = 60
private const val MillisecondsPerMinute :Long = SecondsPerMinute* MillisecondsPerSecond
private const val MinutesPerHour : Long = 60
private const val MillisecondsPerHour : Long = MillisecondsPerMinute* MinutesPerHour
private const val SEEKER_FLOOR = MillisecondsPerMinute * 20

class YellowCard(inputId:Int, inputAudio:MediaPlayer, inputDur:Long, inputContext: MainActivity){
    private val idNum :Int = inputId
    private val cardRow : TableRow = TableRow(inputContext)
    private val id : TextView = TextView(inputContext)
    private val cardChronometer : TextView = TextView(inputContext)
    private var cardBase:Long = SystemClock.elapsedRealtime()
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
        cardChronometer.textSize = 30.toFloat()
        cardChronometer.setTextColor(getColor(inputContext,R.color.black))
        cardChronometer.text = timeFormatter(inputDur,false)
        cardBase = SystemClock.elapsedRealtime() + inputDur


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
        inputContext: MainActivity,
        inputCardPause:Long,
        inputCardBase:Long):this(inputId,inputAudio,inputCardBase-SystemClock.elapsedRealtime(),inputContext){
        val tempHolder = SystemClock.elapsedRealtime()
        cardBase = inputCardBase + tempHolder - inputCardPause
        cardPause = tempHolder
        cardChronometer.text = timeFormatter(cardBase-tempHolder,false)
    }

    private fun clearOut() {
        if(!isTrash) {
            (cardRow.parent as ViewGroup).removeView(cardRow)
            isTrash = true
        }
    }

    fun cardTickListener() {
        cardChronometer.text = timeFormatter(cardBase - SystemClock.elapsedRealtime(),false)
        if (cardBase < SystemClock.elapsedRealtime()){
            audio.start()
            clearOut()
        }
    }


    fun pauseTimer(){
        if(!isTrash) {
            cardPause = SystemClock.elapsedRealtime()
        }
    }
    fun resumeTimer(){
        if(!isTrash) {
            cardBase += SystemClock.elapsedRealtime() - cardPause
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
        return cardBase
    }

}

class MainActivity : ComponentActivity() {

    //scores
    private var scoreLeft:Int = 0
    private var scoreRight:Int = 0

    //state trackers
    val yellowCards: Vector<YellowCard> = Vector(3,3)
    private var numCards = 0
    var isRunning = false
    var flagRunning = true
    var isTimeout = false
    private var pauseTime:Long = SystemClock.elapsedRealtime()

    //base
    var mainBase = SystemClock.elapsedRealtime()
    var flagBase = SystemClock.elapsedRealtime() + SEEKER_FLOOR
    var timeoutBase = SystemClock.elapsedRealtime() + MillisecondsPerMinute

    //audio player
    private var auxCord:MediaPlayer = MediaPlayer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mainChronometer:TextView = findViewById(R.id.chronometer)
        val flagChronometer:TextView = findViewById(R.id.flagCountdown)
        val tempHolder = SystemClock.elapsedRealtime()
        if(savedInstanceState == null){
            mainChronometer.text = timeFormatter(0,true)
            flagChronometer.text = timeFormatter(SEEKER_FLOOR,true)
            mainBase = tempHolder
            flagBase = tempHolder + SEEKER_FLOOR
            //timeoutChronometer is hidden, doesn't need to be initialised here
            pauseTime = tempHolder
        }else{
            //Set main and flag chronometers
            isRunning = savedInstanceState.getBoolean("isRunning")
            mainBase = savedInstanceState.getLong("mainBase")
            flagBase = mainBase + SEEKER_FLOOR
            if(isRunning){
                val buttonPlayPause = findViewById<ImageButton>(R.id.playPauseButton)
                buttonPlayPause.setImageResource(R.drawable.button_pause)
            }else{
                pauseTime = savedInstanceState.getLong("pauseTime")
                mainBase += tempHolder - pauseTime
                pauseTime = tempHolder
                flagBase = mainBase + SEEKER_FLOOR

            }
            mainChronometer.text = timeFormatter(tempHolder-mainBase,true)
            flagChronometer.text = timeFormatter(flagBase - tempHolder,true)

            //Set yellow cards
            val activeCards = savedInstanceState.getInt("ActiveCards")
            for(a in 0 until activeCards){
                val inputId = savedInstanceState.getInt("YC-ID$a")
                val inputCardPause = savedInstanceState.getLong("YC-Pause$a")
                val cardBase = savedInstanceState.getLong("YC-Base$a")
                yellowCards.add(YellowCard(inputId,auxCord,this,inputCardPause,cardBase))
            }
            numCards = savedInstanceState.getInt("numCards")

            //Set Timeout chronometer
            isTimeout = savedInstanceState.getBoolean("isTimeout")
            timeoutBase = savedInstanceState.getLong("timeoutBase")
            if(isTimeout){
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

        outState.putLong("mainBase",mainBase)
        outState.putLong("timeoutBase",timeoutBase)
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
        val mainChronometer:TextView = findViewById(R.id.chronometer)
        val flagChronometer:TextView = findViewById(R.id.flagCountdown)
        val timeoutChronometer:TextView = findViewById(R.id.timeoutCounter)
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

        Handler(Looper.getMainLooper()).post(object : Runnable {
            override fun run() {
                Handler(Looper.getMainLooper()).postDelayed(this,20)
                if(isRunning) {
                    mainChronometer.text = timeFormatter(SystemClock.elapsedRealtime() - mainBase,true)
                    if(flagRunning) {
                        flagChronometer.text =
                            timeFormatter(flagBase - SystemClock.elapsedRealtime(),true)
                        flagTickListener()
                    }
                    for(a in yellowCards.indices.reversed()){
                        if(yellowCards[a].isTrash){
                            yellowCards.removeAt(a)
                        }else{
                            yellowCards[a].cardTickListener()
                        }
                    }
                }
                if(isTimeout) {
                    timeoutChronometer.text =
                        timeFormatter(timeoutBase - SystemClock.elapsedRealtime(),true)
                    timeoutTickListener()
                }
            }
        })

        //button listeners
        buttonPlayPause.setOnClickListener {
            if(isRunning) {
                pauseTime = SystemClock.elapsedRealtime()
                buttonPlayPause.setImageResource(R.drawable.button_play)
                for(a in yellowCards.indices.reversed()){
                    if(yellowCards[a].isTrash){
                        yellowCards.removeAt(a)
                    }else{
                        yellowCards[a].pauseTimer()
                    }
                }
            }else{
                mainBase += SystemClock.elapsedRealtime() - pauseTime
                if(flagRunning) {
                    flagBase = mainBase + SEEKER_FLOOR
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
            mainBase = SystemClock.elapsedRealtime()
            pauseTime = mainBase

            if(isRunning){
                buttonPlayPause.setImageResource(R.drawable.button_play)
                isRunning = false
            }

            flagBase = mainBase
            flagBase += SEEKER_FLOOR
            flagRunning = true

            mainChronometer.text = timeFormatter(0,true)
            flagChronometer.text = timeFormatter(SEEKER_FLOOR,true)

            scoreLeft = 0
            scoreRight = 0
            scoreLeftText.text = scoreLeft.toString()
            scoreRightText.text = scoreRight.toString()

            if (isTimeout) {
                timeoutBase = SystemClock.elapsedRealtime()
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
                timeoutBase = SystemClock.elapsedRealtime()
                buttonTimeout.text = getString(R.string.timeout)
                timeoutRow.visibility = View.GONE
            } else {
                timeoutBase = SystemClock.elapsedRealtime() + MillisecondsPerMinute
                buttonTimeout.text = getString(R.string.ClearTimeout)
                timeoutRow.visibility = View.VISIBLE
            }
            isTimeout = !isTimeout
        }

        buttonTimeoutMinus.setOnClickListener {
            if(isTimeout) {
                timeoutBase -= MillisecondsPerMinute
            }
        }
        buttonTimeoutPlus.setOnClickListener {
            if(isTimeout) {
                timeoutBase += MillisecondsPerMinute
            }
        }

        button1Min.setOnClickListener{
            numCards++
            yellowCards.add(YellowCard(numCards,auxCord, MillisecondsPerMinute,this))
        }

        button2Min.setOnClickListener{
            numCards++
            yellowCards.add(YellowCard(numCards,auxCord, 2*MillisecondsPerMinute,this))
        }
    }

    private fun flagTickListener(){
        if(flagBase < SystemClock.elapsedRealtime() && flagRunning && isRunning){
            auxCord.start()
            flagBase = SystemClock.elapsedRealtime()
            flagRunning = false
        }
    }
    private fun timeoutTickListener() {
        if (timeoutBase < SystemClock.elapsedRealtime()) {
            val timeoutRow = findViewById<TableRow>(R.id.timeoutRow)
            val buttonTimeout = findViewById<Button>(R.id.timeout)
            auxCord.start()
            timeoutBase = SystemClock.elapsedRealtime()
            buttonTimeout.text = getString(R.string.timeout)
            isTimeout = false
            timeoutRow.visibility = View.GONE
        }
    }
}
fun timeFormatter(milliTime:Long, inclMilli:Boolean):String{

    val myMilli = milliTime.absoluteValue
    var toReturn = ""
    var show = false

    val milli:Long = myMilli.mod(MillisecondsPerSecond)
    val sec:Long = (myMilli/ MillisecondsPerSecond).mod(SecondsPerMinute)
    val min:Long = (myMilli/ MillisecondsPerMinute).mod(MinutesPerHour)
    val hour:Long = (myMilli/ MillisecondsPerHour)

    if(milliTime<0){
        toReturn += "-"
    }
    if(hour != 0.toLong()){
        show = true
        toReturn += hour.toString()
        toReturn += ":"
    }
    if(min != 0.toLong() || show){
        toReturn += min.toString().padStart(2,'0')
        toReturn += ":"
    }
    toReturn += sec.toString().padStart(2,'0')
    if(inclMilli) {
        toReturn += "."
        toReturn += milli.toString().padStart(3, '0')
    }

    return toReturn
}