package com.quadtime.timer

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.quadtime.timer.constants.*
import java.util.*

class MainActivity : AppCompatActivity() {

    //scores
    private var scoreLeft: Int = 0
    private var scoreRight: Int = 0

    //state trackers
    val foulCards: Vector<FoulCard> = Vector(3,3)
    private var numCards = 0
    var isRunning = false

    var isTimeout = false
    private var pauseTime: Long = System.currentTimeMillis()

    //timers
    private val mainTimer = Timer()
    private lateinit var heatTimer: HeatTimer
    private lateinit var flagTimer: FlagTimer

    //base
    var mainBase = System.currentTimeMillis()
    var timeoutBase = System.currentTimeMillis() + MillisecondsPerMinute

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
        flagTimer = FlagTimer(klaxon, this)

        val darkModeValues: Array<String> = resources.getStringArray(R.array.dark_mode_values)
        when (myPref.getString(getString(R.string.dark_mode_key), darkModeValues[0])) {
            darkModeValues[0] -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            darkModeValues[1] -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            darkModeValues[2] -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            darkModeValues[3] -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)
        }

        val inBundle = ActivityChecker.getBundle()

        if(!inBundle.isEmpty){
            restoreFromBundle(inBundle)
        }else if(savedInstanceState != null){
            restoreFromBundle(savedInstanceState)
        }else{
            val mainChronometer: TextView = findViewById(R.id.chronometer)
            val tempHolder = System.currentTimeMillis()
            mainChronometer.text = timeFormatter(0,true)

            mainBase = tempHolder
            flagTimer.sync(mainBase, tempHolder)
            //timeoutChronometer is hidden, doesn't need to be initialised here
            pauseTime = tempHolder
            heatTimer.restoreValues(this,pauseTime, tempHolder)
        }

        setListeners()
    }

    override fun onStart(){
        super.onStart()
        val mainChronometer: TextView = findViewById(R.id.chronometer)
        val timeoutChronometer: TextView = findViewById(R.id.timeoutCounter)

        mainTimer.schedule(object: TimerTask() {
            override fun run() {
                runOnUiThread { updateTimers() }
            }
            fun updateTimers() {
                if(isRunning) {
                    mainChronometer.text =
                        timeFormatter(System.currentTimeMillis() - mainBase,true)
                    heatTimer.tickListener()
                    flagTimer.tickListener()

                    for(a in foulCards.indices.reversed()){
                        if(foulCards[a].isTrash){
                            foulCards.removeAt(a)
                        }else{
                            foulCards[a].tickListener()
                        }
                    }
                }
                if(isTimeout) {
                    timeoutChronometer.text =
                        timeFormatter(timeoutBase - System.currentTimeMillis(),false)
                    timeoutTickListener()
                }
            }
        },0, MillisecondsPerUpdate)
    }

    override fun onResume() {
        super.onResume()
        ActivityChecker.activityResumed()
        applySettings()
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
        foulCards.clear()
        klaxon.release()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        saveToBundle(outState)
    }

    private fun saveToBundle(outState: Bundle){
        //foul cards
        //clean up cards first
        for(a in foulCards.indices.reversed()){
            if(foulCards[a].isTrash){
                foulCards.removeAt(a)
            }
        }

        outState.putInt("ActiveCards",foulCards.size)
        for(a in 0 until foulCards.size){
            outState.putInt("YC-ID$a",foulCards[a].getID())
            outState.putLong("YC-Pause$a",foulCards[a].getPause())
            outState.putLong("YC-Base$a",foulCards[a].getBase())
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

    private fun restoreFromBundle(savedInstanceState: Bundle){
        val mainChronometer: TextView = findViewById(R.id.chronometer)
        val tempHolder = System.currentTimeMillis()
        //Set main and flag chronometers
        isRunning = savedInstanceState.getBoolean("isRunning")
        mainBase = savedInstanceState.getLong("mainBase")
        if(isRunning){
            val buttonPlayPause: ImageButton = findViewById(R.id.playPauseButton)
            buttonPlayPause.setImageResource(R.drawable.pause)
            buttonPlayPause.contentDescription = getString(R.string.pause_button)
        }else{
            pauseTime = savedInstanceState.getLong("pauseTime")
            mainBase += tempHolder - pauseTime
            pauseTime = tempHolder
            flagTimer.restoreValues(this, mainBase, tempHolder)
        }
        mainChronometer.text = timeFormatter(tempHolder-mainBase,true)

        //Set foul cards
        val activeCards = savedInstanceState.getInt("ActiveCards")
        for(a in 0 until activeCards){
            val inputId = savedInstanceState.getInt("YC-ID$a")
            var inputCardPause = savedInstanceState.getLong("YC-Pause$a")
            var cardBase = savedInstanceState.getLong("YC-Base$a")

            //this is to prevent the foul card timer from displaying a lower value
            //when pausing the timer and restoring the activity
            if(!isRunning){
                //it has to be done here, and not in foulCard
                //because foulCard doesn't know if it's paused or not
                cardBase += tempHolder - inputCardPause
                inputCardPause = tempHolder
            }
            foulCards.add(FoulCard(inputId,klaxon,this,inputCardPause,cardBase))
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
            if(isRunning){
                buttonTimeout.visibility = View.INVISIBLE
            }
        }

        //Set Scores
        val scoreLeftText: TextView = findViewById(R.id.scoreLeft)
        val scoreRightText: TextView = findViewById(R.id.scoreRight)
        scoreLeft = savedInstanceState.getInt("scoreLeft")
        scoreRight = savedInstanceState.getInt("scoreRight")
        scoreLeftText.text = scoreLeft.toString().padStart(3,'0')
        scoreRightText.text = scoreRight.toString().padStart(3,'0')
    }

    private fun applySettings(){
        setListeners()
        val tempHolder = System.currentTimeMillis()
        val myPref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        val audioVol = myPref.getInt(getString(R.string.audio_vol_key), defAudioVol)
        val vibeOn = myPref.getBoolean(getString(R.string.vibe_on_key),defVibeOn)

        if(!isRunning){
            mainBase += tempHolder - pauseTime
            pauseTime = tempHolder
        }
        flagTimer.updateFlagTimer(this, mainBase, tempHolder)

        klaxon.updateSettings(audioVol,vibeOn)

        heatTimer.updateHeatTimer(this)
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
        val buttonBlue: Button = findViewById(R.id.blue)
        val buttonYellow: Button = findViewById(R.id.yellow)
        val buttonRed: Button = findViewById(R.id.red)

        //Settings
        val myPref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
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

        val blueLength =
            try {
                (myPref.getString(getString(R.string.blue_length_key),"$defBlueLength")
                    ?.toInt() ?: defBlueLength) * MillisecondsPerMinute
            }catch(e: NumberFormatException){
                defBlueLength* MillisecondsPerMinute
            }

        val yellowLength =
            try {
                (myPref.getString(getString(R.string.yellow_length_key),"$defYellowLength")
                    ?.toInt() ?: defYellowLength) * MillisecondsPerMinute
            }catch(e: NumberFormatException){
                defYellowLength* MillisecondsPerMinute
            }

        val redLength =
            try {
                (myPref.getString(getString(R.string.red_length_key),"$defRedLength")
                    ?.toInt() ?: defRedLength) * MillisecondsPerMinute
            }catch(e: NumberFormatException){
                defRedLength* MillisecondsPerMinute
            }
        val confirmReset = myPref.getBoolean(getString(R.string.confirm_reset_key),defConfirmReset)

        val blueNum = (blueLength/MillisecondsPerMinute).toInt()
        buttonBlue.text = resources.getQuantityString(R.plurals.minutes, blueNum, blueNum)

        val yellowNum = (yellowLength/MillisecondsPerMinute).toInt()
        buttonYellow.text = resources.getQuantityString(R.plurals.minutes, yellowNum, yellowNum)

        val redNum = (redLength/MillisecondsPerMinute).toInt()
        buttonRed.text = resources.getQuantityString(R.plurals.minutes, redNum, redNum)


        //button listeners
        buttonSettings.setOnClickListener{
            val intent = Intent(this,SettingsActivity::class.java)
            startActivity(intent)
        }

        buttonPlayPause.setOnClickListener {
            if(isRunning) {
                pauseTime = System.currentTimeMillis()
                buttonPlayPause.setImageResource(R.drawable.play)
                buttonPlayPause.contentDescription = getString(R.string.play_button)
                for(a in foulCards.indices.reversed()){
                    if(foulCards[a].isTrash){
                        foulCards.removeAt(a)
                    }else{
                        foulCards[a].pauseTimer()
                    }
                }
                heatTimer.pauseTimer()
                flagTimer.sync(mainBase,System.currentTimeMillis())
                buttonTimeout.visibility = View.VISIBLE
            }else{
                mainBase += System.currentTimeMillis() - pauseTime
                flagTimer.sync(mainBase, System.currentTimeMillis())
                for(a in foulCards.indices.reversed()){
                    if(foulCards[a].isTrash){
                        foulCards.removeAt(a)
                    }else{
                        foulCards[a].resumeTimer()
                    }
                }
                heatTimer.resumeTimer()
                buttonPlayPause.setImageResource(R.drawable.pause)
                buttonPlayPause.contentDescription = getString(R.string.pause_button)
                buttonTimeout.visibility = View.INVISIBLE
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
                timeoutBase = System.currentTimeMillis()
                buttonTimeout.text = getString(R.string.timeout)
                timeoutRow.visibility = View.GONE
            } else {
                timeoutBase = System.currentTimeMillis() + timeoutLength
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

        buttonBlue.setOnClickListener{
            numCards++
            foulCards.add(FoulCard(numCards,klaxon, blueLength,this))
        }

        buttonYellow.setOnClickListener{
            numCards++
            foulCards.add(FoulCard(numCards,klaxon, yellowLength,this))
        }

        buttonRed.setOnClickListener{
            numCards++
            foulCards.add(FoulCard(numCards,klaxon, redLength,this))
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

        mainBase = System.currentTimeMillis()
        pauseTime = mainBase

        if(isRunning){
            buttonPlayPause.setImageResource(R.drawable.play)
            buttonPlayPause.contentDescription = getString(R.string.play_button)
            isRunning = false
        }

        flagTimer.restoreValues(this,mainBase, mainBase)

        heatTimer.restoreValues(this,mainBase,mainBase)

        mainChronometer.text = timeFormatter(0,true)
        flagChronometer.text = timeFormatter(seekerFloor,true)

        scoreLeft = 0
        scoreRight = 0
        scoreLeftText.text = scoreLeft.toString().padStart(3,'0')
        scoreRightText.text = scoreRight.toString().padStart(3,'0')

        buttonTimeout.visibility = View.VISIBLE
        if (isTimeout) {
            timeoutBase = System.currentTimeMillis()
            buttonTimeout.text = getString(R.string.timeout)
            isTimeout = false
            timeoutRow.visibility = View.GONE
        }

        for(a in foulCards){
            a.clearTimer()
        }
        foulCards.clear()
        numCards = 0
    }

    private fun timeoutTickListener() {
        if (timeoutBase < System.currentTimeMillis()) {
            val timeoutRow: TableRow = findViewById(R.id.timeoutRow)
            val buttonTimeout: Button = findViewById(R.id.timeout)
            klaxon.ping(TimeoutNotification,getString(R.string.notification_timeout_desc))
            timeoutBase = System.currentTimeMillis()
            isTimeout = false
            buttonTimeout.text = getString(R.string.timeout)
            timeoutRow.visibility = View.GONE
        }
    }
}

