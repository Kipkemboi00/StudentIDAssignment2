package au.edu.vu.studentidassignment2

import android.view.View
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import au.edu.vu.studentidassignment2.config.ClassLocation
import au.edu.vu.studentidassignment2.data.AssignmentRepository
import au.edu.vu.studentidassignment2.di.appModule
import au.edu.vu.studentidassignment2.domain.Dashboard
import au.edu.vu.studentidassignment2.ui.MainActivity
import au.edu.vu.studentidassignment2.ui.login.LoginViewModel
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.ext.android.getViewModel
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import org.koin.dsl.module

@RunWith(AndroidJUnit4::class)
class LoginCancellationTest {
    @Test fun cancelRestoresTheFormAndPreventsLateNavigation() {
        val response = CompletableDeferred<String>()
        val repository = object : AssignmentRepository {
            override suspend fun authenticate(location: ClassLocation, username: String, password: String) = response.await()
            override suspend fun loadDashboard(keypass: String) = Dashboard(emptyList(), 0)
        }
        val fakeModule = module { viewModel { LoginViewModel(repository) } }
        loadKoinModules(fakeModule)
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    activity.supportFragmentManager.findFragmentById(R.id.fragment_container)!!
                        .getViewModel<LoginViewModel>().login(ClassLocation.FOOTSCRAY, "12345678", "TestName")
                }
                InstrumentationRegistry.getInstrumentation().waitForIdleSync()
                scenario.onActivity { activity ->
                    val cancel = activity.findViewById<MaterialButton>(R.id.cancel_login)
                    assertEquals(View.VISIBLE, cancel.visibility)
                    assertTrue(!activity.findViewById<MaterialButton>(R.id.login).isEnabled)
                    cancel.performClick()
                }
                response.complete("cancelled-topic")
                InstrumentationRegistry.getInstrumentation().waitForIdleSync()
                scenario.onActivity { activity ->
                    assertTrue(activity.findViewById<MaterialButton>(R.id.login).isEnabled)
                    assertEquals(View.GONE, activity.findViewById<MaterialButton>(R.id.cancel_login).visibility)
                    assertEquals(View.GONE, activity.findViewById<View>(R.id.progress).visibility)
                    assertEquals(activity.getString(R.string.login_title), activity.findViewById<TextView>(R.id.heading).text.toString())
                }
            }
        } finally {
            unloadKoinModules(fakeModule)
            loadKoinModules(appModule)
        }
    }
}
