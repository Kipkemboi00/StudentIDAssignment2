package au.edu.vu.studentidassignment2

import android.view.View
import android.widget.Spinner
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import au.edu.vu.studentidassignment2.config.ClassLocation
import au.edu.vu.studentidassignment2.data.AssignmentRepository
import au.edu.vu.studentidassignment2.data.DashboardParser
import au.edu.vu.studentidassignment2.di.appModule
import au.edu.vu.studentidassignment2.domain.AppException
import au.edu.vu.studentidassignment2.domain.Dashboard
import au.edu.vu.studentidassignment2.domain.FailureReason
import au.edu.vu.studentidassignment2.ui.MainActivity
import au.edu.vu.studentidassignment2.ui.dashboard.DashboardFragment
import au.edu.vu.studentidassignment2.ui.login.LoginFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import org.koin.dsl.module

/** Exercise the actual sign-in button and screen transitions with controlled responses. */
@RunWith(AndroidJUnit4::class)
class LoginFlowTest {
    @Test fun successfulSignInLoadsTheCollectionAndOpensDetails() = withRepository { repository ->
        val entity = DashboardParser().parseEntity(
            """{"title":"Test collection item","year":2025,"description":"Selected item details."}"""
        )
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            submit(scenario)
            scenario.onActivity { activity ->
                assertEquals(activity.getString(R.string.authenticating), activity.findViewById<TextView>(R.id.login).text.toString())
                assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.progress).visibility)
                assertTrue(!activity.findViewById<MaterialButton>(R.id.login).isEnabled)
            }
            repository.authentication.complete("test-topic")
            idle()
            scenario.onActivity { activity ->
                assertEquals(activity.getString(R.string.fetching_dashboard), activity.findViewById<TextView>(R.id.login).text.toString())
            }
            repository.collection.complete(Dashboard(listOf(entity), 1))
            idle()
            scenario.onActivity { activity ->
                assertEquals(Lifecycle.State.RESUMED, activity.lifecycle.currentState)
                assertTrue(activity.supportFragmentManager.findFragmentById(R.id.fragment_container) is DashboardFragment)
                val list = activity.findViewById<RecyclerView>(R.id.entities)
                assertEquals(1, list.adapter!!.itemCount)
                val holder = list.findViewHolderForAdapterPosition(0)
                assertNotNull(holder)
                holder!!.itemView.performClick()
                activity.supportFragmentManager.executePendingTransactions()
            }
            idle()
            scenario.onActivity { activity ->
                assertEquals("Test collection item", activity.findViewById<TextView>(R.id.heading).text.toString())
                assertEquals(Lifecycle.State.RESUMED, activity.lifecycle.currentState)
            }
            assertEquals(1, repository.authenticationCalls)
            assertEquals(1, repository.dashboardCalls)
        }
    }

    @Test fun timeoutKeepsTheAppOpenAndAllowsAnotherSignIn() = withRepository { repository ->
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            submit(scenario)
            repository.authentication.completeExceptionally(AppException(FailureReason.TIMEOUT))
            idle()
            scenario.onActivity { activity ->
                assertEquals(Lifecycle.State.RESUMED, activity.lifecycle.currentState)
                assertTrue(activity.supportFragmentManager.findFragmentById(R.id.fragment_container) is LoginFragment)
                assertEquals(activity.getString(R.string.error_signin_timeout), activity.findViewById<TextView>(R.id.error).text.toString())
                assertEquals(View.GONE, activity.findViewById<View>(R.id.progress).visibility)
                assertTrue(activity.findViewById<MaterialButton>(R.id.login).isEnabled)
            }
            repository.authentication = CompletableDeferred()
            scenario.onActivity { it.findViewById<MaterialButton>(R.id.login).performClick() }
            idle()
            assertEquals(2, repository.authenticationCalls)
            scenario.onActivity { activity ->
                assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.progress).visibility)
                activity.findViewById<MaterialButton>(R.id.cancel_login).performClick()
            }
            idle()
            scenario.onActivity { activity ->
                assertTrue(activity.findViewById<MaterialButton>(R.id.login).isEnabled)
                assertEquals(Lifecycle.State.RESUMED, activity.lifecycle.currentState)
            }
            assertEquals(0, repository.dashboardCalls)
        }
    }

    private fun submit(scenario: ActivityScenario<MainActivity>) {
        scenario.onActivity { activity ->
            activity.findViewById<Spinner>(R.id.location).setSelection(1)
            activity.findViewById<TextInputEditText>(R.id.username).setText("12345678")
            activity.findViewById<TextInputEditText>(R.id.password).setText("TestName")
            activity.findViewById<MaterialButton>(R.id.login).performClick()
        }
        idle()
    }

    private fun idle() = InstrumentationRegistry.getInstrumentation().waitForIdleSync()

    private fun withRepository(test: (ControlledRepository) -> Unit) {
        val repository = ControlledRepository()
        val fakeModule = module { single<AssignmentRepository> { repository } }
        loadKoinModules(fakeModule)
        try {
            test(repository)
        } finally {
            unloadKoinModules(fakeModule)
            loadKoinModules(appModule)
        }
    }

    private class ControlledRepository : AssignmentRepository {
        var authentication = CompletableDeferred<String>()
        val collection = CompletableDeferred<Dashboard>()
        var authenticationCalls = 0
        var dashboardCalls = 0

        override suspend fun authenticate(location: ClassLocation, username: String, password: String): String {
            authenticationCalls++
            assertEquals(ClassLocation.FOOTSCRAY, location)
            return authentication.await()
        }

        override suspend fun loadDashboard(keypass: String): Dashboard {
            dashboardCalls++
            assertEquals("test-topic", keypass)
            return collection.await()
        }
    }
}
