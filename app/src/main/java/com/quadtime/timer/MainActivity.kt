package com.quadtime.timer

import android.content.Intent
import android.content.SharedPreferences
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
import com.quadtime.timer.constants.*
import java.util.*
import kotlin.math.absoluteValue

class MainActivity : AppCompatActivity() {

    //scores
    private var scoreLeft: Int = 0
    private var scoreRight: Int = 0

    //state trackers
    val yellowCards: Vector<YellowCard> = Vector(3,3)
    private var numCards = 0
    var isRunning = false
    var flagRunning = true
    var isTimeout = false
    private var pauseTime: Long = SystemClock.elapsedRealtime()

    //timers
    private val mainTimer = Timer()
    private lateinit var heatTimer: HeatTimer

    //base
    var mainBase = SystemClock.elapsedRealtime()
    var flagBase = SystemClock.elapsedRealtime()
    var timeoutBase = SystemClock.elapsedRealtime() + MillisecondsPerMinute

    //audio, vibration, and notification handler
    private lateinit var klaxon: Alert

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val myPref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        val audioVol = myPref.getInt(getString(R.string.audio_vol_key), defAudioVol)
        val vibeOn = myPref.getBoolean(getString(R.string.vibe_on_key),defVibeOn)
        klaxon = Alert(this,audioVol,vibeOn)
        heatTimer = HeatTimer(klaxon, this)

        val seekerFloor =
            try {
                (myPref.getString(getString(R.string.flag_length_key), "$defFlagLength")?.toInt()
                    ?: defFlagLength) * MillisecondsPerMinute
            }catch(e: NumberFormatException){
                defFlagLength* MillisecondsPerMinute
            }

        val inBundle = ActivityChecker.getBundle()

        if(!inBundle.isEmpty){
            restoreFromBundle(inBundle, seekerFloor)
        }else if(savedInstanceState != null){
            restoreFromBundle(savedInstanceState, seekerFloor)
        }else{
            val mainChronometer: TextView = findViewById(R.id.chronometer)
            val flagChronometer: TextView = findViewById(R.id.flagCountdown)
            val tempHolder = SystemClock.elapsedRealtime()
            mainChronometer.text = timeFormatter(0,true)
            flagChronometer.text = timeFormatter(seekerFloor,true)
            mainBase = tempHolder
            flagBase = tempHolder + seekerFloor
            //timeoutChronometer is hidden, doesn't need to be initialised here
            pauseTime = tempHolder
            heatTimer.restoreValues(this,pauseTime, tempHolder)
        }

