package au.edu.vu.studentidassignment2.ui.details

import android.os.Bundle
import android.view.View
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import au.edu.vu.studentidassignment2.R
import au.edu.vu.studentidassignment2.databinding.FragmentDetailsBinding
import au.edu.vu.studentidassignment2.databinding.ItemFieldBinding
import au.edu.vu.studentidassignment2.ui.MainActivity
import au.edu.vu.studentidassignment2.ui.fieldLabel
import au.edu.vu.studentidassignment2.ui.fieldValue
import org.koin.androidx.viewmodel.ext.android.viewModel

class DetailsFragment : Fragment(R.layout.fragment_details) {
    private val viewModel: DetailsViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentDetailsBinding.bind(view)
        ViewCompat.setAccessibilityHeading(binding.heading, true)
        val entity = viewModel.entity
        fun addField(label: String, value: String) {
            val field = ItemFieldBinding.inflate(layoutInflater, binding.fields, false)
            field.label.text = label
            ViewCompat.setAccessibilityHeading(field.label, true)
            field.value.text = value
            binding.fields.addView(field.root)
        }
        if (entity == null) {
            addField(getString(R.string.details_title), getString(R.string.missing_entity))
            return
        }
        entity.fields.forEach { addField(fieldLabel(it.name), requireContext().fieldValue(it.value)) }
        if (entity.fields.none { it.name.equals("description", ignoreCase = true) }) {
            addField(getString(R.string.description_label), getString(R.string.missing_description))
        }
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as MainActivity).configureToolbar(R.string.details_title, back = true)
    }
}
