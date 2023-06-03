package com.quadtime.timer

import android.content.Intent
import androidx.preference.PreferenceManager
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.CoreMatchers.not

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Before
import org.junit.BeforeClass

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    companion object {
        @JvmStatic
        @BeforeClass
        fun setDefaultSettings() {
            val appContext = InstrumentationRegistry.getInstrumentation().targetContext
            val preferencesEditor = PreferenceManager.getDefaultSharedPreferences(appContext).edit()

            //timer lengths
            preferencesEditor.putString(appContext.getString(R.string.flag_length_key), "20")
            preferencesEditor.putString(appContext.getString(R.string.timeout_length_key), "1")
            preferencesEditor.putString(appContext.getString(R.string.yellow_1_length_key), "1")
            preferencesEditor.putString(appContext.getString(R.string.yellow_2_length_key), "2")
            preferencesEditor.putString(appContext.getString(R.string.heat_length_key), "0")

            //other settings
            preferencesEditor.putString(appContext.getString(R.string.score_inc_key), "10")
            preferencesEditor.putInt(appContext.getString(R.string.audio_vol_key), 100)
            preferencesEditor.putBoolean(appContext.getString(R.string.vibe_on_key), true)
            preferencesEditor.putString(appContext.getString(R.string.dark_mode_key), appContext.getString(R.string.dark_mode_def_value))
            preferencesEditor.putBoolean(appContext.getString(R.string.confirm_reset_key), true)

            preferencesEditor.commit()

        }
    }

    @Before
    fun init() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(appContext,MainActivity::class.java).apply{
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        appContext.startActivity(intent)
    }
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.quadtime.timer", appContext.packageName)
    }

    @Test
    fun playPause(){
        Espresso.onView(withId(R.id.playPauseButton))
            .check(ViewAssertions.matches(ViewMatchers.withContentDescription(R.string.play_button)))
        Espresso.onView(withId(R.id.playPauseButton))
            .perform(ViewActions.click())
        Espresso.onView(withId(R.id.playPauseButton))
            .check(ViewAssertions.matches(ViewMatchers.withContentDescription(R.string.pause_button)))
        Espresso.onView(withId(R.id.playPauseButton))
            .perform(ViewActions.click())
        Espresso.onView(withId(R.id.playPauseButton))
            .check(ViewAssertions.matches(ViewMatchers.withContentDescription(R.string.play_button)))
    }

    @Test
    fun timeout(){

        Espresso.onView(withId(R.id.timeout))
            .check(ViewAssertions.matches(ViewMatchers.withText(R.string.timeout)))

        //check that the timeoutCounter and associated buttons and views are not visible
        Espresso.onView(withId(R.id.timeoutRow))
            .check(ViewAssertions.matches(not(isDisplayed())))
        Espresso.onView(withId(R.id.timeoutCounter))
            .check(ViewAssertions.matches(not(isDisplayed())))
        Espresso.onView(withId(R.id.minus1))
            .check(ViewAssertions.matches(not(isDisplayed())))
        Espresso.onView(withId(R.id.plus1))
            .check(ViewAssertions.matches(not(isDisplayed())))

        //click Timeout button
        Espresso.onView(withId(R.id.timeout))
            .perform(ViewActions.click())


        Espresso.onView(withId(R.id.timeout))
            .check(ViewAssertions.matches(ViewMatchers.withText(R.string.clear_timeout)))

        //check that the timeoutCounter and associated buttons are visible
        Espresso.onView(withId(R.id.timeoutRow))
            .check(ViewAssertions.matches(isDisplayed()))
        Espresso.onView(withId(R.id.timeoutCounter))
            .check(ViewAssertions.matches(isDisplayed()))
        Espresso.onView(withId(R.id.minus1))
            .check(ViewAssertions.matches(isDisplayed()))
        Espresso.onView(withId(R.id.plus1))
            .check(ViewAssertions.matches(isDisplayed()))

    }

    @Test
    fun scores(){
        Espresso.onView(withId(R.id.scoreLeft))
            .check(ViewAssertions.matches(ViewMatchers.withText(R.string.initial_score)))
        Espresso.onView(withId(R.id.scoreRight))
            .check(ViewAssertions.matches(ViewMatchers.withText(R.string.initial_score)))

        Espresso.onView(withId(R.id.scoreUpLeft))
            .perform(ViewActions.click())

        Espresso.onView(withId(R.id.scoreLeft))
            .check(ViewAssertions.matches(ViewMatchers.withText("010")))
        Espresso.onView(withId(R.id.scoreRight))
            .check(ViewAssertions.matches(ViewMatchers.withText(R.string.initial_score)))

        Espresso.onView(withId(R.id.scoreUpRight))
            .perform(ViewActions.click())

        Espresso.onView(withId(R.id.scoreLeft))
            .check(ViewAssertions.matches(ViewMatchers.withText("010")))
        Espresso.onView(withId(R.id.scoreRight))
            .check(ViewAssertions.matches(ViewMatchers.withText("010")))

        Espresso.onView(withId(R.id.scoreDownLeft))
            .perform(ViewActions.click())

        Espresso.onView(withId(R.id.scoreLeft))
            .check(ViewAssertions.matches(ViewMatchers.withText(R.string.initial_score)))
        Espresso.onView(withId(R.id.scoreRight))
            .check(ViewAssertions.matches(ViewMatchers.withText("010")))

        Espresso.onView(withId(R.id.scoreDownRight))
            .perform(ViewActions.click())

        Espresso.onView(withId(R.id.scoreLeft))
            .check(ViewAssertions.matches(ViewMatchers.withText(R.string.initial_score)))
        Espresso.onView(withId(R.id.scoreRight))
            .check(ViewAssertions.matches(ViewMatchers.withText(R.string.initial_score)))
    }

}