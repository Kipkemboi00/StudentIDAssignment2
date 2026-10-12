package au.edu.vu.studentidassignment2.ui

import au.edu.vu.studentidassignment2.FakeAssignmentRepository
import au.edu.vu.studentidassignment2.MainDispatcherRule
import au.edu.vu.studentidassignment2.config.ClassLocation
import au.edu.vu.studentidassignment2.domain.AppException
import au.edu.vu.studentidassignment2.domain.Dashboard
import au.edu.vu.studentidassignment2.domain.FailureReason
import au.edu.vu.studentidassignment2.ui.login.InputError
import au.edu.vu.studentidassignment2.ui.login.LoginState
import au.edu.vu.studentidassignment2.ui.login.LoginViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val repository = FakeAssignmentRepository()
    private val viewModel = LoginViewModel(repository)

    @Test fun `initial state is idle`() { assertEquals(LoginState.Idle, viewModel.state.value) }

    @Test fun `location must be explicitly selected`() {
        viewModel.login(null, "12345678", "TestName")
        assertEquals(LoginState.InvalidInput(InputError.LOCATION_REQUIRED), viewModel.state.value)
        assertEquals(0, repository.authCalls)
    }

    @Test fun `blank student id is rejected without network call`() {
        viewModel.login(ClassLocation.BR, "  ", "TestName")
        assertEquals(LoginState.InvalidInput(InputError.STUDENT_ID_REQUIRED), viewModel.state.value)
        assertEquals(0, repository.authCalls)
    }

    @Test fun `s prefix and non numeric student ids are rejected`() {
        listOf("s12345678", "12a34", "-123", "１２３").forEach {
            viewModel.login(ClassLocation.BR, it, "TestName")
            assertEquals(LoginState.InvalidInput(InputError.STUDENT_ID_FORMAT), viewModel.state.value)
        }
        assertEquals(0, repository.authCalls)
    }

    @Test fun `blank password is rejected`() {
        viewModel.login(ClassLocation.SYDNEY, "12345678", " ")
        assertEquals(LoginState.InvalidInput(InputError.PASSWORD_REQUIRED), viewModel.state.value)
        assertEquals(0, repository.authCalls)
    }

    @Test fun `authentication and dashboard have distinct loading states`() = runTest {
        repository.authenticationGate = CompletableDeferred()
        repository.dashboardGate = CompletableDeferred()
        viewModel.login(ClassLocation.FOOTSCRAY, "12345678", "TestName")
        assertEquals(LoginState.Loading(), viewModel.state.value)
        runCurrent()
        assertEquals(0, repository.dashboardCalls)
        repository.authenticationGate!!.complete("assigned-topic")
        runCurrent()
        assertEquals(LoginState.Loading(fetchingDashboard = true), viewModel.state.value)
        assertEquals("assigned-topic", repository.requestedKeypass)
        repository.dashboardGate!!.complete(repository.dashboard)
        advanceUntilIdle()
        assertEquals(LoginState.Authenticated("assigned-topic", repository.dashboard), viewModel.state.value)
    }

    @Test fun `success trims id but preserves password capitalization and characters`() = runTest {
        viewModel.login(ClassLocation.SYDNEY, " 12345678 ", "TestName ")
        advanceUntilIdle()
        assertEquals(ClassLocation.SYDNEY, repository.submittedLocation)
        assertEquals("12345678", repository.submittedUsername)
        assertEquals("TestName ", repository.submittedPassword)
        assertTrue(viewModel.state.value is LoginState.Authenticated)
        assertEquals("test-topic", repository.requestedKeypass)
    }

    @Test fun `authentication rejection never loads dashboard`() = runTest {
        repository.authError = AppException(FailureReason.INVALID_CREDENTIALS)
        viewModel.login(ClassLocation.BR, "12345678", "TestName")
        advanceUntilIdle()
        assertEquals(LoginState.Error(FailureReason.INVALID_CREDENTIALS), viewModel.state.value)
        assertEquals(0, repository.dashboardCalls)
    }

    @Test fun `network failure during authentication is visible`() = runTest {
        repository.authError = AppException(FailureReason.NETWORK)
        viewModel.login(ClassLocation.BR, "12345678", "TestName")
        advanceUntilIdle()
        assertEquals(LoginState.Error(FailureReason.NETWORK), viewModel.state.value)
    }

    @Test fun `dashboard failure after auth keeps user on login with retryable error`() = runTest {
        repository.dashboardError = AppException(FailureReason.SERVER)
        viewModel.login(ClassLocation.BR, "12345678", "TestName")
        advanceUntilIdle()
        assertEquals(LoginState.Error(FailureReason.SERVER, dashboardRequest = true), viewModel.state.value)
    }

    @Test fun `unexpected failure becomes useful error state`() = runTest {
        repository.authError = IllegalStateException("private internal detail")
        viewModel.login(ClassLocation.BR, "12345678", "TestName")
        advanceUntilIdle()
        assertEquals(LoginState.Error(FailureReason.UNEXPECTED_RESPONSE), viewModel.state.value)
    }

    @Test fun `repeated taps do not start duplicate authentication requests`() = runTest {
        repository.authenticationGate = CompletableDeferred()
        repeat(3) { viewModel.login(ClassLocation.BR, "12345678", "TestName") }
        runCurrent()
        assertEquals(1, repository.authCalls)
        repository.authenticationGate!!.complete("test-topic")
        advanceUntilIdle()
    }

    @Test fun `retry recovers from failure and success is consumed once`() = runTest {
        repository.authError = AppException(FailureReason.NETWORK)
        viewModel.login(ClassLocation.BR, "12345678", "TestName")
        advanceUntilIdle()
        repository.authError = null
        viewModel.login(ClassLocation.BR, "12345678", "TestName")
        advanceUntilIdle()
        assertTrue(viewModel.state.value is LoginState.Authenticated)
        viewModel.navigationHandled()
        assertEquals(LoginState.Idle, viewModel.state.value)
        assertEquals(2, repository.authCalls)
    }

    @Test fun `empty dashboard is still a successful login`() = runTest {
        repository.dashboard = Dashboard(emptyList(), 0)
        viewModel.login(ClassLocation.BR, "12345678", "TestName")
        advanceUntilIdle()
        assertEquals(LoginState.Authenticated("test-topic", repository.dashboard), viewModel.state.value)
    }
}
