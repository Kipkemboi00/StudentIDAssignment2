package au.edu.vu.studentidassignment2.ui

import androidx.lifecycle.SavedStateHandle
import au.edu.vu.studentidassignment2.data.DashboardParser
import au.edu.vu.studentidassignment2.ui.details.DetailsViewModel
import org.junit.Assert.*
import org.junit.Test

class DetailsViewModelTest {
    @Test fun `selected entity including description is restored from arguments`() {
        val json = """{"title":"Chosen item","description":"Full description","optional":null}"""
        val viewModel = DetailsViewModel(DashboardParser(), SavedStateHandle(mapOf(DetailsViewModel.ENTITY_JSON to json)))
        val entity = requireNotNull(viewModel.entity)
        assertEquals(listOf("title", "description", "optional"), entity.fields.map { it.name })
        assertEquals("Full description", entity.fields[1].value)
    }

    @Test fun `missing or invalid saved item produces recoverable missing state`() {
        assertNull(DetailsViewModel(DashboardParser(), SavedStateHandle()).entity)
        assertNull(DetailsViewModel(DashboardParser(), SavedStateHandle(mapOf(DetailsViewModel.ENTITY_JSON to "[]"))).entity)
    }
}
