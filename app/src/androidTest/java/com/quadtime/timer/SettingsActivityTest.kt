package com.quadtime.timer

import android.content.Context
import android.content.Intent
import androidx.appcompat.widget.AppCompatEditText
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.quadtime.timer.constants.*
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
class SettingsActivityTest {
    private val appContext: Context = InstrumentationRegistry.getInstrumentation().targetContext

    companion object {
        @JvmStatic
        @AfterClass
        fun setDefaultSettings() {
            val appContext = InstrumentationRegistry.getInstrumentation().targetContext
            val preferencesEditor = PreferenceManager.getDefaultSharedPreferences(appContext).edit()

            //timer lengths
            preferencesEditor.putString(appContext.getString(R.string.flag_length_key), "$defFlagLength")
            preferencesEditor.putString(appContext.getString(R.string.timeout_length_key), "$defTimeoutLength")
            preferencesEditor.putString(appContext.getString(R.string.yellow_1_length_key), "$defYellow1Length")
            preferencesEditor.putString(appContext.getString(R.string.yellow_2_length_key), "$defYellow2Length")
            preferencesEditor.putString(appContext.getString(R.string.heat_length_key), "$defHeatLength")

            //other settings
            preferencesEditor.putString(appContext.getString(R.string.score_inc_key), "$defScoreInc")
            preferencesEditor.putInt(appContext.getString(R.string.audio_vol_key), defAudioVol)
            preferencesEditor.putBoolean(appContext.getString(R.string.vibe_on_key), defVibeOn)
            preferencesEditor.putString(appContext.getString(R.string.dark_mode_key), appContext.getString(R.string.dark_mode_def_value))
            preferencesEditor.putBoolean(appContext.getString(R.string.confirm_reset_key), defConfirmReset)

            preferencesEditor.commit()
        }
    }

    @Before
    fun init() {
        setDefaultSettings()
        Intents.init()
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(appContext,MainActivity::class.java).apply{
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        appContext.startActivity(intent)
        Thread.sleep(500)
    }

    @After
    fun cleanup(){
        try {
            Espresso.onView(withId(R.id.resetButton))
                .perform(ViewActions.click())
        }catch(e: NoMatchingViewException){
            //if the reset button isn't available, then we might be in the settingsActivity
            //so press back and see if that works.
            Espresso.onView(withContentDescription(androidx.appcompat.R.string.abc_action_bar_up_description))
                .perform(ViewActions.click())
            Espresso.onView(withId(R.id.resetButton))
                .perform(ViewActions.click())
        }
        Espresso.onView(withText(R.string.reset_positive))
            .perform(ViewActions.click())

        Intents.release()
    }

    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.quadtime.timer", appContext.packageName)
    }

    @Test
    fun settingsButton(){
        //check that using the settings button gets us to the settings page
        Espresso.onView(withId(R.id.settingsButton))
            .perform(ViewActions.click())
        intended(hasComponent(SettingsActivity::class.java.name))
        //click the back button in the action bar to return to the main activity
        Espresso.onView(withContentDescription(androidx.appcompat.R.string.abc_action_bar_up_description))
            .perform(ViewActions.click())
        //check that we're on the main activity
        intended(hasComponent(MainActivity::class.java.name))
    }

    @Test
    fun settingsBackPress(){
        //check that using the settings button gets us to the settings page
        Espresso.onView(withId(R.id.settingsButton))
            .perform(ViewActions.click())
        intended(hasComponent(SettingsActivity::class.java.name))
        //click the back button in the action bar to return to the main activity
        Espresso.pressBack()
        //check that we're on the main activity
        intended(hasComponent(MainActivity::class.java.name))
    }

