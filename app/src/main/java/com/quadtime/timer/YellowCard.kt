package com.quadtime.timer

import android.os.*
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView

class YellowCard(inputId:Int, inputAlert: Alert, inputDur:Long, inputContext: MainActivity){
    private val idNum :Int = inputId
    private val cardRow : TableRow = TableRow(inputContext)
    private val id : TextView = TextView(inputContext)
    private val cardChronometer : TextView = TextView(inputContext)
    private var cardBase:Long = SystemClock.elapsedRealtime()
    private var cardPause : Long = SystemClock.elapsedRealtime()
    private val cardClear : Button = Button(inputContext)
    private val siren : Alert = inputAlert
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
        cardClear.setPadding(50,50,50,50)
        cardClear.text = inputContext.getString(R.string.clear_card_timer,idNum)
        cardClear.setOnClickListener {
            clearOut()
        }
    }

    constructor(
        inputId:Int,
        inputAlert: Alert,
        inputContext: MainActivity,
        inputCardPause:Long,
        inputCardBase:Long):this(inputId,inputAlert,inputCardBase- SystemClock.elapsedRealtime(),inputContext){
        val tempHolder = SystemClock.elapsedRealtime()
        cardBase = inputCardBase
        cardPause = inputCardPause
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
            siren.ping()
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