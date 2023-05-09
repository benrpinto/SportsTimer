package com.quadtime.timer

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
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
private const val defAudioVol : Int = 100
private const val defVibeOn : Boolean = true
private const val defConfirmReset : Boolean = true

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
    private lateinit var klaxon:Alert

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val myPref = PreferenceManager.getDefaultSharedPreferences(this)

        val audioVol = myPref.getInt(getString(R.string.audio_vol_key), defAudioVol)
        val vibeOn = myPref.getBoolean(getString(R.string.vibe_on_key),defVibeOn)
        klaxon = Alert(this,audioVol,vibeOn)

        val seekerFloor =
            try {
                myPref.getString(getString(R.string.flag_length_key), "$defFlagLength").toString().toInt()* MillisecondsPerMinute
            }catch(e:NumberFormatException){
                0
            }

        val inBundle = ActivityChecker.getBundle()

        if(!inBundle.isEmpty){
            restoreFromBundle(inBundle, seekerFloor)
        }else if(savedInstanceState != null){
            restoreFromBundle(savedInstanceState, seekerFloor)
        }else{
            val mainChronometer:TextView = findViewById(R.id.chronometer)
            val flagChronometer:TextView = findViewById(R.id.flagCountdown)
            val tempHolder = SystemClock.elapsedRealtime()
            mainChronometer.text = timeFormatter(0,true)
            flagChronometer.text = timeFormatter(seekerFloor,true)
            mainBase = tempHolder
            flagBase = tempHolder + seekerFloor
            //timeoutChronometer is hidden, doesn't need to be initialised here
            pauseTime = tempHolder
        }

        setListeners()
    }

    override fun onStart(){
        super.onStart()
        val mainChronometer:TextView = findViewById(R.id.chronometer)
        val flagChronometer:TextView = findViewById(R.id.flagCountdown)
        val timeoutChronometer:TextView = findViewById(R.id.timeoutCounter)

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
    }

    override fun onResume() {
        super.onResume()
        ActivityChecker.activityResumed()
    }

    override fun onPause() {
        super.onPause()
        ActivityChecker.activityPaused()
    }

    override fun onDestroy(){
        super.onDestroy()
        val outBundle = Bundle()
        saveToBundle(outBundle)
        ActivityChecker.setBundle(outBundle)

        mainTimer.cancel()
        yellowCards.clear()
        klaxon.release()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        saveToBundle(outState)
    }

    private fun saveToBundle(outState: Bundle){
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
    }

    private fun restoreFromBundle(savedInstanceState: Bundle, seekerFloor:Long){
        val mainChronometer:TextView = findViewById(R.id.chronometer)
        val flagChronometer:TextView = findViewById(R.id.flagCountdown)
        val tempHolder = SystemClock.elapsedRealtime()
        //Set main and flag chronometers
        isRunning = savedInstanceState.getBoolean("isRunning")
        mainBase = savedInstanceState.getLong("mainBase")
        flagBase = mainBase + seekerFloor
        if(isRunning){
            val buttonPlayPause = findViewById<ImageButton>(R.id.playPauseButton)
            buttonPlayPause.setImageResource(R.drawable.pause)
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
            var inputCardPause = savedInstanceState.getLong("YC-Pause$a")
            var cardBase = savedInstanceState.getLong("YC-Base$a")

            //this is to prevent the yellow card timer from displaying a lower value
            //when pausing the timer and restoring the activity
            if(!isRunning){
                //it has to be done here, and not in yellowCard
                //because yellowCard doesn't know if it's paused or not
                cardBase += tempHolder - inputCardPause
                inputCardPause = tempHolder
            }
            yellowCards.add(YellowCard(inputId,klaxon,this,inputCardPause,cardBase))
        }
        numCards = savedInstanceState.getInt("numCards")

        //Set Timeout chronometer
        isTimeout = savedInstanceState.getBoolean("isTimeout")
        timeoutBase = savedInstanceState.getLong("timeoutBase")
        if(isTimeout){
            val buttonTimeout = findViewById<Button>(R.id.timeout)
            val timeoutRow = findViewById<TableRow>(R.id.timeoutRow)
            buttonTimeout.text = getString(R.string.clear_timeout)
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

    private fun setListeners(){
        //timers
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
                myPref.getString(getString(R.string.flag_length_key), "$defFlagLength").toString().toInt()* MillisecondsPerMinute
            }catch(e:NumberFormatException){
                0
            }
        val scoreIncrement =
            try {
                myPref.getString(getString(R.string.score_inc_key),"$defScoreInc").toString().toInt()
            }catch(e:NumberFormatException){
                0
            }
        val timeoutLength =
            try {
                myPref.getString(getString(R.string.timeout_length_key),"$defTimeoutLength").toString().toInt()* MillisecondsPerMinute
            }catch(e:NumberFormatException){
                0
            }

        val yellow1Length =
            try {
                myPref.getString(getString(R.string.yellow_1_length_key),"$defYellow1Length").toString().toInt()* MillisecondsPerMinute
            }catch(e:NumberFormatException){
                0
            }

        val yellow2Length =
            try {
                myPref.getString(getString(R.string.yellow_2_length_key),"$defYellow2Length").toString().toInt()* MillisecondsPerMinute
            }catch(e:NumberFormatException){
                0
            }
        val confirmReset = myPref.getBoolean(getString(R.string.confirm_reset_key),defConfirmReset)

        val y1Text = (yellow1Length/MillisecondsPerMinute).toString() + " minute"
        button1Min.text = y1Text
        val y2Text = (yellow2Length/MillisecondsPerMinute).toString() + " minute"
        button2Min.text = y2Text

        //button listeners
        buttonSettings.setOnClickListener{
            val intent = Intent(this,SettingsActivity::class.java)
            startActivity(intent)
        }

        buttonPlayPause.setOnClickListener {
            if(isRunning) {
                pauseTime = SystemClock.elapsedRealtime()
                buttonPlayPause.setImageResource(R.drawable.play)
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
                buttonPlayPause.setImageResource(R.drawable.pause)
            }
            isRunning = !isRunning
        }

        buttonReset.setOnClickListener{
            if (confirmReset) {
                showConfirmDialog()
            }else{
                resetTimer()
            }
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
                buttonTimeout.text = getString(R.string.clear_timeout)
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
            yellowCards.add(YellowCard(numCards,klaxon, yellow1Length,this))
        }

        button2Min.setOnClickListener{
            numCards++
            yellowCards.add(YellowCard(numCards,klaxon, yellow2Length,this))
        }
    }

    private fun showConfirmDialog(){
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.apply{
            setTitle(getString(R.string.reset_title))
            setMessage(getString(R.string.reset_message))
            setPositiveButton(getString(R.string.reset_positive)){ _, _->
                resetTimer()
            }
            setNegativeButton(getString(R.string.cancel)){ _, _->}
        }.create().show()
    }

    private fun resetTimer(){
        //timers
        val mainChronometer:TextView = findViewById(R.id.chronometer)
        val flagChronometer:TextView = findViewById(R.id.flagCountdown)
        val timeoutRow = findViewById<TableRow>(R.id.timeoutRow)

        //scores
        val scoreLeftText = findViewById<TextView>(R.id.scoreLeft)
        val scoreRightText = findViewById<TextView>(R.id.scoreRight)

        //buttons
        val buttonPlayPause = findViewById<ImageButton>(R.id.playPauseButton)
        val buttonTimeout = findViewById<Button>(R.id.timeout)

        val myPref = PreferenceManager.getDefaultSharedPreferences(this)
        val seekerFloor =
            try {
                myPref.getString(getString(R.string.flag_length_key), "$defFlagLength").toString().toInt()* MillisecondsPerMinute
            }catch(e:NumberFormatException){
                0
            }

        mainBase = SystemClock.elapsedRealtime()
        pauseTime = mainBase

        if(isRunning){
            buttonPlayPause.setImageResource(R.drawable.play)
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

    private fun flagTickListener(){
        if(flagBase < SystemClock.elapsedRealtime() && flagRunning && isRunning){
            klaxon.ping()
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
            klaxon.ping()
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