        setListeners()
    }

    override fun onStart(){
        super.onStart()
        val mainChronometer: TextView = findViewById(R.id.chronometer)
        val flagChronometer: TextView = findViewById(R.id.flagCountdown)
        val timeoutChronometer: TextView = findViewById(R.id.timeoutCounter)

        mainTimer.schedule(object: TimerTask() {
            override fun run() {
                runOnUiThread { updateTimers() }
            }
            fun updateTimers() {
                if(isRunning) {
                    mainChronometer.text =
                        timeFormatter(SystemClock.elapsedRealtime() - mainBase,true)
                    heatTimer.tickListener()
                    if(flagRunning) {
                        flagChronometer.text =
                            timeFormatter(flagBase - SystemClock.elapsedRealtime(),true)
                        flagTickListener()
                    }
                    for(a in yellowCards.indices.reversed()){
                        if(yellowCards[a].isTrash){
                            yellowCards.removeAt(a)
                        }else{
                            yellowCards[a].tickListener()
                        }
                    }
                }
                if(isTimeout) {
                    timeoutChronometer.text =
                        timeFormatter(timeoutBase - SystemClock.elapsedRealtime(),false)
                    timeoutTickListener()
                }
            }
        },0, MillisecondsPerUpdate)
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
        outState.putLong("heatPause",heatTimer.getPause())
        outState.putLong("heatBase",heatTimer.getSaveBase())
        outState.putInt("scoreLeft",scoreLeft)
        outState.putInt("scoreRight",scoreRight)
        outState.putInt("numCards",numCards)
        outState.putBoolean("isRunning",isRunning)
        outState.putBoolean("isTimeout",isTimeout)
        outState.putLong("pauseTime",pauseTime)
    }

    private fun restoreFromBundle(savedInstanceState: Bundle, seekerFloor: Long){
        val mainChronometer: TextView = findViewById(R.id.chronometer)
        val flagChronometer: TextView = findViewById(R.id.flagCountdown)
        val tempHolder = SystemClock.elapsedRealtime()
        //Set main and flag chronometers
        isRunning = savedInstanceState.getBoolean("isRunning")
        mainBase = savedInstanceState.getLong("mainBase")
        flagBase = mainBase + seekerFloor
        if(isRunning){
            val buttonPlayPause: ImageButton = findViewById(R.id.playPauseButton)
            buttonPlayPause.setImageResource(R.drawable.pause)
            buttonPlayPause.contentDescription = getString(R.string.pause_button)
        }else{
            pauseTime = savedInstanceState.getLong("pauseTime")
            mainBase += tempHolder - pauseTime
            pauseTime = tempHolder
            flagBase = mainBase + seekerFloor

        }
        mainChronometer.text = timeFormatter(tempHolder-mainBase,true)
        if (flagBase - tempHolder >= 0L) {
            flagRunning = true
            flagChronometer.text = timeFormatter(flagBase - tempHolder,true)
        }else{
            flagRunning = false
            flagChronometer.text = timeFormatter(0,true)
        }

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

        //set heat timer
        val inputHeatPause = savedInstanceState.getLong("heatPause")
        val inputHeatBase = savedInstanceState.getLong("heatBase")

        heatTimer.restoreValues(this,inputHeatPause, inputHeatBase)

        //Set Timeout chronometer
        isTimeout = savedInstanceState.getBoolean("isTimeout")
        timeoutBase = savedInstanceState.getLong("timeoutBase")
        if(isTimeout){
            val buttonTimeout: Button = findViewById(R.id.timeout)
            val timeoutRow: TableRow = findViewById(R.id.timeoutRow)
            buttonTimeout.text = getString(R.string.clear_timeout)
            timeoutRow.visibility = View.VISIBLE
        }

        //Set Scores
        val scoreLeftText: TextView = findViewById(R.id.scoreLeft)
        val scoreRightText: TextView = findViewById(R.id.scoreRight)
        scoreLeft = savedInstanceState.getInt("scoreLeft")
        scoreRight = savedInstanceState.getInt("scoreRight")
        scoreLeftText.text = scoreLeft.toString().padStart(3,'0')
        scoreRightText.text = scoreRight.toString().padStart(3,'0')
    }

    private fun setListeners(){
        //timers
        val timeoutRow: TableRow = findViewById(R.id.timeoutRow)

        //scores
        val scoreLeftText: TextView = findViewById(R.id.scoreLeft)
        val scoreRightText: TextView = findViewById(R.id.scoreRight)

        //buttons
        val buttonSettings: ImageButton = findViewById(R.id.settingsButton)
        val buttonPlayPause: ImageButton = findViewById(R.id.playPauseButton)
        val buttonReset: ImageButton = findViewById(R.id.resetButton)
        val buttonUpLeft: ImageButton = findViewById(R.id.scoreUpLeft)
        val buttonUpRight: ImageButton = findViewById(R.id.scoreUpRight)
        val buttonDownLeft: ImageButton = findViewById(R.id.scoreDownLeft)
        val buttonDownRight: ImageButton = findViewById(R.id.scoreDownRight)
        val buttonTimeout: Button = findViewById(R.id.timeout)
        val buttonTimeoutMinus: Button = findViewById(R.id.minus1)
        val buttonTimeoutPlus: Button = findViewById(R.id.plus1)
        val button1Min: Button = findViewById(R.id.yellow1)
        val button2Min: Button = findViewById(R.id.yellow2)

        //Settings
        val myPref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val seekerFloor =
            try {
                (myPref.getString(getString(R.string.flag_length_key), "$defFlagLength")?.toInt()
                    ?: defFlagLength) * MillisecondsPerMinute
            }catch(e: NumberFormatException){
                defFlagLength* MillisecondsPerMinute
            }
        val scoreIncrement =
            try {
                (myPref.getString(getString(R.string.score_inc_key),"$defScoreInc")?.toInt()
                    ?: defScoreInc)
            }catch(e: NumberFormatException){
                defScoreInc
            }
        val timeoutLength =
            try {
                (myPref.getString(getString(R.string.timeout_length_key),"$defTimeoutLength")?.toInt()
                    ?: defTimeoutLength) * MillisecondsPerMinute
            }catch(e: NumberFormatException){
                defTimeoutLength* MillisecondsPerMinute
            }

        val yellow1Length =
            try {
                (myPref.getString(getString(R.string.yellow_1_length_key),"$defYellow1Length")
                    ?.toInt() ?: defYellow1Length) * MillisecondsPerMinute
            }catch(e: NumberFormatException){
                defYellow1Length* MillisecondsPerMinute
            }

        val yellow2Length =
            try {
                (myPref.getString(getString(R.string.yellow_2_length_key),"$defYellow2Length")
                    ?.toInt() ?: defYellow2Length) * MillisecondsPerMinute
            }catch(e: NumberFormatException){
                defYellow2Length* MillisecondsPerMinute
            }
        val confirmReset = myPref.getBoolean(getString(R.string.confirm_reset_key),defConfirmReset)

        val y1Num = (yellow1Length/MillisecondsPerMinute).toInt()
        button1Min.text = resources.getQuantityString(R.plurals.minutes, y1Num, y1Num)

        val y2Num = (yellow2Length/MillisecondsPerMinute).toInt()
        button2Min.text = resources.getQuantityString(R.plurals.minutes, y2Num, y2Num)


        //button listeners
        buttonSettings.setOnClickListener{
            val intent = Intent(this,SettingsActivity::class.java)
            startActivity(intent)
        }

        buttonPlayPause.setOnClickListener {
            if(isRunning) {
                pauseTime = SystemClock.elapsedRealtime()
                buttonPlayPause.setImageResource(R.drawable.play)
                buttonPlayPause.contentDescription = getString(R.string.play_button)
                for(a in yellowCards.indices.reversed()){
                    if(yellowCards[a].isTrash){
                        yellowCards.removeAt(a)
                    }else{
                        yellowCards[a].pauseTimer()
                    }
                }
                heatTimer.pauseTimer()
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
                heatTimer.resumeTimer()
                buttonPlayPause.setImageResource(R.drawable.pause)
                buttonPlayPause.contentDescription = getString(R.string.pause_button)
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
        val mainChronometer: TextView = findViewById(R.id.chronometer)
        val flagChronometer: TextView = findViewById(R.id.flagCountdown)
        val timeoutRow: TableRow = findViewById(R.id.timeoutRow)

        //scores
        val scoreLeftText: TextView = findViewById(R.id.scoreLeft)
        val scoreRightText: TextView = findViewById(R.id.scoreRight)

        //buttons
        val buttonPlayPause: ImageButton = findViewById(R.id.playPauseButton)
        val buttonTimeout: Button = findViewById(R.id.timeout)

        val myPref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val seekerFloor =
            try {
                (myPref.getString(getString(R.string.flag_length_key), "$defFlagLength")?.toInt()
                    ?: defFlagLength) * MillisecondsPerMinute
            }catch(e: NumberFormatException){
                defFlagLength* MillisecondsPerMinute
            }

        mainBase = SystemClock.elapsedRealtime()
        pauseTime = mainBase

        if(isRunning){
            buttonPlayPause.setImageResource(R.drawable.play)
            buttonPlayPause.contentDescription = getString(R.string.play_button)
            isRunning = false
        }

        flagBase = mainBase
        flagBase += seekerFloor
        flagRunning = true

        heatTimer.restoreValues(this,mainBase,mainBase)

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
            val myPref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
            val nonZeroSeekerFloor = myPref.getString(getString(R.string.flag_length_key), "$defFlagLength") != "0"
            //Do not ping if the seeker floor is set to 0
            if (nonZeroSeekerFloor){
                klaxon.ping(FlagNotification, getString(R.string.notification_flag_desc))
            }

            flagBase = SystemClock.elapsedRealtime()
            flagRunning = false
            val flagChronometer: TextView = findViewById(R.id.flagCountdown)
            flagChronometer.text = timeFormatter(0,true)
        }
    }
    private fun timeoutTickListener() {
        if (timeoutBase < SystemClock.elapsedRealtime()) {
            val timeoutRow: TableRow = findViewById(R.id.timeoutRow)
            val buttonTimeout: Button = findViewById(R.id.timeout)
            klaxon.ping(TimeoutNotification,getString(R.string.notification_timeout_desc))
            timeoutBase = SystemClock.elapsedRealtime()
            isTimeout = false
            buttonTimeout.text = getString(R.string.timeout)
            timeoutRow.visibility = View.GONE
        }
    }
}

fun timeFormatter(milliTime: Long, inclMilli: Boolean): String{
    val toReturn = StringBuilder()
    val myMilli = milliTime.absoluteValue
    val milli: Long = myMilli.mod(MillisecondsPerSecond)/ MillisecondsPerTenth
    val sec: Long = (myMilli/ MillisecondsPerSecond).mod(SecondsPerMinute)
    val min: Long = (myMilli/ MillisecondsPerMinute).mod(MinutesPerHour)

    if(milliTime < 0){
        toReturn.append("-")
    }

    if(myMilli > MillisecondsPerHour){
        val hour: Long = (myMilli/ MillisecondsPerHour)
        toReturn.append(hour)
        toReturn.append(":")
    }

    toReturn.append(min.toString().padStart(2,'0'))
    toReturn.append(":")

    toReturn.append(sec.toString().padStart(2,'0'))
    if(inclMilli) {
        toReturn.append(".")
        toReturn.append(milli)
    }
    return toReturn.toString()
}