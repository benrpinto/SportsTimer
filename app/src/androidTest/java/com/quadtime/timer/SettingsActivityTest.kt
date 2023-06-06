package com.quadtime.timer

import android.content.Intent
import androidx.preference.PreferenceManager
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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

    companion object {
        @JvmStatic
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
        Espresso.onView(withId(R.id.resetButton))
            .perform(ViewActions.click())
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

}