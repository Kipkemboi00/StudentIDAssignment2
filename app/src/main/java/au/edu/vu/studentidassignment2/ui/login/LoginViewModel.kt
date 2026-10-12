package au.edu.vu.studentidassignment2.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.edu.vu.studentidassignment2.config.ClassLocation
import au.edu.vu.studentidassignment2.data.AssignmentRepository
import au.edu.vu.studentidassignment2.domain.AppException
import au.edu.vu.studentidassignment2.domain.Dashboard
import au.edu.vu.studentidassignment2.domain.FailureReason
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class InputError { LOCATION_REQUIRED, STUDENT_ID_REQUIRED, STUDENT_ID_FORMAT, PASSWORD_REQUIRED }

sealed interface LoginState {
    data object Idle : LoginState
    data class Loading(val fetchingDashboard: Boolean = false) : LoginState
    data class InvalidInput(val error: InputError) : LoginState
    data class Error(val reason: FailureReason, val dashboardRequest: Boolean = false) : LoginState
    data class Authenticated(val keypass: String, val dashboard: Dashboard) : LoginState
}

class LoginViewModel(private val repository: AssignmentRepository) : ViewModel() {
    private val mutableState = MutableStateFlow<LoginState>(LoginState.Idle)
    val state = mutableState.asStateFlow()

    fun login(location: ClassLocation?, username: String, password: String) {
        if (state.value is LoginState.Loading || state.value is LoginState.Authenticated) return
        val studentId = username.trim()
        val invalidInput = when {
            location == null -> InputError.LOCATION_REQUIRED
            studentId.isBlank() -> InputError.STUDENT_ID_REQUIRED
            !studentId.matches(Regex("[0-9]+")) -> InputError.STUDENT_ID_FORMAT
            password.isBlank() -> InputError.PASSWORD_REQUIRED
            else -> null
        }
        if (invalidInput != null) {
            mutableState.value = LoginState.InvalidInput(invalidInput)
            return
        }
        mutableState.value = LoginState.Loading()
        viewModelScope.launch {
            var fetchingDashboard = false
            try {
                // Preserve password case and characters: the API is case sensitive.
                val keypass = repository.authenticate(requireNotNull(location), studentId, password)
                fetchingDashboard = true
                mutableState.value = LoginState.Loading(fetchingDashboard = true)
                val dashboard = repository.loadDashboard(keypass)
                mutableState.value = LoginState.Authenticated(keypass, dashboard)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.value = LoginState.Error(
                    (error as? AppException)?.reason ?: FailureReason.UNEXPECTED_RESPONSE,
                    dashboardRequest = fetchingDashboard
                )
            }
        }
    }

    fun navigationHandled() {
        if (state.value is LoginState.Authenticated) mutableState.value = LoginState.Idle
    }
}
