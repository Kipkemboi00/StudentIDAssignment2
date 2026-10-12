package au.edu.vu.studentidassignment2.ui

import au.edu.vu.studentidassignment2.domain.Entity
import au.edu.vu.studentidassignment2.domain.EntityField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EntityPresentationTest {
    @Test fun humanReadableNameWinsOverAnEarlierIdentifier() {
        val entity = Entity(listOf(EntityField("id", "42"), EntityField("bookTitle", "The garden")), "{}")
        assertEquals("The garden", entity.titleField()?.value)
    }

    @Test fun descriptionIsNeverUsedAsACardHeading() {
        val entity = Entity(listOf(EntityField("Description", "A long article")), "{}")
        assertNull(entity.titleField())
    }

    @Test fun otherSchemasFallBackToTheirFirstAvailableValue() {
        val entity = Entity(listOf(EntityField("name", null), EntityField("category", "Botany")), "{}")
        assertEquals("Botany", entity.titleField()?.value)
    }
}
