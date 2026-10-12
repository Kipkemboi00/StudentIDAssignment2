package au.edu.vu.studentidassignment2.ui.details

import android.os.Bundle
import android.view.View
import android.view.Gravity
import android.view.ViewGroup
import android.widget.GridLayout
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import au.edu.vu.studentidassignment2.R
import au.edu.vu.studentidassignment2.databinding.FragmentDetailsBinding
import au.edu.vu.studentidassignment2.databinding.ItemFieldBinding
import au.edu.vu.studentidassignment2.databinding.ItemDescriptionBinding
import au.edu.vu.studentidassignment2.ui.MainActivity
import au.edu.vu.studentidassignment2.ui.fieldLabel
import au.edu.vu.studentidassignment2.ui.fieldValue
import au.edu.vu.studentidassignment2.ui.titleField
import org.koin.androidx.viewmodel.ext.android.viewModel

class DetailsFragment : Fragment(R.layout.fragment_details) {
    private val viewModel: DetailsViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentDetailsBinding.bind(view)
        ViewCompat.setAccessibilityHeading(binding.heading, true)
        val entity = viewModel.entity
        fun addDescription(label: String, value: String) {
            val article = ItemDescriptionBinding.inflate(layoutInflater, binding.descriptions, false)
            article.label.text = label
            ViewCompat.setAccessibilityHeading(article.label, true)
            article.value.text = value
            binding.descriptions.addView(article.root)
        }
        fun addField(label: String, value: String) {
            val field = ItemFieldBinding.inflate(layoutInflater, binding.fields, false)
            field.label.text = label
            ViewCompat.setAccessibilityHeading(field.label, true)
            field.value.text = value
            // Longer values and formatted JSON need the full width; short facts share a row.
            val span = if (value.length > 50 || value.contains('\n')) 2 else 1
            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = ViewGroup.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, span, 1f)
                setGravity(Gravity.FILL_HORIZONTAL)
            }
            binding.fields.addView(field.root, params)
        }
        binding.factsHeading.isVisible = !entity?.summaryFields.isNullOrEmpty()
        binding.fields.isVisible = binding.factsHeading.isVisible
        if (entity == null) {
            addDescription(getString(R.string.details_title), getString(R.string.missing_entity))
            return
        }
        binding.heading.text = entity.titleField()?.value ?: getString(R.string.details_heading)
        ViewCompat.setAccessibilityHeading(binding.factsHeading, true)
        entity.summaryFields.forEach { addField(fieldLabel(it.name), requireContext().fieldValue(it.value)) }
        val descriptions = entity.fields.filter { it.name.equals("description", ignoreCase = true) }
        descriptions.forEach { addDescription(fieldLabel(it.name), requireContext().fieldValue(it.value)) }
        if (descriptions.isEmpty()) {
            addDescription(getString(R.string.description_label), getString(R.string.missing_description))
        }
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as MainActivity).configureToolbar(R.string.details_title, back = true)
    }
}
