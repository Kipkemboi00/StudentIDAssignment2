package au.edu.vu.studentidassignment2.ui.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import au.edu.vu.studentidassignment2.data.DashboardParser
import au.edu.vu.studentidassignment2.domain.Entity

class DetailsViewModel(parser: DashboardParser, savedStateHandle: SavedStateHandle) : ViewModel() {
    // Fragment arguments survive rotation and process recreation without a second API request.
    val entity: Entity? = savedStateHandle.get<String>(ENTITY_JSON)?.let {
        runCatching { parser.parseEntity(it) }.getOrNull()
    }

    companion object { const val ENTITY_JSON = "entity_json" }
}
