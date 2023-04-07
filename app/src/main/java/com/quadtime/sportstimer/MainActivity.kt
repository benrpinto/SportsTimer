package com.quadtime.sportstimer

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import java.util.*
import kotlin.math.absoluteValue

private const val MillisecondsPerSecond : Long = 1000
private const val SecondsPerMinute : Long = 60
private const val MillisecondsPerMinute :Long = SecondsPerMinute* MillisecondsPerSecond
private const val MinutesPerHour : Long = 60
private const val MillisecondsPerHour : Long = MillisecondsPerMinute* MinutesPerHour

//default settings
private const val defFlagLength : Int = 20
private const val defScoreInc : Int = 10
private const val defTimeoutLength : Int = 1
private const val defYellow1Length : Int = 1
private const val defYellow2Length : Int = 2
private const val defAudioOn : Boolean = true

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

        //chronometer content
        cardChronometer.layoutParams = TableRow.LayoutParams(
            TableRow.LayoutParams.MATCH_PARENT,
            TableRow.LayoutParams.WRAP_CONTENT,
            1.0f
        )
        cardChronometer.textSize = 30.toFloat()
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

class MainActivity : AppCompatActivity() {

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

    //timer
    private val mainTimer = Timer()

    //base
    var mainBase = SystemClock.elapsedRealtime()
    var flagBase = SystemClock.elapsedRealtime()
    var timeoutBase = SystemClock.elapsedRealtime() + MillisecondsPerMinute

    //audio player
    private var auxCord:MediaPlayer = MediaPlayer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val myPref = PreferenceManager.getDefaultSharedPreferences(this)
        val audioOn = myPref.getBoolean("audioOn", defAudioOn)
        val seekerFloor =
            try {
                myPref.getString("flagLength", "$defFlagLength").toString().toInt()* MillisecondsPerMinute
            }catch(e:NumberFormatException){
                0
            }

