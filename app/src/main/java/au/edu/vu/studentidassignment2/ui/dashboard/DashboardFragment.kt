package au.edu.vu.studentidassignment2.ui.dashboard

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import au.edu.vu.studentidassignment2.R
import au.edu.vu.studentidassignment2.databinding.FragmentDashboardBinding
import au.edu.vu.studentidassignment2.ui.MainActivity
import au.edu.vu.studentidassignment2.ui.failureMessage
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class DashboardFragment : Fragment(R.layout.fragment_dashboard) {
    private var viewBinding: FragmentDashboardBinding? = null
    private val binding get() = requireNotNull(viewBinding)
    private val viewModel: DashboardViewModel by activityViewModel()
    private var entityAdapter: EntityAdapter? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewBinding = FragmentDashboardBinding.bind(view)
        ViewCompat.setAccessibilityHeading(binding.heading, true)
        ViewCompat.setAccessibilityHeading(binding.messageTitle, true)
        entityAdapter = EntityAdapter { (requireActivity() as MainActivity).showDetails(it) }
        binding.entities.layoutManager = LinearLayoutManager(requireContext())
        binding.entities.adapter = entityAdapter
        binding.retry.setOnClickListener { viewModel.refresh() }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { render(it) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as MainActivity).configureToolbar(R.string.dashboard_title, dashboard = true)
    }

    private fun render(state: DashboardState) {
        binding.progress.isVisible = state is DashboardState.Loading
        binding.entities.isVisible = state is DashboardState.Content
        binding.messagePanel.isVisible = state is DashboardState.Empty || state is DashboardState.Error
        binding.count.isVisible = state is DashboardState.Content || state is DashboardState.Empty
        when (state) {
            is DashboardState.Content -> {
                entityAdapter?.submitList(state.dashboard.entities)
                binding.count.text = getString(R.string.entity_count, state.dashboard.entities.size, state.dashboard.entityTotal)
            }
            is DashboardState.Empty -> {
                entityAdapter?.submitList(emptyList())
                binding.count.text = getString(R.string.entity_count, 0, state.entityTotal)
                binding.messageTitle.setText(R.string.empty_title)
                binding.messageBody.setText(R.string.empty_message)
                binding.retry.setText(R.string.refresh)
            }
            is DashboardState.Error -> {
                binding.messageTitle.setText(R.string.request_failed)
                binding.messageBody.text = requireContext().failureMessage(state.reason)
                binding.retry.setText(R.string.retry)
            }
            DashboardState.SignedOut -> (requireActivity() as MainActivity).showLogin()
            DashboardState.Loading -> Unit
        }
    }

    override fun onDestroyView() {
        binding.entities.adapter = null
        entityAdapter = null
        viewBinding = null
        super.onDestroyView()
    }
}
