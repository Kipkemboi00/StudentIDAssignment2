package au.edu.vu.studentidassignment2.ui.dashboard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.edu.vu.studentidassignment2.data.AssignmentRepository
import au.edu.vu.studentidassignment2.domain.AppException
import au.edu.vu.studentidassignment2.domain.Dashboard
import au.edu.vu.studentidassignment2.domain.FailureReason
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface DashboardState {
    data object Loading : DashboardState
    data object SignedOut : DashboardState
    data class Content(val dashboard: Dashboard) : DashboardState
    data class Empty(val entityTotal: Int) : DashboardState
    data class Error(val reason: FailureReason) : DashboardState
}

class DashboardViewModel(
    private val repository: AssignmentRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val mutableState = MutableStateFlow<DashboardState>(DashboardState.SignedOut)
    val state = mutableState.asStateFlow()
    private var loadJob: Job? = null

    init {
        if (savedStateHandle.get<String>(KEYPASS) != null) refresh()
    }

    fun openSession(keypass: String, dashboard: Dashboard) {
        loadJob?.cancel()
        savedStateHandle[KEYPASS] = keypass
        show(dashboard)
    }

    fun refresh() {
        val keypass = savedStateHandle.get<String>(KEYPASS) ?: return
        if (loadJob?.isActive == true) return
        mutableState.value = DashboardState.Loading
        loadJob = viewModelScope.launch {
            try {
                show(repository.loadDashboard(keypass))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.value = DashboardState.Error(
                    (error as? AppException)?.reason ?: FailureReason.UNEXPECTED_RESPONSE
                )
            }
        }
    }

    fun signOut() {
        loadJob?.cancel()
        savedStateHandle.remove<String>(KEYPASS)
        mutableState.value = DashboardState.SignedOut
    }

    private fun show(dashboard: Dashboard) {
        mutableState.value = if (dashboard.entities.isEmpty()) DashboardState.Empty(dashboard.entityTotal)
        else DashboardState.Content(dashboard)
    }

    private companion object { const val KEYPASS = "session_keypass" }
}