        val mainChronometer:TextView = findViewById(R.id.chronometer)
        val flagChronometer:TextView = findViewById(R.id.flagCountdown)
        val tempHolder = SystemClock.elapsedRealtime()
        if(savedInstanceState == null){
            mainChronometer.text = timeFormatter(0,true)
            flagChronometer.text = timeFormatter(seekerFloor,true)
            mainBase = tempHolder
            flagBase = tempHolder + seekerFloor
            //timeoutChronometer is hidden, doesn't need to be initialised here
            pauseTime = tempHolder
        }else{
            //Set main and flag chronometers
            isRunning = savedInstanceState.getBoolean("isRunning")
            mainBase = savedInstanceState.getLong("mainBase")
            flagBase = mainBase + seekerFloor
            if(isRunning){
                val buttonPlayPause = findViewById<ImageButton>(R.id.playPauseButton)
                buttonPlayPause.setImageResource(R.drawable.button_pause)
            }else{
                pauseTime = savedInstanceState.getLong("pauseTime")
                mainBase += tempHolder - pauseTime
                pauseTime = tempHolder
                flagBase = mainBase + seekerFloor

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
            scoreLeftText.text = scoreLeft.toString().padStart(3,'0')
            scoreRightText.text = scoreRight.toString().padStart(3,'0')
        }

        auxCord.release()
        auxCord = MediaPlayer.create(this, R.raw.ping)
        if(!audioOn){
            auxCord.setVolume(0F,0F)
        }
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
        val buttonSettings = findViewById<ImageButton>(R.id.settingsButton)
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

        //Settings
        val myPref = PreferenceManager.getDefaultSharedPreferences(this)
        val seekerFloor =
            try {
                myPref.getString("flagLength", "$defFlagLength").toString().toInt()* MillisecondsPerMinute
            }catch(e:NumberFormatException){
                0
            }
        val scoreIncrement =
            try {
                myPref.getString("scoreInc","$defScoreInc").toString().toInt()
            }catch(e:NumberFormatException){
                0
            }
        val timeoutLength =
            try {
                myPref.getString("timeoutLength","$defTimeoutLength").toString().toInt()* MillisecondsPerMinute
            }catch(e:NumberFormatException){
                0
            }

        val yellow1Length =
            try {
                myPref.getString("yellow1Length","$defYellow1Length").toString().toInt()* MillisecondsPerMinute
            }catch(e:NumberFormatException){
                0
            }

        val yellow2Length =
            try {
                myPref.getString("yellow2Length","$defYellow2Length").toString().toInt()* MillisecondsPerMinute
            }catch(e:NumberFormatException){
                0
            }

        val y1Text = (yellow1Length/MillisecondsPerMinute).toString() + " minute"
        button1Min.text = y1Text
        val y2Text = (yellow2Length/MillisecondsPerMinute).toString() + " minute"
        button2Min.text = y2Text

        mainTimer.schedule(object : TimerTask() {
            override fun run() {
             runOnUiThread { updateTimers() }
            }
            fun updateTimers() {
                if(isRunning) {
                    mainChronometer.text =
                        timeFormatter(SystemClock.elapsedRealtime() - mainBase,true)
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
                        timeFormatter(timeoutBase - SystemClock.elapsedRealtime(),false)
                    timeoutTickListener()
                }
            }
        },0,20)

        //button listeners
        buttonSettings.setOnClickListener{
            val intent = Intent(this,SettingsActivity::class.java)
            startActivity(intent)
        }

        buttonPlayPause.setOnClickListener {
            buttonSettings.visibility = View.INVISIBLE
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
                    flagBase = mainBase + seekerFloor
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
            buttonSettings.visibility = View.VISIBLE
            mainBase = SystemClock.elapsedRealtime()
            pauseTime = mainBase

            if(isRunning){
                buttonPlayPause.setImageResource(R.drawable.button_play)
                isRunning = false
            }

            flagBase = mainBase
            flagBase += seekerFloor
            flagRunning = true

            mainChronometer.text = timeFormatter(0,true)
            flagChronometer.text = timeFormatter(seekerFloor,true)

            scoreLeft = 0
            scoreRight = 0
            scoreLeftText.text = scoreLeft.toString().padStart(3,'0')
            scoreRightText.text = scoreRight.toString().padStart(3,'0')

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
            scoreLeft += scoreIncrement
            scoreLeftText.text = scoreLeft.toString().padStart(3,'0')
        }
        buttonUpRight.setOnClickListener{
            scoreRight += scoreIncrement
            scoreRightText.text = scoreRight.toString().padStart(3,'0')
        }
        buttonDownLeft.setOnClickListener{
            scoreLeft = maxOf(scoreLeft-scoreIncrement,0)
            scoreLeftText.text = scoreLeft.toString().padStart(3,'0')
        }
        buttonDownRight.setOnClickListener{
            scoreRight = maxOf(scoreRight-scoreIncrement,0)
            scoreRightText.text = scoreRight.toString().padStart(3,'0')
        }

        buttonTimeout.setOnClickListener {
            if (isTimeout) {
                timeoutBase = SystemClock.elapsedRealtime()
                buttonTimeout.text = getString(R.string.timeout)
                timeoutRow.visibility = View.GONE
            } else {
                timeoutBase = SystemClock.elapsedRealtime() + timeoutLength
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
            yellowCards.add(YellowCard(numCards,auxCord, yellow1Length,this))
        }

        button2Min.setOnClickListener{
            numCards++
            yellowCards.add(YellowCard(numCards,auxCord, yellow2Length,this))
        }
    }

    private fun flagTickListener(){
        if(flagBase < SystemClock.elapsedRealtime() && flagRunning && isRunning){
            auxCord.start()
            flagBase = SystemClock.elapsedRealtime()
            flagRunning = false
            val flagChronometer:TextView = findViewById(R.id.flagCountdown)
            flagChronometer.text = timeFormatter(0,true)
        }
    }
    private fun timeoutTickListener() {
        if (timeoutBase < SystemClock.elapsedRealtime()) {
            val timeoutRow = findViewById<TableRow>(R.id.timeoutRow)
            val buttonTimeout = findViewById<Button>(R.id.timeout)
            auxCord.start()
            timeoutBase = SystemClock.elapsedRealtime()
            isTimeout = false
            buttonTimeout.text = getString(R.string.timeout)
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
    if(min != 0.toLong() || show||!inclMilli){
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