    @Test
    fun settingsApplied(){
        val newYC1 = 11
        val newYC2 = 12
        val newFlagTime = 13
        //check that the yellow cards and the flag timer are showing their default values
        Espresso.onView(withId(R.id.yellow1))
            .check(
                ViewAssertions.matches(withText(
                    appContext.resources.getQuantityString(R.plurals.minutes,
                        defYellow1Length, defYellow1Length
                    )
                )))
        Espresso.onView(withId(R.id.yellow2))
            .check(
                ViewAssertions.matches(withText(
                    appContext.resources.getQuantityString(R.plurals.minutes,
                        defYellow2Length, defYellow2Length
                    )
                )))
        Espresso.onView(withId(R.id.flagCountdown))
            .check(ViewAssertions.matches(withText("$defFlagLength:00.0")))

        //go to settings page, and make sure that we're actually there
        Espresso.onView(withId(R.id.settingsButton))
            .perform(ViewActions.click())
        intended(hasComponent(SettingsActivity::class.java.name))

        //click the yellow card 1 setting
        Espresso.onView(withId(androidx.preference.R.id.recycler_view))
            .perform(RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText(R.string.set_length_yellow_card_1_t)),
                ViewActions.click()
            ))
        //put the new value in the dialog box
        Espresso.onView(allOf(
                        withResourceName("edit"),
                        isAssignableFrom(AppCompatEditText::class.java)
                        ))
            .inRoot(isDialog())
            .check(ViewAssertions.matches(isDisplayed()))
            .perform(ViewActions.clearText())
            .perform(ViewActions.typeText("$newYC1"))
        //then click ok
        Espresso.onView(withText("OK"))
            .inRoot(isDialog())
            .check(ViewAssertions.matches(isDisplayed()))
            .perform(ViewActions.click())

        //click the yellow card 2 setting
        Espresso.onView(withId(androidx.preference.R.id.recycler_view))
            .perform(RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText(R.string.set_length_yellow_card_2_t)),
                ViewActions.click()
            ))
        //put the new value in the dialog box
        Espresso.onView(allOf(
            withResourceName("edit"),
            isAssignableFrom(AppCompatEditText::class.java)
        ))
            .inRoot(isDialog())
            .check(ViewAssertions.matches(isDisplayed()))
            .perform(ViewActions.clearText())
            .perform(ViewActions.typeText("$newYC2"))
        //then click ok
        Espresso.onView(withText("OK"))
            .inRoot(isDialog())
            .check(ViewAssertions.matches(isDisplayed()))
            .perform(ViewActions.click())

        //click the Secondary timer setting
        Espresso.onView(withId(androidx.preference.R.id.recycler_view))
            .perform(RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText(R.string.set_length_flag_t)),
                ViewActions.click()
            ))
        //put the new value in the dialog box
        Espresso.onView(allOf(
            withResourceName("edit"),
            isAssignableFrom(AppCompatEditText::class.java)
        ))
            .inRoot(isDialog())
            .check(ViewAssertions.matches(isDisplayed()))
            .perform(ViewActions.clearText())
            .perform(ViewActions.typeText("$newFlagTime"))
        //then click ok
        Espresso.onView(withText("OK"))
            .inRoot(isDialog())
            .check(ViewAssertions.matches(isDisplayed()))
            .perform(ViewActions.click())

        //click the back button in the action bar to return to the main activity
        Espresso.onView(withContentDescription(androidx.appcompat.R.string.abc_action_bar_up_description))
            .perform(ViewActions.click())

        Espresso.onView(withId(R.id.yellow1))
            .check(
                ViewAssertions.matches(withText(
                    appContext.resources.getQuantityString(R.plurals.minutes,
                        newYC1, newYC1
                    )
                )))

        Espresso.onView(withId(R.id.yellow2))
            .check(
                ViewAssertions.matches(withText(
                    appContext.resources.getQuantityString(R.plurals.minutes,
                        newYC2, newYC2
                    )
                )))

        Espresso.onView(withId(R.id.flagCountdown))
            .check(ViewAssertions.matches(withText("$newFlagTime:00.0")))
        //check that we're on the main activity
        intended(hasComponent(MainActivity::class.java.name))
    }

    @Test
    fun settingsAppliedBackPress(){
        val newYC1 = 11
        val newYC2 = 12
        val newFlagTime = 13
        //check that the yellow cards and the flag timer are showing their default values
        Espresso.onView(withId(R.id.yellow1))
            .check(
                ViewAssertions.matches(withText(
                    appContext.resources.getQuantityString(R.plurals.minutes,
                        defYellow1Length, defYellow1Length
                    )
                )))
        Espresso.onView(withId(R.id.yellow2))
            .check(
                ViewAssertions.matches(withText(
                    appContext.resources.getQuantityString(R.plurals.minutes,
                        defYellow2Length, defYellow2Length
                    )
                )))
        Espresso.onView(withId(R.id.flagCountdown))
            .check(ViewAssertions.matches(withText("$defFlagLength:00.0")))

        //go to settings page, and make sure that we're actually there
        Espresso.onView(withId(R.id.settingsButton))
            .perform(ViewActions.click())
        intended(hasComponent(SettingsActivity::class.java.name))

        //click the yellow card 1 setting
        Espresso.onView(withId(androidx.preference.R.id.recycler_view))
            .perform(RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText(R.string.set_length_yellow_card_1_t)),
                ViewActions.click()
            ))
        //put the new value in the dialog box
        Espresso.onView(allOf(
            withResourceName("edit"),
            isAssignableFrom(AppCompatEditText::class.java)
        ))
            .inRoot(isDialog())
            .check(ViewAssertions.matches(isDisplayed()))
            .perform(ViewActions.clearText())
            .perform(ViewActions.typeText("$newYC1"))
        //then click ok
        Espresso.onView(withText("OK"))
            .inRoot(isDialog())
            .check(ViewAssertions.matches(isDisplayed()))
            .perform(ViewActions.click())

        //click the yellow card 2 setting
        Espresso.onView(withId(androidx.preference.R.id.recycler_view))
            .perform(RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText(R.string.set_length_yellow_card_2_t)),
                ViewActions.click()
            ))
        //put the new value in the dialog box
        Espresso.onView(allOf(
            withResourceName("edit"),
            isAssignableFrom(AppCompatEditText::class.java)
        ))
            .inRoot(isDialog())
            .check(ViewAssertions.matches(isDisplayed()))
            .perform(ViewActions.clearText())
            .perform(ViewActions.typeText("$newYC2"))
        //then click ok
        Espresso.onView(withText("OK"))
            .inRoot(isDialog())
            .check(ViewAssertions.matches(isDisplayed()))
            .perform(ViewActions.click())

        //click the Secondary timer setting
        Espresso.onView(withId(androidx.preference.R.id.recycler_view))
            .perform(RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText(R.string.set_length_flag_t)),
                ViewActions.click()
            ))
        //put the new value in the dialog box
        Espresso.onView(allOf(
            withResourceName("edit"),
            isAssignableFrom(AppCompatEditText::class.java)
        ))
            .inRoot(isDialog())
            .check(ViewAssertions.matches(isDisplayed()))
            .perform(ViewActions.clearText())
            .perform(ViewActions.typeText("$newFlagTime"))
        //then click ok
        Espresso.onView(withText("OK"))
            .inRoot(isDialog())
            .check(ViewAssertions.matches(isDisplayed()))
            .perform(ViewActions.click())

        //backPress to get back to main activity
        Espresso.pressBack()

        Espresso.onView(withId(R.id.yellow1))
            .check(
                ViewAssertions.matches(withText(
                    appContext.resources.getQuantityString(R.plurals.minutes,
                        newYC1, newYC1
                    )
                )))

        Espresso.onView(withId(R.id.yellow2))
            .check(
                ViewAssertions.matches(withText(
                    appContext.resources.getQuantityString(R.plurals.minutes,
                        newYC2, newYC2
                    )
                )))

        Espresso.onView(withId(R.id.flagCountdown))
            .check(ViewAssertions.matches(withText("$newFlagTime:00.0")))
        //check that we're on the main activity
        intended(hasComponent(MainActivity::class.java.name))
    }

}