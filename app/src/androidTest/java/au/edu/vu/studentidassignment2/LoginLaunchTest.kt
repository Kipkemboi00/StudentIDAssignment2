package au.edu.vu.studentidassignment2

import android.widget.Spinner
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import au.edu.vu.studentidassignment2.ui.MainActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Device tests exercise real resources and Fragment lifecycles without authenticating. */
@RunWith(AndroidJUnit4::class)
class LoginLaunchTest {
    @Test fun loginLaunchesWithBlankCredentialsAndNoBackIcon() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(Lifecycle.State.RESUMED, activity.lifecycle.currentState)
                assertTrue(activity.findViewById<TextInputEditText>(R.id.username).text.isNullOrEmpty())
                assertTrue(activity.findViewById<TextInputEditText>(R.id.password).text.isNullOrEmpty())
                assertNull(activity.findViewById<MaterialToolbar>(R.id.toolbar).navigationIcon)
                assertEquals("Collections", activity.findViewById<MaterialToolbar>(R.id.toolbar).title.toString())
                assertEquals(activity.getString(R.string.login), activity.findViewById<TextView>(R.id.heading).text.toString())
                assertEquals(0, activity.findViewById<Spinner>(R.id.location).selectedItemPosition)
            }
        }
    }

    @Test fun loginSurvivesActivityRecreation() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.recreate()
            scenario.onActivity { activity ->
                assertEquals(Lifecycle.State.RESUMED, activity.lifecycle.currentState)
                assertTrue(activity.findViewById<TextInputEditText>(R.id.username).text.isNullOrEmpty())
                assertNull(activity.findViewById<MaterialToolbar>(R.id.toolbar).navigationIcon)
            }
        }
    }

    @Test fun submittingWithoutLocationShowsValidationError() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<MaterialButton>(R.id.login).performClick()
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                assertEquals(activity.getString(R.string.location_required), activity.findViewById<TextView>(R.id.error).text.toString())
            }
        }
    }
}
