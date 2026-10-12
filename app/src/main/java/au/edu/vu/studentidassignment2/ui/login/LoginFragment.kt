package au.edu.vu.studentidassignment2.ui.login

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import au.edu.vu.studentidassignment2.R
import au.edu.vu.studentidassignment2.config.AppConfig
import au.edu.vu.studentidassignment2.config.ClassLocation
import au.edu.vu.studentidassignment2.databinding.FragmentLoginBinding
import au.edu.vu.studentidassignment2.ui.MainActivity
import au.edu.vu.studentidassignment2.ui.dashboard.DashboardViewModel
import au.edu.vu.studentidassignment2.ui.failureMessage
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class LoginFragment : Fragment(R.layout.fragment_login) {
    private var viewBinding: FragmentLoginBinding? = null
    private val binding get() = requireNotNull(viewBinding)
    private val viewModel: LoginViewModel by viewModel()
    private val dashboardViewModel: DashboardViewModel by activityViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewBinding = FragmentLoginBinding.bind(view)
        ViewCompat.setAccessibilityHeading(binding.heading, true)
        binding.location.adapter = ArrayAdapter.createFromResource(
            requireContext(), R.array.class_locations, android.R.layout.simple_spinner_item
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        if (savedInstanceState == null) {
            binding.location.setSelection(AppConfig.defaultLocation?.let { it.ordinal + 1 } ?: 0)
        }
        binding.login.setOnClickListener { submit() }
        binding.password.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) { submit(); true } else false
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { render(it) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as MainActivity).configureToolbar(R.string.app_name)
    }

    private fun submit() {
        val selectedLocation = ClassLocation.entries.getOrNull(binding.location.selectedItemPosition - 1)
        WindowInsetsControllerCompat(requireActivity().window, binding.root)
            .hide(WindowInsetsCompat.Type.ime())
        viewModel.login(selectedLocation, binding.username.text.toString(), binding.password.text.toString())
    }

    private fun render(state: LoginState) {
        val loading = state is LoginState.Loading
        binding.progress.isVisible = loading
        binding.login.isEnabled = !loading
        binding.username.isEnabled = !loading
        binding.password.isEnabled = !loading
        binding.location.isEnabled = !loading
        binding.login.setText(when {
            state is LoginState.Loading && state.fetchingDashboard -> R.string.fetching_dashboard
            loading -> R.string.authenticating
            else -> R.string.login
        })
        binding.usernameLayout.error = null
        binding.passwordLayout.error = null
        val message = when (state) {
            is LoginState.InvalidInput -> getString(when (state.error) {
                InputError.LOCATION_REQUIRED -> R.string.location_required
                InputError.STUDENT_ID_REQUIRED -> R.string.id_required
                InputError.STUDENT_ID_FORMAT -> R.string.id_format
                InputError.PASSWORD_REQUIRED -> R.string.password_required
            }).also {
                when (state.error) {
                    InputError.STUDENT_ID_REQUIRED, InputError.STUDENT_ID_FORMAT -> binding.usernameLayout.error = it
                    InputError.PASSWORD_REQUIRED -> binding.passwordLayout.error = it
                    InputError.LOCATION_REQUIRED -> Unit
                }
            }
            is LoginState.Error -> requireContext().failureMessage(state.reason).let {
                if (state.dashboardRequest) getString(R.string.error_dashboard_login, it) else it
            }
            else -> null
        }
        binding.error.text = message
        binding.error.isVisible = message != null
        if (state is LoginState.Authenticated) {
            dashboardViewModel.openSession(state.keypass, state.dashboard)
            binding.password.text?.clear()
            viewModel.navigationHandled()
            (requireActivity() as MainActivity).showDashboard()
        }
    }

    override fun onDestroyView() {
        viewBinding = null
        super.onDestroyView()
    }
}
