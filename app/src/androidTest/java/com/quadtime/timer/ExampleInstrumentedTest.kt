package com.quadtime.timer

import android.content.Intent
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

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

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

}