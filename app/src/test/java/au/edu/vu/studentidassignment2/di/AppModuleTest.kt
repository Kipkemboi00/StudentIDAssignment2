package au.edu.vu.studentidassignment2.di

import androidx.lifecycle.SavedStateHandle
import au.edu.vu.studentidassignment2.MainDispatcherRule
import au.edu.vu.studentidassignment2.data.ApiAssignmentRepository
import au.edu.vu.studentidassignment2.data.AssignmentApi
import au.edu.vu.studentidassignment2.data.AssignmentRepository
import au.edu.vu.studentidassignment2.ui.dashboard.DashboardViewModel
import au.edu.vu.studentidassignment2.ui.details.DetailsViewModel
import au.edu.vu.studentidassignment2.ui.login.LoginViewModel
import okhttp3.OkHttpClient
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class AppModuleTest {
    @get:Rule val main = MainDispatcherRule()

    @Test fun `production DI graph constructs API repository and all viewmodels`() {
        val application = koinApplication {
            modules(appModule, module { factory { SavedStateHandle() } })
        }
        try {
            val koin = application.koin
            assertTrue(koin.get<AssignmentRepository>() is ApiAssignmentRepository)
            assertNotNull(koin.get<AssignmentApi>())
            assertNotNull(koin.get<LoginViewModel>())
            assertNotNull(koin.get<DashboardViewModel>())
            assertNotNull(koin.get<DetailsViewModel>())
            assertSame(koin.get<AssignmentRepository>(), koin.get<AssignmentRepository>())
            assertSame(koin.get<OkHttpClient>(), koin.get<OkHttpClient>())
        } finally {
            application.close()
        }
    }
}
