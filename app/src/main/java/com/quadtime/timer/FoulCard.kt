package com.quadtime.timer

import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import com.quadtime.timer.constants.YCNotification
import com.quadtime.timer.constants.timeFormatter

class FoulCard(inputId: Int, inputAlert: Alert, inputDur: Long, inputContext: MainActivity){
    private val idNum: Int = inputId
    private val cardRow: TableRow = TableRow(inputContext)
    private val id: TextView = TextView(inputContext)
    private val timerChronometer: TextView = TextView(inputContext)
    private var timerBase: Long = System.currentTimeMillis()
    private var timerPause: Long = System.currentTimeMillis()
    private val timerClear: Button = Button(inputContext)
    private val siren: Alert = inputAlert
    private val notificationText: String = inputContext.getString(R.string.notification_fc_desc,inputId)
    var isTrash: Boolean = false

    init {
        val cardTable: TableLayout = inputContext.findViewById(R.id.cardTable)
        cardTable.addView(cardRow)
        //put elements into the row
        cardRow.addView(id)
        cardRow.addView(timerChronometer)
        cardRow.addView(timerClear)
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
        timerChronometer.layoutParams = TableRow.LayoutParams(
            TableRow.LayoutParams.MATCH_PARENT,
            TableRow.LayoutParams.WRAP_CONTENT,
            1.0f
        )
        timerChronometer.textSize = 30.toFloat()
        timerChronometer.text = timeFormatter(inputDur,false)
        timerBase = System.currentTimeMillis() + inputDur


        //card clear button content
        timerClear.layoutParams = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT,
            1.0f
        )
        timerClear.setPadding(50,50,50,50)
        timerClear.text = inputContext.getString(R.string.clear_card_timer,idNum)
        timerClear.setOnClickListener {
            clearOut()
        }
    }

    constructor(
        inputId: Int,
        inputAlert: Alert,
        inputContext: MainActivity,
        inputCardPause: Long,
        inputCardBase: Long):this(inputId,inputAlert,inputCardBase- System.currentTimeMillis(),inputContext){
        val tempHolder = System.currentTimeMillis()
        timerBase = inputCardBase
        timerPause = inputCardPause
        timerChronometer.text = timeFormatter(timerBase-tempHolder,false)
    }

    private fun clearOut() {
        if(!isTrash) {
            (cardRow.parent as ViewGroup).removeView(cardRow)
            isTrash = true
        }
    }

    fun tickListener() {
        timerChronometer.text = timeFormatter(timerBase - System.currentTimeMillis(),false)
        if (timerBase < System.currentTimeMillis()){
            siren.ping(YCNotification + idNum, notificationText)
            clearOut()
        }
    }

    fun pauseTimer(){
        if(!isTrash) {
            timerPause = System.currentTimeMillis()
        }
    }
    fun resumeTimer(){
        if(!isTrash) {
            timerBase += System.currentTimeMillis() - timerPause
        }
    }
    fun clearTimer(){
        clearOut()
    }

    fun getID(): Int{
        return idNum
    }

    fun getPause(): Long{
        return timerPause
    }

    fun getBase(): Long{
        return timerBase
    }
}