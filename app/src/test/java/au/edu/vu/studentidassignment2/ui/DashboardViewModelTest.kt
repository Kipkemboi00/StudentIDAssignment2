package au.edu.vu.studentidassignment2.ui

import androidx.lifecycle.SavedStateHandle
import au.edu.vu.studentidassignment2.FakeAssignmentRepository
import au.edu.vu.studentidassignment2.MainDispatcherRule
import au.edu.vu.studentidassignment2.domain.AppException
import au.edu.vu.studentidassignment2.domain.Dashboard
import au.edu.vu.studentidassignment2.domain.FailureReason
import au.edu.vu.studentidassignment2.ui.dashboard.DashboardState
import au.edu.vu.studentidassignment2.ui.dashboard.DashboardViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val repository = FakeAssignmentRepository()
    private val savedState = SavedStateHandle()
    private val viewModel = DashboardViewModel(repository, savedState)

    @Test fun `no session means signed out without request`() = runTest {
        viewModel.refresh()
        advanceUntilIdle()
        assertEquals(DashboardState.SignedOut, viewModel.state.value)
        assertEquals(0, repository.dashboardCalls)
    }

    @Test fun `login data is reused without a duplicate dashboard call`() {
        viewModel.openSession("topic", repository.dashboard)
        assertEquals(DashboardState.Content(repository.dashboard), viewModel.state.value)
        assertEquals(0, repository.dashboardCalls)
        assertEquals(setOf("session_keypass"), savedState.keys())
    }

    @Test fun `refresh emits loading then content`() = runTest {
        viewModel.openSession("topic", repository.dashboard)
        repository.dashboardGate = CompletableDeferred()
        viewModel.refresh()
        assertEquals(DashboardState.Loading, viewModel.state.value)
        runCurrent()
        assertEquals("topic", repository.requestedKeypass)
        repository.dashboardGate!!.complete(repository.dashboard)
        advanceUntilIdle()
        assertEquals(DashboardState.Content(repository.dashboard), viewModel.state.value)
    }

    @Test fun `empty result uses empty state and preserves server total`() {
        viewModel.openSession("topic", Dashboard(emptyList(), 5))
        assertEquals(DashboardState.Empty(5), viewModel.state.value)
    }

    @Test fun `network failure can be retried`() = runTest {
        viewModel.openSession("topic", repository.dashboard)
        repository.dashboardError = AppException(FailureReason.NETWORK)
        viewModel.refresh()
        advanceUntilIdle()
        assertEquals(DashboardState.Error(FailureReason.NETWORK), viewModel.state.value)
        repository.dashboardError = null
        viewModel.refresh()
        advanceUntilIdle()
        assertEquals(DashboardState.Content(repository.dashboard), viewModel.state.value)
    }

    @Test fun `expired session is exposed distinctly`() = runTest {
        viewModel.openSession("topic", repository.dashboard)
        repository.dashboardError = AppException(FailureReason.SESSION_EXPIRED)
        viewModel.refresh()
        advanceUntilIdle()
        assertEquals(DashboardState.Error(FailureReason.SESSION_EXPIRED), viewModel.state.value)
    }

    @Test fun `restored keypass reloads collection after process recreation`() = runTest {
        val restored = DashboardViewModel(repository, SavedStateHandle(mapOf("session_keypass" to "restored-topic")))
        assertEquals(DashboardState.Loading, restored.state.value)
        advanceUntilIdle()
        assertEquals("restored-topic", repository.requestedKeypass)
        assertEquals(DashboardState.Content(repository.dashboard), restored.state.value)
    }

    @Test fun `repeated refresh requests are coalesced`() = runTest {
        viewModel.openSession("topic", repository.dashboard)
        repository.dashboardGate = CompletableDeferred()
        repeat(3) { viewModel.refresh() }
        runCurrent()
        assertEquals(1, repository.dashboardCalls)
        repository.dashboardGate!!.complete(repository.dashboard)
        advanceUntilIdle()
    }

    @Test fun `sign out clears saved session and cancels pending refresh`() = runTest {
        viewModel.openSession("topic", repository.dashboard)
        repository.dashboardGate = CompletableDeferred()
        viewModel.refresh()
        runCurrent()
        viewModel.signOut()
        repository.dashboardGate!!.complete(repository.dashboard)
        advanceUntilIdle()
        assertEquals(DashboardState.SignedOut, viewModel.state.value)
        assertTrue(savedState.keys().isEmpty())
    }

    @Test fun `unexpected exception becomes error state`() = runTest {
        viewModel.openSession("topic", repository.dashboard)
        repository.dashboardError = IllegalArgumentException("bad data")
        viewModel.refresh()
        advanceUntilIdle()
        assertEquals(DashboardState.Error(FailureReason.UNEXPECTED_RESPONSE), viewModel.state.value)
    }
}
