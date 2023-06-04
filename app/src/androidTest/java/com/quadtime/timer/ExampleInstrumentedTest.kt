package com.quadtime.timer

import android.content.Intent
import androidx.preference.PreferenceManager
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.quadtime.timer.constants.MillisecondsPerSecond
import org.hamcrest.CoreMatchers.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

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
        @AfterClass
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
        Thread.sleep(500)
    }

    @After
    fun cleanup(){
        Espresso.onView(withId(R.id.resetButton))
            .perform(ViewActions.click())
        Espresso.onView(withText(R.string.reset_positive))
            .perform(ViewActions.click())
    }

    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.quadtime.timer", appContext.packageName)
    }

    @Test
    fun playPause(){
        //check that play-pause button starts off saying play, and timer is 00:00.0
        Espresso.onView(withId(R.id.playPauseButton))
            .check(ViewAssertions.matches(withContentDescription(R.string.play_button)))
        Espresso.onView(withId(R.id.chronometer))
            .check(ViewAssertions.matches(withText("00:00.0")))

        //click the play button
        Espresso.onView(withId(R.id.playPauseButton))
            .perform(ViewActions.click())
        Thread.sleep(500)
        //should be 0.5 seconds
        //check that the button says pause, and the timer has changed to a value less than 1 second
        Espresso.onView(withId(R.id.chronometer))
            .check(ViewAssertions.matches(not(withText("00:00.0"))))
            .check(ViewAssertions.matches(withText(containsString("00:00"))))
        Espresso.onView(withId(R.id.playPauseButton))
            .check(ViewAssertions.matches(withContentDescription(R.string.pause_button)))

        //click the pause button
        Espresso.onView(withId(R.id.playPauseButton))
            .perform(ViewActions.click())
        //check that timer is still less than 1 second
        Espresso.onView(withId(R.id.chronometer))
            .check(ViewAssertions.matches(withText(containsString("00:00"))))
        //check that the button says play
        Espresso.onView(withId(R.id.playPauseButton))
            .check(ViewAssertions.matches(withContentDescription(R.string.play_button)))
        //wait for 1 second, and then check that the timer is still less than 1 second
        Thread.sleep(1000)
        Espresso.onView(withId(R.id.chronometer))
            .check(ViewAssertions.matches(withText(containsString("00:00"))))

        //click the play button
        Espresso.onView(withId(R.id.playPauseButton))
            .perform(ViewActions.click())
        Thread.sleep(600)
        //should be 1.1 seconds
        //check that the timer now displays a value for 1 second
        Espresso.onView(withId(R.id.chronometer))
            .check(ViewAssertions.matches(withText(containsString("00:01"))))
    }

    @Test
    fun timeout(){
        Espresso.onView(withId(R.id.timeout))
            .check(ViewAssertions.matches(withText(R.string.timeout)))
        timeoutHidden()

        //click Timeout button
        Espresso.onView(withId(R.id.timeout))
            .perform(ViewActions.click())
            .check(ViewAssertions.matches(withText(R.string.clear_timeout)))
        timeoutVisible()

        // press the -1 button to make the timer expire
        Espresso.onView(withId(R.id.minus1))
            .perform(ViewActions.click())
        timeoutHidden()

        //check that the plus 1 and minus 1 button change the timeout
        Espresso.onView(withId(R.id.timeout))
            .perform(ViewActions.click())
        Espresso.onView(withId(R.id.plus1))
            .perform(ViewActions.click())
        Espresso.onView(withId(R.id.timeoutCounter))
            .check(ViewAssertions.matches(isDisplayed()))
            .check(ViewAssertions.matches(withText(containsString("01:"))))
        Espresso.onView(withId(R.id.minus1))
            .perform(ViewActions.click())
        Espresso.onView(withId(R.id.timeoutCounter))
            .check(ViewAssertions.matches(withText(containsString("00:"))))
        Espresso.onView(withId(R.id.timeout))
            .perform(ViewActions.click())
        timeoutHidden()
    }

    //check that the timeoutCounter and associated buttons and views are not visible
    private fun timeoutHidden(){
        Espresso.onView(withId(R.id.timeoutRow))
            .check(ViewAssertions.matches(not(isDisplayed())))
        Espresso.onView(withId(R.id.timeoutCounter))
            .check(ViewAssertions.matches(not(isDisplayed())))
        Espresso.onView(withId(R.id.minus1))
            .check(ViewAssertions.matches(not(isDisplayed())))
        Espresso.onView(withId(R.id.plus1))
            .check(ViewAssertions.matches(not(isDisplayed())))
    }

    //check that the timeoutCounter and associated buttons are visible
    private fun timeoutVisible(){
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
    fun yellowCards(){
        //Check that the yellow card buttons are there and that there aren't any yellow cards
        Espresso.onView(withId(R.id.yellow1))
            .check(ViewAssertions.matches(withText("1 MINUTE")))
        Espresso.onView(withId(R.id.yellow2))
            .check(ViewAssertions.matches(withText("2 MINUTES")))
        Espresso.onView(withText(containsString("CLEAR CARD")))
            .check(ViewAssertions.doesNotExist())

        //start a 1 minute yellow card timer
        Espresso.onView(withId(R.id.yellow1))
            .perform(ViewActions.click())

        //check that the yellow card ID, timer, and clear card button is created
        //click the clear card button
        //check that the yellow card does not exist
        Espresso.onView(
            allOf(
                hasSibling(withText("CLEAR CARD 1")),
                withText("1")
            )
        )
            .check(ViewAssertions.matches(isDisplayed()))
        Espresso.onView(
            allOf(
                hasSibling(withText("CLEAR CARD 1")),
                withText("01:00")
            )
        )
            .check(ViewAssertions.matches(isDisplayed()))
        Espresso.onView(withText("CLEAR CARD 1"))
            .check(ViewAssertions.matches(isDisplayed()))
            .perform(ViewActions.click())
            .check(ViewAssertions.doesNotExist())


        //start a 1 minute yellow card timer
        Espresso.onView(withId(R.id.yellow1))
            .perform(ViewActions.click())

        //check that the yellow card is created with an id of 2
        //click the clear card button
        //check that the yellow card does not exist
        Espresso.onView(
            allOf(
                hasSibling(withText("CLEAR CARD 2")),
                withText("2")
            )
        )
            .check(ViewAssertions.matches(isDisplayed()))
        Espresso.onView(
            allOf(
                hasSibling(withText("CLEAR CARD 2")),
                withText("01:00")
            )
        )
            .check(ViewAssertions.matches(isDisplayed()))
        Espresso.onView(withText("CLEAR CARD 2"))
            .check(ViewAssertions.matches(isDisplayed()))
            .perform(ViewActions.click())
            .check(ViewAssertions.doesNotExist())

        //click reset button
        Espresso.onView(withId(R.id.resetButton))
            .perform(ViewActions.click())
        Espresso.onView(withText(R.string.reset_positive))
            .perform(ViewActions.click())

        //click the play button
        Espresso.onView(withId(R.id.playPauseButton))
            .perform(ViewActions.click())

        //start a 1 minute yellow card timer
        Espresso.onView(withId(R.id.yellow1))
            .perform(ViewActions.click())

        //wait 5 milliseconds so it's definitely 59 seconds
        Thread.sleep(5)
        //check that the yellow card is created
        Espresso.onView(
            allOf(
                hasSibling(withText("CLEAR CARD 1")),
                hasSibling(withText("1")),
                withText(containsString(":"))
            )
        )
            .check(ViewAssertions.matches(withText("00:59")))
        Espresso.onView(
            allOf(
                hasSibling(withText("CLEAR CARD 1")),
                withText("1")
            )
        )
            .check(ViewAssertions.matches(isDisplayed()))
        //wait a second, and check that the timer has gone down
        Thread.sleep(MillisecondsPerSecond)
        Espresso.onView(
            allOf(
                hasSibling(withText("CLEAR CARD 1")),
                hasSibling(withText("1")),
                withText(containsString(":"))
            )
        )
            .check(ViewAssertions.matches(withText("00:58")))
        //pause for two seconds, check that the timer doesn't go down, and resume the timer
        Espresso.onView(withId(R.id.playPauseButton))
            .perform(ViewActions.click())
        Thread.sleep(2*MillisecondsPerSecond)
        Espresso.onView(
            allOf(
                hasSibling(withText("CLEAR CARD 1")),
                hasSibling(withText("1")),
                withText(containsString(":"))
            )
        )
            .check(ViewAssertions.matches(withText("00:58")))
        Espresso.onView(withId(R.id.playPauseButton))
            .perform(ViewActions.click())
        //wait until there's 1 second left on the yellow card, check that the timer is still there
        Thread.sleep(58*MillisecondsPerSecond - 5)
        Espresso.onView(withText("CLEAR CARD 1"))
            .check(ViewAssertions.matches(isDisplayed()))
        //wait until there's no time left on the timer, and check that it is not present
        Thread.sleep(1* MillisecondsPerSecond)
        Espresso.onView(withText("CLEAR CARD 1"))
            .check(ViewAssertions.doesNotExist())
    }

    @Test
    fun scores(){
        Espresso.onView(withId(R.id.scoreLeft))
            .check(ViewAssertions.matches(withText(R.string.initial_score)))
        Espresso.onView(withId(R.id.scoreRight))
            .check(ViewAssertions.matches(withText(R.string.initial_score)))

        Espresso.onView(withId(R.id.scoreUpLeft))
            .perform(ViewActions.click())

        Espresso.onView(withId(R.id.scoreLeft))
            .check(ViewAssertions.matches(withText("010")))
        Espresso.onView(withId(R.id.scoreRight))
            .check(ViewAssertions.matches(withText(R.string.initial_score)))

        Espresso.onView(withId(R.id.scoreUpRight))
            .perform(ViewActions.click())

        Espresso.onView(withId(R.id.scoreLeft))
            .check(ViewAssertions.matches(withText("010")))
        Espresso.onView(withId(R.id.scoreRight))
            .check(ViewAssertions.matches(withText("010")))

        Espresso.onView(withId(R.id.scoreDownLeft))
            .perform(ViewActions.click())

        Espresso.onView(withId(R.id.scoreLeft))
            .check(ViewAssertions.matches(withText(R.string.initial_score)))
        Espresso.onView(withId(R.id.scoreRight))
            .check(ViewAssertions.matches(withText("010")))

        Espresso.onView(withId(R.id.scoreDownRight))
            .perform(ViewActions.click())

        Espresso.onView(withId(R.id.scoreLeft))
            .check(ViewAssertions.matches(withText(R.string.initial_score)))
        Espresso.onView(withId(R.id.scoreRight))
            .check(ViewAssertions.matches(withText(R.string.initial_score)))
    }

}