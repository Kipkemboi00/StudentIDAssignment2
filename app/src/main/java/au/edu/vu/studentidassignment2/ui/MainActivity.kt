package au.edu.vu.studentidassignment2.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.os.bundleOf
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import au.edu.vu.studentidassignment2.R
import au.edu.vu.studentidassignment2.databinding.ActivityMainBinding
import au.edu.vu.studentidassignment2.domain.Entity
import au.edu.vu.studentidassignment2.ui.dashboard.DashboardFragment
import au.edu.vu.studentidassignment2.ui.dashboard.DashboardViewModel
import au.edu.vu.studentidassignment2.ui.details.DetailsFragment
import au.edu.vu.studentidassignment2.ui.details.DetailsViewModel
import au.edu.vu.studentidassignment2.ui.login.LoginFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val dashboardViewModel: DashboardViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.refresh -> { dashboardViewModel.refresh(); true }
                R.id.sign_out -> { dashboardViewModel.signOut(); showLogin(); true }
                else -> false
            }
        }
        if (savedInstanceState == null) showLogin()
    }

    fun configureToolbar(title: Int, back: Boolean = false, dashboard: Boolean = false, visible: Boolean = true) {
        binding.toolbar.isVisible = visible
        window.statusBarColor = ContextCompat.getColor(this, if (visible) R.color.background else R.color.primary)
        WindowInsetsControllerCompat(window, binding.root).isAppearanceLightStatusBars = visible
        binding.toolbar.setTitle(title)
        binding.toolbar.navigationIcon = if (back) AppCompatResources.getDrawable(this, R.drawable.ic_back) else null
        binding.toolbar.setNavigationContentDescription(R.string.back)
        binding.toolbar.menu.clear()
        if (dashboard) binding.toolbar.inflateMenu(R.menu.dashboard)
    }

    fun showDashboard() {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.fragment_container, DashboardFragment())
        }
    }

    fun showDetails(entity: Entity) {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.fragment_container, DetailsFragment().apply {
                arguments = bundleOf(DetailsViewModel.ENTITY_JSON to entity.rawJson)
            })
            addToBackStack("details")
        }
    }

    fun showLogin() {
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.fragment_container, LoginFragment())
        }
    }
}
